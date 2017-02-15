package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_GREATER_OR_EQUAL;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS_OR_EQUAL;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;

import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkViewport;

import nidefawl.qubes.gl.Engine;

public class VkPipelines {

    public static VkPipeline main = new VkPipeline();
    public static VkPipeline screen2d = new VkPipeline();

    public static void buildPipeLine(VKContext vkContext, VkPipeline pipe) {
        pipe.destroyPipeLine(vkContext);
        pipe.viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
        pipe.viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
        pipe.viewport.width(vkContext.swapChain.width).height(vkContext.swapChain.height);
        pipe.scissors.extent().width(vkContext.swapChain.width).height(vkContext.swapChain.height);
        pipe.depthStencilState.depthCompareOp(Engine.INVERSE_Z_BUFFER ? VK_COMPARE_OP_GREATER_OR_EQUAL : VK_COMPARE_OP_LESS_OR_EQUAL);
        pipe.buildPipeline(vkContext);

    }

    public static void destroyShutdown(VKContext vkContext) {
        main.destroy(vkContext);
    }

}
