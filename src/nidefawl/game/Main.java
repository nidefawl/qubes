package nidefawl.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Locale;

import nidefawl.qubes.*;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.assets.Textures;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.gui.GuiOverlayStats;
import nidefawl.qubes.input.Movement;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TimingHelper;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vec3;
import nidefawl.qubes.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector3f;

public class Main extends GLGame {
    static int               initWidth  = 1024;
    static int               initHeight = 512;
    static {
        GLGame.appName = "Qubes";
    }
    static public final Main instance   = new Main();

    public static void main(String[] args) {
        instance.startGame();
    }

    boolean              limitFPS      = true;
    public static boolean              show      = true;
    long                 lastClickTime = System.currentTimeMillis() - 5000L;

    public GuiOverlayStats   statsOverlay;
    public GuiOverlayDebug   debugOverlay;
    private Gui          gui;

    boolean              handleClick   = false;
    float                winX, winY;

     FontRenderer fontSmall;
    final PlayerSelf     entSelf       = new PlayerSelf(1);
    Movement             movement      = new Movement();
    World world = null;

    public Main() {
        super(20);
        GLGame.displayWidth = initWidth;
        GLGame.displayHeight = initHeight;
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
                        limitFPS = !limitFPS;
                    }
                    break;
                case Keyboard.KEY_F3:
                    if (isDown) {
                        show = !show;
                    }
                    break;
                case Keyboard.KEY_F5:
                    if (isDown) {
                        Engine.textures.genNoise();
                    }
                    break;
                case Keyboard.KEY_F2:
                    if (isDown) {
                        Engine.setShadow();
                    }
                    break;
                case Keyboard.KEY_ESCAPE:
                    shutdown();
                    break;
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
        this.handleClick = true;
        winX = (float) Mouse.getX();
        winY = (float) Mouse.getY();
    }

    Vec3 mousePos = new Vec3();
    
    public void render(float fTime) {
//        fogColor.scale(0.4F);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        Vec3 vUnproject = null;
        TimingHelper.start(6);
        Engine.fb.bind();
        Engine.fb.clearFrameBuffer();
        Engine.checkGLError("drawDebug");
        //                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        Engine.worldRenderer.renderWorld(fTime);
        if (this.handleClick) {
            this.handleClick = false;
            vUnproject = Engine.unproject(winX, winY);
        }
        Engine.fb.unbindCurrentFrameBuffer();
        TimingHelper.end(6);
        TimingHelper.start(7);
        glActiveTexture(GL_TEXTURE0);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity(); // Reset The Projection Matrix
        GL11.glOrtho(0, displayWidth, displayHeight, 0, -100, 100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glClear(GL_DEPTH_BUFFER_BIT|GL_COLOR_BUFFER_BIT);
        glDepthMask(true);
        glClearColor(0.71F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_ALWAYS);
        glDepthMask(false);
        TimingHelper.end(7);
        Engine.outRenderer.render(fTime);
        Engine.outRenderer.renderFinal(fTime);
        TimingHelper.start(8);
        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_ALPHA_TEST);
        glDisable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
        GL11.glOrtho(0, displayWidth, displayHeight, 0, -100, 100);
//      glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(0));
//      Tess.instance.draw(GL_QUADS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity(); // Reset The Projection Matrix
        GL11.glOrtho(0, displayWidth, displayHeight, 0, -100, 100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        if (show) {
            if (this.debugOverlay != null) {
                this.debugOverlay.render(fTime);
            }
        }
        TimingHelper.end(8);
        TimingHelper.start(9);

        if (vUnproject != null) {

            mousePos.set(vUnproject);
            BlockPos blockPos = mousePos.toBlock();
            int blockX = blockPos.x;
            int blockY = blockPos.y;
            int blockZ = blockPos.z;
            //            int i = this.world.getBiome(blockX, blockY, blockZ);
            //            int id = this.world.getTypeId(blockX, blockY, blockZ);
            String msg = "";
            msg += String.format("win:  %d %d\n", (int) winX, (int) winY);
            msg += String.format("Coordinate:  %d %d %d\n", blockX, blockY, blockZ);
            //            msg += String.format("Block:           %d\n", id);
            //            msg += String.format("Biome:          %s\n", BiomeGenBase.byId[i].biomeName);
            msg += String.format("Chunk:          %d/%d", blockX >> 4, blockZ >> 4);

            if (this.statsOverlay != null) {
                this.statsOverlay.setMessage(msg);
            }
        }
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_TEXTURE_2D);

        if (this.statsOverlay != null) {
            this.statsOverlay.render(fTime);
        }
        TimingHelper.end(9);

    }

    long lastShaderLoadTime = 0L;
    @Override
    public void onStatsUpdated() {
        if (this.statsOverlay != null) {
            this.statsOverlay.update();
        }
        if (System.currentTimeMillis()-lastShaderLoadTime > 2111000/* && Keyboard.isKeyDown(Keyboard.KEY_F9)*/) {
            lastShaderLoadTime = System.currentTimeMillis();
            Engine.shaders.reload();
        }
    }

    @Override
    public void preRenderUpdate(float f) {
        if (limitFPS) {
            limitFpsTo(20);
        }
        this.entSelf.updateInputDirect(movement);
        Engine.camera.set(this.entSelf, f);
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
    }

    public void addDebugOnScreen(String string) {
        if (this.statsOverlay != null) {
            this.statsOverlay.setMessage(string);
        }
    }
}
