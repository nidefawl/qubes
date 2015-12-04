package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.assets.AssetVoxModel;
import nidefawl.qubes.block.Block;
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
import nidefawl.qubes.input.*;
import nidefawl.qubes.item.Stack;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.logging.IErrorHandler;
import nidefawl.qubes.models.voxel.ModelVox;
import nidefawl.qubes.network.client.NetworkClient;
import nidefawl.qubes.network.client.ThreadConnect;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketCSetBlock;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.perf.TimingHelper2;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.*;
import nidefawl.qubes.util.*;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.*;

public class Game extends GameBase implements IErrorHandler {

    static public Game instance;
    public static boolean show    = false;
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
    public Movement        movement           = new Movement();
    public final Selection selection          = new Selection();
    public boolean         follow             = true;
    private float          lastCamX;
    private float          lastCamY;
    private float          lastCamZ;
    public Stack           selBlock           = new Stack(0);
    long                   lastShaderLoadTime = System.currentTimeMillis();
    float                  px, py, pz;

    public ArrayList<EditBlockTask> edits           = Lists.newArrayList();
    public int                      step            = 0;
    public boolean                  updateRenderers = true;
    private TesselatorState debugChunks;
    static boolean showGrid=false;
    public boolean thirdPerson = true;

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

    final float[] loadProgress = new float[4];
    @Override
    public void initGame() {
        debugChunks = new TesselatorState();
        loadSettings();
        selection.init();
        AssetManager.getInstance().init();
        Engine.init();
        loadRender(0, 0);
        TextureManager.getInstance().init();
        loadRender(0, 0.1f);
        BlockTextureArray.getInstance().init();
        loadRender(0, 0.2f);
        AssetTexture tex_map_grass = AssetManager.getInstance().loadPNGAsset("textures/colormap_grass.png");
        ColorMap.grass.set(tex_map_grass);
        loadRender(0, 0.4f);
        AssetTexture tex_map_foliage = AssetManager.getInstance().loadPNGAsset("textures/colormap_foliage.png");
        ColorMap.foliage.set(tex_map_foliage);
        loadRender(0, 0.6f);

        
        
        
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
        loadRender(0, 0.8f);
    }
    @Override
    public void loadRender(int step, float f) {
        loadRender(step, f, "");
    }
    public void loadRender(int step, float f, String string) {
        int tw = Game.displayWidth;
        int th = Game.displayHeight;
        int oldW = (int) (tw*0.6f*loadProgress[step]);
        loadProgress[step] = f;
        int newW = (int) (tw*0.6f*loadProgress[step]);
        if (oldW == newW) return;
        float x = 0;
        float y = 0;
        float l = tw*0.2f;
        float barh = 8;
        float barsH = (barh*1.2f) * (loadProgress.length-1);
        float barsTop = (th-barsH)/2.0f;
        if (isCloseRequested()) {
            shutdown();
            return;
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
        for (int i = 0; i < 3; i++) {
            p+=loadProgress[i];
        }
        p/=3f;
        float w = tw*0.6f*p;
        Tess.instance.setColor(0xffffff, 0xff);
        Tess.instance.add(x + l + w, y + barsTop-2, 0, 1, 1);
        Tess.instance.add(x + l, y + barsTop-2, 0, 0, 1);
        Tess.instance.add(x + l, y + barsTop+barsH+2, 0, 0, 0);
        Tess.instance.add(x + l + w, y + barsTop+barsH+2, 0, 1, 0);
        Tess.instance.drawQuads();
        FontRenderer font = FontRenderer.get("Arial", 16, 1, 18);
        if (font != null) {
            Shaders.textured.enable();
            font.drawString("Loading... "+string, x+l+2, y+barsTop+barsH+10+font.getLineHeight(), -1, true, 1.0f);
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
    }
    public void lateInitGame() {
        loadRender(0, 1f);
        BlockTextureArray.getInstance().reload();
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F8) {
            public void onDown() {
                setVSync(!getVSync());
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F9) {
            public void onDown() {
                Engine.toggleWireFrame();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F10) {
            public void onDown() {
//                Engine.flushRenderTasks();
//                Engine.regionRenderer.resetAll();
//                Engine.toggleDrawMode();
                BlockTextureArray.getInstance().reload();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_R) {
            public void onDown() {
                updateRenderers=!updateRenderers;
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_ENTER) {
            public void onDown() {
                if (world != null)
                    showGUI(new GuiChatInput());
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_T) {
            public void onDown() {
                if (world != null)
                    showGUI(new GuiChatInput());
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_Z) {
            public void onDown() {
                Engine.flushRenderTasks();
                Engine.regionRenderer.resetAll();
                Engine.USE_TRIANGLES=!Engine.USE_TRIANGLES;
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F3) {
            public void onDown() {
                show = !show;
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F4) {
            public void onDown() {
                toggleTestMode();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F) {
            public void onDown() {
                reposModel();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F5) {
            public void onDown() {
//              setWorld(new WorldClient(null));
//              Engine.worldRenderer.flush();
//              Engine.textures.refreshNoiseTextures();
//              PlayerSelf p = getPlayer();
//              if (p != null) {
//                  Random rand = new Random();
//                  p.move(rand.nextInt(300)-150, rand.nextInt(100)+100, rand.nextInt(300)-150);
//              }
                lastShaderLoadTime = System.currentTimeMillis();
                thirdPerson = !thirdPerson;
//                Shaders.initShaders();
//                Engine.worldRenderer.initShaders();
////                Engine.worldRenderer.reloadModel();
////                  Engine.regionRenderer.initShaders();
////                  Engine.shadowRenderer.initShaders();
//                  Engine.outRenderer.initShaders();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F2) {
            public void onDown() {
                showGrid = !showGrid;
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F1) {
            public void onDown() {
                Engine.regionRenderer.reRender();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F11) {
            public void onDown() {
                toggleTiming = true;
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F12) {
            public void onDown() {
                TimingHelper.dump();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_F6) {
            public void onDown() {
                TimingHelper2.dump();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_Q) {
            public void onDown() {
                selection.quarterMode = !selection.quarterMode;
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_ESCAPE) {
            public void onDown() {
                if (world != null) {
                    setWorld(null);
                }
                if (client != null) {
                    client.disconnect("Quit");
                }
                if (gui == null)
                    showGUI(new GuiMainMenu());
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_N) {
            public void onDown() {
                showGUI(new GuiSelectWorld());
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_B) {
            public void onDown() {
                showGUI(new GuiSelectBlock());
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_U) {
            public void onDown() {
                edits.clear();
                step = 0;
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_I) {
            public void onDown() {
                if (step-1 >= 0) {
                    step--;
                    if (edits.size()>step)
                    edits.get(step).apply(world);
                }
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_O) {
            public void onDown() {
//                if (step+1 < edits.size()) {
//                    step++;
//                    edits.get(step).apply(world);
//                }
                if (player != null)
                    world.addLight(new Vector3f(player.pos).translate(0, 1, 0));
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_H) {
            public void onDown() {
                if (player != null) {
                    player.toggleFly();
                }
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_KP_SUBTRACT) {
            public void onDown() {
                if (world != null)
                for (int i = 0; i < 22; i++) {

                    world.removeLight(0);
                }
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_KP_ADD) {
            public void onDown() {

                Engine.worldRenderer.reloadModel();
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_KP_MULTIPLY) {
            public void onDown() {
                if (player != null)
                    world.spawnLights(player.pos.toBlock());
            }
        });
        Keyboard.addKeyBinding(new Keybinding(GLFW.GLFW_KEY_M) {
            public void onDown() {
                selection.toggleMode();
            }
        });
        ChatManager.getInstance().loadInputHistory();
        
    }
    boolean testMode = false;
    PlayerSelf remotePlayer;
    PlayerSelf testPlayer;
    WorldClient remoteWorld;
    WorldClient testWorld;
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
    protected void reposModel() {
        int x = GameMath.floor(lastCamX);
        int y = GameMath.floor(lastCamY)-1;
        int z = GameMath.floor(lastCamZ);
        Engine.worldRenderer.setModelPos((float)x, (float)y, (float)z);
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
    }
    
    protected void onTextInput(long window, int codepoint) {
        Keybinding k = Keyboard.getKeyBinding(codepoint);
        if (k != null && k.isEnabled() && k.isPressed()) {
            return;
        }
        if (this.gui != null) {
            if (this.gui.onTextInput(codepoint)) {
                return;
            }
        }
    }
    int skipChars = 0;
    @Override
    protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
        if (window == windowId) {
          Keybinding k = Keyboard.getKeyBinding(key);
          if (k != null && k.isEnabled() && k.isPressed()) {
              k.update(action);
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
//            this.gui.onMouseClick(button, action);
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
                for (int b = 0; this.selBlock.id< 0&&b < Block.HIGHEST_BLOCK_ID+1; b++) {
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
            this.gui.onMouseClick(button, action);
        } else {


            boolean b = Mouse.isGrabbed();
            boolean isDown = Mouse.getState(action);
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
                selection.resetSelection();
            }
        }
    }

    @Override
    public void input(float fTime) {
        double mdX = Mouse.getDX();
        double mdY = Mouse.getDY();
        this.movement.update(mdX, -mdY);
    }

    public void setGrabbed(boolean b) {
        this.movement.setGrabbed(b);
        Mouse.setCursorPosition(displayWidth / 2, displayHeight / 2);
        Mouse.setGrabbed(b);
    }



    public void render(float fTime) {
        //      fogColor.scale(0.4F);
        if (Game.DO_TIMING)
            TimingHelper.startSec("world");
        if (this.world != null) {
            glDisable(GL_BLEND);
            if (Game.DO_TIMING)
                TimingHelper.startSec("ShadowPass");
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("renderShadowPass");
            Engine.shadowRenderer.renderShadowPass(this.world, fTime);
            Engine.getSceneFB().bind();
            Engine.getSceneFB().clearFrameBuffer();

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            if (Game.DO_TIMING)
                TimingHelper.endStart("renderWorld");

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("renderWorld");
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            Engine.worldRenderer.renderWorld(this.world, fTime);
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("renderWorld");

            if (Game.DO_TIMING)
                TimingHelper.endStart("unbindCurrentFrameBuffer");

            FrameBuffer.unbindFramebuffer();
            if (Game.DO_TIMING)
                TimingHelper.endSec();
            if (Game.DO_TIMING)
                TimingHelper.endSec();
        }
        if (Game.DO_TIMING)
            TimingHelper.startSec("screen");
        if (Game.DO_TIMING)
            TimingHelper.startSec("prepare");

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("glClear");
        glDisable(GL_BLEND);
        
        glClearColor(0.71F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

        if (this.world != null) {
            if (Game.DO_TIMING)
                TimingHelper.endStart("final");

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("HBAO");

            if (getVendor() != GPUVendor.INTEL) {
                HBAOPlus.renderAO();
            }

            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("GLNativeLib.renderAO");
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            if (show) {
                GuiOverlayDebug dbg = Game.instance.debugOverlay;
                dbg.preDbgFB(true);
                dbg.drawDebug();
                dbg.postDbgFB();
            }
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("Deferred Pre0");
            Engine.outRenderer.bindFB();
            Engine.outRenderer.clearFrameBuffer();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("Deferred Pass0");
            Engine.outRenderer.render(this.world, fTime, 0);
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("TransparentPass");

            boolean secondPass = true;
            if (secondPass) {
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
                GPUProfiler.start("Deferred ReflAndBlur");

            glDisable(GL_BLEND); // don't blend ssr
            Engine.outRenderer.renderReflAndBlur(this.world, fTime);

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("renderFinal");
            Engine.outRenderer.renderFinal(this.world, fTime);
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();

            
            boolean pass = true;//Engine.renderWireFrame || !Engine.worldRenderer.debugBBs.isEmpty();
            if (pass) {
                glDisable(GL_CULL_FACE);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("forwardPass");

                if (Game.DO_TIMING)
                    TimingHelper.endStart("forwardPass");

                Engine.getSceneFB().bindRead();
                //             
                GL30.glBlitFramebuffer(0, 0, displayWidth, displayHeight, 0, 0, displayWidth, displayHeight, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
                FrameBuffer.unbindReadFramebuffer();

                if (!Engine.worldRenderer.debugBBs.isEmpty()) {

                    if (Game.DO_TIMING)
                        TimingHelper.startSec("renderDebugBB");

                    if (GPUProfiler.PROFILING_ENABLED)
                        GPUProfiler.start("renderDebugBB");
                    Engine.worldRenderer.renderDebugBB(this.world, fTime);
                    if (GPUProfiler.PROFILING_ENABLED)
                        GPUProfiler.end();

                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("renderDebugBB");
                    if (Game.DO_TIMING)
                        TimingHelper.endSec();
                }

                if (Engine.renderWireFrame) {
                    if (Game.DO_TIMING)
                        TimingHelper.startSec("renderNormals");
                    Engine.worldRenderer.renderNormals(this.world, fTime);
                    glEnable(GL_POLYGON_OFFSET_FILL);
                    glPolygonOffset(-3.4f, 2.f);
                    Engine.worldRenderer.renderTerrainWireFrame(this.world, fTime);
                    glDisable(GL_POLYGON_OFFSET_FILL);
                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("renderNormals");
                    if (Game.DO_TIMING)
                        TimingHelper.endSec();
                }
                if (Game.DO_TIMING)
                    TimingHelper.startSec("renderBlockHighlight");
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.start("renderBlockHighlight");
                selection.renderBlockHighlight(this.world, fTime);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();

                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("renderBlockHighlight");
                if (Game.DO_TIMING)
                    TimingHelper.endSec();
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
                if (Game.DO_TIMING)
                    TimingHelper.endSec();
                glEnable(GL_CULL_FACE);
                glDisable(GL_BLEND);
            }
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

        if (Game.DO_TIMING)
            TimingHelper.endStart("gui");

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("gui");

        if (this.world != null && this.gui == null && this.movement.grabbed()) {
            Shaders.colored.enable();
            if (Game.DO_TIMING)
                TimingHelper.startSec("crosshair");
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
            if (Game.DO_TIMING)
                TimingHelper.endSec();
            Shader.disable();
        }

        if (this.gui != null) {
            this.gui.render(fTime, Mouse.getX(), Mouse.getY());
        } else if (this.world != null) {

            if (showGrid) {
                int ipX = GameMath.floor(px);
                int ipY = GameMath.floor(py);
                int ipZ = GameMath.floor(pz);
                int icX = ipX >> 4;
                int icZ = ipZ >> 4;
                int lenOver = RegionRenderer.LENGTH_OVER;
                lenOver += 8;
                int chunksW = lenOver * RegionRenderer.REGION_SIZE;
                int chunkHalfW = ((lenOver - 1) / 2) * RegionRenderer.REGION_SIZE;
//                int minChunkX = (ipX) - chunkW;
//                int minChunkZ = (ipZ) - chunkW;
//                int maxChunkX = (ipX) + chunkW;
//                int maxChunkZ = (ipZ) + chunkW;
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
            if (show) {
                if (Game.DO_TIMING)
                    TimingHelper.startSec("debugOverlay");
                if (this.debugOverlay != null) {
                    this.debugOverlay.render(fTime, 0, 0);
                }
                if (Game.DO_TIMING)
                    TimingHelper.endSec();
            }
            if (this.chatOverlay != null) {
                this.chatOverlay.render(fTime, 0, 0);
            }
            if (Game.DO_TIMING)
                TimingHelper.startSec("statsOverlay");

            if (this.statsCached != null) {
                this.statsCached.render(fTime, 0, 0);
            }
            if (Game.DO_TIMING)
                TimingHelper.endSec();
        }

        if (Game.DO_TIMING)
            TimingHelper.endSec();
        glEnable(GL_DEPTH_TEST);

        if (Game.DO_TIMING)
            TimingHelper.endSec();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();

    }

    @Override
    public void onStatsUpdated() {
        if (this.statsCached != null) {
            this.statsCached.refresh();
        }
        if (System.currentTimeMillis()-lastShaderLoadTime > 4000/* && Keyboard.isKeyDown(GLFW.GLFW_KEY_F9)*/) {
//          System.out.println("initShaders");
          lastShaderLoadTime = System.currentTimeMillis();
//          Shaders.initShaders();
//          Engine.worldRenderer.initShaders();
//          Engine.worldRenderer.reloadModel();
//            Engine.regionRenderer.initShaders();
//            Engine.shadowRenderer.initShaders();
//            Engine.outRenderer.initShaders();
//            
//          BlockTextureArray.getInstance().reloadTexture("log.png");
//          BlockTextureArray.getInstance().reloadTexture("log_top.png");
//          BlockTextureArray.getInstance().reloadTexture("tallgrass.png");
//          BlockTextureArray.getInstance().reloadTexture("dirt.png");
//          BlockTextureArray.getInstance().reloadTexture("stone.png");
//          BlockTextureArray.getInstance().reloadTexture("grass_side.png");
//          BlockTextureArray.getInstance().reloadTexture("grass_side_overlay.png");
//          Engine.textures.refreshNoiseTextures();
//          AssetTexture tex_map_grass = AssetManager.getInstance().loadPNGAsset("textures/colormap_grass.png");
//          ColorMap.grass.set(tex_map_grass);
//          AssetTexture tex_map_foliage = AssetManager.getInstance().loadPNGAsset("textures/colormap_foliage.png");
//          ColorMap.foliage.set(tex_map_foliage);
        }
    }

    @Override
    public void postRenderUpdate(float f) {
        Engine.worldRenderer.rendered = Engine.regionRenderer.rendered;
    }
    @Override
    public void preRenderUpdate(float f) {
        Engine.regionRenderer.rendered = 0;
        if (this.client != null) {
            String reason = this.client.getClient().getDisconnectReason();
            if (reason!=null) {
                setWorld(null);
                setPlayer(null);
                this.client = null;
                showGUI(new GuiDisconnected(reason));
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
            player.updateInputDirect(movement);
            float yaw = player.yaw;
            float pitch = player.pitch;
            Engine.camera.setOrientation(yaw, pitch, settings.thirdpersonDistance);

            if (follow) {
                lastCamX = px;
                lastCamY = py;
                lastCamZ = pz;
            }
            
            px = (float) (player.lastPos.x + (player.pos.x - player.lastPos.x) * f) + 0;
            py = (float) (player.lastPos.y + (player.pos.y - player.lastPos.y) * f) + 1.62F;
            pz = (float) (player.lastPos.z + (player.pos.z - player.lastPos.z) * f) + 0;
            if (thirdPerson) {
                Vector3f camPos = Engine.camera.getThirdPersonOffset();
                px+=camPos.x;
                py+=camPos.y;
                pz+=camPos.z;
            }
            
            if (this.world.lights.size() > 0) {
                DynamicLight l = this.world.lights.get(0);
                l.loc.x = px;
                l.loc.y = py;
                l.loc.z = pz;
//                int colorI = Color.HSBtoRGB((ticksran+f)/60, 1, 1);
//                l.intensity = 0.4F;
//                l.color = new Vector3f(1*l.intensity);
//                l.color.x = (float) (((colorI>>16)&0xFF) / 255.0F * l.intensity);
//                l.color.y = (float) (((colorI>>8)&0xFF) / 255.0F * l.intensity);
//                l.color.z = (float) (((colorI>>0)&0xFF) / 255.0F * l.intensity);
            }
          Engine.camera.setPosition(px, py, pz);
//          if (Engine.worldRenderer.qmodel!=null)
//          Engine.worldRenderer.qmodel.setHeadOrientation(yaw, pitch);
        }

        Engine.setLightPosition(this.world.getLightPosition());
        Engine.updateCamera();
        Engine.updateShadowProjections(f);
        UniformBuffer.updateUBO(this.world, f);
        if (player != null) {
            float winX, winY;

            if (this.movement.grabbed()) {
                winX = (float) displayWidth/2.0F;
                winY = (float) displayHeight/2.0F;
            } else {
                winX = (float) Mouse.getX();
                winY = (float) (displayHeight-Mouse.getY());
                if (winX < 0) winX = 0; if (winX > displayWidth) winX = 1;
                if (winY < 0) winY = 0; if (winY > displayHeight) winY = 1;
            }
            if (this.gui == null) {
                Engine.updateMouseOverView(winX, winY);
                selection.update(world, px, py, pz);
            }
        }
        
        if (this.world != null && updateRenderers) {
            float renderRegionX = follow ? px : lastCamX;
//            float renderRegionY = follow ? py : lastCamY;
            float renderRegionZ = follow ? pz : lastCamZ;
            int xPosP = GameMath.floor(renderRegionX)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
            int zPosP = GameMath.floor(renderRegionZ)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
            if (Engine.updateRenderOffset) {
                Engine.regionRenderer.reRender();
            }
            Engine.regionRenderer.update(this.world, px, py, pz, xPosP, zPosP, f);
        }
    }
    boolean reinittexthook = false;
    boolean wasGrabbed = false;
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
        }
    }
    @Override
    public void tick() {
       if (this.world != null)
           this.world.tickUpdate();
       if (this.gui != null) {
           this.gui.update();
       }
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
}
