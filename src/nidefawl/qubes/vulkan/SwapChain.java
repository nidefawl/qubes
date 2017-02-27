package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static nidefawl.qubes.vulkan.VulkanInit.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.vulkan.*;

public final class SwapChain {
    
    public long[]                   images          = null;
    public long[]                   imageViews      = null;
    public long                     swapchainHandle = VK_NULL_HANDLE;
    public int                      width           = 0;
    public int                      height          = 0;
    public long[]                   framebuffers;
    public VkSwapchainCreateInfoKHR swapchainCI;
    private final VKContext         ctxt;
    public int                      numImages;
    public boolean swapChainAquired;
    public SwapChain(VKContext ctxt) {
        this.ctxt = ctxt;
    }
    public boolean isVsync() {
        if (swapchainCI!=null&&swapchainCI.presentMode() == VK_PRESENT_MODE_FIFO_KHR) {
            return true;
        }
        return false;
    }
    public void setup(int newWidth, int newHeight, boolean vsync) {
        if (this.imageViews != null) {
            for (int i = 0; i < this.imageViews.length; i++) {
                vkDestroyImageView(ctxt.device, this.imageViews[i], null);
            }
        }
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
        int imageUseageFlags = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

        // Set additional usage flag for blitting from the swapchain images if supported
        VkFormatProperties formatProps = VkFormatProperties.calloc();
        vkGetPhysicalDeviceFormatProperties(ctxt.getPhysicalDevice(), ctxt.colorFormat, formatProps);
        if ((formatProps.optimalTilingFeatures() & VK_FORMAT_FEATURE_BLIT_DST_BIT) != 0) {
            imageUseageFlags |= VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
        }
        formatProps.free();
        this.swapchainCI = VkSwapchainCreateInfoKHR.calloc()
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .pNext(NULL)
                .surface(ctxt.surface)
                .minImageCount(desiredNumberOfSwapchainImages)
                .imageFormat(ctxt.colorFormat)
                .imageColorSpace(ctxt.colorSpace)
                .imageUsage(imageUseageFlags)
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
        long[] imageViews = new long[this.numImages];

        LongBuffer pBufferView = memAllocLong(1);
        VkImageViewCreateInfo colorAttachmentView = VkImageViewCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .pNext(NULL)
                .format(ctxt.colorFormat)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .flags(VK_FLAGS_NONE);
        colorAttachmentView.components()
                .r(VK_COMPONENT_SWIZZLE_R)
                .g(VK_COMPONENT_SWIZZLE_G)
                .b(VK_COMPONENT_SWIZZLE_B)
                .a(VK_COMPONENT_SWIZZLE_A);
        colorAttachmentView.subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);
        for (int i = 0; i < this.numImages; i++) {
            colorAttachmentView.image(images[i]);
            err = vkCreateImageView(ctxt.device, colorAttachmentView, null, pBufferView);
            imageViews[i] = pBufferView.get(0);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to create image view: " + VulkanErr.toString(err));
            }
        }
        colorAttachmentView.free();
        memFree(pBufferView);
        memFree(pSwapchainImages);

        this.images = images;
        this.imageViews = imageViews;
        this.swapchainHandle = swapChain;
    }
}