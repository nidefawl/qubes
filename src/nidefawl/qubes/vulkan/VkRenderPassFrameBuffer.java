package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkSubpassDependency.Buffer;

public class VkRenderPassFrameBuffer extends VkRenderPass {

    public VkRenderPassFrameBuffer(boolean clearImage, boolean hasDepth) {
        //TODO: try VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL here and remove imagebarrier in swapchains blit function
        VkAttachmentDescription n = addColorAttachment(0, VK_FORMAT_R8G8B8A8_UNORM).finalLayout(VK_IMAGE_LAYOUT_GENERAL); 

        if (hasDepth) {
            VkAttachmentDescription d = addDepthAttachment(1, VK_FORMAT_D24_UNORM_S8_UINT);
        }
//        n.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
        if (!clearImage) {
            n.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
            n.initialLayout(n.finalLayout());
            n.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
        }
    }
    @Override
    public void build(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) 
        {
            VkAttachmentReference.Buffer colorRefShadowPass = VkAttachmentReference.callocStack(nColorAttachments, stack);
            colorRefShadowPass.get(0)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_GENERAL);
            VkAttachmentReference depthRefShadowPass = null;
            if (hasDepthAttachement) {
                depthRefShadowPass = VkAttachmentReference.callocStack(stack)
                        .attachment(1)
                        .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            }
            

            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.callocStack(2, stack);
            subpassDependencies.get(0)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .dstStageMask (VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT|VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask (VK_ACCESS_MEMORY_READ_BIT)
                    .dstAccessMask (VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT 
                            | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT
                            | VK_ACCESS_COLOR_ATTACHMENT_READ_BIT 
                            | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
            subpassDependencies.get(1)
                    .srcSubpass(1)
                    .dstSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT|VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask (VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask (VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT 
                            | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT
                            | VK_ACCESS_COLOR_ATTACHMENT_READ_BIT 
                            | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask (VK_ACCESS_MEMORY_READ_BIT)
                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.callocStack(1, stack);
            subpasses.get(0)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(0)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorRefShadowPass.remaining())
                    .pColorAttachments(colorRefShadowPass)
                    .pDepthStencilAttachment(depthRefShadowPass)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(null);
            buildRenderPass(ctxt, subpasses, subpassDependencies);
        }
    }
}
