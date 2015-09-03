package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import nidefawl.game.*;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.*;
import nidefawl.qubes.input.Keyboard;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.input.Movement;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.perf.TimingHelper2;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.ClientWorld;
import nidefawl.qubes.world.Light;
import nidefawl.qubes.world.World;

public class Game extends GameBase {


    public static boolean  show               = false;
    
    long         lastClickTime = System.currentTimeMillis() - 5000L;
    private long lastTimeLoad  = System.currentTimeMillis();

    public GuiOverlayStats statsOverlay;
    public Gui statsCached;
    public GuiOverlayDebug debugOverlay;
    private Gui            gui;

    FontRenderer       fontSmall;
    final PlayerSelf entSelf            = new PlayerSelf(1);
    public Movement  movement           = new Movement();
    ClientWorld      world              = null;
    public boolean   follow             = true;
    private float      lastCamX;
    private float      lastCamY;
    private float      lastCamZ;
    private RayTrace   rayTrace;
    public int         selBlock           = 0;
    long               lastShaderLoadTime = System.currentTimeMillis();
    private boolean    doLoad             = true;
    
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
        setWorld(new ClientWorld(1, 0x1234, Engine.regionLoader));
        this.entSelf.move(-800, 222, 1540);
        this.entSelf.yaw=6.72F;
        this.entSelf.pitch=38.50F;
//        this.entSelf.move(-870.42F, 103.92F-1.3F, 1474.25F);
//        this.entSelf.toggleFly();
    }
    public void setWorld(ClientWorld world) {
        if (this.world != null) {
            this.world.onLeave();
        }
        this.world = world;
        this.world.addEntity(this.entSelf);
    }
    @Override
    protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
        if (window == windowId) {
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
                case Keyboard.KEY_F3:
                    if (isDown) {
                        show = !show;
                    }
                    break;
                case Keyboard.KEY_F5:
                    if (isDown) {
                        Engine.flushRenderTasks();
                        Engine.regionRenderThread.flush();
                        Engine.regionLoader.flush();
                        Engine.regionRenderer.flush();
                        int nId = 1;
                        if (this.world != null) {
                            nId = this.world.worldId+1;
                        }
                        setWorld(new ClientWorld(nId, 0x123, Engine.regionLoader));
//                        Engine.worldRenderer.flush();
//                        Engine.textures.refreshNoiseTextures();
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
                    shutdown();
                    break;
                case Keyboard.KEY_H:
                    if (isDown) {
                        this.entSelf.toggleFly();
                    }
                    break;
                case Keyboard.KEY_KP_SUBTRACT:
                    if (isDown)
                        for (int i = 0; i < 22; i++) {

                            this.world.removeLight(0);
                        }
                    break;
                case Keyboard.KEY_KP_ADD:
                    if (isDown)
                    this.world.addLight(new Vector3f(entSelf.pos).translate(0,1,0));
                    break;
                case Keyboard.KEY_KP_MULTIPLY:
                    if (isDown)
                    this.world.spawnLights(entSelf.pos.toBlock());
                    break;
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
        Engine.worldRenderer.renderWorld(this.world, fTime);
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
            TimingHelper.endStart("renderDebugBB");

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("renderDebugBB");
        Engine.worldRenderer.renderDebugBB(this.world, fTime);
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderDebugBB");
        
        if (Game.DO_TIMING)
            TimingHelper.endStart("unbindCurrentFrameBuffer");

        FrameBuffer.unbindFramebuffer();
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        if (Game.DO_TIMING)
            TimingHelper.startSec("screen");
        if (Game.DO_TIMING)
            TimingHelper.startSec("prepare");

      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("glClear");
      glClearColor(0.71F, 0.82F, 1.00F, 1F);
      glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
//      glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      glDisable(GL_BLEND);
      glEnable(GL_DEPTH_TEST);
      glDepthFunc(GL_ALWAYS);
//      glEnable(GL_TEXTURE_2D);
      glActiveTexture(GL_TEXTURE0);
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
      if (Game.DO_TIMING) TimingHelper.endStart("final");

      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("bindOrthoUBO");
      UniformBuffer.bindOrthoUBO();
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
      glDepthMask(false);
      Engine.outRenderer.render(this.world, fTime);
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("renderFinal");
      Engine.outRenderer.renderFinal(this.world, fTime);
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("renderWireFrame");
      glDepthMask(true);
      glDepthFunc(GL_LEQUAL);
      glEnable(GL_BLEND);
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
      if (Engine.renderWireFrame) {
          glDisable(GL_CULL_FACE);
          if (Game.DO_TIMING) TimingHelper.endStart("renderWireFrame");
          Engine.getSceneFB().bindRead();
          GL30.glBlitFramebuffer(0, 0, displayWidth, displayHeight, 0, 0, displayWidth, displayHeight, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
          FrameBuffer.unbindReadFramebuffer();
          UniformBuffer.bindProjUBO();
          if (Game.DO_TIMING) TimingHelper.endStart("renderNormals");
          Engine.worldRenderer.renderNormals(this.world, fTime);
          Engine.worldRenderer.renderTerrainWireFrame(this.world, fTime);
          if (Game.GL_ERROR_CHECKS)
              Engine.checkGLError("renderNormals");
          UniformBuffer.bindOrthoUBO();
          glEnable(GL_CULL_FACE);
      }
      

      glDisable(GL_DEPTH_TEST);
      glActiveTexture(GL_TEXTURE0);
      if (Game.DO_TIMING) TimingHelper.endStart("gui");
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("gui");
      
      if (this.movement.grabbed()) {
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
//      glBindTexture(GL_TEXTURE_2D, Engine.fbShadow.getTexture(0));
//      Engine.drawFullscreenQuad();
//      Shader.disable();

      if (show) {
          if (Game.DO_TIMING) TimingHelper.startSec("debugOverlay");
          if (this.debugOverlay != null) {
              this.debugOverlay.render(fTime);
          }
          if (Game.DO_TIMING) TimingHelper.endSec();
      }
      if (Game.DO_TIMING) TimingHelper.startSec("statsOverlay");

      if (this.statsCached != null) {
          this.statsCached.render(fTime);
      }
      if (Game.DO_TIMING) TimingHelper.endSec();
      
      if (Game.DO_TIMING) TimingHelper.endSec();
      glEnable(GL_DEPTH_TEST);

      if (Game.DO_TIMING) TimingHelper.endSec();
      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
      
  }

    @Override
    public void onStatsUpdated(float dTime) {
        if (this.statsCached != null) {
            this.statsCached.update(dTime);
        }
        if (System.currentTimeMillis()-lastShaderLoadTime > 222000/* && Keyboard.isKeyDown(Keyboard.KEY_F9)*/) {
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
        if (this.world != null) {
            this.world.updateFrame(f);
            this.entSelf.updateInputDirect(movement);
        }
//        float sinY = GameMath.sin(GameMath.degreesToRadians(entSelf.yaw));
//        float cosY = GameMath.cos(GameMath.degreesToRadians(entSelf.yaw));
//        float forward = 1;
//        float strafe = 0;
//        float fx = -forward * sinY + strafe * cosY;
//        float fz = forward * cosY + strafe * sinY;
        float px = (float) (entSelf.lastPos.x + (entSelf.pos.x - entSelf.lastPos.x) * f) + 0;
        float py = (float) (entSelf.lastPos.y + (entSelf.pos.y - entSelf.lastPos.y) * f) + 1.62F;
        float pz = (float) (entSelf.lastPos.z + (entSelf.pos.z - entSelf.lastPos.z) * f) + 0;
        Light l = this.world.lights.get(0);
        l.loc.x = px;
        l.loc.y = py;
        l.loc.z = pz;
//        int colorI = Color.HSBtoRGB((ticksran+f)/60, 1, 1);
        l.intensity = 7.6F;
        l.color = new Vector3f(1*l.intensity);
//        l.color.x = (float) (((colorI>>16)&0xFF) / 255.0F * l.intensity);
//        l.color.y = (float) (((colorI>>8)&0xFF) / 255.0F * l.intensity);
//        l.color.z = (float) (((colorI>>0)&0xFF) / 255.0F * l.intensity);
        float yaw = entSelf.yaw;
        float pitch = entSelf.pitch;
        Engine.camera.setPosition(px, py, pz);
        Engine.camera.setOrientation(yaw, pitch);
        Engine.setLightPosition(this.world.getLightPosition());
        Engine.updateCamera();
        Engine.updateShadowProjections(f);
        UniformBuffer.updateUBO(this.world, f);
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
        
        if (this.world != null) {
            Engine.regionLoader.finishTasks();
            if (follow) {
                lastCamX = Engine.camera.getPosition().x;
                lastCamY = Engine.camera.getPosition().y;
                lastCamZ = Engine.camera.getPosition().z;
            }
            int xPosP = GameMath.floor(lastCamX)>>(4+Region.REGION_SIZE_BITS);
            int zPosP = GameMath.floor(lastCamZ)>>(4+Region.REGION_SIZE_BITS);
            if (doLoad && System.currentTimeMillis() >= lastTimeLoad) {
                int i = Engine.regionLoader.updateRegions(xPosP, zPosP, follow);
//                if (i != 0) {
//                    System.out.println("Queued "+i+" regions for load");
//                }
                lastTimeLoad += 122L;
            }
            //HACKY
//            int nRegions = 0;
            Engine.regionRenderer.update(lastCamX, lastCamY, lastCamZ, xPosP, zPosP, f);
//            if (!startRender)
//            startRender = nRegions > 4;
        }
//        boolean releaseMouse = !this.mouseClicked || (Keyboard.isKeyDown(Keyboard.KEY_LEFT_CONTROL) ? !Mouse.isButtonDown(0) : true);
//                
//        if (releaseMouse) {
//        }
    }
    
    @Override
    public void onResize(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
            if (this.statsCached != null) {
                this.statsCached.setSize(displayWidth, displayHeight);
                this.statsCached.setPos(0, 0);
            }
            this.debugOverlay.setPos(0, 0);
            this.debugOverlay.setSize(displayWidth, displayHeight);
        }
    }
    @Override
    public void tick() {
       if (this.world != null)
           this.world.tickUpdate();
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
}
