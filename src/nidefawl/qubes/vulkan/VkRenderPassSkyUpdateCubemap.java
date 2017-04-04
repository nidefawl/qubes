package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkSubpassDependency.Buffer;

public class VkRenderPassSkyUpdateCubemap extends VkRenderPass {

    public VkRenderPassSkyUpdateCubemap() {
        for (int i = 0; i < 6; i++)
            addColorAttachment(i, VK_FORMAT_R16G16B16A16_SFLOAT)
                .finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.nAttachments = 6;
        this.nColorAttachments = 1;
    }
    @Override
    public void build(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) 
        {
            VkAttachmentReference.Buffer[] colorReferences = new VkAttachmentReference.Buffer[6];

            IntBuffer[] preserve = new IntBuffer[6];
            for (int i = 0; i < 6; i++)
            {

                colorReferences[i] = VkAttachmentReference.callocStack(1, stack);
                colorReferences[i]
                    .attachment(i)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                if (i > 0) {
                    preserve[i] = stack.callocInt(i);
                    for (int j = 0; j < i; j++) {
                        preserve[i].put(j, j);
                    }
                    preserve[i].flip();
                }
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
                    .srcSubpass(5)
                    .dstSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask (VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask (VK_ACCESS_MEMORY_READ_BIT)
                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.callocStack(6, stack);
            for (int i = 0; i < 6; i++) {
                subpasses.get(i)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(0)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorReferences[i].remaining())
                    .pColorAttachments(colorReferences[i])
                    .pDepthStencilAttachment(null)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(preserve[i]);
            }
            buildRenderPass(ctxt, subpasses, subpassDependencies);
        }
    }
}
