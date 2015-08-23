package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.*;

import org.lwjgl.opengl.GL11;

import nidefawl.game.*;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.*;
import nidefawl.qubes.input.Movement;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.util.Timer;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;

public class Main extends GLGame {
    static int               initWidth  = 1024;
    static int               initHeight = 512;
    static {
        GLGame.appName = "-";
        Locale.setDefault(Locale.US);
    }
    static public final Main instance   = new Main();

    public static void main(String[] args) {
        instance.startGame();
    }

    public static int ticksran;
    public static float renderTime;
    public static boolean  show               = false;
    public static boolean  useBasicShaders    = true;
    public static boolean  matrixSetupMode    = false;
    public static boolean  renderWireFrame    = false;
    public static boolean toggleTiming;
    
    long         lastClickTime = System.currentTimeMillis() - 5000L;
    private long lastTimeLoad  = System.currentTimeMillis();

    public GuiOverlayStats statsOverlay;
    public GuiOverlayDebug debugOverlay;
    private Gui            gui;

    FontRenderer       fontSmall;
    final PlayerSelf   entSelf            = new PlayerSelf(1);
    Movement           movement           = new Movement();
    World              world              = null;
    public int         lastFPS            = 0;
    protected long     timeLastFPS;
    protected long     timeLastFrame;
    public int         tick               = 0;
    public boolean     follow             = true;
    private float      lastCamX;
    private float      lastCamY;
    private float      lastCamZ;
    private RayTrace   rayTrace;
    public int         selBlock           = 0;
    long               lastShaderLoadTime = System.currentTimeMillis();
    private boolean    doLoad             = true;
    public final Timer timer;
    protected boolean  startRender        = false;
    private Object[]   showError;
    
    public Main() {
        this.timer = new Timer(20);
        GLGame.displayWidth = initWidth;
        GLGame.displayHeight = initHeight;
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
        SysInfo info = new SysInfo();
        String title = "LWJGL "+info.lwjglVersion+" - "+info.openGLVersion;
        setTitle(title);
        this.statsOverlay = new GuiOverlayStats();
        this.statsOverlay.setPos(0, 0);
        this.statsOverlay.setSize(displayWidth, displayHeight);
        this.debugOverlay = new GuiOverlayDebug();
        this.debugOverlay.setPos(0, 0);
        this.debugOverlay.setSize(displayWidth, displayHeight);
        Engine.checkGLError("Post startup");
//        setVSync(true);
        
        setWorld(new World(1, 0x1234, Engine.regionLoader));
        this.entSelf.move(-800, 222, 1540);
        this.entSelf.yaw=6.72F;
        this.entSelf.pitch=38.50F;
//        this.entSelf.move(-870.42F, 103.92F-1.3F, 1474.25F);
//        this.entSelf.toggleFly();
        this.rayTrace = new RayTrace();
    }
    public void setWorld(World world) {
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
                        renderWireFrame = !renderWireFrame;
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
                        setWorld(new World(nId, 0x123, Engine.regionLoader));
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
                        useBasicShaders = !useBasicShaders;
                        Engine.switchRenderer(useBasicShaders);
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
                if (isDown) {
                    handleClick(Mouse.getX(), Mouse.getY());
                }
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
        }
    }

    @Override
    public void input(float fTime) {
        double mdX = Mouse.getDX();
        double mdY = Mouse.getDY();
        this.movement.update(mdX, mdY);
    }

    private void setGrabbed(boolean b) {
        this.movement.setGrabbed(b);
        Mouse.setCursorPosition(displayWidth / 2, displayHeight / 2);
        Mouse.setGrabbed(b);
    }

    private void handleClick(double d, double e) {
        this.mouseClicked = true;
    }
    boolean mouseClicked = false;
    
    
    private void setBlock() {
        BlockPos blockPos = rayTrace.getColl();
        if (blockPos != null) {
            BlockPos face = rayTrace.getFace();
            int blockX = blockPos.x;
            int blockY = blockPos.y;
            int blockZ = blockPos.z;
            //            int i = this.world.getBiome(blockX, blockY, blockZ);
            int id = this.world.getType(blockX, blockY, blockZ);
            String msg = "";
            msg += String.format("Coordinate:  %d %d %d\n", blockX, blockY, blockZ);
            msg += String.format("Block:           %d\n", id);
            //            msg += String.format("Biome:          %s\n", BiomeGenBase.byId[i].biomeName);
            msg += String.format("Chunk:          %d/%d", blockX >> 4, blockZ >> 4);

            if (this.statsOverlay != null) {
                this.statsOverlay.setMessage(msg);
            }
            int block = this.selBlock;
            if (block > 0) {
                blockX += face.x;
                blockY += face.y;
                blockZ += face.z;

                this.world.setType(blockX, blockY, blockZ, block, Flags.RENDER);
            } else {

                this.world.setType(blockX, blockY, blockZ, 0, Flags.RENDER);
            }
        }
    }

    public void render(float fTime) {
//      fogColor.scale(0.4F);

        if (Main.DO_TIMING) TimingHelper.startSec("world");
        if (Main.DO_TIMING) TimingHelper.startSec("ShadowPass");
        Engine.worldRenderer.renderShadowPass(this.world, fTime);
        if (Main.DO_TIMING) TimingHelper.endStart("bindFB");
      glEnable(GL_CULL_FACE);
      Engine.getSceneFB().bind();
      if (Main.DO_TIMING) TimingHelper.endStart("clearFrameBuffer");
      Engine.getSceneFB().clearFrameBuffer();
      if (Main.DO_TIMING) TimingHelper.endStart("renderWorld");

      Engine.worldRenderer.renderWorld(this.world, fTime);
      if (Main.renderWireFrame) {
          if (Main.DO_TIMING) TimingHelper.endStart("renderNormals");
//          Engine.worldRenderer.renderNormals(this.world, fTime);
      }
      if (Main.DO_TIMING) TimingHelper.endStart("renderBlockHighlight");
//      Engine.worldRenderer.renderBlockHighlight(this.world, fTime);
      if (Main.DO_TIMING) TimingHelper.endStart("renderDebugBB");
//      Engine.worldRenderer.renderDebugBB(this.world, fTime);
      if (Main.DO_TIMING) TimingHelper.endStart("unbindCurrentFrameBuffer");
      Engine.getSceneFB().unbindCurrentFrameBuffer();
      if (Main.DO_TIMING) TimingHelper.endSec();
      if (Main.DO_TIMING) TimingHelper.endSec();
      if (Main.DO_TIMING) TimingHelper.startSec("screen");
      if (Main.DO_TIMING) TimingHelper.startSec("prepare");

      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity(); // Reset The Projection Matrix
      GL11.glOrtho(0, displayWidth, displayHeight, 0, -100, 100);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      glClearColor(0.71F, 0.82F, 1.00F, 1F);
      glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      glDisable(GL_ALPHA_TEST);
      glDisable(GL_BLEND);
      glEnable(GL_DEPTH_TEST);
      glDepthFunc(GL_ALWAYS);
      glEnable(GL_TEXTURE_2D);
      glActiveTexture(GL_TEXTURE0);
      if (Main.DO_TIMING) TimingHelper.endStart("final");
      
      
      glDepthMask(false);
      Engine.outRenderer.render(fTime);
      Engine.outRenderer.renderFinal(fTime);
      
      if (Main.DO_TIMING) TimingHelper.endStart("gui");
      glDepthMask(true);

      glDepthFunc(GL_LEQUAL);
      glActiveTexture(GL_TEXTURE0);
      glDisable(GL_DEPTH_TEST);
      glEnable(GL_ALPHA_TEST);
      glEnable(GL_BLEND);
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
      
      if (this.movement.grabbed()) {
          if (Main.DO_TIMING) TimingHelper.startSec("crosshair");
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
          if (Main.DO_TIMING) TimingHelper.endSec();
      }
//      GL11.glScalef(0.25F, 0.25F, 1);
//      glBindTexture(GL_TEXTURE_2D, Engine.fbShadow.getTexture(0));
//      Tess.instance.draw(GL_QUADS);

      if (show) {
          if (Main.DO_TIMING) TimingHelper.startSec("debugOverlay");
          if (this.debugOverlay != null) {
              this.debugOverlay.render(fTime);
          }
          if (Main.DO_TIMING) TimingHelper.endSec();
      }
      if (Main.DO_TIMING) TimingHelper.startSec("statsOverlay");

      if (this.statsOverlay != null) {
          this.statsOverlay.render(fTime);
      }
      if (Main.DO_TIMING) TimingHelper.endSec();
      
      if (Main.DO_TIMING) TimingHelper.endSec();
      glEnable(GL_DEPTH_TEST);

      if (Main.DO_TIMING) TimingHelper.endSec();
  }

    @Override
    public void onStatsUpdated(float dTime) {
        if (this.statsOverlay != null) {
            this.statsOverlay.update(dTime);
        }
        if (System.currentTimeMillis()-lastShaderLoadTime > 1000/* && Keyboard.isKeyDown(Keyboard.KEY_F9)*/) {
            lastShaderLoadTime = System.currentTimeMillis();
            Shaders.initShaders();
            Engine.worldRenderer.initShaders();
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
        float yaw = entSelf.yaw;
        float pitch = entSelf.pitch;
        Engine.camera.setPosition(px, py, pz);
        Engine.camera.setOrientation(yaw, pitch);
        Engine.updateCamera();
        float winX, winY;
        
        if (this.movement.grabbed()) {
            winX = (float) displayWidth/2.0F;
            winY = (float) displayHeight/2.0F;
        } else {
            winX = (float) Mouse.getX();
            winY = (float) Mouse.getY();
            if (winX < 0) winX = 0; if (winX > displayWidth) winX = 1;
            if (winY < 0) winY = 0; if (winY > displayHeight) winY = 1;
        }
        Engine.updateMouseOverView(winX, winY);
        Engine.updateSun(f);
        this.rayTrace.reset();
        Engine.worldRenderer.highlight = null;
        if (this.world != null) {
            this.rayTrace.doRaytrace(this.world, Engine.vOrigin, Engine.vDir);
            BlockPos p = this.rayTrace.getColl();
            if (p != null) {
                if ( this.mouseClicked) {
                    setBlock();
                }   
            }
            //TODO: add some better logic for highlighting, don't render "into" camera
            if (p != null && !(p.x == GameMath.floor(px) && p.y == GameMath.floor(py) && p.z == GameMath.floor(pz))) {
                Engine.worldRenderer.highlight = p;
            }
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
        boolean releaseMouse = !this.mouseClicked || (Keyboard.isKeyDown(Keyboard.KEY_LEFT_CONTROL) ? !Mouse.isButtonDown(0) : true);
                
        if (releaseMouse) {
            this.mouseClicked = false;
        }
    }
    
    @Override
    public void onResize(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
            if (this.statsOverlay != null) {
                this.statsOverlay.setSize(displayWidth, displayHeight);
                this.statsOverlay.setPos(0, 0);
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
    
    @Override
    protected void onDestroy() {
        Engine.stop();
        TextureManager.getInstance().destroy();
        Tess.destroyAll();
    }

    public void setException(GameError error) {
        if (Thread.currentThread() != getMainThread()) {
            this.showError = new Object[] { "Error occured", Arrays.asList(new String[] { "There was an internal error"}), error };
            return;
        }
        showErrorScreen( "Error occured", Arrays.asList(new String[] { "There was an internal error"}), error );
    }


    private void showErrorScreen(String title, List<String> desc, Throwable throwable) {
        try {
            if (this.wasrunning) {
                onDestroy();
            }
            destroyContext();
            try {
                Thread.sleep(120L);
            } catch (InterruptedException interruptedexception) {
            }

            initDisplay();

            Engine.baseInit();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("errorscreen - initdisplay");
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("errorscreen - glClearColor");
            GL11.glShadeModel(GL11.GL_SMOOTH);
            glActiveTexture(GL_TEXTURE0);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("errorscreen - glActiveTexture(GL_TEXTURE0)");
            glEnable(GL_ALPHA_TEST);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("errorscreen - GL_ALPHA_TEST");
            glEnable(GL_BLEND);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("errorscreen - GL_BLEND");
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("errorscreen - glBlendFunc");
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glDepthMask(true);
            glColorMask(true, true, true, true);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("errorscreen - glColorMask");
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
//            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
//            GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
            setVSync(true);

            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen");
            Mouse.setGrabbed(false);
            GuiCrash guiCrash = new GuiCrash(title, desc, throwable);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glShadeModel(GL11.GL_FLAT);
            glActiveTexture(GL_TEXTURE0);
            glEnable(GL_ALPHA_TEST);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glDepthMask(true);
            glColorMask(true, true, true, true);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            GL11.glViewport(0, 0, displayWidth, displayHeight);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glViewport");
            while (!isCloseRequested()) {
                checkResize();
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen checkResize");
                updateInput();
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen updateInput");
                guiCrash.setPos(0, 0);
                guiCrash.setSize(displayWidth, displayHeight);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen guiCrash.setSize");
                GL11.glMatrixMode(GL_PROJECTION);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glMatrixMode(GL_PROJECTION)");
                GL11.glLoadIdentity();
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glLoadIdentity");
                GL11.glOrtho(0, displayWidth, displayHeight, 0, 0, 10);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glOrtho");
                GL11.glMatrixMode(GL_MODELVIEW);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glMatrixMode(GL_MODELVIEW)");
                GL11.glLoadIdentity();
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glLoadIdentity");
                GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glClearColor");
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glClear");
                guiCrash.render(0);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen guiCrash.render");
                updateDisplay();
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen updateDisplay");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            System.exit(1);
        }
    }

    @Override
    public void runFrame() {
        if (toggleTiming) {
            toggleTiming = false;
            DO_TIMING = !DO_TIMING;
            TimingHelper.reset();
        }
        if (isCloseRequested()) {
            shutdown();
            return;
        }
        if (Main.DO_TIMING) TimingHelper.startSec("pre render");
        checkResize();
        timer.calculate();
        ticksran += timer.ticks;
        renderTime = timer.partialTick;
        for (int i = 0; i < timer.ticks; i++) {
            this.tick();
            tick++;
        }
        Stats.uniformCalls = 0;
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("pre render");
        if (Main.DO_TIMING)
            TimingHelper.endSec();
        if (Main.DO_TIMING)
            TimingHelper.startSec("input");
        updateInput();
        input(renderTime);
        if (Main.DO_TIMING)
            TimingHelper.endSec();
        if (Main.DO_TIMING)
            TimingHelper.startSec("preRenderUpdate");
        preRenderUpdate(renderTime);
//        if (!startRender) {
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            return;
//        }
        if (Main.DO_TIMING)
            TimingHelper.endSec();
        render(renderTime);
        if (Main.DO_TIMING)
            TimingHelper.startSec("postRenderUpdate");
        postRenderUpdate(renderTime);
        if (Main.DO_TIMING)
            TimingHelper.endSec();
        //        if (Main.DO_TIMING) TimingHelper.start(14);
        //        GL11.glFlush();
        //        if (Main.DO_TIMING) TimingHelper.end(14);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("render");
        if (Main.DO_TIMING)
            TimingHelper.startSec("Display.update");
        updateDisplay();
        if (Main.DO_TIMING)
            TimingHelper.endSec();
        if (Main.DO_TIMING)
            TimingHelper.startSec("calcFPS");
        long now = System.nanoTime();
        float took = timer.el;
        Stats.avgFrameTime = Stats.avgFrameTime * 0.95F + (took) * 0.05F;
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("Post render");
        Stats.fpsCounter++;
        double l = (timer.absTime - timeLastFPS) / 1000.0D;
        if (l >= 1) {
            timeLastFPS = timer.absTime;
            lastFPS = Stats.fpsCounter;
            Stats.fpsCounter = 0;
            onStatsUpdated(1.0F);
        }
        if (Main.DO_TIMING)
            TimingHelper.endSec();
        if (this.showError != null) {
            this.showErrorScreen((String)showError[0], (List)showError[1], (Throwable)showError[2]);
        }
    }

    @Override
    public void mainLoop() {
        try {
            this.running = true;
            this.wasrunning = true;
            setVSync(this.vsync);
            initGame();
            Engine.init();
            onResize(displayWidth, displayHeight);
            timer.calculate();
            timeLastFrame = System.nanoTime();
            timeLastFPS = timer.absTime;
            while (this.running) {
                runFrame();
            }
        } catch (Throwable t) {
            showErrorScreen("The game crashed", Arrays.asList(new String[] { "The game has crashed"}), t);
        } finally {
            if (this.wasrunning) {
                onDestroy();
            }
            destroyContext();
        }
    }
    @Override
    public void initGLContext() {
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(true);
        int a = GL11.GL_TEXTURE_MATRIX;
        glColorMask(true, true, true, true);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
//        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
//        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        setVSync(true);
        try {
            List<String> list = GL.validateCaps();
            if (!list.isEmpty()) {
                ArrayList<String> desc = new ArrayList<>();
                desc.add("You graphics card does not support some of the required OpenGL features");
                desc.add("");
                for (String s : list) {
                    desc.add(s);
                }
                showErrorScreen("Failed starting game", desc, null);
                return;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }
    }


    protected void limitFpsTo(int fpsLimit) {
        long now = System.nanoTime();
        long el = now - timeLastFrame;
        timeLastFrame = now;
        double elD = el / 10000000.D;
        int timePerFrame = (int) Math.ceil(1000.0D / (float) fpsLimit);
        int sleepD = (int) Math.ceil(timePerFrame - elD);
        if (sleepD >= 1) {
            try {
                Thread.sleep((long) sleepD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
