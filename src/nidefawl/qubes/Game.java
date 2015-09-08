package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.*;
import nidefawl.qubes.input.*;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.logging.IErrorHandler;
import nidefawl.qubes.network.client.NetworkClient;
import nidefawl.qubes.network.client.ThreadConnect;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketCSetBlock;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.perf.TimingHelper2;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.WorldClient;
import nidefawl.qubes.world.World;

public class Game extends GameBase implements IErrorHandler {


    public static boolean  show               = false;
    
    long         lastClickTime = System.currentTimeMillis() - 5000L;
    private long lastTimeLoad  = System.currentTimeMillis();

    public GuiOverlayStats statsOverlay;
    public GuiCached statsCached;
    public GuiOverlayDebug debugOverlay;
    private Gui            gui;

    FontRenderer       fontSmall;
    public Movement  movement           = new Movement();
    WorldClient      world              = null;
    public boolean   follow             = true;
    private float      lastCamX;
    private float      lastCamY;
    private float      lastCamZ;
    private RayTrace   rayTrace;
    public int         selBlock           = 0;
    long               lastShaderLoadTime = System.currentTimeMillis();
    private boolean    doLoad             = true;
    PlayerProfile profile = new PlayerProfile();
    private ThreadConnect connect;

    private NetworkClient client;
    float px, py, pz;
    PlayerSelf player;

    public ArrayList<EditBlockTask> edits = Lists.newArrayList();
    public int step = 0;

    private boolean updateRenderers = true;

    static public Game instance;
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
//        TimingHelper.setName(0, "Final_Prepare");
//        TimingHelper.setName(1, "Final_Stage0");
//        TimingHelper.setName(2, "Final_Stage1");
//        TimingHelper.setName(3, "Final_Stage2");
////        TimingHelper.setName(4, "Final_Stage3");
//        TimingHelper.setName(5, "Final_StageLast");
//        TimingHelper.setName(6, "RenderScene");
//        TimingHelper.setName(7, "PreFinalStage");
//        TimingHelper.setName(8, "PostFinalStage");
//        TimingHelper.setName(9, "GUIStats");
//        TimingHelper.setName(10, "UpdateTimer");
//        TimingHelper.setName(11, "PreRenderUpdate");
//        TimingHelper.setName(12, "Input");
//        TimingHelper.setName(13, "EngineUpdate");
//        TimingHelper.setName(15, "DisplayUpdate");
//        TimingHelper.setName(16, "CalcFPS");
//        TimingHelper.setName(17, "Final_Stage0to1Mipmap");
//        TimingHelper.setName(18, "Final_Stage1to2Mipmap");
    }

    @Override
    public void initGame() {
        AssetManager.getInstance().init();
        Engine.init();
        TextureManager.getInstance().init();
        BlockTextureArray.getInstance().init();
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
    }
    public void lateInitGame() {
        BlockTextureArray.getInstance().reload();
//        setWorld(new WorldClient(null));
//        this.entSelf.move(-800, 222, 1540);
//        this.entSelf.yaw=6.72F;
//        this.entSelf.pitch=38.50F;
//        this.entSelf.move(-870.42F, 103.92F-1.3F, 1474.25F);
//        this.entSelf.toggleFly();
    }
    public void setWorld(WorldClient world) {
        if (this.world != null) {
            this.world.onLeave();
            Engine.flushRenderTasks();
            Engine.regionRenderer.flush();
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
    }

    protected void onTextInput(long window, int codepoint) {
        if (this.gui != null) {
            if (this.gui.onTextInput(codepoint)) {
                return;
            }
        }
    }
    @Override
    protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
        if (window == windowId) {

            if (this.gui != null) {
                if (this.gui.onKeyPress(key, scancode, action, mods)) {
                    return;
                }
            }
            boolean isDown = Keyboard.getState(action);
            switch (key) {
                case Keyboard.KEY_F8:
                    if (isDown) {
                        setVSync(!getVSync());
                    }
                    break;
                case Keyboard.KEY_F9:
                    if (isDown) {
                        Engine.toggleWireFrame();
                    }
                    break;
                case Keyboard.KEY_R:
                    if (isDown) {
                        updateRenderers=!updateRenderers;
                    }
                    break;
                case Keyboard.KEY_F3:
                    if (isDown) {
                        show = !show;
                    }
                    break;
                case Keyboard.KEY_F5:
                    if (isDown) {
//                        setWorld(new WorldClient(null));
//                        Engine.worldRenderer.flush();
//                        Engine.textures.refreshNoiseTextures();
                        PlayerSelf p = getPlayer();
                        if (p != null) {
                            Random rand = new Random();
                            p.move(rand.nextInt(300)-150, rand.nextInt(100)+100, rand.nextInt(300)-150);
                        }
                    }
                    break;
                case Keyboard.KEY_F2:
                    if (isDown) {
                        this.follow = !this.follow;
                    }
                    break;
                case Keyboard.KEY_F1:
                    if (isDown) {
                        Engine.regionRenderer.reRender();
                    }
                    break;
                case Keyboard.KEY_F11:
                    if (isDown) {
                        toggleTiming = true;
                    }
                    break;
                case Keyboard.KEY_F12:
                    if (isDown) {
                        TimingHelper.dump();
                    }
                    break;
                case Keyboard.KEY_F6:
                    if (isDown) {
                        TimingHelper2.dump();
                    }
                    break;
                case Keyboard.KEY_ESCAPE:
                    if (this.world != null) {
                        setWorld(null);
                    }
                    if (this.client != null) {
                        this.client.disconnect("Quit");
                    }
                    if (this.gui == null)
                        showGUI(new GuiMainMenu());
                    break;
            }
            if (this.world != null) {

                switch (key) {
                    case Keyboard.KEY_U:
                        if (isDown) {
                            this.edits.clear();
                            step = 0;
                        }
                        break;
                    case Keyboard.KEY_I:
                        if (isDown) {
                            if (step-1 >= 0) {
                                step--;
                                if (edits.size()>step)
                                edits.get(step).apply(world);
                            }
                        }
                        break;
                    case Keyboard.KEY_O:
                        if (isDown) {
                            if (step+1 < edits.size()) {
                                step++;
                                edits.get(step).apply(world);
                            }
                        }
                        break;
                    case Keyboard.KEY_H:
                        if (isDown & this.player != null) {
                            this.player.toggleFly();
                        }
                        break;
                    case Keyboard.KEY_KP_SUBTRACT:
                        if (isDown)
                            for (int i = 0; i < 22; i++) {

                                this.world.removeLight(0);
                            }
                        break;
                    case Keyboard.KEY_KP_ADD:
                        if (isDown && this.player != null)
                            this.world.addLight(new Vector3f(this.player.pos).translate(0, 1, 0));
                        break;
                    case Keyboard.KEY_KP_MULTIPLY:
                        if (isDown && this.player != null)
                            this.world.spawnLights(this.player.pos.toBlock());
                        break;
                }
            }
            if (key == Keyboard.KEY_0) {
                this.selBlock = 0;
            } else if (key >= Keyboard.KEY_1 && key <= Keyboard.KEY_9) {
                this.selBlock = key - Keyboard.KEY_1;
                if (!Block.isValid(this.selBlock)) {
                    this.selBlock = 0;
                }
            }
        }
    }
    public void onMouseClick(long window, int button, int action, int mods) {
        if (this.gui != null) {
            this.gui.onMouseClick(button, action);
        } else {


            boolean b = Mouse.isGrabbed();
            boolean isDown = Mouse.getState(action);
            long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
            switch (button) {
                case 0:
                    Engine.selection.clicked(button, isDown);
                    break;
                case 1:
                    if (isDown ) {
                        setGrabbed(!b);
                        b = !b;
                    }
                    break;
            }
            if (b != this.movement.grabbed()) {
                setGrabbed(b);
                Engine.selection.resetSelection();
            }
        }
    }

    @Override
    public void input(float fTime) {
        double mdX = Mouse.getDX();
        double mdY = Mouse.getDY();
        this.movement.update(mdX, -mdY);
    }

    private void setGrabbed(boolean b) {
        this.movement.setGrabbed(b);
        Mouse.setCursorPosition(displayWidth / 2, displayHeight / 2);
        Mouse.setGrabbed(b);
    }



    public void render(float fTime) {
//      fogColor.scale(0.4F);

        if (Game.DO_TIMING)
            TimingHelper.startSec("world");
        if (this.world != null) {
            if (Game.DO_TIMING)
                TimingHelper.startSec("ShadowPass");
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("renderShadowPass");
            Engine.shadowRenderer.renderShadowPass(this.world, fTime);
            glEnable(GL_CULL_FACE);
            Engine.getSceneFB().bind();
            Engine.getSceneFB().clearFrameBuffer();
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            
            if (Game.DO_TIMING)
                TimingHelper.endStart("renderWorld");
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("renderWorld");
            glDisable(GL_CULL_FACE);
            Engine.worldRenderer.renderWorld(this.world, fTime);
            glEnable(GL_CULL_FACE);
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
            
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("renderWorld");
            
            if (Game.DO_TIMING)
                TimingHelper.endStart("renderBlockHighlight");
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("renderBlockHighlight");
                Engine.selection.renderBlockHighlight(this.world, fTime);
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
            
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("renderBlockHighlight");
            
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

      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("glClear");
      glClearColor(0.71F, 0.82F, 1.00F, 1F);
      glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
      

      glActiveTexture(GL_TEXTURE0);
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("bindOrthoUBO");
      UniformBuffer.bindOrthoUBO();
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
      if (this.world != null) {
//        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
          glDisable(GL_BLEND);
          glEnable(GL_DEPTH_TEST);
          glDepthFunc(GL_ALWAYS);
//          glEnable(GL_TEXTURE_2D);
          if (Game.DO_TIMING) TimingHelper.endStart("final");

          glDepthMask(false);
          Engine.outRenderer.render(this.world, fTime);
          if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("renderFinal");
          Engine.outRenderer.renderFinal(this.world, fTime);
          if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
          glDepthMask(true);
          glDepthFunc(GL_LEQUAL);
          glEnable(GL_BLEND);
          glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
          boolean pass = Engine.renderWireFrame || !Engine.worldRenderer.debugBBs.isEmpty();
          if (pass) {
              if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("forwardPass");

              glDisable(GL_CULL_FACE);
              if (Game.DO_TIMING) TimingHelper.endStart("forwardPass");
              Engine.getSceneFB().bindRead();
              GL30.glBlitFramebuffer(0, 0, displayWidth, displayHeight, 0, 0, displayWidth, displayHeight, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
              FrameBuffer.unbindReadFramebuffer();
              UniformBuffer.bindProjUBO();
              
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
                    glPolygonOffset(-1.1f, 2.f);
                    Engine.worldRenderer.renderTerrainWireFrame(this.world, fTime);
                    glDisable(GL_POLYGON_OFFSET_FILL);
                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("renderNormals");
                    if (Game.DO_TIMING)
                        TimingHelper.endSec();
              }
              UniformBuffer.bindOrthoUBO();
              glEnable(GL_CULL_FACE);
              if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
          }
          glDisable(GL_DEPTH_TEST);
      } else {

//          glDepthFunc(GL_LEQUAL);
          glEnable(GL_BLEND);
          glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//          glActiveTexture(GL_TEXTURE0);
      }

      

      if (Game.DO_TIMING) TimingHelper.endStart("gui");
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("gui");
      
      if (this.world != null && this.gui == null && this.movement.grabbed()) {
          Shaders.colored.enable();
          if (Game.DO_TIMING) TimingHelper.startSec("crosshair");
          glDisable(GL_TEXTURE_2D);
          Tess.instance.setColor(-1, 100);
          Tess.instance.setOffset(displayWidth/2, displayHeight/2, 0);
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
          glEnable(GL_TEXTURE_2D);
          if (Game.DO_TIMING) TimingHelper.endSec();
          Shader.disable();
      }
//      Shaders.textured.enable();
//      glBindTexture(GL_TEXTURE_2D, Engine.shadowRenderer.getDebugTexture());
//      Engine.drawFullscreenQuad();
//      Shader.disable();

      if (this.gui != null) {
          this.gui.render(fTime, Mouse.getX(), Mouse.getY());
      } else if (this.world != null) {

          if (show) {
              if (Game.DO_TIMING) TimingHelper.startSec("debugOverlay");
              if (this.debugOverlay != null) {
                  this.debugOverlay.render(fTime, 0, 0);
              }
              if (Game.DO_TIMING) TimingHelper.endSec();
          }
          if (Game.DO_TIMING) TimingHelper.startSec("statsOverlay");

          if (this.statsCached != null) {
              this.statsCached.render(fTime, 0, 0);
          }
      }
      if (Game.DO_TIMING) TimingHelper.endSec();
      
      if (Game.DO_TIMING) TimingHelper.endSec();
      glEnable(GL_DEPTH_TEST);

      if (Game.DO_TIMING) TimingHelper.endSec();
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
      
  }

    @Override
    public void onStatsUpdated() {
        if (this.statsCached != null) {
            this.statsCached.refresh();
        }
        if (System.currentTimeMillis()-lastShaderLoadTime > 2200/* && Keyboard.isKeyDown(Keyboard.KEY_F9)*/) {
            lastShaderLoadTime = System.currentTimeMillis();
            Shaders.initShaders();
            Engine.worldRenderer.initShaders();
            Engine.shadowRenderer.initShaders();
            Engine.outRenderer.initShaders();
//            Engine.textures.refreshNoiseTextures();
        }
    }

    @Override
    public void postRenderUpdate(float f) {
    }
    @Override
    public void preRenderUpdate(float f) {
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
//          float sinY = GameMath.sin(GameMath.degreesToRadians(entSelf.yaw));
//          float cosY = GameMath.cos(GameMath.degreesToRadians(entSelf.yaw));
//          float forward = 1;
//          float strafe = 0;
//          float fx = -forward * sinY + strafe * cosY;
//          float fz = forward * cosY + strafe * sinY;
            if (follow) {
                lastCamX = px;
                lastCamY = py;
                lastCamZ = pz;
            }
            px = (float) (player.lastPos.x + (player.pos.x - player.lastPos.x) * f) + 0;
            py = (float) (player.lastPos.y + (player.pos.y - player.lastPos.y) * f) + 1.62F;
            pz = (float) (player.lastPos.z + (player.pos.z - player.lastPos.z) * f) + 0;
            if (this.world.lights.size() > 0) {
                DynamicLight l = this.world.lights.get(0);
                l.loc.x = px;
                l.loc.y = py;
                l.loc.z = pz;
//                int colorI = Color.HSBtoRGB((ticksran+f)/60, 1, 1);
                l.intensity = 7.6F;
                l.color = new Vector3f(1*l.intensity);
//                l.color.x = (float) (((colorI>>16)&0xFF) / 255.0F * l.intensity);
//                l.color.y = (float) (((colorI>>8)&0xFF) / 255.0F * l.intensity);
//                l.color.z = (float) (((colorI>>0)&0xFF) / 255.0F * l.intensity);
            }
          float yaw = player.yaw;
          float pitch = player.pitch;
          Engine.camera.setPosition(px, py, pz);
          Engine.camera.setOrientation(yaw, pitch);       
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
            Engine.updateMouseOverView(winX, winY);
            Engine.selection.update(world, px, py, pz);
        }
        
        if (this.world != null && updateRenderers) {
//            Engine.regionLoader.finishTasks();
            float renderRegionX = follow ? px : lastCamX;
//            float renderRegionY = follow ? py : lastCamY;
            float renderRegionZ = follow ? pz : lastCamZ;
            int xPosP = GameMath.floor(renderRegionX)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
            int zPosP = GameMath.floor(renderRegionZ)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
            int xPosC = GameMath.floor(renderRegionX)>>(Chunk.SIZE_BITS);
            int zPosC = GameMath.floor(renderRegionZ)>>(Chunk.SIZE_BITS);
            if (doLoad && System.currentTimeMillis() >= lastTimeLoad) {
                int halflen = (RegionRenderer.RENDER_DISTANCE+1)*RegionRenderer.REGION_SIZE;
//                world.getChunkManager().ensureLoaded(xPosC, zPosC, halflen);
//                world.getChunkManager().unloadUnused(xPosC, zPosC, halflen+2);
//                int i = Engine.regionLoader.updateRegions(xPosP, zPosP, follow);
//                if (i != 0) {
//                    System.out.println("Queued "+i+" regions for load");
//                }
                lastTimeLoad += 122L;
            }
            //HACKY
//            int nRegions = 0;
            Engine.regionRenderer.update(this.world, px, py, pz, xPosP, zPosP, f);
//            if (!startRender)
//            startRender = nRegions > 4;
        }
//        boolean releaseMouse = !this.mouseClicked || (Keyboard.isKeyDown(Keyboard.KEY_LEFT_CONTROL) ? !Mouse.isButtonDown(0) : true);
//                
//        if (releaseMouse) {
//        }
    }
    
    public void showGUI(Gui gui) {

        if (this.gui != null) {
            this.gui.onClose();

        }
        this.gui = gui;
        if (this.gui != null) {

            this.gui.setPos(0, 0);
            this.gui.setSize(displayWidth, displayHeight);
            this.gui.initGui(this.gui.firstOpen);
            this.gui.firstOpen = false;
            if (Mouse.isGrabbed())
                setGrabbed(false);
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
        }
    }
    @Override
    public void tick() {
       if (this.world != null)
           this.world.tickUpdate();
       if (this.gui != null) {
           this.gui.update();
       }
//       if (this.connect == null && this.client != null && !this.client.isConnected() && this.world != null) {
//           this.setWorld(null);
//           showGUI(new GuiMainMenu());
//       }
//        matrixSetupMode = Main.ticksran%100<50;
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
}
