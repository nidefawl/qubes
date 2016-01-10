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

import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.logging.LogBufferStream;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.GPUTaskProfile;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.ShaderSource;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.swing.TextDialog;

public abstract class GameBase implements Runnable {
    public static String  appName         = "LWJGL Test App";
    public static int     displayWidth;
    public static int     displayHeight;
    public static boolean GL_ERROR_CHECKS = false;
    public static long    windowId        = 0;
    static int            initWidth       = (int) (1680*0.8);
    static int            initHeight      = (int) (1050*0.8);
    public static int TICKS_PER_SEC = 20;

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
    protected volatile boolean minimized   = false;
    private Thread             thread;
    private int newWidth = initWidth;
    private int newHeight = initHeight;
    private GPUVendor vendor = GPUVendor.OTHER;

    public void startGame() {
        this.thread = new Thread(this, appName + " main thread");
        this.thread.setPriority(Thread.MAX_PRIORITY);
        this.thread.start();
    }

    public GameBase() {
        this.timer = new Timer(TICKS_PER_SEC);
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
    
    public GPUVendor getVendor() {
        return this.vendor;
    }

    @Override
    public final void run() {
        try {
            initDisplay(false);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("early initDisplay");
            initGLContext();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("early initGLContext");
            glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT|GL_ACCUM_BUFFER_BIT|GL_STENCIL_BUFFER_BIT);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("early glClear");
            updateDisplay();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("early updateDisplay");
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }
        try {
            List<String> list = GL.validateCaps();
            if (!list.isEmpty()) {
                ArrayList<String> desc = new ArrayList<>();
                desc.add("You graphics card does not support some of the required OpenGL features");
                desc.add(GL11.glGetString(GL11.GL_VENDOR)+" "+GL11.glGetString(GL11.GL_RENDERER) +" "+GL11.glGetString(GL11.GL_VERSION));
                for (String s : list) {
                    desc.add(s);
                }
                showErrorScreen("Incompatible graphics card", desc, null, false);
                return;
            }
        } catch (Throwable t) {
            showErrorScreen("Failed starting game", Arrays.asList(new String[] { "An unexpected exception occured" }), t, true);
            return;
        }
        try {
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("pre initGLContext");
            initGLContext();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("initGLContext");
            GameContext.lateInit();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("GameContext.lateInit");
            if (this.showError == null) {
                this.showError = GameContext.getInitError();
            }
        } catch (Throwable t) {
            showErrorScreen("Failed starting game", Arrays.asList(new String[] { "An unexpected exception occured" }), t, true);
            return;
        }
        if (this.showError != null) {
            showErrorScreen("Failed starting game", Arrays.asList(new String[] { "An unexpected exception occured" }), this.showError, true);
        }
        mainLoop();
    }

    void initCallbacks() {
        errorCallback = errorCallbackPrint(System.err);
        cbWindowSize = new GLFWWindowSizeCallback() {

            @Override
            public void invoke(long window, int width, int height) {
                newWidth = width;
                newHeight = height;
            }
        };
        cbKeyboard = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                try {
                    onKeyPress(window, key, scancode, action, mods);
                } catch (Throwable t) {
                    setException(new GameError("GLFWKeyCallback", t));
                }
            }
        };
        cbMouseButton = new GLFWMouseButtonCallback() {

            @Override
            public void invoke(long window, int button, int action, int mods) {
                try {
                    onMouseClick(window, button, action, mods);
                } catch (Throwable t) {
                    setException(new GameError("GLFWMouseButtonCallback", t));
                }
            }
        };
        cbScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                try {
                    Mouse.scrollDX += xoffset;
                    Mouse.scrollDY += yoffset;
                    onWheelScroll(window, xoffset, yoffset);
                } catch (Throwable t) {
                    setException(new GameError("GLFWScrollCallback", t));
                }
            }

        };
        cbWindowFocus = new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, int focused) {
                try {
                    Mouse.setGrabbed(false);
                } catch (Throwable t) {
                    setException(new GameError("GLFWWindowFocusCallback", t));
                }
            }
        };
        cbCursorPos = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                try {
                    Mouse.update(xpos, ypos);
                } catch (Throwable t) {
                    setException(new GameError("GLFWCursorPosCallback", t));
                }
            }
        };
        cbText = new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                try {
                    onTextInput(window, codepoint);
                } catch (Throwable t) {
                    setException(new GameError("GLFWCharCallback", t));
                }
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
            glfwWindowHint(GLFW_SAMPLES, 0);
            glfwWindowHint(GLFW_VISIBLE, GL_FALSE);// the window will stay hidden after creation
            glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);// the window will be resizable
            if (!debugContext) {
//              glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//              glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
//                glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
//              glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            }

            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_ERROR_CHECKS ? GL_TRUE : GL_FALSE);
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

            int major, minor, rev;
            major = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_VERSION_MAJOR);
            minor = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_VERSION_MINOR);
            rev = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_REVISION);
            this.vendor = GPUVendor.parse(GL11.glGetString(GL11.GL_VENDOR));
            if (GL_ERROR_CHECKS) {
                System.out.printf("OpenGL version recieved: %d.%d.%d\n", major, minor, rev);
                System.out.printf("Supported OpenGL is %s\n", GL11.glGetString(GL11.GL_VERSION));
                System.out.printf("Supported GLSL is %s\n", GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
            }
            // Setup a key callback. It will be called every time a key is pressed, repeated or released.
            if (GL_ERROR_CHECKS) {
                if (KHRDebug.getInstance() != null) {
//                    GLDebugLog.setup();
                    _checkGLError("GLDebugLog.setup()");
                }
                _checkGLError("Pre startup");
            }

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
        TextureManager.getInstance().destroy();
        Tess.destroyAll();
    }

    public void shutdown() {
        this.running = false;
        Engine.stop();
    }

    protected void checkResize() {
        if (minimized && newWidth*newHeight>0) {
            minimized = false;
            if (isRunning())
                GL11.glViewport(0, 0, displayWidth, displayHeight);
        }
        if (newWidth != displayWidth || newHeight != displayHeight) {
            if (newWidth*newHeight <= 0) {
                minimized = true;
                return;
            }
            System.out.println("resize " + newWidth + "/" + newHeight);
            minimized = false;
            displayWidth = newWidth;
            displayHeight = newHeight;
            if (displayWidth <= 0) {
                displayWidth = 1;
            }
            if (displayHeight <= 0) {
                displayHeight = 1;
            }
            try {
                if (isRunning())
                    GL11.glViewport(0, 0, displayWidth, displayHeight);
                onResize(displayWidth, displayHeight);
            } catch (Throwable t) {
                setException(new GameError("GLFWWindowSizeCallback", t));
            }
        }
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
        if (this.vendor != GPUVendor.INTEL) {
            int vsync = 0;
            if (b) {
                vsync = 1;
                if (GL.getCaps().WGL_EXT_swap_control && GL.getCaps().WGL_EXT_swap_control_tear) {
                    vsync = -1;
                }
            }
            glfwSwapInterval(vsync);
        }
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
        
        
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.startFrame();
        
        if (isCloseRequested()) {
            shutdown();
            return;
        }
        
        checkResize();
        updateTime();
        Stats.uniformCalls = 0;
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("pre render");
        
        
        updateInput();
        input(renderTime);
        
        
        preRenderUpdate(renderTime);
        //        if (!startRender) {
        //            try {
        //                Thread.sleep(10);
        //            } catch (InterruptedException e) {
        //                e.printStackTrace();
        //            }
        //            return;
        //        }
        
        render(renderTime);
        
        postRenderUpdate(renderTime);
        
        //        if (Main.DO_TIMING) TimingHelper.start(14);
        //        GL11.glFlush();
        //        if (Main.DO_TIMING) TimingHelper.end(14);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("render");
        
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("updateDisplay");
        updateDisplay();
        float took = (System.nanoTime() - frameTime) / 1000000F;
        
        
      //TODO: add second counter that calculates frame time len without vsync so we can use this one for frame time based calculation
        Stats.avgFrameTime = Stats.avgFrameTime * 0.95F + (took) * 0.05F;  
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        frameTime = System.nanoTime();
        
        
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

    public boolean loadRender(int step, float f) {
        return false;
    }
    public void mainLoop() {
        try {
            this.running = true;
            this.wasrunning = true;
            initGame();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("initGame");
            onResize(displayWidth, displayHeight);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("initGame onResize");
            timer.calculate();
            timeLastFrame = System.nanoTime();
            timeLastFPS = timer.absTime;
            lateInitGame();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("initGame lateInitGame");
            setVSync(this.vsync);
            while (this.running) {
                runFrame();
            }
        } catch (Throwable t) {
            showErrorScreen("The game crashed", Arrays.asList(new String[] { "An unexpected exception occured" }), t, true);
        } finally {
            if (this.wasrunning) {
                onDestroy();
            }
            destroyContext();
        }
    }

    public void initGLContext() {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glClearColor");
        glActiveTexture(GL_TEXTURE0);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glActiveTexture");
        glEnable(GL_BLEND);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glEnable(GL_BLEND)");
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)");
        glEnable(GL_DEPTH_TEST);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glEnable(GL_DEPTH_TEST)");
        glDepthFunc(GL_LEQUAL);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glDepthFunc(GL_LEQUAL)");
        Engine.enableDepthMask(true);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glDepthMask(true)");
        glColorMask(true, true, true, true);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glColorMask(true, true, true, true)");
        glEnable(GL_CULL_FACE);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glEnable(GL_CULL_FACE)");
        glCullFace(GL_BACK);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glCullFace(GL_BACK)");
        int fastNice = GL_NICEST;//GL_FASTEST;
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL_PERSPECTIVE_CORRECTION_HINT, fastNice)");
        glHint(GL_POINT_SMOOTH_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL_POINT_SMOOTH_HINT, fastNice)");
        glHint(GL_LINE_SMOOTH_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL_LINE_SMOOTH_HINT, fastNice)");
        glHint(GL_POLYGON_SMOOTH_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL_POLYGON_SMOOTH_HINT, fastNice)");
        glHint(GL_FOG_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL_FOG_HINT, fastNice)");
        glHint(GL13.GL_TEXTURE_COMPRESSION_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL13.GL_TEXTURE_COMPRESSION_HINT, fastNice)");
        glHint(GL14.GL_GENERATE_MIPMAP_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL14.GL_GENERATE_MIPMAP_HINT, fastNice)");
        glHint(GL20.GL_FRAGMENT_SHADER_DERIVATIVE_HINT, fastNice);
        GL11.glGetError();
        setVSync(true);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("setVSync(true)");
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
            if (NativeInterface.isPresent()) {
                this.sysExit = false;
                CrashInfo info = new CrashInfo(title, desc);
                info.setLogBuf(buf1);
                if (throwable instanceof ShaderCompileError) {
                    ShaderCompileError sce = (ShaderCompileError) throwable;
                    ShaderSource src = sce.getShaderSource();
                    if (src != null) {
                        buf2 += src.getSource();
                    }
                    info.setErrBuf(buf2);
                }
                info.setErrBuf(buf2);
                info.setException(throwable);
                NativeInterface.getInstance().gameCrashed(info);
                return;
            }
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
    public void setTextHook(boolean state) {
        if (state) {
            glfwSetCharCallback(windowId, cbText);
        } else {
            glfwSetCharCallback(windowId, null);
        }
    }


    protected abstract void onTextInput(long window, int codepoint);
    
    protected abstract void onKeyPress(long window, int key, int scancode, int action, int mods);

    protected abstract void onMouseClick(long window, int button, int action, int mods);

    protected abstract void onWheelScroll(long window, double xoffset, double yoffset);

    public abstract void render(float f);

    public abstract void input(float f);

    public abstract void preRenderUpdate(float f);

    public abstract void postRenderUpdate(float f);

    public abstract void onResize(int displayWidth, int displayHeight);

    public abstract void tick();

    public abstract void initGame();

    public abstract void lateInitGame();
}
