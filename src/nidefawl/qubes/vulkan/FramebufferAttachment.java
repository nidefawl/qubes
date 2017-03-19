package nidefawl.qubes.vulkan;

import static nidefawl.qubes.gl.Engine.vkContext;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkClearAttachment.Buffer;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameLogicError;

public class FramebufferAttachment {
    private final VKContext ctxt;
    private String          tag;

    int                     width;
    int                     height;
    public long             image = VK_NULL_HANDLE;
    public int              imageLayout;
    private long            view;
    private final int             format;
    final int aspectMask;
    final int usage;
    private boolean fixedDimensions;
    private int fixedWidth;
    private int fixedHeight;
    
    public FramebufferAttachment(VKContext ctxt, int vkFormat, int usage) {
        this.ctxt = ctxt;
        this.format = vkFormat;
        this.usage = usage;
        if ((usage & VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) != 0)
        {
            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
            imageLayout = (usage & VK_IMAGE_USAGE_SAMPLED_BIT) != 0 ? VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL : VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
        }
        else if ((usage & VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0)
        {
            if (vkFormat == VK_FORMAT_D32_SFLOAT || vkFormat == VK_FORMAT_D16_UNORM) {
                aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;               
            } else {
                aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT; 
            }
            imageLayout = (usage & VK_IMAGE_USAGE_SAMPLED_BIT) != 0 ? VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL : VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
        } else {
            throw new GameLogicError("Unsuppored usage type");
        }
    }
    public void build(int w, int h) {
        this.width = this.fixedDimensions ? this.fixedWidth : w;
        this.height = this.fixedDimensions ? this.fixedHeight : h;

        try ( MemoryStack stack = stackPush() ) {
            LongBuffer pImage = stack.longs(0);
    
                VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                        .imageType(VK_IMAGE_TYPE_2D)
                        .format(this.format)
                        .mipLevels(1)
                        .arrayLayers(1)
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                        .tiling(VK_IMAGE_TILING_OPTIMAL)
                        .usage(this.usage)
                        .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                imageCreateInfo.extent().width(this.width).height(this.height).depth(1);
                int err = vkCreateImage(this.ctxt.device, imageCreateInfo, null, pImage);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateImage failed: " + VulkanErr.toString(err));
                }
                this.image = pImage.get(0);
                this.ctxt.memoryManager.allocateImageMemory(pImage.get(0), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, VkConstants.TEXTURE_COLOR_MEMORY);

              VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                      .viewType(VK_IMAGE_VIEW_TYPE_2D)
                      .format(this.format)
                      .components(VkComponentMapping.callocStack(stack));
              VkImageSubresourceRange viewSubResRange = view.subresourceRange();
              viewSubResRange.aspectMask(this.aspectMask);
              viewSubResRange.baseMipLevel(0);
              viewSubResRange.levelCount(1);
              viewSubResRange.baseArrayLayer(0);
              viewSubResRange.layerCount(1);
              view.image(this.image);
              LongBuffer pView = stack.longs(0);
              err = vkCreateImageView(this.ctxt.device, view, null, pView);
              if (err != VK_SUCCESS) {
                  throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
              }
              this.view = pView.get(0);
            
        }
    
    }

    public void destroy() {
        if (this.view != VK_NULL_HANDLE) {
            vkDestroyImageView(this.ctxt.device, this.view, null);
            this.view = VK_NULL_HANDLE;
        }
        if (this.image != VK_NULL_HANDLE) {
            vkDestroyImage(this.ctxt.device, this.image, null);
            this.ctxt.memoryManager.releaseImageMemory(this.image);
            this.image = VK_NULL_HANDLE;
        }
    }

    
    public FramebufferAttachment tag(String string) {
        this.tag = string;
        return this;
    }

    @Override
    public String toString() {
        return super.toString()+(this.tag!= null?" "+this.tag:"");
    }

    public static void allocStatic() {
        
    }
    public static void destroyStatic() {
        
    }
    public long getView() {
        return this.view;
    }
    public void setFixedDimension(int w, int h) {
        this.fixedDimensions = true;
        this.fixedWidth = w;
        this.fixedHeight = h;
    }
}