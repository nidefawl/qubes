package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.gl.Engine;

public class FrameBuffer implements RefTrackedResource {
    private final VKContext ctxt;
    private String          tag;
    public long framebuffer;
    private FramebufferAttachment[] attachments = new FramebufferAttachment[8];
    int height;
    int width;
    public boolean[] inUseBy = new boolean[VulkanInit.MAX_NUM_SWAPCHAIN];
    public FrameBuffer(VKContext ctxt) {
        this.ctxt = ctxt;
    }
    public void build(VkRenderPass passgbuffer, int width, int height) {
        this.ctxt.addResource(this);
        this.width = width;
        this.height = height;
        try ( MemoryStack stack = stackPush() ) {
            LongBuffer pAttachments = stack.callocLong(attachments.length);
            for (int i = 0; i < this.attachments.length; i++) {
                if (this.attachments[i] != null) {
                    this.attachments[i].build(width, height);
                    pAttachments.put(this.attachments[i].getView());
                }
            }
            pAttachments.flip();
                
            LongBuffer pImage = stack.longs(0);
            VkFramebufferCreateInfo frameBufferCreateInfo = VkFramebufferCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .layers(1)
                    .renderPass(passgbuffer.get())
                    .width(width)
                    .height(height)
                    .pAttachments(pAttachments);
            int err = vkCreateFramebuffer(this.ctxt.device, frameBufferCreateInfo, null, pImage);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateImage failed: " + VulkanErr.toString(err));
            }
            this.framebuffer = pImage.get(0);
        }
    
    }

    @Override
    public void destroy() {
        this.ctxt.removeResource(this);
        for (int i = 0; i < this.attachments.length; i++) {
            if (this.attachments[i] != null) {
                this.attachments[i].destroy();
            }
        }
        if (this.framebuffer != VK_NULL_HANDLE) {
            vkDestroyFramebuffer(this.ctxt.device, this.framebuffer, null);
            this.framebuffer = VK_NULL_HANDLE;
        }
    }

    public FrameBuffer tag(String string) {
        this.tag = string;
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + (this.tag != null ? " " + this.tag : "");
    }

    public static void allocStatic() {

    }

    public static void destroyStatic() {

    }
    public void addAtt(int idx, int vkFormat, int vkUsage) {
        this.attachments[idx] = new FramebufferAttachment(this.ctxt, vkFormat, vkUsage);
    }
    public long get() {
        return this.framebuffer;
    }
    public FramebufferAttachment getAtt(int idx) {
        return this.attachments[idx];
    }
    public void fromRenderpass(VkRenderPass pass, int depthUsageFlags, int colorAttUsageFlags) {
        for (int i = 0; i < pass.nAttachments; i++) {
            VkAttachmentDescription n = pass.attachments.get(i);
            int flags = pass.attachmentType[i];
            if ((flags & VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) != 0) {
                flags |= colorAttUsageFlags;
            }
            if ((flags & VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
                flags |= depthUsageFlags;
            }
            addAtt(i, n.format(), flags);
        }
    }
    public int getHeight() {
        return this.height;
    }
    public int getWidth() {
        return this.width;
    }
    
    public void setInUse(int frameIdx) {
        inUseBy[frameIdx] = true;
    }
    @Override
    public void flagUse(int idx) {
        this.inUseBy[idx] = true;
    }
    @Override
    public void unflagUse(int idx) {
        this.inUseBy[idx] = false;
    }
    @Override
    public boolean isFree() {
        for (int i = 0 ; i < inUseBy.length; i++) {
            if (inUseBy[i]) {
                return false;
            }
        }
        return true;
    }
}