package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import nidefawl.game.Main;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Timer;
import nidefawl.qubes.util.TimingHelper;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;

public abstract class GLGame implements Runnable {
    public static String       appName    = "LWJGL Test App";
    public static int          displayWidth;
    public static int          displayHeight;
    public static boolean      glDebug    = false;
    public final Timer         timer;
    private int                fpsCounter = 0;
    public int                 lastFPS    = 0;
    private long               timeLastFPS;
    private long               timeLastFrame;
    public int                 fpsLimit;
    private Thread             thread;
    protected volatile boolean running = true;
    public static int          ticksran;
    public static float        renderTime;
    public int                 tick       = 0;
    public int                 uniformCalls;

    public float avgFrameTime = 0F;
    private GameError error;

    public GLGame(float tickLen) {
        this.timer = new Timer(20);
    }

    public void startGame() {
        this.thread = new Thread(this, appName + " main thread");
        this.thread.setPriority(10);
        this.thread.start();
    }

    public void initDisplay() throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(GLGame.displayWidth, GLGame.displayHeight));
        Display.setResizable(true);
        Display.setTitle(GLGame.appName);
        Display.setInitialBackground(1.0F, 1.0F, 1.0F);
        try {
            PixelFormat pixelformat = new PixelFormat();
            pixelformat = pixelformat.withDepthBits(24).withStencilBits(1);
            if (GLGame.glDebug) {
                Display.create(pixelformat, new org.lwjgl.opengl.ContextAttribs().withDebug(true));
            } else {
                Display.create(pixelformat);
            }
        } catch (LWJGLException lwjglexception) {
            lwjglexception.printStackTrace();
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException interruptedexception) {
            }
            Display.create();
        }
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("Pre startup");
        Keyboard.create();
        Mouse.create();
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
    }

    public boolean isRunning() {
        return running;
    }

    private void cleanUpGame() {
        TextureManager.getInstance().destroy();
        Tess.instance.destroy();
        Keyboard.destroy();
        Mouse.destroy();
        Display.destroy();
        System.exit(0);
    }

    @Override
    public void run() {
        try {
            initDisplay();
            initGame();
            Engine.init();
            resize(displayWidth, displayHeight);
            timer.calculate();
            timeLastFrame = System.nanoTime();
            timeLastFPS = timer.absTime;
            this.running = true;
            while (this.running) {
                runTick();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            cleanUpGame();
        }
    }

    public void shutdown() {
        this.running = false;
    }

    public void runTick() throws LWJGLException {

        if (Display.isCloseRequested()) {
            shutdown();
            return;
        }
        if (Main.DO_TIMING)
            TimingHelper.start(10);
        if (Display.getWidth() != displayWidth || Display.getHeight() != displayHeight) {
            displayWidth = Display.getWidth();
            displayHeight = Display.getHeight();
            if (displayWidth <= 0) {
                displayWidth = 1;
            }
            if (displayHeight <= 0) {
                displayHeight = 1;
            }
            resize(displayWidth, displayHeight);
        }
        timer.calculate();
        ticksran += timer.ticks;
        renderTime = timer.partialTick;
        for (int i = 0; i < timer.ticks; i++) {
            this.tick();
            tick++;
        }
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("pre render");
        if (Main.DO_TIMING)
            TimingHelper.end(10);
        if (Main.DO_TIMING)
            TimingHelper.start(11);
        preRenderUpdate(renderTime);
        if (Main.DO_TIMING)
            TimingHelper.end(11);
        if (Main.DO_TIMING)
            TimingHelper.start(12);
        input(renderTime);
        if (Main.DO_TIMING)
            TimingHelper.end(12);
        if (Main.DO_TIMING)
            TimingHelper.start(13);
        Engine.update();
        if (Main.DO_TIMING)
            TimingHelper.end(13);
        render(renderTime);
        //        if (Main.DO_TIMING) TimingHelper.start(14);
        //        GL11.glFlush();
        //        if (Main.DO_TIMING) TimingHelper.end(14);
        if (Main.DO_TIMING)
            TimingHelper.start(15);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("render");
        Display.update();
        if (Main.DO_TIMING)
            TimingHelper.end(15);
        if (fpsLimit > 0) {
            limitFpsTo(fpsLimit);
        }
        if (Main.DO_TIMING)
            TimingHelper.start(16);
        long now = System.nanoTime();
        float took = timer.el;
        avgFrameTime = avgFrameTime * 0.95F + (took) * 0.05F;
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("Post render");
        fpsCounter++;
        double l = (timer.absTime - timeLastFPS) / 1000.0D;
        uniformCalls = Shaders.getAndResetNumCalls();
        if (l >= 1) {
            timeLastFPS = timer.absTime;
            lastFPS = fpsCounter;
            fpsCounter = 0;
            onStatsUpdated(1.0F);
        }
        if (Main.DO_TIMING)
            TimingHelper.end(16);
        if (this.error != null) {
            this.error.printStackTrace();
            shutdown();
            return;
        }
    }

    public abstract void onStatsUpdated(float dSec);

    public void setFPSLimit(int i) {
        this.fpsLimit = i;
    }

    private void limitFpsTo(int i) {
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

    private void resize(int displayWidth, int displayHeight) throws LWJGLException {
        GL11.glViewport(0, 0, displayWidth, displayHeight);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float aspectRatio = (float) displayWidth / (float) displayHeight;
        GLU.gluPerspective(45, aspectRatio, 0.1F, 200010F);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glClear(GL_COLOR_BUFFER_BIT);
        Engine.resize(displayWidth, displayHeight);
        onResize();
    }

    public abstract void render(float f);

    public abstract void input(float f);

    public abstract void preRenderUpdate(float f);

    public abstract void onResize();

    public abstract void tick();

    public abstract void initGame();

    public void setException(GameError error) {
        this.error = error;
    }

}
