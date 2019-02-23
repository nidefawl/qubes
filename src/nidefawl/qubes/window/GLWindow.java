//package nidefawl.qubes.window;
//
//import static org.lwjgl.glfw.GLFW.*;
//import static org.lwjgl.opengl.GL11.*;
//import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
//import static org.lwjgl.opengl.GL13.glActiveTexture;
//import static org.lwjgl.system.MemoryUtil.NULL;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import org.lwjgl.glfw.*;
//import org.lwjgl.opengl.*;
//
//import nidefawl.qubes.GameBase;
//import nidefawl.qubes.gl.GPUVendor;
////import nidefawl.qubes.input.Mouse;
//import nidefawl.qubes.logging.IErrorHandler;
//import nidefawl.qubes.shader.ShaderCompileError;
//import nidefawl.qubes.util.GameError;
//import nidefawl.qubes.util.Timer;
//
//public abstract class GLWindow implements Runnable, IErrorHandler {
//    protected  int            initWidth       = (int) (1920*0.8);
//    protected  int            initHeight      = (int) (1080*0.8);
//    public  int TICKS_PER_SEC = 20;
//    public boolean                      GL_ERROR_CHECKS                 = true;
//    public boolean                      DEBUG_LAYER                     = true;
//    public String                       windowName                      = "";
//    public long                         windowId                        = 0;
//    public  long    windowSurface   = 0;
//    public int                          windowWidth;
//    public int                          windowHeight;
//    // We need to strongly reference callback instances.
//    private GLFWErrorCallback       errorCallback;
//    private GLFWWindowSizeCallback  cbWindowSize;
//    private GLFWKeyCallback         cbKeyboard;
//    private GLFWMouseButtonCallback cbMouseButton;
//    private GLFWScrollCallback      cbScrollCallback;
//    private GLFWWindowFocusCallback cbWindowFocus;
//    private GLFWCursorPosCallback   cbCursorPos;
//    private GLFWCharCallback        cbText;
//    private GLFWFramebufferSizeCallback cbFramebufferSize;
//
//
//    public  boolean      toggleTiming;
//    public  float        renderTime;
//    public  float        absTime;
//    public  int          ticksran;
//    public int                 lastFPS     = 0;
//    protected long             timeLastFPS;
//    protected long             timeLastFrame;
//    public final Timer         timer;
//    public int                 tick        = 0;
//    protected boolean          startRender = false;
//    private long               frameTime;
//    protected boolean          vsync       = true;
//    protected volatile boolean running     = false;
//    protected volatile boolean wasrunning  = false;
//    protected volatile boolean minimized   = false;
//    protected volatile boolean hasWindowFocus = true;
//    boolean               wasGrabbed      = true;
//    boolean needsGrab = false;
//    protected boolean isStarting = true;
//    private Thread             thread;
//    private int newWidth = initWidth;
//    private int newHeight = initHeight;
//    private GPUVendor vendor = GPUVendor.OTHER;
//    public GLCapabilities caps;
//    private  int displayWidth;
//    private  int displayHeight;
////    private GameError          showError;
//
//    public void startThread() {
//        this.thread = new Thread(this, "window "+windowName+ " render thread");
//        this.thread.setPriority(Thread.MAX_PRIORITY);
//        this.thread.start();
//    }
//
//    public GLWindow() {
//        this.timer = new Timer(TICKS_PER_SEC);
//        windowWidth = initWidth;
//        windowHeight = initHeight;
//    }
//
//    public boolean isRunning() {
//        return running;
//    }
//    
//    public GPUVendor getVendor() {
//        return this.vendor;
//    }
//
//    @Override
//    public final void run() {
//        try {
//            initDisplay(DEBUG_LAYER);
//            initGLContext();
//            if (GL_ERROR_CHECKS)
//                checkGLError("early initGLContext");
//            glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT|GL_STENCIL_BUFFER_BIT);
//            if (GL_ERROR_CHECKS)
//                checkGLError("early glClear");
//        
//            updateDisplay();
//            if (GL_ERROR_CHECKS)
//                checkGLError("early updateDisplay");
//        } catch (Throwable t) {
//            t.printStackTrace();
//            return;
//        }
//        try {
//            if (GL_ERROR_CHECKS)
//                checkGLError("pre initGLContext");
//            initGLContext();
//            if (GL_ERROR_CHECKS)
//                checkGLError("initGLContext");
//        } catch (Throwable t) {
//            showErrorScreen("Failed starting game", Arrays.asList(new String[] { "An unexpected exception occured" }), t, true);
//            return;
//        }
//        mainLoop();
//    }
//
//    void initCallbacks() {
//        errorCallback = GLFWErrorCallback.createPrint(System.err);
//        cbWindowSize = new GLFWWindowSizeCallback() {
//
//            @Override
//            public void invoke(long window, int width, int height) {
//                newWidth = width;
//                newHeight = height;
//            }
//        };
//        cbFramebufferSize = new GLFWFramebufferSizeCallback() {
//            @Override
//            public void invoke(long window, int width, int height) {
////                newWidth = width;
////                newHeight = height;
//            }
//        };
//        cbKeyboard = new GLFWKeyCallback() {
//            @Override
//            public void invoke(long window, int key, int scancode, int action, int mods) {
//                try {
//                    onKeyPress(window, key, scancode, action, mods);
//                } catch (Throwable t) {
//                    setException(new GameError("GLFWKeyCallback", t));
//                }
//            }
//        };
//        cbMouseButton = new GLFWMouseButtonCallback() {
//
//            @Override
//            public void invoke(long window, int button, int action, int mods) {
//                try {
//                    onMouseClick(window, button, action, mods);
//                } catch (Throwable t) {
//                    setException(new GameError("GLFWMouseButtonCallback", t));
//                }
//            }
//        };
//        cbScrollCallback = new GLFWScrollCallback() {
//            @Override
//            public void invoke(long window, double xoffset, double yoffset) {
//                try {
//                    Mouse.scrollDX += xoffset;
//                    Mouse.scrollDY += yoffset;
//                    onWheelScroll(window, xoffset, yoffset);
//                } catch (Throwable t) {
//                    setException(new GameError("GLFWScrollCallback", t));
//                }
//            }
//
//        };
//        cbWindowFocus = new GLFWWindowFocusCallback() {
//
//            @Override
//            public void invoke(long window, boolean focused) {
//                try {
//                    hasWindowFocus = focused;
//                    if (!hasWindowFocus)
//                        Mouse.setGrabbed(false);
//                } catch (Throwable t) {
//                    setException(new GameError("GLFWWindowFocusCallback", t));
//                }
//            }
//        };
//        cbCursorPos = new GLFWCursorPosCallback() {
//            @Override
//            public void invoke(long window, double xpos, double ypos) {
//                try {
//                    Mouse.update(xpos, ypos);
//                } catch (Throwable t) {
//                    setException(new GameError("GLFWCursorPosCallback", t));
//                }
//            }
//        };
//        cbText = new GLFWCharCallback() {
//            @Override
//            public void invoke(long window, int codepoint) {
//                try {
//                    onTextInput(window, codepoint);
//                } catch (Throwable t) {
//                    setException(new GameError("GLFWCharCallback", t));
//                }
//            }
//        };
//    }
//
//
//    public void initDisplay(boolean debugContext) {
//        try {
//            initCallbacks();
//            glfwSetErrorCallback(errorCallback);
//            if (glfwInit() != true)
//                throw new IllegalStateException("Unable to initialize GLFW");
//            // Configure our window
//            glfwDefaultWindowHints();// optional, the current window hints are already the default
//            glfwWindowHint(GLFW_VISIBLE, GLFW.GLFW_FALSE);// the window will stay hidden after creation
//            glfwWindowHint(GLFW_RESIZABLE, GLFW.GLFW_TRUE);// the window will be resizable
//
//            glfwWindowHint(GLFW_SAMPLES, 0);
//            if (!debugContext) {
////              glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
////              glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 4);
////              glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
//            }
//          glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
//          glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 4);
//          glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
//
//            if (!GL_ERROR_CHECKS) {
//                glfwWindowHint(GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
//            } else {
//                glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
//            }
//            glfwWindowHint(GLFW_CONTEXT_ROBUSTNESS, GLFW.GLFW_NO_ROBUSTNESS);
//            //            glfwWindowHint(GLFW_RED_BITS, 8);
//            //            glfwWindowHint(GLFW_GREEN_BITS, 8);
//            //            glfwWindowHint(GLFW_BLUE_BITS, 8);
//            //            glfwWindowHint(GLFW_ALPHA_BITS, 8);
//            glfwWindowHint(GLFW_DEPTH_BITS, 24);
//            glfwWindowHint(GLFW_STENCIL_BITS, 1);
//            //            glfwWindowHint(GLFW_DOUBLE_BUFFER, GL_TRUE);//Check Version
//
//            // Create the window
//            windowId = glfwCreateWindow(windowWidth, windowHeight, getAppTitle(), NULL, NULL);
//            if (windowId == NULL)
//                throw new RuntimeException("Failed to create the GLFW window");
//            // Make the OpenGL context current
//            Mouse.init();
//
//            // Get the resolution of the primary monitor
//            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
//            // Center our window
//            glfwSetWindowPos(windowId, (vidmode.width() - windowWidth) / 2, (vidmode.height() - windowHeight) / 2);
//            glfwShowWindow(windowId);
//            glfwSetWindowSizeCallback(windowId, cbWindowSize);
//            glfwSetKeyCallback(windowId, cbKeyboard);
//            glfwSetMouseButtonCallback(windowId, cbMouseButton);
//            glfwSetScrollCallback(windowId, cbScrollCallback);
//            glfwSetWindowFocusCallback(windowId, cbWindowFocus);
//            glfwSetCursorPosCallback(windowId, cbCursorPos);
//            glfwSetFramebufferSizeCallback(windowId, cbFramebufferSize);
//            glfwMakeContextCurrent(windowId);
//            caps = org.lwjgl.opengl.GL.createCapabilities();
//            // Make the window visible
////                glfwSetFramebufferSizeCallback(windowId, new GLFWFramebufferSizeCallback() {
////                    
////                    @Override
////                    public void invoke(long window, int width, int height) {
////                        System.out.println("FRAMEBUFFER NOW "+width+"/"+height);
////                    }
////                });
//
//            int major, minor, rev;
//            major = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_VERSION_MAJOR);
//            minor = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_VERSION_MINOR);
//            rev = glfwGetWindowAttrib(windowId, GLFW_CONTEXT_REVISION);
//            this.vendor = GPUVendor.parse(GL11.glGetString(GL11.GL_VENDOR));
//            if (GL_ERROR_CHECKS) {
//                System.out.printf("OpenGL version recieved: %d.%d.%d\n", major, minor, rev);
//                System.out.printf("Supported OpenGL is %s\n", GL11.glGetString(GL11.GL_VERSION));
//                System.out.printf("Supported GLSL is %s\n", GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
//            }
//            // Setup a key callback. It will be called every time a key is pressed, repeated or released.
//            if (GL_ERROR_CHECKS) {
////                    if (KHRDebug. != null) {
////                        GLDebugLog.setup();
////                        _checkGLError("GLDebugLog.setup()");
////                    }
////                    _checkGLError("Pre startup");
//            }
//            if (GL_ERROR_CHECKS)
//                checkGLError("initDisplay");
//
//        } catch (Throwable t) {
//            throw new RuntimeException(t);
//        }
//    }
//    public String getAppTitle() {
//        return windowName;
//    }
//
//    protected void destroyContext() {
//        if(DEBUG_LAYER) System.err.println("GameBase.destroyContext");
//        cbFramebufferSize.free();
//        cbKeyboard.free();
//        cbMouseButton.free();
//        cbScrollCallback.free();
//        cbWindowSize.free();
//        cbWindowFocus.free();
//        cbCursorPos.free();
//        errorCallback.free();
//        cbText.free();
////        glfwTerminate();
//        windowId = 0;
//    }
//
//    protected void onDestroy() {
//    }
//
//    public void shutdown() {
//        this.running = false;
//        destroyContext();
//    }
//    
//    /** 
//     * This method is horribly hacky!! 
//     */
//    public void checkResize() {
//        if (minimized && newWidth*newHeight>0) {
//            minimized = false;
//            if (isRunning())
//                setDefaultViewport();
//        }
//        boolean resize = newWidth != windowWidth || newHeight != windowHeight;
//        if (resize) {
//            if (newWidth*newHeight <= 0) {
//                minimized = true;
//                return;
//            }
//            System.out.printf("Resize %d,%d -> %d,%d\n", windowWidth, windowHeight, newWidth, newHeight);
//            minimized = false;
//            windowWidth = newWidth;
//            windowHeight = newHeight;
//            if (windowWidth <= 0) {
//                windowWidth = 1;
//            }
//            if (windowHeight <= 0) {
//                windowHeight = 1;
//            }
//            updateRenderResolution(windowWidth, windowHeight);
//            try {
//                onWindowResize(windowWidth, windowHeight);
//                setDefaultViewport();
//            } catch (Throwable t) {
//                setException(new GameError("GLFWWindowSizeCallback", t));
//            }
//        }
//    }
//
//    public void setVSync(boolean b) {
//        this.vsync = b;
//        setVSync_impl(b);
//    }
//
//    public boolean getVSync() {
//        return this.vsync;
//    }
//
//    public Thread getMainThread() {
//        return this.thread;
//    }
//
//    private static boolean _checkGLError(String s) {
//        int i = GL11.glGetError();
//        if (i != 0) {
//            String s1 = getGlErrorString(i);
//            throw new RuntimeException("Error - " + s + ": " + s1);
//        }
//        return false;
//    }
//
//    public static String getGlErrorString(int error_code) {
//        switch (error_code) {
//            case GL11.GL_NO_ERROR:
//                return "No error";
//            case GL11.GL_INVALID_ENUM:
//                return "Invalid enum";
//            case GL11.GL_INVALID_VALUE:
//                return "Invalid value";
//            case GL11.GL_INVALID_OPERATION:
//                return "Invalid operation";
//            case GL11.GL_STACK_OVERFLOW:
//                return "Stack overflow";
//            case GL11.GL_STACK_UNDERFLOW:
//                return "Stack underflow";
//            case GL11.GL_OUT_OF_MEMORY:
//                return "Out of memory";
//            case ARBImaging.GL_TABLE_TOO_LARGE:
//                return "Table too large";
//            case EXTFramebufferObject.GL_INVALID_FRAMEBUFFER_OPERATION_EXT:
//                return "Invalid framebuffer operation";
//            default:
//                return "ErrorCode " + error_code;
//        }
//    }
//
//    public void updateDisplay() {
//        glfwSwapBuffers(windowId);// swap the color buffers
//    }
//
//    public boolean isCloseRequested() {
//        return glfwWindowShouldClose(windowId);
//    }
//
//    void setVSync_impl(boolean b) {
//        if (this.vendor != GPUVendor.INTEL) {
//            int vsync = 0;
//            if (b) {
//                vsync = -1;
//            }
//            glfwSwapInterval(vsync);
//        }
//    }
//
//    public void updateInput() {
//        glfwPollEvents();
//    }
//
//    String lastTitle = "";
//    public void setTitle(String title) {
//        if (!lastTitle.equals(title)) {
//            glfwSetWindowTitle(windowId, title);
//            lastTitle = title;
//        }
//        
//    }
//
//    protected void limitFpsTo(int fpsLimit) {
//        long now = System.nanoTime();
//        long el = now - timeLastFrame;
//        timeLastFrame = now;
//        double elD = el / 10000000.D;
//        int timePerFrame = (int) Math.ceil(1000.0D / (float) fpsLimit);
//        int sleepD = (int) Math.ceil(timePerFrame - elD);
//        if (sleepD >= 1) {
//            try {
//                Thread.sleep((long) sleepD);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void runFrame() {
//        if (toggleTiming) {
//            toggleTiming = false;
//        }
//        
//        
//        if (isCloseRequested()) {
//            shutdown();
//            return;
//        }
//        checkResize();
//        if (!this.running) {
//            return;
//        }
//        updateTime();
//        if (GL_ERROR_CHECKS)
//            checkGLError("pre render");
//        
//        
//        updateInput();
//        if (!this.running) {
//            return;
//        }
//        input(renderTime);
//        
//        
//        if (!this.running) {
//            return;
//        }
//        
//        if (Mouse.isGrabbed() != needsGrab()) {
//            
//            Mouse.setGrabbed(needsGrab());
////            if (b != this.movement.grabbed()) {
////                setGrabbed(b);
////            }
//        }
//        preRenderUpdate(renderTime);
//        
//        //        if (!startRender) {
//        //            try {
//        //                Thread.sleep(10);
//        //            } catch (InterruptedException e) {
//        //                e.printStackTrace();
//        //            }
//        //            return;
//        //        }
//        if (!this.running) {
//            return;
//        }
//        
//        render(renderTime);
//        if (!this.running) {
//            return;
//        }
//        
//        postRenderUpdate(renderTime);
//        if (!this.running) {
//            return;
//        }
//        
//        //        if (Main.DO_TIMING) TimingHelper.start(14);
//        //        GL11.glFlush();
//        //        if (Main.DO_TIMING) TimingHelper.end(14);
//        if (GL_ERROR_CHECKS)
//            checkGLError("render");
//        
//        updateDisplay();
//        
//        
//      //TODO: add second counter that calculates frame time len without vsync so we can use this one for frame time based calculation
//        frameTime = System.nanoTime();
//        
//        
//        if (GL_ERROR_CHECKS)
//            checkGLError("Post render");
//        fpsCounter++;
//        fpsInterval = (float) ((timer.absTime - timeLastFPS) / 1000.0D);
//        if (fpsInterval >= 0.5F) {
//            timeLastFPS = timer.absTime;
//            lastFPS = (int) (fpsCounter / fpsInterval);
//            fpsCounter = 0;
//            onStatsUpdated();
//        }
//        
//    }
//    int fpsCounter;
//    float fpsInterval;
//    
//    public boolean needsGrab() {
//        return this.needsGrab && this.hasWindowFocus;
//    }
//
//    public ArrayList<String> glProfileResults = new ArrayList<>();
//
//
//    public void mainLoop() {
//        try {
//            this.running = true;
//            this.wasrunning = true;
//            initGame();
//            if (GL_ERROR_CHECKS)
//                checkGLError("initGame");
//            onWindowResize(windowWidth, windowHeight);
//            setDefaultViewport();
//            if (GL_ERROR_CHECKS)
//                checkGLError("initGame onResize");
//            timer.calculate();
//            timeLastFrame = System.nanoTime();
//            timeLastFPS = timer.absTime;
//            lateInitGame();
//            isStarting = false;
//            if (GL_ERROR_CHECKS)
//                checkGLError("initGame lateInitGame");
//            setVSync(this.vsync);
//            long a = 0;
//            int i = 0;
//            while (this.running) {
//                i++;
//                long s = System.nanoTime();
//                runFrame();
//                s = (System.nanoTime() - s)/1000L;
//                
//                a = (a*99L+s)/100L;
//                if (s > a*2L) {
////                    System.out.println("slow frame "+s);
//                }
////                if (i%200==0) {
//////                    System.out.println(a);
////                }
//                if (i == 1000) {
////                    GL_ERROR_CHECKS=false;
//                }
//            }
//        } catch (Throwable t) {
//            if (DEBUG_LAYER)
//                t.printStackTrace();
//            showErrorScreen("The game crashed", Arrays.asList(new String[] { "An unexpected exception occured" }), t, true);
//        } finally {
//            if (this.wasrunning) {
////                onDestroy();
//            }
////            destroyContext();
////            Thread t2 = new Thread() {
////                public void run() {
////                    try {
////                        //temp hack to clear up references so java can die
////                        System.gc();
////                        Thread.sleep(1000);
////                        System.gc();
////                        Thread.sleep(1000);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
////                };
////            };
////            t2.setName("watch");
////            t2.start();
//        }
//    }
//
//    public void initGLContext() {
//        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glClearColor");
//        glActiveTexture(GL_TEXTURE0);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glActiveTexture");
//        glDisable(GL_DITHER);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glEnable(GL_DITHER)");
//        glEnable(GL_BLEND);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glEnable(GL_BLEND)");
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)");
//        glEnable(GL_DEPTH_TEST);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glEnable(GL_DEPTH_TEST)");
//        glDepthFunc(GL_LEQUAL);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glDepthFunc(GL_LEQUAL)");
//        GL11.glDepthMask(true);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glDepthMask(true)");
//        glColorMask(true, true, true, true);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glColorMask(true, true, true, true)");
//        glEnable(GL_CULL_FACE);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glEnable(GL_CULL_FACE)");
//        glCullFace(GL_BACK);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glCullFace(GL_BACK)");
//        int fastNice = GL_NICEST;//GL_FASTEST;
//        glHint(GL_POINT_SMOOTH_HINT, fastNice);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glHint(GL_POINT_SMOOTH_HINT, fastNice)");
//        glHint(GL_LINE_SMOOTH_HINT, fastNice);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glHint(GL_LINE_SMOOTH_HINT, fastNice)");
//        glHint(GL_POLYGON_SMOOTH_HINT, fastNice);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glHint(GL_POLYGON_SMOOTH_HINT, fastNice)");
//        glHint(GL13.GL_TEXTURE_COMPRESSION_HINT, fastNice);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glHint(GL13.GL_TEXTURE_COMPRESSION_HINT, fastNice)");
//        glHint(GL20.GL_FRAGMENT_SHADER_DERIVATIVE_HINT, fastNice);
//        if (GL_ERROR_CHECKS)
//            checkGLError("glHint(GL20.GL_FRAGMENT_SHADER_DERIVATIVE_HINT, fastNice)");
//        GL11.glGetError();
//        setVSync(true);
//        if (GL_ERROR_CHECKS)
//            checkGLError("setVSync(true)");
//    }
//
//    public void updateTime() {
//        timer.calculate();
//        ticksran += timer.ticks;
//        renderTime = timer.partialTick;
//        absTime = ((ticksran+renderTime)/(float)GameBase.TICKS_PER_SEC);
//        for (int i = 0; i < timer.ticks; i++) {
//            this.tick();
//            tick++;
//        }
//    }
//
//    private void showErrorScreen(String title, List<String> desc, Throwable throwable, boolean b) {
//        try {
//            if (this.wasrunning) {
//                onDestroy();
//            }
//            destroyContext();
//            for (String s : desc) {
//                System.err.println(s);
//            }
//            if (throwable != null)
//            throwable.printStackTrace();
//
//            if (throwable instanceof ShaderCompileError) {
//                ShaderCompileError sce = (ShaderCompileError) throwable;
//                System.out.println(sce.getLog());
//            }
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }
//    }
//
//    public long getTime() {
//        return timer.absTime;
//    }
//    
//
//
//    protected abstract void onTextInput(long window, int codepoint);
//    
//    protected abstract void onKeyPress(long window, int key, int scancode, int action, int mods);
//
//    protected abstract void onWheelScroll(long window, double xoffset, double yoffset);
//
//    public abstract void render(float f);
//
//    public abstract void preRenderUpdate(float f);
//
//    public abstract void postRenderUpdate(float f);
//
//    public abstract void setRenderResolution(int renderWidth, int renderHeight);
//    
//    public void onWindowResize(int displayWidth, int displayHeight) {
//        setRenderResolution(displayWidth, displayHeight);
//    }
//
//    public abstract void tick();
//
//    public abstract void initGame();
//
//    public abstract void lateInitGame();
//    
//    public abstract void onStatsUpdated();
//
//
//    public void onMouseClick(long window, int button, int action, int mods) {
//    }
//    public void setGrabbed(boolean b) {
//    }
//
//    public boolean isGrabbed() {
//        return wasGrabbed;
//    }
//
//    public void input(float fTime) {
//    }
//    public boolean checkGLError(String s) {
//        int i = GL11.glGetError();
//        if (i != 0) {
//            String s1 = GameBase.getGlErrorString(i);
//            throw new GameError("Error - " + s + ": " + s1);
//        }
//        return false;
//    }
//    
//
//    public void updateRenderResolution(int w, int h) {
//        displayWidth = w;
//        displayHeight = h;
//    }
//
//    final static int[] viewport = new int[] {0,0,0,0};
//    public static int[] getViewport() {
//        return viewport;
//    }
//
//    public void setDefaultViewport() {
//        setViewport(0, 0, displayWidth, displayHeight);
//    }
//    public void setViewport(int x, int y, int w, int h) {
//        if (viewport[0] != x || viewport[1] != y || viewport[2] != w || viewport[3] != h) {
//            viewport[0] = x;
//            viewport[1] = y;
//            viewport[2] = w;
//            viewport[3] = h;
//            GL11.glViewport(x, y, w, h);
//        }
//    }
//}