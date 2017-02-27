package nidefawl.qubes;

import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.glfw.GLFWVulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugReport;
import org.lwjgl.vulkan.NVGLSLShader;
import org.lwjgl.vulkan.VkInstance;

import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.LoadingScreen;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.KeybindManager;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.logging.IErrorHandler;
import nidefawl.qubes.logging.LogBufferStream;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.GPUTaskProfile;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.ShaderSource;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vr.VR;
import nidefawl.qubes.vulkan.VKContext;
import nidefawl.qubes.vulkan.VulkanErr;
import nidefawl.qubes.vulkan.VulkanInit;
import nidefawl.qubes.worldgen.TerrainGen;

public abstract class GameBase implements Runnable, IErrorHandler {
    public static String  appName         = "";
    public static int     windowWidth;
    public static int     windowHeight;
    public static int     displayWidth;
    public static int     displayHeight;
    public static int     guiWidth;
    public static int     guiHeight;
    public static boolean GL_ERROR_CHECKS = true;
    public static boolean VR_SUPPORT = false;
    public static boolean DEBUG_LAYER = false;
    public static long    windowId        = 0;
    public static long    windowSurface   = 0;
    protected static int            initWidth       = (int) (1920);
    protected static int            initHeight      = (int) (1080);
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
    static boolean hasTextHook = false;

    public static boolean      toggleTiming;
    public static boolean      DO_TIMING   = false;
    public static float        renderTime;
    public static float        absTime;
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
    protected volatile boolean hasWindowFocus = true;
    protected volatile boolean useWindowSizeAsRenderResolution = true;
    protected boolean renderGui3d = false;
    protected boolean isStarting = true;
    private Thread             thread;
    private int newWidth = initWidth;
    private int newHeight = initHeight;
    private GPUVendor vendor = GPUVendor.OTHER;
    static public GameBase baseInstance;
    public static LoadingScreen loadingScreen;
    public KeybindManager  movement = new KeybindManager();
    public Gui            gui;
    boolean               reinittexthook  = false;
    boolean               wasGrabbed      = true;
    public GLCapabilities caps;
    public final boolean isVulkan;
    public VKContext vkContext;

    public void startGame() {
        this.thread = new Thread(this, appName + " main thread");
        this.thread.setPriority(Thread.MAX_PRIORITY);
        this.thread.start();
    }

    public GameBase() {
        this.isVulkan = Boolean.valueOf(System.getProperty("renderer.vulkan"));
        if (this.isVulkan) {
            GL_ERROR_CHECKS = false;
        }
        baseInstance = this;
        this.timer = new Timer(TICKS_PER_SEC);
        displayWidth = initWidth;
        displayHeight = initHeight;
        guiWidth = displayWidth;
        guiHeight = displayHeight;
        windowWidth = displayWidth;
        windowHeight = displayHeight;
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
            initDisplay(DEBUG_LAYER);
            if (isVulkan) {
                
            } else {
                initGLContext();
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("early initGLContext");
                glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT|GL_STENCIL_BUFFER_BIT);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("early glClear");
            }
            updateDisplay();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("early updateDisplay");
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }
        if (isVulkan) {
            
        } else {
            try {
                List<String> list = GL.validateCaps(caps);
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
                TerrainGen.init();
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
        }
        mainLoop();
    }

    void initCallbacks() {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
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
            public void invoke(long window, boolean focused) {
                try {
                    hasWindowFocus = focused;
                    if (!hasWindowFocus)
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
            if (glfwInit() != true)
                throw new IllegalStateException("Unable to initialize GLFW");
            if (isVulkan) {
                if (!glfwVulkanSupported()) {
                    throw new AssertionError("GLFW failed to find the Vulkan loader");
                }

                String[] requiredInstanceExtensions = new String[0];
                String[] requiredDeviceExtensions = new String[] {
                        VK_KHR_SWAPCHAIN_EXTENSION_NAME, 
//                        NVGLSLShader.VK_NV_GLSL_SHADER_EXTENSION_NAME
                    };
                String[] activeLayers = new String[0];
                if (debugContext) {
                    activeLayers = new String[] {
                        "VK_LAYER_LUNARG_standard_validation"
                    };
                    requiredInstanceExtensions = new String[] {
                        "VK_EXT_debug_report"
                    };
                }
                VulkanInit.initStatic(activeLayers, requiredInstanceExtensions, requiredDeviceExtensions, debugContext);

                // Create the Vulkan instance
                Engine.vkContext = vkContext = VulkanInit.createContext();
//                final VkDebugReportCallbackEXT debugCallback = new VkDebugReportCallbackEXT() {
//                    public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
//                        System.err.println("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));
//                        return 0;
//                    }
//                };
//                final long debugCallbackHandle = setupDebugging(instance, VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback);
//                final VkPhysicalDevice physicalDevice = getFirstPhysicalDevice(instance);
//                final DeviceAndGraphicsQueueFamily deviceAndGraphicsQueueFamily = createDeviceAndGetGraphicsQueueFamily(physicalDevice);
//                final VkDevice device = deviceAndGraphicsQueueFamily.device;
//                int queueFamilyIndex = deviceAndGraphicsQueueFamily.queueFamilyIndex;
//                final VkPhysicalDeviceMemoryProperties memoryProperties = deviceAndGraphicsQueueFamily.memoryProperties;

            }
            // Configure our window
            glfwDefaultWindowHints();// optional, the current window hints are already the default
            glfwWindowHint(GLFW_VISIBLE, GLFW.GLFW_FALSE);// the window will stay hidden after creation
            glfwWindowHint(GLFW_RESIZABLE, GLFW.GLFW_TRUE);// the window will be resizable
            if (isVulkan) {
                glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

            } else {
                glfwWindowHint(GLFW_SAMPLES, 0);
                if (!debugContext) {
//                  glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
//                  glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 4);
//                  glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
                }

                if (!GL_ERROR_CHECKS) {
                    glfwWindowHint(GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
                } else {
                    glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
                }
                glfwWindowHint(GLFW_CONTEXT_ROBUSTNESS, GLFW.GLFW_NO_ROBUSTNESS);
                //            glfwWindowHint(GLFW_RED_BITS, 8);
                //            glfwWindowHint(GLFW_GREEN_BITS, 8);
                //            glfwWindowHint(GLFW_BLUE_BITS, 8);
                //            glfwWindowHint(GLFW_ALPHA_BITS, 8);
                glfwWindowHint(GLFW_DEPTH_BITS, 24);
                glfwWindowHint(GLFW_STENCIL_BITS, 1);
                //            glfwWindowHint(GLFW_DOUBLE_BUFFER, GL_TRUE);//Check Version
            }

            // Create the window
            windowId = glfwCreateWindow(displayWidth, displayHeight, getAppTitle(), NULL, NULL);
            if (windowId == NULL)
                throw new RuntimeException("Failed to create the GLFW window");
            // Make the OpenGL context current
            Mouse.init();

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            // Center our window
            glfwSetWindowPos(windowId, (vidmode.width() - displayWidth) / 2, (vidmode.height() - displayHeight) / 2);

            glfwShowWindow(windowId);
            glfwSetWindowSizeCallback(windowId, cbWindowSize);
            glfwSetKeyCallback(windowId, cbKeyboard);
            glfwSetMouseButtonCallback(windowId, cbMouseButton);
            glfwSetScrollCallback(windowId, cbScrollCallback);
            glfwSetWindowFocusCallback(windowId, cbWindowFocus);
            glfwSetCursorPosCallback(windowId, cbCursorPos);
            if (isVulkan) {
                try ( MemoryStack stack = stackPush() ) {
                    LongBuffer pSurface = stack.longs(0);
                    int err = glfwCreateWindowSurface(vkContext.vk, windowId, null, pSurface);
                    if (err != VK_SUCCESS) {
                        throw new AssertionError("Failed to create surface: " + VulkanErr.toString(err));
                    }
                    windowSurface = pSurface.get(0);
                }
                VulkanInit.initContext(vkContext, windowSurface, windowWidth, windowHeight, vsync);
            } else {
                glfwMakeContextCurrent(windowId);
                caps = org.lwjgl.opengl.GL.createCapabilities();
                // Make the window visible
//                glfwSetFramebufferSizeCallback(windowId, new GLFWFramebufferSizeCallback() {
//                    
//                    @Override
//                    public void invoke(long window, int width, int height) {
//                        System.out.println("FRAMEBUFFER NOW "+width+"/"+height);
//                    }
//                });

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
//                    if (KHRDebug. != null) {
//                        GLDebugLog.setup();
//                        _checkGLError("GLDebugLog.setup()");
////                    }
//                    _checkGLError("Pre startup");
                }
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("initDisplay");
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public String getAppTitle() {
        return appName;
    }

    protected void destroyContext() {
        if (isVulkan) {
            if (vkContext != null) {
                try {
                    vkContext.shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            VulkanInit.destroyStatic();
        }
        cbKeyboard.free();
        cbMouseButton.free();
        cbScrollCallback.free();
        cbWindowSize.free();
        cbWindowFocus.free();
        cbCursorPos.free();
        errorCallback.free();
        cbText.free();
        glfwTerminate();
        windowId = 0;
    }

    protected void onDestroy() {
        TextureManager.getInstance().destroy();
        FontRenderer.destroy();
        Tess.destroyAll();
    }

    public void shutdown() {
        this.running = false;
        Engine.stop();
        AsyncTasks.shutdown();
        if (VR.isInit()) VR.shutdown();
        FontRenderer.destroy();
        destroyContext();
    }

    public void checkResize() {
        if (minimized && newWidth*newHeight>0) {
            minimized = false;
            if (isRunning()&&!isVulkan)
                Engine.setDefaultViewport();
        }
        if (newWidth != windowWidth || newHeight != windowHeight) {
            if (newWidth*newHeight <= 0) {
                minimized = true;
                return;
            }
            System.out.printf("Resize %d,%d -> %d,%d\n", windowWidth, windowHeight, newWidth, newHeight);
            minimized = false;
            windowWidth = newWidth;
            windowHeight = newHeight;
            if (windowWidth <= 0) {
                windowWidth = 1;
            }
            if (windowHeight <= 0) {
                windowHeight = 1;
            }
            if (!canRenderGui3d()) {
                guiWidth = windowWidth;
                guiHeight = windowHeight;
            }
            if (useWindowSizeAsRenderResolution) {
                displayWidth = windowWidth;
                displayHeight = windowHeight;
            }
            try {
                onWindowResize(windowWidth, windowHeight);
                if (vkContext != null) {
                    vkContext.updateSwapchain(windowWidth, windowHeight, vsync);
                }
                if (isRunning()&&!isVulkan)
                    Engine.setDefaultViewport();
            } catch (Throwable t) {
                setException(new GameError("GLFWWindowSizeCallback", t));
            }
        } else if (vkContext != null && vkContext.reinitSwapchain) {
            vkContext.updateSwapchain(windowWidth, windowHeight, vsync);
        }
    }

    public abstract void onStatsUpdated();

    public void setVSync(boolean b) {
        if (VR_SUPPORT) {
            b = false;
        }
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
        if (!isVulkan) {
            glfwSwapBuffers(windowId);// swap the color buffers
        }
    }

    public boolean isCloseRequested() {
        return glfwWindowShouldClose(windowId);
    }

    void setVSync_impl(boolean b) {
        if (isVulkan) {
            if (vkContext.swapChain.isVsync() != b)
                vkContext.reinitSwapchain = true;
        } else if (this.vendor != GPUVendor.INTEL) {
            int vsync = 0;
            if (b) {
                vsync = -1;
            }
            glfwSwapInterval(vsync);
        }
    }

    public void updateInput() {
        glfwPollEvents();
        updateGuiContext();
    }

    private void updateGuiContext() {
        if (GuiContext.input != null && !GuiContext.input.focused) {
            GuiContext.input.focused=false;
            GuiContext.input = null;
        }
        boolean reqTextHook=(GuiContext.input != null && GuiContext.input.isFocusedAndContext());
        if (hasTextHook()!=reqTextHook) {
            System.out.println("reinit text hook -> "+reqTextHook);
            setTextHook(reqTextHook);
        }
        if (!canRenderGui3d()) {
            GuiContext.mouseX = Mouse.getX();
            GuiContext.mouseY = Mouse.getY();   
        }
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
        Stats.resetDrawCalls();
        
        if (isCloseRequested()) {
            if (vkContext != null) {
                vkContext.syncAllFences();
            }
            shutdown();
            return;
        }
        
        checkResize();
        if (!this.running) {
            return;
        }
        updateTime();
        Stats.uniformCalls = 0;
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("pre render");
        
        
        updateInput();
        if (!this.running) {
            return;
        }
        input(renderTime);
        
        
        if (!this.running) {
            return;
        }
        
        if (Mouse.isGrabbed() != needsGrab()) {
            
            Mouse.setGrabbed(needsGrab());
//            if (b != this.movement.grabbed()) {
//                setGrabbed(b);
//            }
        }
        boolean b = Mouse.isGrabbed();
        if (b != this.movement.grabbed()) {
            setGrabbed(b);
        }
        if (vkContext != null) {
            Engine.preRenderUpdateVK();
            vkContext.preRender();
        }
        preRenderUpdate(renderTime);
        if (vkContext != null) {
            vkContext.finishUpload();
        }
        
        //        if (!startRender) {
        //            try {
        //                Thread.sleep(10);
        //            } catch (InterruptedException e) {
        //                e.printStackTrace();
        //            }
        //            return;
        //        }
        if (!this.running) {
            return;
        }
        
        render(renderTime);
        if (!this.running) {
            return;
        }
        
        postRenderUpdate(renderTime);
        if (!this.running) {
            return;
        }
        if (vkContext != null) {
            vkContext.postRender();
        }
        
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
        Stats.lastFrameTimeD = took;
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

    public boolean needsGrab() {
        return this.needsGrab && this.hasWindowFocus;
    }

    public ArrayList<String> glProfileResults = new ArrayList<>();


    public void mainLoop() {
        try {
            this.running = true;
            this.wasrunning = true;
            initGame();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("initGame");
            onWindowResize(displayWidth, displayHeight);
            if (!isVulkan) {
                Engine.setDefaultViewport();
            }
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("initGame onResize");
            timer.calculate();
            timeLastFrame = System.nanoTime();
            timeLastFPS = timer.absTime;
            if (isVulkan) {
                vkContext.lateInit(0);    
            }
            lateInitGame();
            if (isVulkan) {
                vkContext.lateInit(1);    
            }
            isStarting = false;
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("initGame lateInitGame");
            setVSync(this.vsync);
            long a = 0;
            int i = 0;
            while (this.running) {
                i++;
                long s = System.nanoTime();
                runFrame();
                s = (System.nanoTime() - s)/1000L;
                
                a = (a*99L+s)/100L;
                if (s > a*2L) {
//                    System.out.println("slow frame "+s);
                }
//                if (i%200==0) {
////                    System.out.println(a);
//                }
                if (i == 1000) {
//                    GL_ERROR_CHECKS=false;
                }
                DumbPool.reset();
            }
        } catch (Throwable t) {
            showErrorScreen("The game crashed", Arrays.asList(new String[] { "An unexpected exception occured" }), t, true);
        } finally {
            if (this.wasrunning) {
//                onDestroy();
            }
//            destroyContext();
//            Thread t2 = new Thread() {
//                public void run() {
//                    try {
//                        //temp hack to clear up references so java can die
//                        System.gc();
//                        Thread.sleep(1000);
//                        System.gc();
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                };
//            };
//            t2.setName("watch");
//            t2.start();
        }
    }

    public void initGLContext() {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glClearColor");
        glActiveTexture(GL_TEXTURE0);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glActiveTexture");
        glDisable(GL_DITHER);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glEnable(GL_DITHER)");
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
        glHint(GL_POINT_SMOOTH_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL_POINT_SMOOTH_HINT, fastNice)");
        glHint(GL_LINE_SMOOTH_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL_LINE_SMOOTH_HINT, fastNice)");
        glHint(GL_POLYGON_SMOOTH_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL_POLYGON_SMOOTH_HINT, fastNice)");
        glHint(GL13.GL_TEXTURE_COMPRESSION_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL13.GL_TEXTURE_COMPRESSION_HINT, fastNice)");
        glHint(GL20.GL_FRAGMENT_SHADER_DERIVATIVE_HINT, fastNice);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glHint(GL20.GL_FRAGMENT_SHADER_DERIVATIVE_HINT, fastNice)");
        GL11.glGetError();
        setVSync(true);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("setVSync(true)");
    }

    public void updateTime() {
        timer.calculate();
        ticksran += timer.ticks;
        renderTime = timer.partialTick;
        absTime = ((ticksran+renderTime)/(float)GameBase.TICKS_PER_SEC);
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
            for (String s : desc) {
                System.err.println(s);
            }
            if (throwable != null)
            throwable.printStackTrace();

            if (throwable instanceof ShaderCompileError) {
                ShaderCompileError sce = (ShaderCompileError) throwable;
                System.out.println(sce.getLog());
            }
            Thread.sleep(150000);
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
    
    public static boolean hasTextHook() {
        return hasTextHook;
    }
    public void setTextHook(boolean state) {
        hasTextHook = state;
        if (state) {
            glfwSetCharCallback(windowId, cbText);
        } else {
            glfwSetCharCallback(windowId, null);
        }
    }


    protected abstract void onTextInput(long window, int codepoint);
    
    protected abstract void onKeyPress(long window, int key, int scancode, int action, int mods);

    protected abstract void onWheelScroll(long window, double xoffset, double yoffset);

    public abstract void render(float f);

    public abstract void preRenderUpdate(float f);

    public abstract void postRenderUpdate(float f);

    public abstract void setRenderResolution(int renderWidth, int renderHeight);
    
    public void onWindowResize(int displayWidth, int displayHeight) {
        setRenderResolution(displayWidth, displayHeight);
    }

    public abstract void tick();

    public abstract void initGame();

    public abstract void lateInitGame();

    
    public void showGUI(Gui gui) {
        if (gui==this.gui) {
            return;
        }
        if (gui != null && this.gui == null) {
            if (!canRenderGui3d() && Mouse.isGrabbed()) {
                setGrabbed(false);
                wasGrabbed = true;
            }
        }
        if (this.gui != null) {
            this.gui.onClose();
            onGuiClosed(this.gui, gui);
        }
        Gui prevGui = this.gui;
        this.gui = gui;
        if (this.gui != null) {
            this.gui.setPos(0, 0);
            this.gui.setSize(guiWidth, guiHeight);
            this.gui.initGui(this.gui.firstOpen);
            onGuiOpened(this.gui, prevGui);
            this.gui.firstOpen = false;
            if (!canRenderGui3d() && Mouse.isGrabbed()) {
                setGrabbed(false);
                wasGrabbed = true;
            }
        } else {
            if (wasGrabbed) {
                Game.instance.setGrabbed(true);
            }
            wasGrabbed = false;
        }
        reinittexthook = true;
    }

    public void onGuiClosed(Gui gui, Gui targetGui) {
    }

    public void onGuiOpened(Gui gui, Gui prevGui) {
    }

    int throttleClick=0;
    boolean needsGrab = false; // for non-game impl. only
    public void onMouseClick(long window, int button, int action, int mods) {
        if (this.gui != null) {
//            if (this.world == null) {
                if (GuiWindowManager.onMouseClick(button, action)) {
                    return;
                }
//            }
            if (!this.gui.onMouseClick(button, action)) {
            }
        } else {
            if (GuiWindowManager.onMouseClick(button, action)) {
                return;
            }


            boolean b = Mouse.isGrabbed();
            boolean isDown = Mouse.getState(action);
            if (throttleClick > 0) {
                return;
            }
//            if (b)
//                dig.onMouseClick(button, isDown);
            switch (button) {
                case 0:
//                    selection.clicked(button, isDown);
//                    if (this.player != null) {
//                        this.player.clicked(button, isDown);
//                    }
                    break;
                case 1:
                    if (isDown ) {
                        this.needsGrab = !this.needsGrab;
                    }
                    break;
                case 2:
//                    selection.clicked(button, isDown);
                    break;
            }
            if (b != this.movement.grabbed()) {
                setGrabbed(b);
            }
        }
    }
    public void setGrabbed(boolean b) {
        if (b != this.movement.grabbed()) {
            this.movement.setGrabbed(b);
            Mouse.setCursorPosition(displayWidth / 2, displayHeight / 2);
            Mouse.setGrabbed(b);
        }
    }

    public boolean isGrabbed() {
        return this.movement.grabbed();
    }

    public void input(float fTime) {
        double mdX = Mouse.getDX();
        double mdY = Mouse.getDY();
        if (this.movement.grabbed()) {
            this.movement.update(mdX, -mdY);
        } else {
            GuiWindowManager.mouseMove(mdX, -mdY);
        }
    }

    public Gui getGui() {
        return this.gui;
    }

    static String getValue(String[] args, int i, String arg) {
        String val = i+1<args.length ? args[i+1] : null;
        int idx = args[i].indexOf("=");
        if (idx > 0 && args[i].length()>idx+1) {
            val = args[i].substring(idx+1);
        }
        if (val != null) {
            if (val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length()-1);
            }
            if (val.startsWith("'") && val.endsWith("'")) {
                val = val.substring(1, val.length()-1);
            }
        }
        if (val == null || val.isEmpty()) {
            throw new RuntimeException("Please provide arg for --"+arg);
        }
        return val;
    }
    public void parseCmdArgs(String[] args) {
    }

    public void toggleVR() {
        boolean hadVR = VR_SUPPORT;
        if (!VR.initCalled) {
            VR.initApp();
        }
        VR_SUPPORT = VR.isInit() && !VR_SUPPORT;
        if (!VR_SUPPORT && hadVR) {
            updateGui3dMode();
            Game.displayWidth=windowWidth;
            Game.displayHeight=windowHeight;
            setRenderResolution(displayWidth, displayHeight);
            Engine.resizeProjection(Game.displayWidth, Game.displayHeight);
            Engine.setViewport(0, 0, Game.displayWidth, Game.displayHeight);
            if (Engine.outRenderer != null)
                Engine.outRenderer.initShaders();
        } else if (VR_SUPPORT && !hadVR) {
            updateGui3dMode();
            Game.displayWidth=VR.renderWidth;
            Game.displayHeight=VR.renderHeight;
            setRenderResolution(displayWidth, displayHeight);
            Engine.resizeProjection(Game.displayWidth, Game.displayHeight);
            Engine.setViewport(0, 0, Game.displayWidth, Game.displayHeight);
            if (Engine.outRenderer != null)
                Engine.outRenderer.initShaders();
            setVSync(false);
        }
    }
    protected void updateGui3dMode() {
        
    }

    protected void setSceneViewport() {
        if (VR_SUPPORT) {
            Game.displayWidth=VR.renderWidth;
            Game.displayHeight=VR.renderHeight;
        } else {
            Game.displayWidth=windowWidth;
            Game.displayHeight=windowHeight;
        }
        updateProjection();
    }
    protected void setVRViewport() {
        Game.displayWidth=VR.renderWidth;
        Game.displayHeight=VR.renderHeight;
        updateProjection();
    }
    protected void setWindowViewport() {
        Game.displayWidth=windowWidth;
        Game.displayHeight=windowHeight;
        updateProjection();
    }
    protected void setGUIViewport() {
        Game.displayWidth=guiWidth;
        Game.displayHeight=guiHeight;
        updateProjection();
    }
    protected void updateProjection() {
        Engine.resizeProjection(Game.displayWidth, Game.displayHeight);
        Engine.setViewport(0, 0, Game.displayWidth, Game.displayHeight);
        UniformBuffer.updateOrtho();
    }

    public void onControllerButton(int controllerIdx, int button, int eventType) {
    }
    
    public boolean canRenderGui3d() {
        return this.renderGui3d;
    }

    public void rebuildRenderCommands() {
    }
}
