package nidefawl.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.Locale;

import nidefawl.qubes.GLGame;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.gui.GuiOverlayStats;
import nidefawl.qubes.input.Movement;
import nidefawl.qubes.render.RegionRenderThread;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

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

    public static boolean  GL_ERROR_CHECKS = true;
    public static boolean  DO_TIMING       = false;
    public static boolean  show            = false;
    public static boolean  useShaders      = true;
    public static boolean  useEmptyShaders      = false;
    public static boolean  matrixSetupMode = false;
    public static boolean  renderWireFrame = false;
    
    long                   lastClickTime = System.currentTimeMillis() - 5000L;
    private long               lastTimeLoad          = System.currentTimeMillis();

    public GuiOverlayStats statsOverlay;
    public GuiOverlayDebug debugOverlay;
    private Gui            gui;


    FontRenderer           fontSmall;
    final PlayerSelf       entSelf       = new PlayerSelf(1);
    Movement               movement      = new Movement();
    World                  world         = null;
    public boolean follow = true;
    private float lastCamX;
    private float lastCamZ;
    private RayTrace rayTrace;
    public int selBlock = 0;

    public Main() {
        super(20);
        GLGame.displayWidth = initWidth;
        GLGame.displayHeight = initHeight;
        TimingHelper.setName(0, "Final_Prepare");
        TimingHelper.setName(1, "Final_Stage0");
        TimingHelper.setName(2, "Final_Stage1");
        TimingHelper.setName(3, "Final_Stage2");
//        TimingHelper.setName(4, "Final_Stage3");
        TimingHelper.setName(5, "Final_StageLast");
        TimingHelper.setName(6, "RenderScene");
        TimingHelper.setName(7, "PreFinalStage");
        TimingHelper.setName(8, "PostFinalStage");
        TimingHelper.setName(9, "GUIStats");
        TimingHelper.setName(10, "UpdateTimer");
        TimingHelper.setName(11, "PreRenderUpdate");
        TimingHelper.setName(12, "Input");
        TimingHelper.setName(13, "EngineUpdate");
        TimingHelper.setName(15, "DisplayUpdate");
        TimingHelper.setName(16, "CalcFPS");
        TimingHelper.setName(17, "Final_Stage0to1Mipmap");
        TimingHelper.setName(18, "Final_Stage1to2Mipmap");
    }

    @Override
    public void initGame() {
        this.statsOverlay = new GuiOverlayStats();
        this.statsOverlay.setPos(0, 0);
        this.statsOverlay.setSize(displayWidth, displayHeight);
        this.debugOverlay = new GuiOverlayDebug();
        this.debugOverlay.setPos(0, 0);
        this.debugOverlay.setSize(displayWidth, displayHeight);
        Engine.checkGLError("Post startup");
//        setVSync(true);
        this.world = new World(1, 0x123, Engine.regionLoader);
        this.entSelf.move(0, 140, 0);
        this.rayTrace = new RayTrace();
    }

    @Override
    public void input(float fTime) {
        double mdX = Mouse.getDX();
        double mdY = Mouse.getDY();
        boolean b = Mouse.isGrabbed();

        // Break up our movement into components along the X, Y and Z axis
        while (Keyboard.next()) {
            int key = Keyboard.getEventKey();
            boolean isDown = Keyboard.getEventKeyState();
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
                        this.world = new World(this.world.worldId+1, 123, Engine.regionLoader);
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
                        Engine.regionLoader.reRender();
                    }
                    break;
                case Keyboard.KEY_F11:
                    if (isDown) {
                        DO_TIMING = !DO_TIMING;
                        TimingHelper.reset();
                    }
                    break;
                case Keyboard.KEY_F12:
                    if (isDown) {
                        TimingHelper.dump();
                    }
                    break;
                case Keyboard.KEY_F6:
                    if (isDown) {
                        Engine.flushRenderTasks();
                        useShaders = !useShaders;
                        Engine.regionLoader.reRender();
                    }
                    break;
                case Keyboard.KEY_ESCAPE:
                    shutdown();
                    break;
            }
            if (key >= Keyboard.KEY_1 && key <= Keyboard.KEY_0) {
                this.selBlock = key - Keyboard.KEY_1;
                if (!Block.isValid(this.selBlock)) {
                    this.selBlock = 0;
                }
            }
        }
        while (Mouse.next()) {
            int key = Mouse.getEventButton();
            boolean isDown = Mouse.getEventButtonState();
            long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
            switch (key) {
                case 0:
                    if (isDown) {
                        handleClick(Mouse.getEventX(), Mouse.getEventY());
                    }
                    break;
                case 1:
                    if (isDown ) {
                        setGrabbed(!b);
                        b = !b;
                    }
                    break;
            }
        }
        if (b != this.movement.grabbed()) {
            setGrabbed(b);
        }
        this.movement.update(mdX, mdY);
    }

    private void setGrabbed(boolean b) {
        this.movement.setGrabbed(b);
        Mouse.setCursorPosition(displayWidth / 2, displayHeight / 2);
        Mouse.setGrabbed(b);
    }

    private void handleClick(int eventX, int eventY) {
        System.out.println("click");
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

      if (Main.useShaders) {
          Engine.worldRenderer.renderShadowPass(this.world, fTime);
      }

      if (Main.DO_TIMING) TimingHelper.start(6);
      glDisable(GL_CULL_FACE);
      Engine.getSceneFB().bind();
      Engine.getSceneFB().clearFrameBuffer();

//      glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
      Engine.worldRenderer.renderWorld(this.world, fTime);
//      glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
      if (Main.renderWireFrame) {
          Engine.worldRenderer.renderNormals(this.world, fTime);
      }
      
      Engine.worldRenderer.renderBlockHighlight(this.world, fTime);
      
      Engine.getSceneFB().unbindCurrentFrameBuffer();
      if (Main.DO_TIMING) TimingHelper.end(6);
      if (Main.DO_TIMING) TimingHelper.start(7);

      glEnable(GL_CULL_FACE);

      glDisable(GL_LIGHTING);
      glDisable(GL_COLOR_MATERIAL);
      glDisable(GL_LIGHT0);
      glDisable(GL_LIGHT1);

      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity(); // Reset The Projection Matrix
      GL11.glOrtho(0, displayWidth, displayHeight, 0, -100, 100);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      GL11.glClear(GL_DEPTH_BUFFER_BIT|GL_COLOR_BUFFER_BIT);
      glClearColor(0.71F, 0.82F, 1.00F, 1F);
      glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      glDisable(GL_ALPHA_TEST);
      glDisable(GL_BLEND);
      glEnable(GL_DEPTH_TEST);
      glDepthFunc(GL_ALWAYS);
      glEnable(GL_TEXTURE_2D);
      glActiveTexture(GL_TEXTURE0);
      glDepthMask(false);
      if (Main.DO_TIMING) TimingHelper.end(7);
      if (Main.useShaders) {
          Engine.outRenderer.render(fTime);
          Engine.outRenderer.renderFinal(fTime);
      }
      if (Main.DO_TIMING) TimingHelper.start(8);

      glDepthMask(true);

      glDepthFunc(GL_LEQUAL);
      glActiveTexture(GL_TEXTURE0);
      glDisable(GL_DEPTH_TEST);
      glEnable(GL_ALPHA_TEST);
      glEnable(GL_BLEND);
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      if (!Main.useShaders) {
          glBindTexture(GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
          Engine.drawFullscreenQuad();
      }
      if (this.movement.grabbed()) {
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
      
      }
//      GL11.glScalef(0.25F, 0.25F, 1);
//      glBindTexture(GL_TEXTURE_2D, Engine.fbShadow.getTexture(0));
//      Tess.instance.draw(GL_QUADS);

      GL11.glLoadIdentity();
      if (show) {
          if (this.debugOverlay != null) {
              this.debugOverlay.render(fTime);
          }
      }
      if (Main.DO_TIMING) TimingHelper.end(8);
      if (Main.DO_TIMING) TimingHelper.start(9);


      if (this.statsOverlay != null) {
          this.statsOverlay.render(fTime);
      }
      if (Main.DO_TIMING) TimingHelper.end(9);
      glEnable(GL_DEPTH_TEST);

  }

    long lastShaderLoadTime = 0L;
    private boolean doLoad = true;
    @Override
    public void onStatsUpdated(float dTime) {
        if (this.statsOverlay != null) {
            this.statsOverlay.update(dTime);
        }
        if (System.currentTimeMillis()-lastShaderLoadTime > 4224000/* && Keyboard.isKeyDown(Keyboard.KEY_F9)*/) {
            lastShaderLoadTime = System.currentTimeMillis();
            Engine.shaders.reload();
//            Engine.textures.refreshNoiseTextures();
        }
    }

    @Override
    public void postRenderUpdate(float f) {
    }
    @Override
    public void preRenderUpdate(float f) {
        this.entSelf.updateInputDirect(movement);
        if (Main.DO_TIMING)
            TimingHelper.start(13);
        float px = entSelf.lastPos.x + (entSelf.pos.x - entSelf.lastPos.x) * f;
        float py = entSelf.lastPos.y + (entSelf.pos.y - entSelf.lastPos.y) * f;
        float pz = entSelf.lastPos.z + (entSelf.pos.z - entSelf.lastPos.z) * f;
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
        if (Main.DO_TIMING)
            TimingHelper.end(13);
        Engine.updateSun(f);
        this.rayTrace.reset();
        if (this.world != null) {
            this.rayTrace.doRaytrace(this.world, Engine.vOrigin, Engine.vDir);
            Engine.worldRenderer.highlight = this.rayTrace.getColl();
            Engine.regionLoader.finishTasks();
            if (follow) {
                lastCamX = Engine.camera.getPosition().x;
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
            RegionRenderThread thread = Engine.regionRenderThread;
            thread.finishTasks();
            //HACKY
            int nRegions = 0;
            Engine.worldRenderer.flushRegions();
            for (int xx = -RegionLoader.LOAD_DIST; xx <= RegionLoader.LOAD_DIST; xx++) {
                for (int zz = -RegionLoader.LOAD_DIST; zz <= RegionLoader.LOAD_DIST; zz++) {
                    Region r = Engine.regionLoader.getRegion(xx + xPosP, zz + zPosP);
                    if (r != null)  {
                        if (!thread.busy()) {
                            if (r.state == Region.STATE_LOAD_COMPLETE && r.renderState == Region.RENDER_STATE_INIT) {
                                //                      System.out.println("thread.offer");
                                if (thread.offer(r)) {
                                } else {
    
                                    //                          System.out.println("thread seems busys");
                                }
                            }
                        }
                        if (r.isRenderable) {
                            Engine.worldRenderer.putRegion(r);
                            nRegions++;
                        }
                    }
                }
            }
            if (!startRender)
            startRender = nRegions > 4;
        }
    }

    @Override
    public void onResize() {
        if (this.statsOverlay != null) {
            this.statsOverlay.setSize(displayWidth, displayHeight);
            this.statsOverlay.setPos(0, 0);
        }
        this.debugOverlay.setPos(0, 0);
        this.debugOverlay.setSize(displayWidth, displayHeight);
    }
    @Override
    public void tick() {
        this.entSelf.tickUpdate();
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
