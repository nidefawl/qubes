package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

public class VkPipelineLayout {

    public long pipelineLayout = VK_NULL_HANDLE;

    public void build(VKContext ctxt, long... descriptorSets) {
        build(ctxt, descriptorSets, null);
    }
    public void build(VKContext ctxt, long[] descriptorSets, VkPushConstantRange.Buffer pushconstantRanages) {
        try ( MemoryStack stack = stackPush() ) {
            VkPipelineLayoutCreateInfo pipelineLayoutCI = pipelineLayoutCreateInfo();
            LongBuffer pPipelineLayout = memAllocLong(1);
            LongBuffer pipelineLayoutCIDescPtr = memAllocLong(descriptorSets.length);
            pipelineLayoutCIDescPtr.put(descriptorSets);
            pipelineLayoutCIDescPtr.rewind();
            pipelineLayoutCI.pSetLayouts(pipelineLayoutCIDescPtr);
            pipelineLayoutCI.pPushConstantRanges(pushconstantRanages);
            int err = vkCreatePipelineLayout(ctxt.device, pipelineLayoutCI, null, pPipelineLayout);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreatePipelineLayout failed: " + VulkanErr.toString(err));
            }
            this.pipelineLayout = pPipelineLayout.get(0);
            System.out.println("pipeline layout "+pipelineLayout);
        }
    }
    public static VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo() {
        VkPipelineLayoutCreateInfo descriptorSetLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack();
        descriptorSetLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
        return descriptorSetLayoutCreateInfo;
    }
    public void destroy(VKContext vkContext) {
        vkDestroyPipelineLayout(vkContext.device, this.pipelineLayout, null);
    }

}
