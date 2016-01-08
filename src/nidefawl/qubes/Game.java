package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.*;

import java.io.File;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.IDMappingBlocks;
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
import nidefawl.qubes.gui.windows.GuiInventory;
import nidefawl.qubes.gui.windows.GuiCrafting;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.*;
import nidefawl.qubes.item.*;
import nidefawl.qubes.logging.IErrorHandler;
import nidefawl.qubes.models.BlockModelManager;
import nidefawl.qubes.models.ItemModelManager;
import nidefawl.qubes.network.client.NetworkClient;
import nidefawl.qubes.network.client.ThreadConnect;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketCSetBlock;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.perf.TimingHelper2;
import nidefawl.qubes.render.gui.SingleBlockRenderAtlas;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.*;
import nidefawl.qubes.texture.array.BlockNormalMapArray;
import nidefawl.qubes.texture.array.BlockTextureArray;
import nidefawl.qubes.texture.array.ItemTextureArray;
import nidefawl.qubes.util.*;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;
import nidefawl.qubes.world.WorldClientBenchmark;

public class Game extends GameBase implements IErrorHandler {

    static public Game instance;
    PlayerProfile         profile = new PlayerProfile();
    public final ClientSettings          settings  = new ClientSettings();

    public GuiOverlayStats statsOverlay;
    public GuiCached       statsCached;
    public GuiOverlayDebug debugOverlay;
    public GuiOverlayChat  chatOverlay;
    private Gui            gui;

    private ThreadConnect  connect;
    private NetworkClient  client;
    WorldClient            world              = null;
    PlayerSelf             player;
    public InputController        movement           = new InputController();
    public final DigController   dig           = new DigController();
    public final Selection selection          = new Selection();
    public boolean         follow             = true;
    
    public BlockStack           selBlock           = new BlockStack(0);
    public long                   lastShaderLoadTime = System.currentTimeMillis();
    public final Vector3f vCam = new Vector3f();
    public final Vector3f vPlayer = new Vector3f();
    public final Vector3f vLastCam = new Vector3f();
    public final Vector3f vLastPlayer = new Vector3f();

    public boolean                  updateRenderers = true;
    private TesselatorState debugChunks;
    public static boolean showGrid=false;
    public boolean thirdPerson = true;
    boolean reinittexthook = false;
    boolean wasGrabbed = false;
    public String serverAddr;
    boolean testMode = false;
    PlayerSelf remotePlayer;
    PlayerSelf testPlayer;
    WorldClient remoteWorld;
    WorldClient testWorld;

    int skipChars = 0;
    private BaseStack testStack = new ItemStack(Item.pickaxe);
    private BaseStack testStack2 = new ItemStack(Item.axe);
    
    private GameMode mode = GameMode.PLAY;
    final float[] loadProgress = new float[5];
    
    public GameMode getMode() {
        return this.mode;
    }

    public void connectTo(String host) {
        try {
            String[] split = host.split(":");
            int port = 21087;
            if (split.length > 1) {
                port = StringUtil.parseInt(split[1], port);
            }
            host = split[0];
            connect = new ThreadConnect(host, port);
            connect.startThread();
            showGUI(new GuiConnecting(connect));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Game() {
        super();
    }

    @Override
    public void initGame() {
        debugChunks = new TesselatorState();
        loadProfile();
        loadSettings();
        InputController.initKeybinds();
        selection.init();
        dig.init();
        AssetManager.getInstance().init();
        FontRenderer.init();
        Engine.init();
        loadRender(0, 0, "Initializing");
        TextureManager.getInstance().init();
        BlockModelManager.getInstance().init();
        ItemModelManager.getInstance().init();
        ItemTextureArray.getInstance().init();
        SingleBlockRenderAtlas.getInstance().init();
        AssetTexture tex_map_grass = AssetManager.getInstance().loadPNGAsset("textures/colormap_grass.png");
        ColorMap.grass.set(tex_map_grass);
        AssetTexture tex_map_foliage = AssetManager.getInstance().loadPNGAsset("textures/colormap_foliage.png");
        ColorMap.foliage.set(tex_map_foliage);

        
        
        SysInfo info = new SysInfo();
        String title = "LWJGL "+info.lwjglVersion+" - "+info.openGLVersion;
        setTitle(title);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("initGame 1");
        this.statsOverlay = new GuiOverlayStats();
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("initGame 2");
        this.statsCached = new GuiCached(statsOverlay); 
        this.statsCached.setPos(0, 0);
        this.statsCached.setSize(displayWidth, displayHeight);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("initGame 3");
        this.debugOverlay = new GuiOverlayDebug();
        this.debugOverlay.setPos(0, 0);
        this.debugOverlay.setSize(displayWidth, displayHeight);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("initGame 4");
        Engine.checkGLError("Post startup");
        loadRender(0, 0.5f, "Initializing");
    }
    @Override
    public boolean loadRender(int step, float f) {
        return loadRender(step, f, "");
    }
    public boolean loadRender(int step, float f, String string) {
        int tw = Game.displayWidth;
        int th = Game.displayHeight;
        int oldW = (int) (tw*0.6f*loadProgress[step]);
        int newW = (int) (tw*0.6f*(f));
        float fd=Math.abs(newW-oldW);
        if (fd<15f && f < 1 && f > 0) return false;
        loadProgress[step] = f;
        int nzero = 0;
        for (int i = 0; i < loadProgress.length; i++) {
            if (loadProgress[i] == 0)
                nzero++;
            else if (nzero > 0 && loadProgress[i] > 0) {
                throw new GameError("out of order");
            }
        }
        float x = 0;
        float y = 0;
        float l = tw*0.2f;
        float barh = 8;
        float barsH = (barh*1.2f) * (loadProgress.length-1);
        float barsTop = (th-barsH)/2.0f;
        if (isCloseRequested()) {
            shutdown();
            return false;
        }
        checkResize();
        updateTime();
        updateInput();
        Engine.updateOrthoMatrix(displayWidth, displayHeight);
        UniformBuffer.updateOrtho();
        glClearColor(0.71F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("loadRender glClear");
        Shaders.colored.enable();
        Tess.instance.resetState();
        Tess.instance.setColor(0x0, 0xff);
        Tess.instance.add(x + tw, y, 0, 1, 1);
        Tess.instance.add(x, y, 0, 0, 1);
        Tess.instance.add(x, y + th, 0, 0, 0);
        Tess.instance.add(x + tw, y + th, 0, 1, 0);
        Tess.instance.drawQuads();
        float p = 0;
        for (int i = 0; i < loadProgress.length; i++) {
            p+=loadProgress[i];
        }
        p/=(float)loadProgress.length;
        float w = tw*0.6f*p;
        Tess.instance.setColor(0xffffff, 0xff);
        Tess.instance.add(x + l + w, y + barsTop-2, 0, 1, 1);
        Tess.instance.add(x + l, y + barsTop-2, 0, 0, 1);
        Tess.instance.add(x + l, y + barsTop+barsH+2, 0, 0, 0);
        Tess.instance.add(x + l + w, y + barsTop+barsH+2, 0, 1, 0);
        Tess.instance.drawQuads();
        FontRenderer font = FontRenderer.get(null, 16, 1, 18);
        if (font != null) {
            Shaders.textured.enable();
            font.drawString(string, x+l+2, y+barsTop+barsH+10+font.getLineHeight(), -1, true, 1.0f);
        }
//        for (int i = 0; i < loadProgress.length; i++) {
//            
//             w = tw*0.6f*loadProgress[i];
//            Tess.instance.setColor(0x666666, 0xff);
//            Tess.instance.add(x + l + w, y + barsTop, 0, 1, 1);
//            Tess.instance.add(x + l, y + barsTop, 0, 0, 1);
//            Tess.instance.add(x + l, y + barsTop+barh, 0, 0, 0);
//            Tess.instance.add(x + l + w, y + barsTop+barh, 0, 1, 0);
//            Tess.instance.drawQuads();
//            barsTop+=barh+2;
//        }
        Shader.disable();
        updateDisplay();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("loadRender updateDisplay");
        return true;
    }
    public void lateInitGame() {
        dig.reloadTextures();
        loadRender(0, 0.8f, "Loading... Item Models");
        ItemModelManager.getInstance().reload();
        loadRender(0, 0.9f, "Loading... Block Models");
        BlockModelManager.getInstance().reload();
        loadRender(0, 1f, "Loading... Item Textures");
        ItemTextureArray.getInstance().reload();
        BlockTextureArray.getInstance().reload();
        BlockNormalMapArray.getInstance().reload();
        ChatManager.getInstance().loadInputHistory();

        InputController.load();
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
        this.selection.reset();
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

    /**
     * 
     */
    public void reposModel() {
        Engine.worldRenderer.setModelPos(vPlayer.x, vPlayer.y-1, vPlayer.z);
    }

    public void setWorld(WorldClient world) {
        this.selection.reset();
        if (this.world != null) {
            this.world.onLeave();
            Engine.flushRenderTasks();
            Engine.regionRenderer.resetAll();
        }
        this.world = world;
        if (this.world != null) {
            this.world.onLoad();
        }
    }
    
    @Override
    public void shutdown() {
        super.shutdown();
        setWorld(null);
        ChatManager.getInstance().saveInputHistory();
        AsyncTasks.shutdown();
    }
    
    protected void onTextInput(long window, int codepoint) {
//        Keybinding k = Keyboard.getKeyBinding(codepoint);
//        if (k != null && k.isEnabled() && k.isPressed()) {
//            return;
//        }
        if (this.gui != null) {
            if (this.gui.onTextInput(codepoint)) {
                return;
            }
        }
        if (GuiWindowManager.requiresTextInput()) {
            if (GuiWindowManager.onTextInput(codepoint)) {
                return;
            }
        } 
    }
    
    @Override
    protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
        if (window == windowId) {
          Keybinding k = InputController.getKeyBinding(key);
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
            this.gui.onWheelScroll(xoffset, yoffset);
        } else {
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
    int throttleClick=0;
    public void onMouseClick(long window, int button, int action, int mods) {
        if (this.gui != null) {
            if (!this.gui.onMouseClick(button, action)) {
                if (this.world == null) {

                    if (GuiWindowManager.onMouseClick(button, action)) {
                        return;
                    }
                }
            }
        } else {
            if (GuiWindowManager.onMouseClick(button, action)) {
                return;
            }


            boolean b = Mouse.isGrabbed();
            boolean isDown = Mouse.getState(action);
            if (throttleClick > 0) {
                return;
            }
            if (b)
                dig.onMouseClick(button, isDown);
            switch (button) {
                case 0:
                    selection.clicked(button, isDown);
                    if (this.player != null) {
                        this.player.clicked(button, isDown);
                    }
                    break;
                case 1:
                    if (isDown ) {
                        setGrabbed(!b);
                        b = !b;
                    }
                    break;
                case 2:
                    selection.clicked(button, isDown);
                    break;
            }
            if (b != this.movement.grabbed()) {
                setGrabbed(b);
            }
        }
    }

    @Override
    public void input(float fTime) {
        double mdX = Mouse.getDX();
        double mdY = Mouse.getDY();
        if (this.movement.grabbed()) {
            this.movement.update(mdX, -mdY);
        } else {
            GuiWindowManager.mouseMove(mdX, -mdY);
        }
    }

    public void setGrabbed(boolean b) {
        if (b != this.movement.grabbed()) {
            this.movement.setGrabbed(b);
            selection.resetSelection();
            Mouse.setCursorPosition(displayWidth / 2, displayHeight / 2);
            Mouse.setGrabbed(b);
            this.dig.onGrabChange(this.movement.grabbed());
        }
    }



    public void render(float fTime) {
        //      fogColor.scale(0.4F);
        
        if (this.world != null) {
            glDisable(GL_BLEND);
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("ShadowPass");
            Engine.shadowRenderer.renderShadowPass(this.world, fTime);
            Engine.getSceneFB().bind();
            Engine.getSceneFB().clearFrameBuffer();

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("MainPass");
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("renderWorld");

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            Engine.worldRenderer.renderWorld(this.world, fTime);
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("renderWorld");

            

            FrameBuffer.unbindFramebuffer();
            
            
        }
        
        

        glDisable(GL_BLEND);
        glClearColor(0.71F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        

        if (this.world != null) {
//            FrameBuffer.unbindFramebuffer();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("lightCompute 0");
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
            Engine.outRenderer.bindFB();
            Engine.outRenderer.clearFrameBuffer();
            Engine.outRenderer.render(this.world, fTime, 0);
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
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                Engine.getSceneFB().bind();
                Engine.getSceneFB().clearColorBlack();
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("World");
                Engine.worldRenderer.renderTransparent(world, fTime);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();

                Engine.outRenderer.bindFB();

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

            boolean firstPerson = !this.thirdPerson && this.mode == GameMode.PLAY;
            if (firstPerson) {
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("firstPerson");
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                Engine.getSceneFB().bind();
                Engine.getSceneFB().clearDepth();
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("World");
                Engine.worldRenderer.renderFirstPerson(world, fTime);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("lightCompute 1");
                Engine.lightCompute.render(this.world, fTime, 1);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();

                Engine.outRenderer.bindFB();

                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("Deferred");

                Engine.outRenderer.render(this.world, fTime, 2);

                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
            }
            
            

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("SSR + CalcLum");

            glDisable(GL_BLEND); // don't blend ssr
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            Engine.outRenderer.renderReflAndBlur(this.world, fTime);


            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("Bloom");
            
            Engine.outRenderer.renderBloom(this.world, fTime);
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            glEnable(GL_DEPTH_TEST);
            glDepthMask(true);
            
            boolean pass = true;//Engine.renderWireFrame || !Engine.worldRenderer.debugBBs.isEmpty();
            if (pass) {
                glDisable(GL_CULL_FACE);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                if (firstPerson) {
                    glDepthRange(0, 0.04);
                    glColorMask(false, false, false, false);
                    Engine.worldRenderer.renderFirstPerson(world, fTime);
                    glColorMask(true, true, true, true);
                    glDepthRange(0, 1f);
                }
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("ForwardPass");
                
                


                if (!Engine.worldRenderer.debugBBs.isEmpty()) {

                    

                    if (GPUProfiler.PROFILING_ENABLED)
                        GPUProfiler.start("BB Debug");
                    Engine.worldRenderer.renderDebugBB(this.world, fTime);
                    if (GPUProfiler.PROFILING_ENABLED)
                        GPUProfiler.end();

                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("renderDebugBB");
                    
                }

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
                selection.renderBlockHighlight(this.world, fTime);

                dig.renderDigging(world, fTime);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();

                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("BlockHighlight");
                
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
                
                glEnable(GL_CULL_FACE);
                glDisable(GL_BLEND);
            }
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("Final");
            GLDebugTextures selTex = GLDebugTextures.getSelected();
//            &&ticksran%40<20
            if (selTex != null) {
                GLDebugTextures.drawFullScreen(selTex);
                if (selTex.pass.contains("compute_light_0") && selTex.name.equals("output")) {
                    Engine.lightCompute.renderDebug();
                }
            } else {

                Engine.outRenderer.renderFinal(this.world, fTime);
            }
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

        }
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
            if (glGetBoolean(GL_BLEND)) {
                System.err.println("Expected GL_BLEND == false post render");
            }
        }
        
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glClear(GL_DEPTH_BUFFER_BIT);
        glDisable(GL_DEPTH_TEST);

        

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("gui");

        if (this.world != null && this.gui == null && this.movement.grabbed()) {
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

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (this.gui != null) {
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("gui");
            this.gui.render(fTime, Mouse.getX(), Mouse.getY());
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            if (this.world == null) {

                glEnable(GL_DEPTH_TEST);
                GuiWindowManager.getInstance().render(fTime, Mouse.getX(), Mouse.getY());
                glDisable(GL_DEPTH_TEST);
            }
        } else if (this.world != null) {
            if (showGrid) {
                int ipX = GameMath.floor(vCam.x);
                int ipY = GameMath.floor(vCam.y);
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
            glEnable(GL_DEPTH_TEST);
            GuiWindowManager.getInstance().render(fTime, Mouse.getX(), Mouse.getY());
            glDisable(GL_DEPTH_TEST);
            Shaders.textured.enable();
//            Engine.itemRender.drawItem(this.testStack, displayWidth/2, displayHeight/2, 32, 32);
            
//            Engine.itemRender.drawItem(this.testStack2, displayWidth/2+32, displayHeight/2, 32, 32);
            if (this.chatOverlay != null) {
                this.chatOverlay.render(fTime, 0, 0);
            }

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("stats");
            if (this.statsCached != null) {
                this.statsCached.render(fTime, 0, 0);
            }
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
        }else {
        }

        if (GLDebugTextures.show) {
//          
//          if (this.debugOverlay != null) {
//              this.debugOverlay.render(fTime, 0, 0);
//          }
//          
            
          GLDebugTextures.drawAll(displayWidth, displayHeight);
      }
        
        glEnable(GL_DEPTH_TEST);

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

    }

    @Override
    public void onStatsUpdated() {
        if (this.statsCached != null) {
            this.statsCached.refresh();
        }
        if (System.currentTimeMillis()-lastShaderLoadTime >3241/* && Keyboard.isKeyDown(GLFW.GLFW_KEY_F9)*/) {
//          System.out.println("initShaders");
            lastShaderLoadTime = System.currentTimeMillis();
          Shaders.initShaders();
//          Engine.lightCompute.initShaders();
//          Engine.worldRenderer.reloadModel();
//          Engine.worldRenderer.initShaders();
//          Engine.regionRenderer.initShaders();
//            Engine.shadowRenderer.initShaders();
//            Engine.outRenderer.initShaders();
//            SingleBlockRenderAtlas.getInstance().reset();
//            
        }
    }

    @Override
    public void postRenderUpdate(float f) {
        Engine.worldRenderer.rendered = Engine.regionRenderer.rendered;
    }
    @Override
    public void preRenderUpdate(float f) {
        Engine.regionRenderer.rendered = 0;
        boolean b = Mouse.isGrabbed();
        if (b != this.movement.grabbed()) {
            setGrabbed(b);
        }
        if (this.client != null) {
            String reason = this.client.getClient().getDisconnectReason();
            if (reason!=null) {
                setWorld(null);
                setPlayer(null);
                this.client = null;
                if (this.gui == null || !"userrequest".equals(reason)) {
                    showGUI(new GuiDisconnected(reason));
                }
            } else {
                this.client.update();
            }
        }
        if (this.world == null) {
            UniformBuffer.updateUBO(this.world, f);
            if (this.gui == null && this.client == null) {
                this.showGUI(new GuiMainMenu());
            }
            return;
        }
        
        this.world.updateFrame(f);
        PlayerSelf player = getPlayer();
        if (player != null) {
            this.dig.preRenderUpdate();
            player.updateInputDirect(movement);
            float yaw = player.yaw;
            float pitch = player.pitch;
            Engine.camera.setOrientation(yaw, pitch, thirdPerson, settings.thirdpersonDistance);

            if (follow) {
                vLastCam.set(vCam);
            }
            vLastPlayer.set(vPlayer);
            
            vPlayer.x = (float) (player.lastPos.x + (player.pos.x - player.lastPos.x) * f) + 0;
            vPlayer.y = (float) (player.lastPos.y + (player.pos.y - player.lastPos.y) * f) + 1.62F;
            vPlayer.z = (float) (player.lastPos.z + (player.pos.z - player.lastPos.z) * f) + 0;
            vCam.set(vPlayer);
            if (thirdPerson) {
                Vector3f camPos = Engine.camera.getCameraOffset();
                vCam.addVec(camPos);
            }
            
            if (this.world.lights.size() > 0) {
//                DynamicLight l = this.world.lights.get(0);
//                l.loc.set(vPlayer);
//                int colorI = Color.HSBtoRGB((ticksran+f)/60, 1, 1);
//                l.intensity = 0.4F;
//                l.color = new Vector3f(1*l.intensity);
//                l.color.x = (float) (((colorI>>16)&0xFF) / 255.0F * l.intensity);
//                l.color.y = (float) (((colorI>>8)&0xFF) / 255.0F * l.intensity);
//                l.color.z = (float) (((colorI>>0)&0xFF) / 255.0F * l.intensity);
            }
          Engine.camera.setPosition(vCam);
//          if (Engine.worldRenderer.qmodel!=null)
//          Engine.worldRenderer.qmodel.setHeadOrientation(yaw, pitch);
        }

        Engine.setLightPosition(this.world.getLightPosition());
        Engine.updateCamera();
        Engine.updateShadowProjections(f);
        UniformBuffer.updateUBO(this.world, f);
        if (player != null) {
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
            if (this.gui == null) {
                Engine.updateMouseOverView(winX, winY, this.movement.grabbed());
                selection.update(world, vCam.x, vCam.y, vCam.z);
            }
        }
        
        if (this.world != null && updateRenderers) {
            float renderRegionX = follow ? vCam.x : vLastCam.x;
//            float renderRegionY = follow ? py : lastCamY;
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
    
    public void showGUI(Gui gui) {

        if (gui != null && this.gui == null) {
            if (Mouse.isGrabbed()) {
                setGrabbed(false);
                wasGrabbed = true;
            }
        }
        if (this.gui != null) {
            this.gui.onClose();
        }
        this.gui = gui;
        if (this.gui != null) {
            this.gui.setPos(0, 0);
            this.gui.setSize(displayWidth, displayHeight);
            this.gui.initGui(this.gui.firstOpen);
            this.gui.firstOpen = false;
            if (Mouse.isGrabbed()) {
                setGrabbed(false);
                wasGrabbed = true;
            }
        } else {
            if (wasGrabbed) {
                Game.instance.setGrabbed(true);
            }
            wasGrabbed = false;
        }
        reinittexthook = true;
            
    }
    
    @Override
    public void updateInput() {
        super.updateInput();
        if (reinittexthook) {
            reinittexthook = false;
            if (this.gui != null && this.gui.requiresTextInput()) {
                setTextHook(true);
            } else if (GuiWindowManager.requiresTextInput()) {
                setTextHook(true);
            } else {
                setTextHook(false);
            }
        }
    }

    @Override
    public void onResize(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
            if (this.statsCached != null) {
                this.statsCached.setSize(displayWidth, displayHeight);
                this.statsCached.setPos(0, 0);
            }
            if (this.debugOverlay != null) {
                this.debugOverlay.setPos(0, 0);
                this.debugOverlay.setSize(displayWidth, displayHeight);
            }
            if (this.gui != null) {
                this.gui.setPos(0, 0);
                this.gui.setSize(displayWidth, displayHeight);
                this.gui.initGui(this.gui.firstOpen);
            }
            this.chatOverlay = new GuiOverlayChat();
            this.chatOverlay.setPos(8, displayHeight-displayHeight/3-8);
            this.chatOverlay.setSize(displayWidth/2, displayHeight/3);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("onResize");
            GLDebugTextures.onResize();
        }
    }
    @Override
    public void tick() {
       if (this.world != null)
           this.world.tickUpdate();
       if (this.gui != null) {
           this.gui.update();
       }
       GuiWindowManager.update();
       this.dig.update();
       ChatManager.getInstance().saveInputHistory();
       Engine.regionRenderer.tickUpdate();
       Engine.worldRenderer.tickUpdate();
//       if (this.connect == null && this.client != null && !this.client.isConnected() && this.world != null) {
//           this.setWorld(null);
//           showGUI(new GuiMainMenu());
//       }
//        matrixSetupMode = Main.ticksran%100<50;
       if (this.settings.lazySave()) {
           this.saveSettings();
       }
       if(this.throttleClick > 0) {
           this.throttleClick--;
       }
       AsyncTasks.completeTasks();
    }

    public void returnToMenu() {
        if (world != null) {
            setWorld(null);
        }
        showGUI(new GuiMainMenu());  
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
        this.connect.cancel();
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
     * @param intersect
     * @param quarterMode 
     */
    public void blockClicked(RayTraceIntersection intersect, boolean quarterMode) {
        if (this.statsOverlay != null) {
            this.statsOverlay.blockClicked(intersect);
        }
        int faceHit = intersect.face;
        BlockPos pos = intersect.blockPos;
        if (quarterMode) {
            faceHit |= 0x8;
            pos = new BlockPos();
            pos.set(intersect.blockPos);
            pos.x*=2;
            pos.y*=2;
            pos.z*=2;
            pos.x+=intersect.q.x;
            pos.y+=intersect.q.y;
            pos.z+=intersect.q.z;
        }
        sendPacket(new PacketCSetBlock(world.getId(), pos, intersect.pos, faceHit, this.selBlock.copy()));
    }

    /**
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    public boolean isInSelection(int ix, int iy, int iz) {
        return this.selection.contains(ix, iy, iz);
    }

    public Selection getSelection() {
        return this.selection;
    }

    public Gui getGui() {
        return this.gui;
    }
}
