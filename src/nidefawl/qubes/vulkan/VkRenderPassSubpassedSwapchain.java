package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public class VkRenderPassSubpassedSwapchain extends VkRenderPass {

    public VkRenderPassSubpassedSwapchain() {
    }
    
    @Override
    public void build(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) 
        {
            reset();
            addColorAttachment(0, ctxt.colorFormat).format(ctxt.colorFormat)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            addDepthAttachment(1, ctxt.depthFormat).format(ctxt.depthFormat)
                    .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.callocStack(1, stack)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkAttachmentReference depthReference = VkAttachmentReference.callocStack(stack)
                    .attachment(1)
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            VkAttachmentReference.Buffer colorReference2 = VkAttachmentReference.callocStack(1, stack)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.callocStack(2, stack);
            subpasses.get(0)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(0)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorReference.remaining())
                    .pColorAttachments(colorReference)
                    .pDepthStencilAttachment(depthReference)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(null);
            subpasses.get(1)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(0)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorReference2.remaining())
                    .pColorAttachments(colorReference2)
                    .pDepthStencilAttachment(null)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(null);
            

            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.callocStack(3, stack);
            subpassDependencies.get(0)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .dstStageMask (VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask (VK_ACCESS_MEMORY_READ_BIT)
                    .dstAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
            subpassDependencies.get(1)
                    .srcSubpass(0)
                    .dstSubpass(1)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
            subpassDependencies.get(2)
                    .srcSubpass(1)
                    .dstSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask (VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask (VK_ACCESS_MEMORY_READ_BIT)
                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);

            buildRenderPass(ctxt, subpasses, subpassDependencies);
        }
    }
}
