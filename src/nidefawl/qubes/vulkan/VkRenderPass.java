package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.util.Arrays;

import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkClearValue;

import nidefawl.qubes.gl.Engine;

public abstract class VkRenderPass {

    public final VkClearValue.Buffer clearValues = VkClearValue.calloc(8);
    public final VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(8);

    int                            nAttachments;
    int                            nColorAttachments;
    public boolean                 hasDepthAttachement;
    public final int[]             attachmentType = new int[8];
    
    long                           renderPass;

    
    public VkRenderPass() {
        reset();
        VkRenderPasses.registerPass(this);
    }
    
    void reset() {
        hasDepthAttachement = false;
        nAttachments = 0;
        nColorAttachments = 0;
        Arrays.fill(attachmentType, 0);
    }

     VkAttachmentDescription addColorAttachment(int idx, int colorFormat) {
        nColorAttachments++;
        nAttachments++;
        attachmentType[idx] = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        VkAttachmentDescription n = attachments.get(idx);
        n
          .format(colorFormat)
          .samples(VK_SAMPLE_COUNT_1_BIT)
          .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
          .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
          .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
          .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
          .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
          .finalLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        clearValues.get(idx).color()
            .float32(0, 0)
            .float32(1, 0)
            .float32(2, 0)
            .float32(3, 0);
        return n;
    }
     VkAttachmentDescription addDepthAttachment(int idx, int depthFormat) {
        hasDepthAttachement = true;
        nAttachments++;
        attachmentType[idx] = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
        VkAttachmentDescription n = attachments.get(idx);
        n.format(depthFormat)
          .samples(VK_SAMPLE_COUNT_1_BIT)
          .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
          .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
          .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
          .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
          .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
          .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        return n;
    }
    public void destroyRenderPass(VKContext ctxt) {
        if (this.renderPass != VK_NULL_HANDLE) {
            vkDestroyRenderPass(ctxt.device, this.renderPass, null);
            this.renderPass = VK_NULL_HANDLE;
        }
    }
    public abstract void build(VKContext ctxt);

    public void destroy(VKContext vkContext) {
        destroyRenderPass(vkContext);
        attachments.free();
    }

    public long get() {
        return renderPass;
    }

    public void initClearValues(boolean inverseZ) {
        for (int i = 0; i < attachmentType.length; i++) {
            if (attachmentType[i] == VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) {
                clearValues.get(i).depthStencil().set(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f, 0);
            }
        }
        
    }
}
