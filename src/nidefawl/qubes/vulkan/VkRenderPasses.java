package nidefawl.qubes.vulkan;

public class VkRenderPasses {
    static boolean isInit = false;
    public static VkRenderPass passShadow = new VkRenderPassShadow();
    public static VkRenderPass passSubpassSwapchain = new VkRenderPassSubpassedSwapchain();
    public static VkRenderPass passTerrain = new VkRenderPassGBuffer();

    public static void init(VKContext ctxt) {
        isInit = true;
        passShadow.destroyRenderPass(ctxt);
        passShadow.build(ctxt);
        passTerrain.destroyRenderPass(ctxt);
        passTerrain.build(ctxt);
        passSubpassSwapchain.destroyRenderPass(ctxt);
        passSubpassSwapchain.build(ctxt);
    }

    public static void destroyShutdown(VKContext vkContext) {
        isInit = false;
        passShadow.destroy(vkContext);
        passTerrain.destroy(vkContext);
        passSubpassSwapchain.destroy(vkContext);
    }

    public static boolean isInit() {
        return isInit;
    }

    public static void initClearValues(boolean inverseZ) {
        passShadow.initClearValues(inverseZ);
        passTerrain.initClearValues(inverseZ);
        passSubpassSwapchain.initClearValues(inverseZ);
    }
}
