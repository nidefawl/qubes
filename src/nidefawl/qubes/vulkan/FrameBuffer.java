package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

public class FrameBuffer implements RefTrackedResource {
    private final VKContext        ctxt;
    public long                    framebuffer;
    int                            height;
    int                            width;
    public FramebufferAttachment[] attachments     = new FramebufferAttachment[8];
    public boolean[]               isReferencedAtt = new boolean[8];
    public boolean[]               inUseBy         = new boolean[VkConstants.MAX_NUM_SWAPCHAIN];
    private String                 tag;
    public FrameBuffer(VKContext ctxt) {
        this.ctxt = ctxt;
    }
    public void copyAtt(FrameBuffer frameBufferScene, int i) {
        this.attachments[i] = frameBufferScene.attachments[i];
        this.isReferencedAtt[i] = true;
    }
    public void build(VkRenderPass passgbuffer, int width, int height) {
        this.ctxt.addResource(this);
        this.width = width;
        this.height = height;
        try ( MemoryStack stack = stackPush() ) {
            LongBuffer pAttachments = stack.callocLong(attachments.length);
            for (int i = 0; i < this.attachments.length; i++) {
                if (this.attachments[i] != null) {
                    if (!this.isReferencedAtt[i])
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
                throw new AssertionError("vkCreateFramebuffer failed: " + VulkanErr.toString(err));
            }
            this.framebuffer = pImage.get(0);
        }
    
    }

    @Override
    public void destroy() {
        this.ctxt.removeResource(this);
        for (int i = 0; i < this.attachments.length; i++) {
            if (this.attachments[i] != null && !this.isReferencedAtt[i]) {
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
    public void addAtt(int idx, VkAttachmentDescription n, int vkUsage) {
        this.attachments[idx] = new FramebufferAttachment(this.ctxt, n, vkUsage);
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
            addAtt(i, n, flags);
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
    public void onEndRenderPass() {
        for (int i = 0; i < this.attachments.length; i++) {
            if (this.attachments[i] != null) {
                
//                System.err.println(this.tag+" att["+i+"] goes from "+VulkanErr.imageLayoutToStr(this.attachments[i].currentLayout)+" to "+VulkanErr.imageLayoutToStr(this.attachments[i].finalLayout));

                this.attachments[i].currentLayout = this.attachments[i].finalLayout;
            }
        }
    }
    public void onBeginRenderPass() {
        for (int i = 0; i < this.attachments.length; i++) {
            if (this.attachments[i] != null) {
                if (this.attachments[i].initialLayout != VK_IMAGE_LAYOUT_UNDEFINED 
                        && this.attachments[i].currentLayout != VK_IMAGE_LAYOUT_PREINITIALIZED 
                        && this.attachments[i].currentLayout != VK_IMAGE_LAYOUT_UNDEFINED 
                        && this.attachments[i].currentLayout != this.attachments[i].initialLayout) {
                    System.err.println(this.tag+" att["+i+"] isn't in correct layout "+this.attachments[i].currentLayout+", expected "+this.attachments[i].initialLayout);
                    
                }
            }
        }
    }
}