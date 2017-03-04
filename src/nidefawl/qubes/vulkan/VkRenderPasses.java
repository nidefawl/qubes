package nidefawl.qubes.vulkan;

public class VkRenderPasses {
    static boolean isInit = false;
    public static VkRenderPass passGBufferSubpass = new VkRenderPassGBufferSubpass();
    public static VkRenderPass passSubpassSwapchain = new VkRenderPassSubpassedSwapchain();
    public static VkRenderPass passgbuffer = new VkRenderPassGBuffer();

    public static void init(VKContext ctxt) {
        isInit = true;
//        passGBufferSubpass.destroyRenderPass(ctxt);
//        passGBufferSubpass.build(ctxt);
        passgbuffer.destroyRenderPass(ctxt);
        passgbuffer.build(ctxt);
        passSubpassSwapchain.destroyRenderPass(ctxt);
        passSubpassSwapchain.build(ctxt);
    }

    public static void destroyShutdown(VKContext vkContext) {
        isInit = false;
        passGBufferSubpass.destroy(vkContext);
        passgbuffer.destroy(vkContext);
        passSubpassSwapchain.destroy(vkContext);
    }

    public static boolean isInit() {
        return isInit;
    }

    public static void initClearValues(boolean inverseZ) {
//        passGBufferSubpass.initClearValues(inverseZ);
        passgbuffer.initClearValues(inverseZ);
        passSubpassSwapchain.initClearValues(inverseZ);
    }
}
