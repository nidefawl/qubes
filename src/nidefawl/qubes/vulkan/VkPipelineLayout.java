package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.opengl.EXTDebugLabel;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

public class VkPipelineLayout {

    public long pipelineLayout = VK_NULL_HANDLE;
    private String name;
    public VkPipelineLayout() {
    }

    public VkPipelineLayout(String name) {
        this.name = name;
    }
    

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
            System.out.println("pipeline layout "+this.name+" = "+Long.toHexString(pipelineLayout));
//            if (this.name != null) {
//                VkDebugMarkerObjectNameInfoEXT pNameInfo = VkDebugMarkerObjectNameInfoEXT.callocStack()
//                        .sType(EXTDebugMarker.VK_STRUCTURE_TYPE_DEBUG_MARKER_OBJECT_NAME_INFO_EXT)
//                        .pNext(0L).objectType(EXTDebugReport.VK_DEBUG_REPORT_OBJECT_TYPE_PIPELINE_EXT)
//                        .object(this.pipelineLayout).pObjectName(stack.UTF8(this.name));
//                EXTDebugMarker.vkDebugMarkerSetObjectNameEXT(ctxt.device, pNameInfo);
//            }
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

    public String getName() {
        return this.name;
    }

}
