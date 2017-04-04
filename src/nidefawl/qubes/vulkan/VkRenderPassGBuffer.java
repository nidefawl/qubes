package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkSubpassDependency.Buffer;

public class VkRenderPassGBuffer extends VkRenderPass {

    public VkRenderPassGBuffer(int pass) {
        VkAttachmentDescription color = addColorAttachment(0, VK_FORMAT_R16G16B16A16_SFLOAT).finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VkAttachmentDescription normal = addColorAttachment(1, VK_FORMAT_R16G16B16A16_SFLOAT).finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VkAttachmentDescription material = addColorAttachment(2, VK_FORMAT_R16G16B16A16_UINT).finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VkAttachmentDescription light = addColorAttachment(3, VK_FORMAT_R16G16B16A16_SFLOAT).finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VkAttachmentDescription depth = addDepthAttachment(4, VK_FORMAT_D32_SFLOAT);
        depth.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
        depth.initialLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
        depth.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
        if (pass == 0) {
            depth.finalLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        }
        if (pass == 1) {
            normal.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            normal.initialLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            material.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            material.initialLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            light.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            light.initialLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            depth.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            depth.initialLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
        }
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
                    .attachment(4)
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.callocStack(2, stack);
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
            buildRenderPass(ctxt, subpasses, subpassDependencies);
        }
    }
}
