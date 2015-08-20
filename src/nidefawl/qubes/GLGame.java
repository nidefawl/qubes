package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nidefawl.game.Main;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.GuiCrash;
import nidefawl.qubes.shader.AdvShaders;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

public abstract class GLGame implements Runnable {
    public static String       appName    = "LWJGL Test App";
    public static int          displayWidth;
    public static int          displayHeight;
    public static boolean      glDebug    = false;
    public final Timer         timer;
    public int                 lastFPS    = 0;
    private long               timeLastFPS;
    private long               timeLastFrame;
    protected boolean          vsync        = true;
    private Thread             thread;
    protected volatile boolean running = false;
    protected volatile boolean wasrunning = false;
    public static int          ticksran;
    public static float        renderTime;
    public int                 tick       = 0;
    
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
//        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
//        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        setVSync(true);
    }

    public boolean isRunning() {
        return running;
    }

    private void cleanUpGame() {
        if (this.wasrunning) {
            Engine.stop();
            TextureManager.getInstance().destroy();
            Tess.destroyAll();
        }
        Keyboard.destroy();
        Mouse.destroy();
        Display.destroy();
        System.exit(0);
    }

    @Override
    public void run() {
        try {
            initDisplay();
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }

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
        try {
            this.running = true;
            this.wasrunning = true;
            setVSync(this.vsync);
            initGame();
            Engine.init();
            resize(displayWidth, displayHeight);
            timer.calculate();
            timeLastFrame = System.nanoTime();
            timeLastFPS = timer.absTime;
            while (this.running) {
                runFrame();
            }
        } catch (Throwable t) {
            showErrorScreen("The game crashed", Arrays.asList(new String[] { "The game has crashed"}), t);
        } finally {
            cleanUpGame();
        }
    }

    private void showErrorScreen(String title, List<String> desc, Throwable throwable) {
        try {
            Keyboard.destroy();
            Mouse.destroy();
            Display.destroy();
            try {
                Thread.sleep(120L);
            } catch (InterruptedException interruptedexception) {
            }
            
            initDisplay();
            
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
            while (!Display.isCloseRequested()) {
                checkResize();
                guiCrash.setPos(0, 0);
                guiCrash.setSize(displayWidth, displayHeight);
                GL11.glMatrixMode(GL_PROJECTION);
                GL11.glLoadIdentity();
                GL11.glOrtho(0, displayWidth, displayHeight, 0, 0, 10);
                GL11.glMatrixMode(GL_MODELVIEW);
                GL11.glLoadIdentity();
                GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
                guiCrash.render(0);
                Display.update();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            System.exit(1);
        }
    }

    public void shutdown() {
        this.running = false;
    }
    protected boolean startRender = false;
    private Object[] showError;
    public void runFrame() throws LWJGLException {

        if (Display.isCloseRequested()) {
            shutdown();
            return;
        }
        if (Main.DO_TIMING)
            TimingHelper.start(10);
        checkResize();
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
            TimingHelper.start(12);
        input(renderTime);
        if (Main.DO_TIMING)
            TimingHelper.end(12);
        if (Main.DO_TIMING)
            TimingHelper.start(11);
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
            TimingHelper.end(11);
        render(renderTime);
        postRenderUpdate(renderTime);
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
        if (Main.DO_TIMING)
            TimingHelper.start(16);
        long now = System.nanoTime();
        float took = timer.el;
        Stats.avgFrameTime = Stats.avgFrameTime * 0.95F + (took) * 0.05F;
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("Post render");
        Stats.fpsCounter++;
        double l = (timer.absTime - timeLastFPS) / 1000.0D;
        Stats.uniformCalls = AdvShaders.getAndResetNumCalls();
        if (l >= 1) {
            timeLastFPS = timer.absTime;
            lastFPS = Stats.fpsCounter;
            Stats.fpsCounter = 0;
            onStatsUpdated(1.0F);
        }
        if (Main.DO_TIMING)
            TimingHelper.end(16);
        if (this.showError != null) {
            this.showErrorScreen((String)showError[0], (List)showError[1], (Throwable)showError[2]);
        }
    }

    private void checkResize() throws LWJGLException {
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
    }

    public abstract void onStatsUpdated(float dSec);

    public void setVSync(boolean b) {
        this.vsync = b;
        Display.setSwapInterval(b ? 1 : 0);
    }
    public boolean getVSync() {
        return this.vsync;
    }

    private void limitFpsTo(int fpsLimit) {
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
        if (this.isRunning()) {
            Engine.resize(displayWidth, displayHeight);
            onResize();
        }
    }

    public abstract void render(float f);

    public abstract void input(float f);

    public abstract void preRenderUpdate(float f);

    public abstract void postRenderUpdate(float f);

    public abstract void onResize();

    public abstract void tick();

    public abstract void initGame();

    public void setException(GameError error) {
        if (Thread.currentThread() != thread) {
            this.showError = new Object[] { "Error occured", Arrays.asList(new String[] { "There was an internal error"}), error };
            return;
        }
        showErrorScreen( "Error occured", Arrays.asList(new String[] { "There was an internal error"}), error );
    }

}
