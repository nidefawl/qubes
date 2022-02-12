package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkSubpassDependency.Buffer;

import nidefawl.qubes.gl.Engine;

public class VkRenderPassDeferred extends VkRenderPass {
    public boolean isClearPass = false;
    public boolean useMaterial = false;
    public boolean useVelocity = false;
    public VkRenderPassDeferred(boolean clear) {
        this.isClearPass = clear;
    }
    public void init(VKContext ctxt) {
        reset();
        this.useMaterial = Engine.getRenderMaterialBuffer();
        this.useVelocity = Engine.getRenderVelocityBuffer();
        int idx = 0;
        VkAttachmentDescription col = addColorAttachment(idx, VK_FORMAT_R16G16B16A16_SFLOAT);
        if (!this.isClearPass) {
            col.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
            col.initialLayout(col.finalLayout());
            col.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
        } else {
            col.finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }
        idx++;
        if (useMaterial) {
            VkAttachmentDescription mat = addColorAttachment(idx, VK_FORMAT_R16G16B16A16_SFLOAT);
            
            if (!this.isClearPass) {
                mat.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
                mat.initialLayout(mat.finalLayout());
                mat.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            } else {
                mat.finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);    
            }
            idx++;
        }
        if (useVelocity) {
            VkAttachmentDescription vel = addColorAttachment(idx, VK_FORMAT_R16G16B16A16_SFLOAT);
            if (!this.isClearPass) {
                vel.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
                vel.initialLayout(vel.finalLayout());
                vel.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            } else {
                vel.finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }
            idx++;
        }
        buildPass(ctxt);
        initClearValues(Engine.INVERSE_Z_BUFFER);
    }
    @Override
    public void build(VKContext ctxt) {
    }
    public void buildPass(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() )
        {
            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.callocStack(nColorAttachments, stack);
            for (int i = 0; i < nColorAttachments; i++) {
                colorReference.get(i).attachment(i)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            }
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
                    .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)
                    .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.callocStack(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(0)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorReference.remaining())
                    .pColorAttachments(colorReference)
                    .pDepthStencilAttachment(null)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(null);
            buildRenderPass(ctxt, subpasses, subpassDependencies);
        }
    }
}
