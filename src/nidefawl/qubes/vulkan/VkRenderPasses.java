package nidefawl.qubes.vulkan;

public class VkRenderPasses {
    public static VkRenderPass[] passes = new VkRenderPass[8];
    static boolean isInit = false;
    

    public static VkRenderPass passSwapchain = new VkRenderPassSwapchain();
    public static VkRenderPass passShadow = new VkRenderPassShadow();
//    public static VkRenderPass passSubpassSwapchain = new VkRenderPassSubpassedSwapchain();
    public static VkRenderPass passTerrain = new VkRenderPassGBuffer();
    public static VkRenderPass passDeferred = new VkRenderPassDeferred();
    public static VkRenderPass passFramebuffer = new VkRenderPassFrameBuffer(true, true);
    public static VkRenderPass passFramebufferNoClear = new VkRenderPassFrameBuffer(false, true);
    public static VkRenderPass passFramebufferNoDepth = new VkRenderPassFrameBuffer(true, false);

    public static void init(VKContext ctxt) {
        isInit = true;
        for (int i = 0; i < passes.length; i++) {
            if (passes[i] != null) {
                passes[i].destroyRenderPass(ctxt);
                passes[i].build(ctxt);
            }
        }
    }
    
    public static boolean isInit() {
        return isInit;
    }

    public static void destroyShutdown(VKContext ctxt) {
        isInit = false;
        for (int i = 0; i < passes.length; i++) {
            if (passes[i] != null) {
                passes[i].destroy(ctxt);
            }
        }
    }

    public static void initClearValues(boolean inverseZ) {
        for (int i = 0; i < passes.length; i++) {
            if (passes[i] != null) {
                passes[i].initClearValues(inverseZ);
            }
        }
    }
    
    public static void registerPass(VkRenderPass vkRenderPass) {
        int idx = -1;
        for (int i = 0; i < passes.length; i++) {
            if (passes[i] == null) {
                idx = i;
            }
        }
        passes[idx] = vkRenderPass;
    }
}