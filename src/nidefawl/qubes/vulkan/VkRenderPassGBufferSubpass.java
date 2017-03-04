package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkSubpassDependency.Buffer;

public class VkRenderPassGBufferSubpass extends VkRenderPass {

    private Buffer subpassDependencies;
    public VkRenderPassGBufferSubpass() {
        addColorAttachment(0, VK_FORMAT_R16G16B16A16_SFLOAT);
        addDepthAttachment(1, VK_FORMAT_D32_SFLOAT);
        addColorAttachment(2, VK_FORMAT_R16G16B16A16_SFLOAT);
        addColorAttachment(3, VK_FORMAT_R16G16B16A16_UINT);
        addColorAttachment(4, VK_FORMAT_R16G16B16A16_SFLOAT);//was VK_FORMAT_R16G16B16_SFLOAT, but unsupported: TODO: check format support
        addColorAttachment(5, VK_FORMAT_R8G8B8A8_UNORM);
        addDepthAttachment(6, VK_FORMAT_D32_SFLOAT);
        attachments.limit(nAttachments);
        clearValues.limit(nAttachments);
    }
    @Override
    public void build(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) 
        {
            IntBuffer pPreserveShadowPass = stack.callocInt(2);
            VkAttachmentReference.Buffer colorRefShadowPass = VkAttachmentReference.callocStack(nColorAttachments, stack);
            colorRefShadowPass.get(0).attachment(5).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkAttachmentReference depthRefShadowPass = VkAttachmentReference.callocStack(stack)
                    .attachment(6)
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            pPreserveShadowPass.put(0, 5).put(1, 6);
            
            VkAttachmentReference.Buffer colorReferenceGbuffer = VkAttachmentReference.callocStack(nColorAttachments, stack);
            colorReferenceGbuffer.get(0).attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            colorReferenceGbuffer.get(1).attachment(2).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            colorReferenceGbuffer.get(2).attachment(3).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            colorReferenceGbuffer.get(3).attachment(4).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthRefGbuffer = VkAttachmentReference.callocStack(stack)
                    .attachment(1)
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            this.subpassDependencies = VkSubpassDependency.callocStack(3, stack);
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
            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.callocStack(2, stack);
            subpasses.get(0)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(0)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorRefShadowPass.remaining())
                    .pColorAttachments(colorRefShadowPass)
                    .pDepthStencilAttachment(depthRefShadowPass)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(null);
            subpasses.get(1)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(0)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorReferenceGbuffer.remaining())
                    .pColorAttachments(colorReferenceGbuffer)
                    .pDepthStencilAttachment(depthRefGbuffer)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(pPreserveShadowPass);
            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pNext(NULL)
                    .pAttachments(this.attachments)
                    .pSubpasses(subpasses)
                    .pDependencies(subpassDependencies);
            System.out.println(renderPassInfo.pSubpasses().get(0).pColorAttachments().get(0).layout());
            LongBuffer pRenderPass = stack.longs(0);
            int err = vkCreateRenderPass(ctxt.device, renderPassInfo, null, pRenderPass);
            this.renderPass = pRenderPass.get(0);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to create clear render pass: " + VulkanErr.toString(err));
            }
        }
    }
}
