package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkSubpassDependency.Buffer;

public class VkRenderPassGBuffer extends VkRenderPass {

    private Buffer subpassDependencies;
    public VkRenderPassGBuffer() {
        addColorAttachment(0, VK_FORMAT_R16G16B16A16_SFLOAT);
        addDepthAttachment(1, VK_FORMAT_D32_SFLOAT);
        attachments.limit(nAttachments);
        clearValues.limit(nAttachments);
    }
    @Override
    public void build(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) 
        {
            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.callocStack(nColorAttachments, stack);
            for (int i = 0; i < nColorAttachments; i++) {
                colorReference.get(i).attachment(i)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            }
            VkAttachmentReference depthReference = VkAttachmentReference.callocStack(stack)
                    .attachment(1)
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            this.subpassDependencies = VkSubpassDependency.callocStack(2, stack);
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
                    .dstSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask (VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask (VK_ACCESS_MEMORY_READ_BIT)
                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.callocStack(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(0)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorReference.remaining())
                    .pColorAttachments(colorReference)
                    .pDepthStencilAttachment(depthReference)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(null);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pNext(NULL)
                    .pAttachments(this.attachments)
                    .pSubpasses(subpasses)
                    .pDependencies(subpassDependencies);
            LongBuffer pRenderPass = stack.longs(0);
            int err = vkCreateRenderPass(ctxt.device, renderPassInfo, null, pRenderPass);
            this.renderPass = pRenderPass.get(0);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to create clear render pass: " + VulkanErr.toString(err));
            }
        }
    }
}
