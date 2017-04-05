package nidefawl.qubes.vulkan;

public class VkRenderPasses {
    public static VkRenderPass[] passes = new VkRenderPass[32];
    static boolean isInit = false;
    

//  public static VkRenderPass passSubpassSwapchain = new VkRenderPassSubpassedSwapchain();
    public static VkRenderPass passSwapchain = new VkRenderPassSwapchain();
    public static VkRenderPass passShadow = new VkRenderPassShadow();
    public static VkRenderPass passTerrain_Pass0 = new VkRenderPassGBuffer(0);
    public static VkRenderPass passTerrain_Pass1 = new VkRenderPassGBuffer(1);
    public static VkRenderPass passTerrain_Pass2 = new VkRenderPassGBuffer(2);
    public static VkRenderPass passSkyGenerate = new VkRenderPassSkyUpdate();
    public static VkRenderPass passSkyGenerateCubemap = new VkRenderPassSkyUpdateCubemap();
    public static VkRenderPass passSkySample = new VkRenderPassSkySample();
    public static VkRenderPass passPostRGBA16F = new VkRenderPassPost(true);
    public static VkRenderPass passFramebuffer = new VkRenderPassFrameBuffer(true, true);
    public static VkRenderPass passFramebufferNoClear = new VkRenderPassFrameBuffer(false, true);
    public static VkRenderPass passFramebufferNoDepth = new VkRenderPassFrameBuffer(true, false);
    public static VkRenderPass passTonemap = new VkRenderPassTonemap(false);
    public static VkRenderPass passPostTonemapOverlays = new VkRenderPassTonemap(true);
    public static VkRenderPass passAAEdge = new VkRenderPassAA(false);
    public static VkRenderPass passAAWeight = new VkRenderPassAA(true);
    public static VkRenderPassDeferred passPostDeferred = new VkRenderPassDeferred(true);
    public static VkRenderPassDeferred passPostDeferredNoClear = new VkRenderPassDeferred(false);

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
