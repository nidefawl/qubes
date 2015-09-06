package nidefawl.qubes;

import static org.lwjgl.glfw.Callbacks.errorCallbackPrint;
import static org.lwjgl.glfw.Callbacks.glfwSetCallback;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.logging.LogBufferStream;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.GPUTaskProfile;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.swing.TextDialog;

public abstract class GameBase implements Runnable {
    public static String  appName         = "LWJGL Test App";
    public static int     displayWidth;
    public static int     displayHeight;
    public static boolean glDebug         = false;
    public static boolean GL_ERROR_CHECKS = true;
    public static long    windowId        = 0;
    static int            initWidth       = 1024;
    static int            initHeight      = 512;

    // We need to strongly reference callback instances.
    private GLFWErrorCallback       errorCallback;
    private GLFWWindowSizeCallback  cbWindowSize;
    private GLFWKeyCallback         cbKeyboard;
    private GLFWMouseButtonCallback cbMouseButton;
    private GLFWScrollCallback      cbScrollCallback;
    private GLFWWindowFocusCallback cbWindowFocus;
    private GLFWCursorPosCallback   cbCursorPos;
    private GLFWCharCallback        cbText;

    public static boolean      toggleTiming;
    public static boolean      DO_TIMING   = false;
    public static float        renderTime;
    public static int          ticksran;
    public int                 lastFPS     = 0;
    protected long             timeLastFPS;
    protected long             timeLastFrame;
    public final Timer         timer;
    public int                 tick        = 0;
    protected boolean          startRender = false;
    private GameError          showError;
    private LogBufferStream    outStream;
    private LogBufferStream    errStream;
    private long               frameTime;
    protected boolean          vsync       = true;
    protected volatile boolean running     = false;
    protected volatile boolean wasrunning  = false;
    protected volatile boolean sysExit     = true;
    private Thread             thread;

    public void startGame() {
        this.thread = new Thread(this, appName + " main thread");
        this.thread.setPriority(Thread.MAX_PRIORITY);
        this.thread.start();
    }

    public GameBase() {
        this.timer = new Timer(20);
        displayWidth = initWidth;
        displayHeight = initHeight;
        outStream = new LogBufferStream(System.out);
        errStream = new LogBufferStream(System.err);
        System.out.flush();
        System.err.flush();
        System.setOut(outStream);
        System.setErr(errStream);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public final void run() {
        try {
            initDisplay(false);
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }

        initGLContext();
        mainLoop();
    }

    void initCallbacks() {
        errorCallback = errorCallbackPrint(System.err);
        cbWindowSize = new GLFWWindowSizeCallback() {

            @Override
            public void invoke(long window, int width, int height) {
                displayWidth = width;
                displayHeight = height;
                if (displayWidth <= 0) {
                    displayWidth = 1;
                }
                if (displayHeight <= 0) {
                    displayHeight = 1;
                }
                System.out.println("resize " + displayWidth + "/" + displayHeight);

                if (isRunning())
                    GL11.glViewport(0, 0, displayWidth, displayHeight);
                onResize(displayWidth, displayHeight);
            }
        };
        cbKeyboard = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                onKeyPress(window, key, scancode, action, mods);
            }
        };
        cbMouseButton = new GLFWMouseButtonCallback() {

            @Override
            public void invoke(long window, int button, int action, int mods) {
                onMouseClick(window, button, action, mods);
            }
        };
        cbScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                Mouse.scrollDX += xoffset;
                Mouse.scrollDY += yoffset;
            }

        };
        cbWindowFocus = new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, int focused) {
                Mouse.setGrabbed(false);
            }
        };
        cbCursorPos = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                Mouse.update(xpos, ypos);
            }
        };
        cbText = new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                onTextInput(window, codepoint);
            }
        };
    }

    public void initDisplay(boolean debugContext) {
        try {
            initCallbacks();
            glfwSetErrorCallback(errorCallback);
            // Initialize GLFW. Most GLFW functions will not work before doing this.
            if (glfwInit() != GL11.GL_TRUE)
                throw new IllegalStateException("Unable to initialize GLFW");

            // Configure our window
            glfwDefaultWindowHints();// optional, the current window hints are already the default
            glfwWindowHint(GLFW_VISIBLE, GL_FALSE);// the window will stay hidden after creation
            glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);// the window will be resizable
            if (!debugContext) {
                //              glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
                //              glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
                //              glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
                //              glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            }

            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, glDebug ? GL_TRUE : GL_FALSE);
            //            glfwWindowHint(GLFW_RED_BITS, 8);
            //            glfwWindowHint(GLFW_GREEN_BITS, 8);
            //            glfwWindowHint(GLFW_BLUE_BITS, 8);
            //            glfwWindowHint(GLFW_ALPHA_BITS, 8);
            glfwWindowHint(GLFW_DEPTH_BITS, 24);
            glfwWindowHint(GLFW_STENCIL_BITS, 1);
            //            glfwWindowHint(GLFW_DOUBLE_BUFFER, GL_TRUE);//Check Version

            // Create the window
            windowId = glfwCreateWindow(displayWidth, displayHeight, appName, NULL, NULL);
            if (windowId == NULL)
                throw new RuntimeException("Failed to create the GLFW window");
            // Make the OpenGL context current
            Mouse.init();

            // Get the resolution of the primary monitor
            ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            // Center our window
            glfwSetWindowPos(windowId, (GLFWvidmode.width(vidmode) - displayWidth) / 2, (GLFWvidmode.height(vidmode) - displayHeight) / 2);

            glfwMakeContextCurrent(windowId);
            org.lwjgl.opengl.GL.createCapabilities();
            // Make the window visible
            glfwShowWindow(windowId);
            glfwSetCallback(windowId, cbWindowSize);
            glfwSetKeyCallback(windowId, cbKeyboard);
            glfwSetCallback(windowId, cbMouseButton);
            glfwSetCallback(windowId, cbScrollCallback);
            glfwSetWindowFocusCallback(windowId, cbWindowFocus);
            glfwSetCallback(windowId, cbCursorPos);
            glfwSetCallback(windowId, cbText);

            int major, minor, rev;
            major = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_VERSION_MAJOR);
            minor = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_VERSION_MINOR);
            rev = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_REVISION);
            System.out.printf("OpenGL version recieved: %d.%d.%d\n", major, minor, rev);
            System.out.printf("Supported OpenGL is %s\n", GL11.glGetString(GL11.GL_VERSION));
            System.out.printf("Supported GLSL is %s\n", GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
            // Setup a key callback. It will be called every time a key is pressed, repeated or released.
            if (GL_ERROR_CHECKS)
                _checkGLError("Pre startup");

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    protected void destroyContext() {
        cbKeyboard.release();
        cbMouseButton.release();
        cbScrollCallback.release();
        cbWindowSize.release();
        cbWindowFocus.release();
        errorCallback.release();
        glfwTerminate();
        windowId = 0;
    }

    protected void onDestroy() {
        Engine.stop();
        TextureManager.getInstance().destroy();
        Tess.destroyAll();
    }

    public void shutdown() {
        this.running = false;
    }

    protected void checkResize() {
    }

    public abstract void onStatsUpdated();

    public void setVSync(boolean b) {
        this.vsync = b;
        setVSync_impl(b);
    }

    public boolean getVSync() {
        return this.vsync;
    }

    public Thread getMainThread() {
        return this.thread;
    }

    private static boolean _checkGLError(String s) {
        int i = GL11.glGetError();
        if (i != 0) {
            String s1 = getGlErrorString(i);
            throw new RuntimeException("Error - " + s + ": " + s1);
        }
        return false;
    }

    public static String getGlErrorString(int error_code) {
        switch (error_code) {
            case GL11.GL_NO_ERROR:
                return "No error";
            case GL11.GL_INVALID_ENUM:
                return "Invalid enum";
            case GL11.GL_INVALID_VALUE:
                return "Invalid value";
            case GL11.GL_INVALID_OPERATION:
                return "Invalid operation";
            case GL11.GL_STACK_OVERFLOW:
                return "Stack overflow";
            case GL11.GL_STACK_UNDERFLOW:
                return "Stack underflow";
            case GL11.GL_OUT_OF_MEMORY:
                return "Out of memory";
            case ARBImaging.GL_TABLE_TOO_LARGE:
                return "Table too large";
            case EXTFramebufferObject.GL_INVALID_FRAMEBUFFER_OPERATION_EXT:
                return "Invalid framebuffer operation";
            default:
                return "ErrorCode " + error_code;
        }
    }

    public void updateDisplay() {
        glfwSwapBuffers(windowId);// swap the color buffers
    }

    public boolean isCloseRequested() {
        return glfwWindowShouldClose(windowId) != GL_FALSE;
    }

    protected void setVSync_impl(boolean b) {
        int vsync = 0;
        if (b) {
            vsync = 1;
            if (GL.getCaps().WGL_EXT_swap_control && GL.getCaps().WGL_EXT_swap_control_tear) {
                vsync = -1;
            }
        }
        glfwSwapInterval(vsync);
    }

    public void updateInput() {
        // Poll for window events. The key callback above will only be
        // invoked during this call.
        glfwPollEvents();
    }

    public void setTitle(String title) {
        glfwSetWindowTitle(windowId, title);
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

    public void runFrame() {
        if (toggleTiming) {
            toggleTiming = false;
            DO_TIMING = !DO_TIMING;
            TimingHelper.reset();
        }
        if (Game.DO_TIMING)
            TimingHelper.check();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.startFrame();
        if (isCloseRequested()) {
            shutdown();
            return;
        }
        if (Game.DO_TIMING)
            TimingHelper.startSec("pre render");
        checkResize();
        updateTime();
        Stats.uniformCalls = 0;
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("pre render");
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        if (Game.DO_TIMING)
            TimingHelper.startSec("input");
        updateInput();
        input(renderTime);
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        if (Game.DO_TIMING)
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
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        render(renderTime);
        if (Game.DO_TIMING)
            TimingHelper.startSec("postRenderUpdate");
        postRenderUpdate(renderTime);
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        //        if (Main.DO_TIMING) TimingHelper.start(14);
        //        GL11.glFlush();
        //        if (Main.DO_TIMING) TimingHelper.end(14);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("render");
        if (Game.DO_TIMING)
            TimingHelper.startSec("Display.update");
        float took = (System.nanoTime() - frameTime) / 1000000F;
        Stats.avgFrameTime = Stats.avgFrameTime * 0.95F + (took) * 0.05F;
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("updateDisplay");
        updateDisplay();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        frameTime = System.nanoTime();
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        if (Game.DO_TIMING)
            TimingHelper.startSec("calcFPS");
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("Post render");
        Stats.fpsCounter++;
        Stats.fpsInteval = (timer.absTime - timeLastFPS) / 1000.0D;
        if (Stats.fpsInteval >= 0.5F) {
            timeLastFPS = timer.absTime;
            lastFPS = (int) (Stats.fpsCounter / Stats.fpsInteval);
            Stats.fpsCounter = 0;
            onStatsUpdated();
        }
        if (Game.DO_TIMING)
            TimingHelper.endSec();
        if (this.showError != null) {
            //            this.showErrorScreen((String)showError[0], (List)showError[1], (Throwable)showError[2], true);
            throw this.showError;
        }
        if (GPUProfiler.PROFILING_ENABLED) {
            GPUProfiler.endFrame();
            GPUTaskProfile tp;
            while ((tp = GPUProfiler.getFrameResults()) != null) {
                glProfileResults.clear();
                tp.dump(glProfileResults);
                GPUProfiler.recycle(tp);
            }
        }
    }

    public ArrayList<String> glProfileResults = new ArrayList<>();

    public void mainLoop() {
        try {
            this.running = true;
            this.wasrunning = true;
            setVSync(this.vsync);
            initGame();
            onResize(displayWidth, displayHeight);
            timer.calculate();
            timeLastFrame = System.nanoTime();
            timeLastFPS = timer.absTime;
            lateInitGame();
            while (this.running) {
                runFrame();
            }
        } catch (Throwable t) {
            showErrorScreen("The game crashed", Arrays.asList(new String[] { "The game has crashed" }), t, true);
        } finally {
            if (this.wasrunning) {
                onDestroy();
            }
            destroyContext();
        }
    }

    public void initGLContext() {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(true);
        glColorMask(true, true, true, true);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_FASTEST);
        glHint(GL_POINT_SMOOTH_HINT, GL_FASTEST);
        glHint(GL_LINE_SMOOTH_HINT, GL_FASTEST);
        glHint(GL_POLYGON_SMOOTH_HINT, GL_FASTEST);
        glHint(GL_FOG_HINT, GL_FASTEST);
        glHint(GL13.GL_TEXTURE_COMPRESSION_HINT, GL_FASTEST);
        glHint(GL14.GL_GENERATE_MIPMAP_HINT, GL_FASTEST);
        glHint(GL20.GL_FRAGMENT_SHADER_DERIVATIVE_HINT, GL_FASTEST);
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
                showErrorScreen("Failed starting game", desc, null, false);
                return;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }
    }

    public void updateTime() {
        timer.calculate();
        ticksran += timer.ticks;
        renderTime = timer.partialTick;
        for (int i = 0; i < timer.ticks; i++) {
            this.tick();
            tick++;
        }
    }

    private void showErrorScreen(String title, List<String> desc, Throwable throwable, boolean b) {
        try {
            if (this.wasrunning) {
                onDestroy();
            }
            destroyContext();
            String buf1 = outStream.getLogString();
            String buf2 = errStream.getLogString();
            TextDialog dlg = new TextDialog(title, desc, throwable, b);
            dlg.prepend(buf1);
            dlg.prepend(buf2);
            dlg.setVisible(displayWidth, displayHeight);
            while (dlg.isVisible()) {
                Thread.sleep(100);
            }
            if (dlg.reqRestart) {
                this.wasrunning = false;
                this.running = false;
                this.sysExit = false;
                //                Client.main(Main.lastargs);
                return;
            }
            /*Tess.useClientStates = true;
            
            initDisplay(true);
            
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
            //            Engine.updateOrthoMatrix(displayWidth, displayHeight);
            //            Shaders.updateUBO();
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
                GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glClearColor");
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen glClear");
                guiCrash.render(0);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen guiCrash.render");
                updateDisplay();
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("showErrorScreen updateDisplay");
            }*/
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (this.sysExit)
                System.exit(1);
        }
    }

    public void setException(GameError error) {
        this.showError = error;
    }

    public long getTime() {
        return timer.absTime;
    }


    protected abstract void onTextInput(long window, int codepoint);
    
    protected abstract void onKeyPress(long window, int key, int scancode, int action, int mods);

    protected abstract void onMouseClick(long window, int button, int action, int mods);

    public abstract void render(float f);

    public abstract void input(float f);

    public abstract void preRenderUpdate(float f);

    public abstract void postRenderUpdate(float f);

    public abstract void onResize(int displayWidth, int displayHeight);

    public abstract void tick();

    public abstract void initGame();

    public abstract void lateInitGame();
}
