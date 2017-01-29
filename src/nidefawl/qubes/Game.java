package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.*;

import java.io.File;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import nidefawl.qubes.assets.RenderAssets;
import nidefawl.qubes.async.AsyncTask;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.IDMappingBlocks;
import nidefawl.qubes.chat.channel.GlobalChannel;
import nidefawl.qubes.chat.client.ChatManager;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.config.ClientSettings;
import nidefawl.qubes.config.InvalidConfigException;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.entity.PlayerSelfBenchmark;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.*;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.*;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.models.BlockModelManager;
import nidefawl.qubes.models.EntityModelManager;
import nidefawl.qubes.models.ItemModelManager;
import nidefawl.qubes.network.client.ClientHandler;
import nidefawl.qubes.network.client.NetworkClient;
import nidefawl.qubes.network.client.ThreadConnect;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketChatMessage;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.gui.SingleBlockRenderAtlas;
import nidefawl.qubes.render.gui.VRGuiRenderer;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.server.LocalGameServer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.texture.array.*;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vr.VR;
import nidefawl.qubes.vr.VREvents;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;
import nidefawl.qubes.world.WorldClientBenchmark;

public class Game extends GameBase {

    static public Game instance;
    static final String buildName = "Indev alpha ";
    static final String buildVersion = "0.1";
    static final String BUILD_CODE = "::BUILD_CODE::";
    static final String buildIdentifier = String.format("%s %s.%s", buildName, buildVersion, BUILD_CODE);
    
    PlayerProfile         profile = new PlayerProfile();
    public final ClientSettings          settings  = new ClientSettings();

    public GuiOverlayStats statsOverlay;
    public GuiCached       statsCached;
    public GuiOverlayChat  chatOverlay;

    private ThreadConnect      connect;
    private NetworkClient      client;
    WorldClient                world     = null;
    PlayerSelf                 player;
    
    final CameraController cameraController = new CameraController();
    WorldPlayerController worldPlayerController = new WorldPlayerController();
    public final DigController         dig                = new DigController();
    public final Selection     leftSelection = new Selection();
    public final Selection     rightSelection = new Selection();
    public final PositionMouseOver rightMouseOver = new PositionMouseOver();
    public final PositionMouseOver leftMouseOver = new PositionMouseOver();
    public boolean             follow    = true;

    public BlockStack selBlock           = new BlockStack(Block.stones.getFirst());
    public long       lastShaderLoadTime = System.currentTimeMillis();

    public final Vector3f vCam               = new Vector3f();
    public final Vector3f vPlayer            = new Vector3f();
    public final Vector3f vLastCam           = new Vector3f();
    public final Vector3f vLastPlayer        = new Vector3f();

    /**
     * third person distance
     */
    private float                  lastTpDistance;

    public boolean        updateRenderers = true;
    public static boolean showGrid        = false;
    public boolean        thirdPerson     = false;
    boolean               testMode        = false;
    public String         serverAddr;
    PlayerSelf            remotePlayer;
    PlayerSelf            testPlayer;
    WorldClient           remoteWorld;
    WorldClient           testWorld;

    int throttleClick=0;
    public boolean showControllers;
    public final LocalGameServer server = new LocalGameServer();
    VRGuiRenderer vrGui = new VRGuiRenderer();
    final static Vector3f tmp = new Vector3f();
    RayTrace rayTrace = new RayTrace() {
        public boolean rayTraceBlock(Block block) {
            if (block.isFullBB())
                return true;
            return !block.isTransparent();
        };
    };
    
    private GameMode mode = GameMode.BUILD;
    
    public GameMode getMode() {
        return this.mode;
    }

    public void connectTo(String host) {
        String[] split = host.split(":");
        int port = 21087;
        if (split.length > 1) {
            port = StringUtil.parseInt(split[1], port);
        }
        connectTo(split[0], port, true);
    }
    public void connectTo(String host, int port, boolean isLocalAttempt) {
        try {
            connect = new ThreadConnect(host, port, isLocalAttempt);
            connect.startThread();
            showGUI(new GuiConnecting(connect));
        } catch (Exception e) {
            showGUI(new GuiDisconnected("Failed connecting to "+host+"\n"+e.getMessage()));
            e.printStackTrace();
        }
    }
    
    public Game() {
        super();
        appName = "Not Minecraft";
        useWindowSizeAsRenderResolution = false;
        instance = this;
    }

    @Override
    public void initGame() {
        GameBase.loadingScreen = new LoadingScreen();
        loadProfile();
        loadSettings();
        if (this.serverAddr == null) {
            this.serverAddr = this.settings.lastserver;
        }
        if (this.serverAddr == null) {
            this.serverAddr = "nide.ddns.net:21087";
        }
        KeybindManager.initKeybinds();
        leftSelection.init();
        rightSelection.init();
        dig.init();
        FontRenderer.init();
        Engine.init(EngineInitSettings.INIT_ALL);
        loadingScreen.setProgress(0, 0, "Initializing");
        TextureManager.getInstance().init();
        BlockModelManager.getInstance().init();
        ItemModelManager.getInstance().init();
        ItemTextureArray.getInstance().init();
        SingleBlockRenderAtlas.getInstance().init();

        
        
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("initGame 1");
        this.statsOverlay = new GuiOverlayStats();
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("initGame 2");
        this.statsCached = new GuiCached(statsOverlay); 
        this.statsCached.setPos(0, 0);
        this.statsCached.setSize(displayWidth, displayHeight);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("initGame 3");
        Engine.checkGLError("Post startup");
        loadingScreen.setProgress(0, 0.5f, "Initializing");
        this.vrGui.init();
        
        
    }

    public String getAppTitle() {
        return String.format("%s - %s", appName, buildIdentifier);
    }


    public void lateInitGame() {
        dig.reloadTextures();
        RenderAssets.load(this.settings.renderSettings, loadingScreen);
        ChatManager.getInstance().loadInputHistory();
        KeybindManager.load();
        isStarting = true;
        updateGui3dMode();
        
    }

    public void toggleGameMode() {
        if (this.mode == GameMode.PLAY) {
            this.mode = GameMode.BUILD;
        } else if (this.mode == GameMode.BUILD) {
            this.mode = GameMode.EDIT;
        } else if (this.mode == GameMode.EDIT) {
            this.mode = GameMode.SELECT;
        } else {
            this.mode = GameMode.PLAY;
        }
        leftSelection.reset();
        rightSelection.reset();
    }

    protected void toggleTestMode() {
        if (testMode) {
            testMode = false;
            setWorld(remoteWorld);
            setPlayer(remotePlayer);
        } else {
            remoteWorld = world;
            remotePlayer = player;
            testMode = true;
            testWorld = new WorldClientBenchmark();
            testPlayer = new PlayerSelfBenchmark();
            testWorld.addEntity(testPlayer);
            setWorld(testWorld);
            setPlayer(testPlayer);
        }
    }

    public void setWorld(WorldClient world) {
        leftSelection.reset();
        rightSelection.reset();
        if (this.world != null) {
            this.world.onLeave();
            Engine.flushRenderTasks();
            Engine.regionRenderer.resetAll();
        }
        this.world = world;
        if (this.world != null) {
            this.world.onLoad();
        }
        updateGui3dMode();
    }
    public void updateGui3dMode() {
        this.vrGui.reset();
        this.renderGui3d = VR_SUPPORT || this.settings.gui3d;
        if (this.renderGui3d) {
            guiWidth = 1920;
            guiHeight = 1080;
            int x = guiWidth/2-guiWidth/6;
            this.chatOverlay.setPos(x, guiHeight/2-guiHeight/6);
            this.chatOverlay.setSize(guiWidth/3, guiHeight/3);
        } else {
            guiWidth = windowWidth;
            guiHeight = windowHeight;
            this.chatOverlay.setPos(8, guiHeight-guiHeight/3-8);
            this.chatOverlay.setSize(guiWidth/2, guiHeight/3);
        }
        GuiContext.canDragWindows=!this.renderGui3d;
        GuiContext.canWindowsFocusChange=!this.renderGui3d;
        GuiContext.hasOverride=false;
        GuiContext.mouseOverOverride=null;
        showGUI(null);
    }
    
    @Override
    public void shutdown() {
        this.server.stop();
        super.shutdown();
        setWorld(null);
        ChatManager.getInstance().saveInputHistory();
    }
    
    protected void onTextInput(long window, int codepoint) {
//        Keybinding k = Keyboard.getKeyBinding(codepoint);
//        if (k != null && k.isEnabled() && k.isPressed()) {
//            return;
//        }
        if (GuiContext.input != null) {
            GuiContext.input.onTextInput(codepoint);
        }
//        if (this.gui != null) {
//            if (this.gui.onTextInput(codepoint)) {
//                return;
//            }
//        }
//        if (GuiWindowManager.requiresTextInput()) {
//            
//            if (GuiWindowManager.onTextInput(codepoint)) {
//                return;
//            }
//        } 
    }
    
    @Override
    protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
        if (window == windowId) {
            if (key == -1) { // ALT + Print Screen (maybe more)
                return;
            }
            if (GuiContext.input != null) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    GuiContext.input.focused = false;
                    GuiContext.input = null;
                }
//                return;
            }
            Keybinding k = KeybindManager.getKeyBinding(key);
            if (k != null && k.isEnabled() && k.isPressed()) {
                k.update(action);
                return;
            }
            if (GuiWindowManager.onKeyPress(key, scancode, action, mods)) {
                return;
            }
            if (this.gui != null) {
                if (this.gui.onKeyPress(key, scancode, action, mods)) {
                    return;
                }
            }
            if (k != null && k.isEnabled() && !k.isPressed()) {
                k.update(action);
                return;
            }
            boolean isDown = Keyboard.getState(action);
            if (!isDown) {
                return;
            }
            if (key == GLFW.GLFW_KEY_0) {
                this.selBlock.id = 0;
                this.selBlock.data = 0;
            } else if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
                this.selBlock.id = key - GLFW.GLFW_KEY_1;
                this.selBlock.data = 0;
                if (!Block.isValid(this.selBlock.id))
                    this.selBlock.id = 0;
            }
        }
    }
    
    @Override
    protected void onWheelScroll(long window, double xoffset, double yoffset) {
        if (this.gui != null) {
            if (this.world == null) {
                if (GuiWindowManager.onWheelScroll(xoffset, yoffset)) {
                    return;
                }
            }
            this.gui.onWheelScroll(xoffset, yoffset);
        } else {
            if (GuiWindowManager.onWheelScroll(xoffset, yoffset)) {
                return;
            }
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
                this.settings.thirdpersonDistance += yoffset*-0.2f;
                if (this.settings.thirdpersonDistance < 1) {
                    this.settings.thirdpersonDistance = 1;
                }
                if (this.settings.thirdpersonDistance > 30) {
                    this.settings.thirdpersonDistance = 30;
                }
                this.settings.save();
                return;
            }
            this.selBlock.id += yoffset > 0 ? -1 : 1;
            if (!Block.isValid(this.selBlock.id)) {
                int maxBlock = 0;
                for (int b = 0; this.selBlock.id< 0&&b < IDMappingBlocks.HIGHEST_BLOCK_ID+1; b++) {
                    if (Block.get(b) != null) {
                        maxBlock = b;
                    }
                }
                this.selBlock.id = this.selBlock.id< 0?maxBlock:0;
            }
            if (statsCached != null) {

                this.statsCached.refresh();
            }
            
        }
        
    }
    
    public void onMouseClick(long window, int button, int action, int mods) {
        if (this.gui != null) {
            if (this.world == null) {
            }
            if (GuiWindowManager.onMouseClick(button, action)) {
                return;
            }
            if (!this.gui.onMouseClick(button, action)) {
            }
        } else {
            if (GuiWindowManager.onMouseClick(button, action)) {
                return;
            }
            if (!canRenderGui3d() && GuiWindowManager.anyWindowVisible()) {
                return;
            }


            boolean isDown = Mouse.getState(action);
            if (throttleClick > 0) {
                return;
            }
//            if (b)
//                dig.onMouseClick(button, isDown);
            getSelection(0).clicked(button, isDown);
//            rightSelection.clicked(button, isDown);
            switch (button) {
                case 0:
                    if (this.player != null) {
                        this.player.clicked(button, isDown);
                    }
                    break;
                case 2:
//                    selection.clicked(button, isDown);
                    break;
            }
        }
    }

    public void setGrabbed(boolean b) {
        if (b != this.movement.grabbed()) {
            this.movement.setGrabbed(b);
            leftSelection.resetSelection();
            rightSelection.resetSelection();
            Mouse.setCursorPosition(displayWidth / 2, displayHeight / 2);
            Mouse.setGrabbed(b);
            this.dig.onGrabChange(this.movement.grabbed());
        }
    }

    public boolean needsGrab() {
        if (canRenderGui3d()) {
            return true;
        }
        if (!hasWindowFocus) 
            return false;
        if (this.world == null) {
            return false;
        }
        return this.gui==null&&!GuiWindowManager.anyWindowVisible();
    }


    public void render(float fTime) {
        if (canRenderGui3d()) {
            setGUIViewport();
            Engine.checkGLError("setGUIProjection");
            vrGui.renderGUIs(fTime);
        }
        glEnable(GL_DEPTH_TEST);
        Engine.setBlend(false);
        glClearColor(0f, 0f, 0f, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        if (this.world != null) {
            Engine.skyRenderer.renderSky(this.world, fTime);
            Engine.shadowRenderer.renderShadowPass(this.world, fTime);
        } 
        setSceneViewport();

        for (int eye = 0; eye < (VR_SUPPORT ? 2 : 1); eye++) {
            if (VR_SUPPORT) {
                Engine.getMatSceneP().load(eye == 0 ? VR.cam.projLeft : VR.cam.projRight);
                Engine.getMatSceneP().update();
                Engine.setViewMatrix(VR.getViewMat(eye));
                VR.setViewPort(eye);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("setCameraAndViewport");
            }
            FrameBuffer finalTarget = VR_SUPPORT ? VR.getFB(eye) : null;
            if (this.world != null) {
                renderWorld(fTime, eye, finalTarget);    
            } else if (canRenderGui3d()) {
                Engine.getSceneFB().bind();
                Engine.getSceneFB().clearColor();
                Engine.setBlend(true);
                glDisable(GL_DEPTH_TEST);
//                Shaders.textured.enable();
//                GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, TMgr.getEmptyWhite());
//                Engine.drawFullscreenQuad();
                
                render3dGUI(fTime);
                
                glEnable(GL_DEPTH_TEST);
                Engine.setBlend(false);
                //TODO: add material info for predicated thresholidng 
                Engine.outRenderer.renderAA(Engine.getSceneFB().getTexture(0), 0, finalTarget);
            }
            
//            if (VR_SUPPORT && (showControllers||gui!=null||true)) {
//                FrameBuffer finalTarget = VR.getFB(eye);
//                finalTarget.bind();
//                finalTarget.clearDepth();
//                
//                Engine.updateCamera(VR.getPoseMat(eye), Vector3f.ZERO);
//                UniformBuffer.updateUBO(/*null*/world, fTime);
//                VR.renderControllers();
//                Engine.updateCamera(VR.getViewMat(eye), Vector3f.ZERO);
//                UniformBuffer.updateUBO(/*null*/world, fTime);
//                vrGui.render(fTime, fbGUIFixed.getTexture(0));
//                FrameBuffer.unbindFramebuffer();
//                
//            }
        }
            
        if (VR_SUPPORT) {

            if (GLDebugTextures.isShow()) {
                GLDebugTextures.readTexture(true, "VR", "left", VR.getFB(0).getTexture(0));
                GLDebugTextures.readTexture(true, "VR", "right", VR.getFB(1).getTexture(0));
            }

            
            FrameBuffer.unbindFramebuffer();
            if (this.world != null||canRenderGui3d()) {
                VR.Submit();
            }
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("VR.Submit");

            Game.displayWidth=windowWidth;
            Game.displayHeight=windowHeight;
            updateProjection();
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("updateProjection");
            VR.drawFullscreenCompanion(windowWidth, windowHeight);
            {
//                Engine.setBlend(true);
//                GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, this.fbGUIFixed.getTexture(0));
//
//                Shaders.textured.enable();
//                Tess.instance.setColorF(-1, 1f);
////                Tess.instance.setOffset(displayWidth / 2, displayHeight / 2, 0);
//                float x = 0;
//                float y = 0;
//                float w = windowWidth;
//                float h = windowHeight;
//                Tess.instance.add(x, y, 0, 0, 1);
//                Tess.instance.add(x, y+h, 0, 0, 0);
//                Tess.instance.add(x+w, y+h, 0, 1, 0);
//                Tess.instance.add(x+w, y, 0, 1, 1);
//                Tess.instance.drawQuads();
////                GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//                GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
//                Engine.setBlend(false);
            }
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("drawFullscreenCompanion");
        }
        
        Engine.setBlend(true);

        glClear(GL_DEPTH_BUFFER_BIT);
        glDisable(GL_DEPTH_TEST);

        

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("gui");

        if (this.world != null && this.movement.grabbed()) {
            boolean gui3d = canRenderGui3d();
            if ((gui3d && !has3dGUIMouseFocus()) || (!gui3d && this.gui == null)) {
                renderCrossHair(fTime);
            }
        }

        double mx = Mouse.getX();
        double my = Mouse.getY();
        if (this.statsCached != null) {
            this.statsCached.setSize(displayWidth, displayHeight);
            this.statsCached.render(fTime, 0, 0);
        }
        if (!canRenderGui3d()) {
            renderGui(fTime, mx, my);
        }

        if (this.gui == null && this.world != null && !canRenderGui3d()) {

            if (showGrid) {
                renderChunkGrid(fTime);
            }
            
            if (!VR_SUPPORT) {
                glEnable(GL_DEPTH_TEST);
                GuiWindowManager.getInstance().render(fTime, mx, my);
                glDisable(GL_DEPTH_TEST);
            }
            Shaders.textured.enable();
            if (this.chatOverlay != null) {
                this.chatOverlay.render(fTime, 0, 0);
            }
        }

        if (GLDebugTextures.show) {
            GLDebugTextures.drawAll(displayWidth, displayHeight);
        }
        

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
    }

    private void renderCrossHair(float fTime) {
        Shaders.colored.enable();
        Tess.instance.setColor(-1, 100);
        Tess.instance.setOffset(displayWidth / 2, displayHeight / 2, 0);
        float height = 1;
        float w = 8;
        Tess.instance.add(-w, height, 0);
        Tess.instance.add(w, height, 0);
        Tess.instance.add(w, -height, 0);
        Tess.instance.add(-w, -height, 0);
        Tess.instance.add(-height, w, 0);
        Tess.instance.add(height, w, 0);
        Tess.instance.add(height, -w, 0);
        Tess.instance.add(-height, -w, 0);
        Tess.instance.draw(GL_QUADS);
        Shader.disable();
    }

    private void renderChunkGrid(float fTime) {

        int ipX = GameMath.floor(vCam.x);
        int ipZ = GameMath.floor(vCam.z);
        int icX = ipX >> 4;
        int icZ = ipZ >> 4;
        int lenOver = RegionRenderer.LENGTH_OVER;
        lenOver += 8;
        int chunksW = lenOver * RegionRenderer.REGION_SIZE;
        int chunkHalfW = ((lenOver - 1) / 2) * RegionRenderer.REGION_SIZE;
        //                int minChunkX = (ipX) - chunkW;
        //                int minChunkZ = (ipZ) - chunkW;
        //                int maxChunkX = (ipX) + chunkW;
        Tess t = Tess.instance;
        float chunkWPx = 12.0f;
        float screenX = 140;
        float screenZ = 140;
        for (int cX = 0; cX < chunksW; cX++) {
            for (int cZ = 0; cZ < chunksW; cZ++) {
                t.setOffset(screenX + chunkWPx * cX, screenZ + chunkWPx * cZ, 0);
                int worldChunkX = icX - chunkHalfW + cX;
                int worldChunkZ = icZ - chunkHalfW + cZ;
                Chunk c = this.world.getChunk(worldChunkX, worldChunkZ);

                float border = 2;
                MeshedRegion region = Engine.regionRenderer.getByChunkCoord(worldChunkX, -1, worldChunkZ);
                if (region == null) {
                    t.setColorF(0xff0000, 1);
                } else /*if (!region.isRenderable) {
                       t.setColorF(0x555500, 1);
                       } else*/
                {
                    int r = 0x777777;
                    if (Math.abs(region.rX) % 2 == Math.abs(region.rZ) % 2)
                        r = 0;

                    t.setColorF(r, 1);
                }
                t.add(0, chunkWPx);
                t.add(chunkWPx, chunkWPx);
                t.add(chunkWPx, 0);
                t.add(0, 0);
                if (c == null) {
                    t.setColorF(0x993333, 1);
                } else if (!c.isValid) {
                    t.setColorF(0x999933, 1);
                } else if (worldChunkX == icX && worldChunkZ == icZ) {
                    t.setColorF(0x333399, 1);
                } else {
                    t.setColorF(0x339933, 1);
                }
                t.add(border, chunkWPx - border);
                t.add(chunkWPx - border, chunkWPx - border);
                t.add(chunkWPx - border, border);
                t.add(border, border);
            }
        }
        Shaders.colored.enable();
        t.drawQuads();
        Shader.disable();
    
    }

    public void renderGui(float fTime, double mx, double my) {

        if (this.gui != null) {
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("gui");
            GuiWindow window = GuiWindowManager.getMouseOver(mx, my);
            if (window != null && (Gui.selectedButton == null || Gui.selectedButton.parent != gui)) {
                mx -= 10000;
                my -= 10000;
            }
            this.gui.render(fTime, mx, my);
            if (window != null && (Gui.selectedButton == null || Gui.selectedButton.parent != gui)) {
                mx += 10000;
                my += 10000;
            }

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            if (this.world == null) {

            }
            glEnable(GL_DEPTH_TEST);
            GuiWindowManager.getInstance().render(fTime, mx, my);
            glDisable(GL_DEPTH_TEST);
        }
    }

    private void renderWorld(float fTime, int eye, FrameBuffer finalTarget) {
        
        Engine.getSceneFB().bind();
        Engine.getSceneFB().clearFrameBuffer();

        

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("renderWorld");

        Engine.skyRenderer.renderSkybox();

        Engine.worldRenderer.renderWorld(this.world, fTime);
        
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderWorld");

        

//        if (GPUProfiler.PROFILING_ENABLED)
//            GPUProfiler.start("HBAO");
//        if (HBAOPlus.hasContext) {
//
//            HBAOPlus.renderAO();
//            Shader.disable();
//        }
//
//        if (Game.GL_ERROR_CHECKS)
//            Engine.checkGLError("GLNativeLib.renderAO");
//        if (GPUProfiler.PROFILING_ENABLED)
//            GPUProfiler.end();

        

        FrameBuffer.unbindFramebuffer();
        
        
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("MainPass");
//        FrameBuffer.unbindFramebuffer();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("lightCompute 0");
        if (!VR_SUPPORT || eye == 0)
        Engine.lightCompute.render(this.world, fTime, 0);
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("HBAO");
        if (HBAOPlus.hasContext) {

            HBAOPlus.renderAO();
            Shader.disable();
        }

        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("GLNativeLib.renderAO");
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();


        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("Deferred");
        Engine.outRenderer.render(this.world, fTime, 0);
        Engine.checkGLError("Engine.outRenderer.render 0");
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("TransparentPass");

        boolean secondPass = true;
        if (secondPass) {
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("copyPreWaterDepth");
            Engine.outRenderer.copyPreWaterDepth();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            Engine.setBlend(true);
//            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            Engine.getSceneFB().bind();
            Engine.getSceneFB().clearColorBlack();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("World");
            GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
            for (int i = 0; i < 3; i++) {
                GL40.glBlendFuncSeparatei(1+i, GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
            }
            Engine.worldRenderer.renderTransparent(world, fTime);
            GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();


            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("Deferred");

            Engine.outRenderer.render(this.world, fTime, 1);

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
        }
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("copySceneDepthBuffer");
        Engine.outRenderer.copySceneDepthBuffer();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();


        if (Engine.outRenderer.getSsr() > 0) {
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("SSR");
//            if (!VR_SUPPORT || eye == 0)
            Engine.outRenderer.raytraceSSR();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
        }


        boolean firstPerson = !this.thirdPerson;
        if (firstPerson) {
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("firstPerson");
            Engine.setBlend(true);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            Engine.getSceneFB().bind();
            Engine.getSceneFB().clearDepth();
            Engine.getSceneFB().setDrawAll();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("World");
            GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
            for (int i = 0; i < 3; i++) {
                GL40.glBlendFuncSeparatei(1+i, GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
            }
            if (!Game.VR_SUPPORT) {
                Engine.worldRenderer.renderFirstPerson(world, fTime);
            }
            GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("lightCompute 1");
            if (!VR_SUPPORT || eye == 0)
            Engine.lightCompute.render(this.world, fTime, 1);
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();


            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("Deferred");

            Engine.outRenderer.render(this.world, fTime, 2);

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
        }
        

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("Lum");

        if (Engine.outRenderer.getSsr() > 0) {
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("SSR2");
//            if (!VR_SUPPORT || eye == 0)
            Engine.outRenderer.combineSSR();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
        }
        Engine.setBlend(false);
        glDisable(GL_DEPTH_TEST);
        Engine.enableDepthMask(false);
        Engine.outRenderer.renderBlur();


        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("Bloom");
        
        Engine.outRenderer.renderBloom();
        
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

        glEnable(GL_DEPTH_TEST);
        Engine.enableDepthMask(true);
        
        boolean pass = true;//Engine.renderWireFrame || !Engine.worldRenderer.debugBBs.isEmpty();
        if (pass) {
            glDisable(GL_CULL_FACE);
            Engine.setBlend(true);
            if (firstPerson && !VR_SUPPORT) {
                glDepthRange(0, 0.04);
                glColorMask(false, false, false, false);
                Engine.worldRenderer.renderFirstPerson(world, fTime);
                glColorMask(true, true, true, true);
                glDepthRange(0, 1f);
            }
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("ForwardPass");
            
            


            if (!VR_SUPPORT) {
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("BB Debug");
                Engine.worldRenderer.renderDebugBB(this.world, fTime);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
            }

            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("renderDebugBB");
            if (Engine.renderWireFrame) {

                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("Wireframe Debug");
                
                Engine.worldRenderer.renderNormals(this.world, fTime);
                glEnable(GL_POLYGON_OFFSET_FILL);
                glPolygonOffset(-3.4f, 2.f);
                Engine.worldRenderer.renderTerrainWireFrame(this.world, fTime);
                glDisable(GL_POLYGON_OFFSET_FILL);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("renderNormals");
                
            }
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("BlockHighlight");
//            System.out.println(VR.controllerDeviceIndex[1]);
//            if (VR.controllerDeviceIndex[0]>0)
            leftSelection.renderBlockHighlight(this.world, fTime);
            if (VR.controllerDeviceIndex[1]>0)
            rightSelection.renderBlockHighlight(this.world, fTime);

            dig.renderDigging(world, fTime);
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("BlockHighlight");
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();


            
            glEnable(GL_CULL_FACE);
            Engine.setBlend(false);
        }
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("Tonemap");
        FrameBuffer fbOut = Engine.outRenderer.renderTonemap();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

        //fbOut is still bound (is defferred buffer (16 bit RGBA + depth)
        
        GLDebugTextures selTex = GLDebugTextures.getSelected();
        if (selTex != null) {
            if (finalTarget != null) finalTarget.bindAndClear();
            else FrameBuffer.unbindFramebuffer();
            GLDebugTextures.drawFullScreen(selTex);
            if (selTex.pass.contains("compute_light_0") && selTex.name.equals("output")) {
                Engine.lightCompute.renderDebug();
            }
            return;
        }

        //fbOut is still bound (is defferred buffer (16 bit RGBA + depth)
        if (canRenderGui3d()) 
        {

            boolean blitDepthBuffer = false;
            if (blitDepthBuffer) {
                if (finalTarget == null) FrameBuffer.unbindFramebuffer();
                else {
                    finalTarget.bind();
                }
                FrameBuffer bloomOut = Engine.outRenderer.fbBloomOut;//bloom has our current scene depth information
                bloomOut.bindRead();
                int outWidth = finalTarget != null ? finalTarget.getWidth() : displayWidth;
                int outHeight = finalTarget != null ? finalTarget.getHeight() : displayHeight;
                GL30.glBlitFramebuffer(0, 0, bloomOut.getWidth(), bloomOut.getHeight(), 0, 0, outWidth, outHeight, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
                FrameBuffer.unbindReadFramebuffer();
            }

            
            Engine.setBlend(true);
            glDisable(GL_DEPTH_TEST);
            
            render3dGUI(fTime);
            
            glEnable(GL_DEPTH_TEST);
            Engine.setBlend(false);
        
        }

        Engine.outRenderer.renderAA(fbOut.getTexture(0), Engine.outRenderer.fbDeferred.getTexture(1), finalTarget);
        


    
        if (GL_ERROR_CHECKS) {
            if (glGetInteger(GL_DEPTH_FUNC) != GL_LEQUAL) {
                System.err.println("Expected GL_DEPTH_FUNC == GL_LEQUAL post render: "+glGetInteger(GL_DEPTH_FUNC));
            }
            if (!glGetBoolean(GL_DEPTH_TEST)) {
                System.err.println("Expected GL_DEPTH_TEST == true post render");
            }
            if (!glGetBoolean(GL_DEPTH_WRITEMASK)) {
                System.err.println("Expected GL_DEPTH_WRITEMASK == true post render");
            }
            if (GL30.glIsEnabledi(GL_BLEND, 0)) {
                System.err.println("Expected GL_BLEND == false post render");
            }
        }
    }

    private void render3dGUI(float fTime) {
        if (vrGui.hasAny() && canRenderGui3d()) {
            vrGui.render(fTime);
        }
      
        if (VR_SUPPORT) {
            for (int i = 0; i < 1; i++) {
                PositionMouseOver ctrlPos = getMouseOver(i);
                if (ctrlPos.vDir != null) {
                    Shaders.colored3D.enable();
                    Shaders.colored3D.setProgramUniform1f("color_brightness", 1.0f);
                    Tess t = Tess.instance;
                    t.setColorRGBAF(.92f, .92f, .11f, 1.0f);
                    Vector3f vo = Vector3f.pool(ctrlPos.vOrigin);
                    Vector3f vd = Vector3f.pool(ctrlPos.vDir);
                    
                    vd.scale(2);
                    vo.addVec(vd);
                    t.add(vo);
                    
                    vd.scale(10);
                    vo.addVec(vd);
                    t.setColorRGBAF(.97f, .97f, .97f, 1.0f);
                    t.add(vo);
                    GL11.glLineWidth(4);
                    t.draw(GL11.GL_LINES);
                    Shaders.colored3D.setProgramUniform1f("color_brightness", 0.1f);
                }
            }
        }
    }

    @Override
    public void onStatsUpdated() {
        if (this.statsCached != null) {
            this.statsCached.refresh();
        }
        if (System.currentTimeMillis()-lastShaderLoadTime >=2200/* && Keyboard.isKeyDown(GLFW.GLFW_KEY_F9)*/) {
            int iw = Engine.getSceneFB() != null ? Engine.getSceneFB().getWidth() : 0;
            int ih = Engine.getSceneFB() != null ? Engine.getSceneFB().getHeight() : 0;
            String s = String.format("%s - Display %dx%d - Window %dx%d - SceneFB %dx%d - Gui %dx%d", 
                    getAppTitle(), displayWidth, displayHeight, windowWidth, windowHeight, iw, ih, guiWidth, guiHeight);
            setTitle(s);
//            System.out.println(lastFPS);
//          System.out.println("initShaders");
            lastShaderLoadTime = System.currentTimeMillis();
//            Engine.particleRenderer.spawnParticles(10);
//          Shaders.initShaders();
////          Engine.lightCompute.initShaders();
//          Engine.worldRenderer.reloadModel();
//          Engine.renderBatched.initShaders();
//          Engine.worldRenderer.initShaders();
//          Engine.skyRenderer.initShaders();
//          Engine.particleRenderer.initShaders();
//            Engine.skyRenderer.redraw();
//            Engine.outRenderer.initShaders();

//            Engine.regionRenderer.initShaders();
////            Engine.shadowRenderer.initShaders();
//            Engine.outRenderer.initShaders();
//            SingleBlockRenderAtlas.getInstance().reset();
//            ItemModelManager.getInstance().reload();
//            
        }
    }

    @Override
    public void postRenderUpdate(float f) {
        Engine.worldRenderer.rendered = Engine.regionRenderer.rendered;
        if (VR_SUPPORT) VR.updatePose(f);
    }
    
    
    @Override
    public void preRenderUpdate(float f) {
        Engine.blockDraw.processQueue();
        Engine.outRenderer.aoReinit();
        Engine.regionRenderer.rendered = 0;
        if (this.client != null) {
            String reason = this.client.getClient().getDisconnectReason();
            if (reason!=null) {
                setWorld(null);
                setPlayer(null);
                this.client = null;
                System.out.println("disco "+this.server.getStatus());
                if (this.server.getStatus()) {
                    this.server.stop();
                    showGUI(new GuiShutdownServer());
                }else if (this.gui == null || !"userrequest".equals(reason)) {
                    showGUI(new GuiDisconnected(reason));
                } else
                    showGUI(null);
            } else {
                this.client.update();
            }
        }
        if (this.world == null) {
            if (!canRenderGui3d()) {
                if (this.gui == null && this.client == null) {
                    this.showGUI(new GuiMainMenu());
                }
                UniformBuffer.updateUBO(this.world, f);
                return;
            }
        }
        PlayerSelf player = getPlayer();
        if (player != null && this.world != null) {
            updateCameraFromPlayer(player, f);
            Engine.setLightPosition(this.world.getLightPosition());
        } else {
            Vector3f renderPos = this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
            Engine.camera.setPosition(renderPos);
            vCam.set(renderPos);
            vPlayer.set(renderPos);
            Engine.getSunLightModel().setTime(5850);
            Engine.getSunLightModel().updateFrame(f);
            Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
        }

        Engine.updateGlobalRenderOffset(vCam);
        Engine.updateCamera();
        if (VR_SUPPORT) {

//            vPlayer.x = (float) (player.lastPos.x + (player.pos.x - player.lastPos.x) * f) + 0;
//            vPlayer.y = (float) (player.lastPos.y + (player.pos.y - player.lastPos.y) * f) + 1.62F;
//            vPlayer.z = (float) (player.lastPos.z + (player.pos.z - player.lastPos.z) * f) + 0;
//
//            vCam.set(vPlayer);
            
            //calc negative z offset camera to contain both view frustums
            Vector3f combinedEyePos = Vector3f.pool(vCam).add(VR.cam.unifiedFrustumCameraOffset);
            
            Matrix4f mvpHMD = Matrix4f.pool();
            Matrix4f viewInvHMD = Matrix4f.pool();
            
            Matrix4f left = VR.getViewMat(0);
            Matrix4f.invert(left, viewInvHMD);
            Matrix4f.mul(VR.cam.projLeft, left, mvpHMD);
            Matrix4f.mul(mvpHMD, Engine.getMatSceneM(), mvpHMD); // multiply with our scene translation (MatSceneM)
            
            Engine.setFrustum(mvpHMD, viewInvHMD, combinedEyePos);
        } else {
            Engine.updateFrustumFromInternal();
        }
        Engine.updateShadowProjections(f);
        UniformBuffer.updateUBO(this.world, f);
        this.rightMouseOver.reset();
        this.leftMouseOver.reset();
        float winX, winY;

        if (this.movement.grabbed() || (GuiWindowManager.anyWindowVisible())) {
            winX = (float) displayWidth/2.0F;
            winY = (float) displayHeight/2.0F;
        } else {
            winX = (float) Mouse.getX();
            winY = (float) (displayHeight-Mouse.getY());
            if (winX < 0) winX = 0; if (winX > displayWidth) winX = 1;
            if (winY < 0) winY = 0; if (winY > displayHeight) winY = 1;
        }
        if (VR_SUPPORT) {
            for (int i = 0; i < 2; i++) {
                int idx = VR.controllerDeviceIndex[i];
                if (idx > -1) {
                    getMouseOver(i).updateFromController(idx, f);
                }
            }
        } else {
            getMouseOver(0).updateMouseFromScreenPos(winX, winY, displayWidth, displayHeight, this.movement.grabbed() ? Engine.camera.getCameraOffset() : null);

        }
        if (this.world != null && this.player != null) {

            boolean b = canRenderGui3d();
            if ((b&&!has3dGUIMouseFocus())||(!b&&this.gui==null)) {
                if (VR_SUPPORT) {
                    for (int i = 0; i < 2; i++) {
                        int idx = VR.controllerDeviceIndex[i];
                        if (idx > -1) {
                            getSelection(i).update(world, getMouseOver(i), vCam);
                        } else {
                            getSelection(i).reset();
                        }
                    }
                } else {
                    getSelection(0).update(world, getMouseOver(0), vCam);

                }
            } else {

                for (int i = 0; i < 2; i++) {
                    getSelection(i).reset();
                }
            }
        }
        
        
        if (this.world != null) {
            Engine.particleRenderer.preRenderUpdate(this.world, f);
            if (updateRenderers) {
              float renderRegionX = follow ? vCam.x : vLastCam.x;
//              float renderRegionY = follow ? py : lastCamY;
              float renderRegionZ = follow ? vCam.z : vLastCam.x;
              int xPosP = GameMath.floor(renderRegionX)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
              int zPosP = GameMath.floor(renderRegionZ)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
              if (Engine.updateRenderOffset) {
                  Engine.regionRenderer.reRender();
              }
              Engine.regionRenderer.update(this.world, vCam.x, vCam.y, vCam.z, xPosP, zPosP, f);
              Engine.lightCompute.updateLights(this.world, f);
            }
        }
        Matrix4f pose = VR.pose;
        if (!VR_SUPPORT) {
            pose = Matrix4f.pool();
            Engine.camera.calcViewMatrix(pose, false);
        }
        Engine.camera.updateViewDirection(pose);
        if (canRenderGui3d()) {
            
            if (vrGui.hasAny()) {
                vrGui.update(pose, Engine.camera.getPosition(), Engine.camera.getViewDirection(), getMouseOver(0));
            } else { 
                vrGui.reset();
            }
        }
        if (canRenderGui3d()) {
            if (this.gui == null && this.client == null) {
                this.showGUI(new GuiMainMenu());
            }
        }
    }
    private void updateCameraFromPlayer(PlayerSelf player, float f) {


        this.world.updateFrame(f);
        float tpDistance = this.lastTpDistance + (settings.thirdpersonDistance - this.lastTpDistance) * f;
        this.dig.preRenderUpdate();


        if (VR_SUPPORT)
        {
            VR.pose.toEuler(tmp);
            float yaw = 180-(tmp.y*GameMath.P_180_OVER_PI);
            float pitch = (tmp.x*GameMath.P_180_OVER_PI);
            float forward = VR.getAxis(0, 0, 1)*-1.0f;
            float strafe = VR.getAxis(0, 0, 0)*1.0f;
            player.update(pitch, yaw, forward, strafe, 0, false);
        } else {

            player.updateInputDirect(movement);
        }
        
        float distF = player.distanceMoved + (player.distanceMoved-player.prevDistanceMoved)*f;
        distF = -distF*0.6f;
        float f2 = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * f;
        float f3 = player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * f;
        if (!VR_SUPPORT)
        Engine.camera.calcViewShake(distF, f2, f3, f);
        float yaw = player.yaw;
        float pitch = player.pitch;
        Engine.camera.setOrientation(yaw, pitch, thirdPerson, tpDistance);

        if (follow) {
            vLastCam.set(vCam);
        }
        vLastPlayer.set(vPlayer);
        float yOffset = VR_SUPPORT?0.f:1.62F;
        vPlayer.x = (float) (player.lastPos.x + (player.pos.x - player.lastPos.x) * f) + 0;
        vPlayer.y = (float) (player.lastPos.y + (player.pos.y - player.lastPos.y) * f) + yOffset;
        vPlayer.z = (float) (player.lastPos.z + (player.pos.z - player.lastPos.z) * f) + 0;
        
        vCam.set(vPlayer);
        
        if (thirdPerson) {
            Vector3f camOffset = Engine.camera.getCameraOffset();
//            float dist = camOffset.length();
            Vector3f t = Vector3f.pool();
            Vector3f t2 = Vector3f.pool();
            float minDist = tpDistance;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = -1; y <= 1; y++) {
                        t.set(camOffset);
                        t.normalise();
                        t2.set(vCam);
                        t2.x+=x*0.1f;
                        t2.y+=y*0.1f;
                        t2.z+=z*0.1f;
                        this.rayTrace.doRaytrace(getWorld(), t2, t, (int) Math.ceil(tpDistance * 2));
                        if (this.rayTrace.hasHit()) {
                            Vector3f hitPos = this.rayTrace.getHit().pos;
                            hitPos.subtract(t2);
                            float intersectDist = hitPos.length();
                            if (intersectDist < minDist) {
                                minDist = intersectDist;
                            }
                        }

                    }
                }
            }
            if (minDist < tpDistance) {
                camOffset.normalise(); // we do not work on copy here, so we can query the collision distance offset from elsewhere during frame
                camOffset.scale(minDist);
            }
            vCam.addVec(camOffset);
        }
        
        Engine.camera.setPosition(vCam);
    
    }

    private boolean has3dGUIMouseFocus() {
        return canRenderGui3d() && this.vrGui.isMouseOverGui();
    }
    public void onGuiClosed(Gui gui, Gui targetGui) {
        if (canRenderGui3d()) {
            vrGui.removeGui(gui);
        }
    }

    public void onGuiOpened(Gui gui, Gui prevGui) {
        if (canRenderGui3d()) {
            vrGui.addGui(gui, prevGui, Engine.camera.getPosition(), Engine.camera.getViewDirection());
        }
    }
    

    @Override
    public void onWindowResize(int displayWidth, int displayHeight) {
        if (isRunning()) {
            if (this.statsCached != null) {
                this.statsCached.setSize(guiWidth, guiHeight);
                this.statsCached.setPos(0, 0);
            }
            if (this.gui != null) {
                this.gui.setPos(0, 0);
                this.gui.setSize(guiWidth, guiHeight);
                this.gui.initGui(this.gui.firstOpen);
            }
            this.chatOverlay = new GuiOverlayChat();
            this.chatOverlay.setPos(8, guiHeight-guiHeight/3-8);
            this.chatOverlay.setSize(guiWidth/2, guiHeight/3);
        }
        if (!VR_SUPPORT||isStarting) {
            Game.displayWidth=displayWidth;
            Game.displayHeight=displayHeight;
            setRenderResolution(displayWidth, displayHeight);
        }
    }
    @Override
    public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Game.displayWidth=displayWidth;
            Game.displayHeight=displayHeight;
            Engine.resize(displayWidth, displayHeight);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("onResize");
            GLDebugTextures.onResize();
        }
    }
    @Override
    public void tick() {
        if (!isStarting) {
            if (VR_SUPPORT) {
                VR.tick();
            }
            this.vrGui.tickUpdate();
            if (this.world == null)
                this.cameraController.tickUpdate();
            this.lastTpDistance = settings.thirdpersonDistance;
            if (this.world != null)
                this.world.tickUpdate();
            if (this.gui != null) {
                this.gui.update();
            }
            if (this.leftSelection != null) {
                this.leftSelection.update(getWorld());
            }
            if (this.rightSelection != null) {
                this.rightSelection.update(getWorld());
            }
            GuiWindowManager.update();
            this.dig.update();
            ChatManager.getInstance().saveInputHistory();
            Engine.regionRenderer.tickUpdate();
            Engine.worldRenderer.tickUpdate();
            Engine.skyRenderer.tickUpdate();
            Engine.particleRenderer.tickUpdate(this.world);
//            if (this.connect == null && this.client != null && !this.client.isConnected() && this.world != null) {
//                this.setWorld(null);
//                showGUI(new GuiMainMenu());
//            }
//            matrixSetupMode = Main.ticksran % 100 < 50;
            if (this.settings.lazySave()) {
                this.saveSettings();
            }
            if (this.throttleClick > 0) {
                this.throttleClick--;
            }

        }
       AsyncTasks.completeTasks();
    }

    public void returnToMenu() {
        if (world != null) {
            setWorld(null);
        }
        if (client != null) {
            client.disconnect("userrequest");
        }
    }
    
    public boolean isConnected() {
        return this.client!=null;
    }

    public void addDebugOnScreen(String string) {
        if (this.statsOverlay != null) {
            this.statsOverlay.setMessage(string);
        }
    }

    public World getWorld() {
        return this.world;
    }

    public synchronized void setConnection(NetworkClient client) {
        ThreadConnect conn = this.connect;
        if (conn != null) {
            conn.cancel();
        }
        this.connect = null;
        if (this.client != null) {
            this.client.disconnect("Quit");
            this.client = null;
        }
        this.client = client;
    }

    public PlayerSelf getPlayer() {
        return this.player;
    }

    public void setPlayer(PlayerSelf player) {
        this.player = player;
    }

    public void sendPacket(Packet packet) {
        if (this.client != null) {
            this.client.sendPacket(packet);
        }
    }

    public PlayerProfile getProfile() {
        return this.profile;
    }

    /**
     * 
     */
    public void saveSettings() {
        try {
            File f = new File(WorkingEnv.getConfigFolder(), "settings.yml");
            settings.write(f);
        } catch (InvalidConfigException e) {
            e.printStackTrace();
        }
    }
    public void saveProfile() {
        try {
            File fProfile = new File(WorkingEnv.getConfigFolder(), "profile.yml");
            profile.write(fProfile);
        } catch (InvalidConfigException e) {
            e.printStackTrace();
        }
    }
    public void loadProfile() {
        try {
            File fProfile = new File(WorkingEnv.getConfigFolder(), "profile.yml");
            if (fProfile.exists()) {
                profile.load(fProfile);
            }
        } catch (InvalidConfigException e) {
            e.printStackTrace();
        }
    }
    public void loadSettings() {
        try {
            File f = new File(WorkingEnv.getConfigFolder(), "settings.yml");
            settings.load(f);
            if (!f.exists())
                settings.write(f);
        } catch (InvalidConfigException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    public boolean isInSelection(int ix, int iy, int iz) {
        return this.leftSelection.contains(ix, iy, iz);
    }

    public Selection getSelection() {
        return getSelection(0);
    }
    public Selection getSelection(int n) {
        return n == 0 ? this.leftSelection : this.rightSelection;
    }
    public PositionMouseOver getMouseOver(int n) {
        return n == 0 ? this.leftMouseOver : this.rightMouseOver;
    }

    public ClientHandler getClientHandler() {
        NetworkClient client = this.client;
        return client != null ? client.getClient() : null;
    }
    public WorldPlayerController getWPCtrl() {
        return this.worldPlayerController;
    }
    @Override
    public void parseCmdArgs(String[] args) {
        String serverAddr = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && args[i].length()>1) {
                String arg = args[i].substring(1);
                if (arg.startsWith("-") && arg.length()>1) {
                    arg = arg.substring(1);
                }
                switch (arg) {
                    case "server": {
                        serverAddr = getValue(args, i, arg);
                        break;
                    }
                        
                }
            }
        }
        Game.instance.serverAddr = serverAddr;
    }

    @Override
    public void onControllerButton(int controllerIdx, int button, int eventType) {
        if (eventType == VREvents.ButtonPress||eventType == VREvents.ButtonUnpress) {
            switch (button) {
                case VREvents.BUTTON_BACK:
                    getSelection(controllerIdx).clicked(0, eventType == VREvents.ButtonPress);
                    break;
                case VREvents.BUTTON_SIDE:
                    getSelection(controllerIdx).clicked(1, eventType == VREvents.ButtonPress);
                    break;
                case VREvents.BUTTON_TIP:
                    
                    break;
                case VREvents.BUTTON_A:
                    getSelection(controllerIdx).clicked(3, eventType == VREvents.ButtonPress);
//                    if (getSelection(controllerIdx).hasSelection()) {
//                        BlockPos pos = getSelection(controllerIdx).getHit().blockPos;
//                        System.out.println("jump");
//                        Game.instance.sendPacket(new PacketCBlockAction(Game.instance.getWorld().getId(), pos, hit.pos, faceHit, stack));
////                        new PacketCSetBlock(Game.instance.getWorld().getId(), pos, hit.pos, faceHit, stack)
//                    }
////                    Game.instance.sendPacket(new PacketCSetBlock(Game.instance.getWorld().getId(), pos, hit.pos, faceHit, stack));
////                    .pos;
                    break;
                case VREvents.BUTTON_B:
                    break;
            }
        }
    }

    public void processChatInput(String text) {
        if (text.startsWith("/")) {
            if (text.equals("/redrawsky")) {
                Engine.skyRenderer.redraw();
                return;
            }
            if (text.equals("/resize")) {
                return;
            }
            if (text.equals("/gui3d")) {
                this.settings.gui3d = !settings.gui3d;
                updateGui3dMode();
                return;
            }
            if (text.equals("/reloadshaders")) {
                Shaders.initShaders();
                //            Engine.lightCompute.initShaders();
//                Engine.worldRenderer.reloadModel();
//                Engine.renderBatched.initShaders();
//                Engine.shadowRenderer.initShaders();
//                Engine.worldRenderer.initShaders();
//                Engine.skyRenderer.initShaders();
                Engine.outRenderer.initShaders();
//                Engine.particleRenderer.initShaders();
                return;
            }
        }
        sendPacket(new PacketChatMessage(GlobalChannel.TAG, text));
    }
    
    @Override
    public boolean canRenderGui3d() {
        return this.renderGui3d;
    }
}
