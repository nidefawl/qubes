package nidefawl.qubes.vulkan;

import static nidefawl.qubes.gl.Engine.vkContext;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkImageBlit.Buffer;

public final class SwapChain {
    
    public long[]                   images          = null;
    public long                     swapchainHandle = VK_NULL_HANDLE;
    public int                      width           = 0;
    public int                      height          = 0;
    public VkSwapchainCreateInfoKHR swapchainCI;
    VkImageSubresourceRange range;
    VkClearColorValue clearColor;
    private final VKContext         ctxt;
    public int                      numImages;
    public boolean swapChainAquired;
    private int imageUseageFlags;
    private Buffer blit;
    private VkImageSubresourceLayers srcSubresource;
    private VkImageSubresourceLayers dstSubresource;
    private VkOffset3D srcOffset;
    private VkOffset3D srcExtent;
    private VkOffset3D dstOffset;
    private VkOffset3D dstExtent;
    
    public SwapChain(VKContext ctxt) {
        this.ctxt = ctxt;
        clearColor = VkClearColorValue.calloc();
        range = VkImageSubresourceRange.calloc();
        range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            .baseArrayLayer(0)
            .baseMipLevel(0)
            .layerCount(1)
            .levelCount(1);
        blit = VkImageBlit.calloc(1);
        srcSubresource = blit.srcSubresource();
        srcOffset = blit.srcOffsets().get(0);
        srcExtent = blit.srcOffsets().get(1);
        dstSubresource = blit.dstSubresource();
        dstOffset = blit.dstOffsets().get(0);
        dstExtent = blit.dstOffsets().get(1);
    }
    public boolean isVsync() {
        if (swapchainCI!=null&&swapchainCI.presentMode() == VK_PRESENT_MODE_FIFO_KHR) {
            return true;
        }
        return false;
    }
    public void setup(int newWidth, int newHeight, boolean vsync) {
        int err;
        // Get physical device surface properties and formats
        VkSurfaceCapabilitiesKHR surfCaps = VkSurfaceCapabilitiesKHR.calloc();
        err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(ctxt.getPhysicalDevice(), ctxt.surface, surfCaps);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get physical device surface capabilities: " + VulkanErr.toString(err));
        }

        IntBuffer pPresentModeCount = memAllocInt(1);
        err = vkGetPhysicalDeviceSurfacePresentModesKHR(ctxt.getPhysicalDevice(), ctxt.surface, pPresentModeCount, null);
        int presentModeCount = pPresentModeCount.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get number of physical device surface presentation modes: " + VulkanErr.toString(err));
        }

        IntBuffer pPresentModes = memAllocInt(presentModeCount);
        err = vkGetPhysicalDeviceSurfacePresentModesKHR(ctxt.getPhysicalDevice(), ctxt.surface, pPresentModeCount, pPresentModes);
        memFree(pPresentModeCount);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get physical device surface presentation modes: " + VulkanErr.toString(err));
        }

        // Try to use mailbox mode. Low latency and non-tearing
        int swapchainPresentMode = VK_PRESENT_MODE_FIFO_KHR;
        boolean hasFIFO = false;
        for (int i = 0; i < presentModeCount; i++) {
            if (pPresentModes.get(i) == VK_PRESENT_MODE_FIFO_KHR) {
                hasFIFO = true;
                break;
            }
        }
        if (!hasFIFO||!vsync)
        for (int i = 0; i < presentModeCount; i++) {
            if (pPresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                swapchainPresentMode = VK_PRESENT_MODE_MAILBOX_KHR;
                break;
            }
            if ((swapchainPresentMode != VK_PRESENT_MODE_MAILBOX_KHR) && (pPresentModes.get(i) == VK_PRESENT_MODE_IMMEDIATE_KHR)) {
                swapchainPresentMode = VK_PRESENT_MODE_IMMEDIATE_KHR;
            }
        }
        memFree(pPresentModes);

        // Determine the number of images
        int desiredNumberOfSwapchainImages = surfCaps.minImageCount() + 1;
        if ((surfCaps.maxImageCount() > 0) && (desiredNumberOfSwapchainImages > surfCaps.maxImageCount())) {
            desiredNumberOfSwapchainImages = surfCaps.maxImageCount();
        }

        VkExtent2D currentExtent = surfCaps.currentExtent();
        int currentWidth = currentExtent.width();
        int currentHeight = currentExtent.height();
        if (currentWidth != -1 && currentHeight != -1) {
            this.width = currentWidth;
            this.height = currentHeight;
        } else {
            this.width = newWidth;
            this.height = newHeight;
        }

        int preTransform;
        if ((surfCaps.supportedTransforms() & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0) {
            preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
        } else {
            preTransform = surfCaps.currentTransform();
        }
        surfCaps.free();
        if (this.swapchainCI != null)
            this.swapchainCI.free();
        this.imageUseageFlags = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

        // Set additional usage flag for blitting from the swapchain images if supported
        VkFormatProperties formatProps = VkFormatProperties.calloc();
        vkGetPhysicalDeviceFormatProperties(ctxt.getPhysicalDevice(), ctxt.colorFormat, formatProps);
        if ((formatProps.optimalTilingFeatures() & VK_FORMAT_FEATURE_BLIT_DST_BIT) != 0) {
            this.imageUseageFlags |= VK_IMAGE_USAGE_TRANSFER_DST_BIT;
        }
        formatProps.free();
        this.swapchainCI = VkSwapchainCreateInfoKHR.calloc()
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .pNext(NULL)
                .surface(ctxt.surface)
                .minImageCount(desiredNumberOfSwapchainImages)
                .imageFormat(ctxt.colorFormat)
                .imageColorSpace(ctxt.colorSpace)
                .imageUsage(this.imageUseageFlags)
                .preTransform(preTransform)
                .imageArrayLayers(1)
                .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .pQueueFamilyIndices(null)
                .presentMode(swapchainPresentMode)
                .oldSwapchain(this.swapchainHandle)
                .clipped(true)
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
        this.swapchainCI.imageExtent()
                .width(this.width)
                .height(this.height);
        LongBuffer pSwapChain = memAllocLong(1);
        err = vkCreateSwapchainKHR(ctxt.device, this.swapchainCI, null, pSwapChain);
        long swapChain = pSwapChain.get(0);
        memFree(pSwapChain);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create swap chain: " + VulkanErr.toString(err));
        }

        // If we just re-created an existing swapchain, we should destroy the old swapchain at this point.
        // Note: destroying the swapchain also cleans up all its associated presentable images once the platform is done with them.
        if (this.swapchainHandle != VK_NULL_HANDLE) {
            vkDestroySwapchainKHR(ctxt.device, this.swapchainHandle, null);
            this.swapchainHandle = VK_NULL_HANDLE;
        }

        IntBuffer pImageCount = memAllocInt(1);
        err = vkGetSwapchainImagesKHR(ctxt.device, swapChain, pImageCount, null);
        this.numImages = pImageCount.get(0);
        System.out.println("Create swapchain with "+this.numImages+" images");
        
        
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get number of swapchain images: " + VulkanErr.toString(err));
        }

        LongBuffer pSwapchainImages = memAllocLong(this.numImages);
        err = vkGetSwapchainImagesKHR(ctxt.device, swapChain, pImageCount, pSwapchainImages);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get swapchain images: " + VulkanErr.toString(err));
        }
        memFree(pImageCount);
        long[] images = new long[this.numImages];
        pSwapchainImages.get(images, 0, this.numImages);
        memFree(pSwapchainImages);

        this.images = images;
        this.swapchainHandle = swapChain;
    }
    public void destroy() {
        if (this.swapchainHandle != VK_NULL_HANDLE)
        {
            vkDestroySwapchainKHR(ctxt.device, this.swapchainHandle, null);
        }
        swapchainCI.free();
        range.free();
        clearColor.free();
    }
    public boolean canBlitToSwapchain() {
        return (this.imageUseageFlags & VK_IMAGE_USAGE_TRANSFER_SRC_BIT) != 0;
    }
    public void imageClear(VkCommandBuffer commandBuffer, int currentLayout, float r, float g, float b, float a) {
        vkContext.setImageLayout(commandBuffer, vkContext.swapChain.images[VKContext.currentBuffer],
                VK_IMAGE_ASPECT_COLOR_BIT, currentLayout,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT, VK_ACCESS_TRANSFER_WRITE_BIT);
        clearColor.float32(0, r).float32(1, g).float32(2, b).float32(3, a);
        vkCmdClearColorImage(commandBuffer, images[VKContext.currentBuffer], VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                clearColor, range);

    }
    
    public void imageTransitionPresent(VkCommandBuffer commandBuffer, int currentLayout) {

        vkContext.setImageLayout(commandBuffer, this.images[VKContext.currentBuffer], 
                VK_IMAGE_ASPECT_COLOR_BIT, 
                currentLayout,                                  VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, 
                VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,  VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 
                VK_ACCESS_TRANSFER_WRITE_BIT,                   VK_ACCESS_MEMORY_READ_BIT);
    }

    public void blitFramebufferAndPreset(VkCommandBuffer commandBuffer, FrameBuffer frameBuffer, int framebufferAttIdx) {
        srcSubresource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        srcSubresource.layerCount(1);
        dstSubresource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        dstSubresource.layerCount(1);
        srcExtent.set(frameBuffer.getWidth(), frameBuffer.getHeight(), 1);
        dstExtent.set(this.width, this.height, 1);
        vkContext.setImageLayout(commandBuffer, this.images[VKContext.currentBuffer], 
                VK_IMAGE_ASPECT_COLOR_BIT, 
                VK_IMAGE_LAYOUT_UNDEFINED,             VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,     VK_PIPELINE_STAGE_TRANSFER_BIT, 
                VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,  VK_ACCESS_TRANSFER_WRITE_BIT);
        vkCmdBlitImage(commandBuffer, frameBuffer.getAtt(framebufferAttIdx).image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, this.images[VKContext.currentBuffer],
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, blit, VK_FILTER_LINEAR);
        imageTransitionPresent(commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
    }
}