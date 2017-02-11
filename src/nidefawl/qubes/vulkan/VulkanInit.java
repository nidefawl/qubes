package nidefawl.qubes.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public class VulkanInit {

    static PointerBuffer enabledVulkanLayers = null;
    public static final int VK_FLAGS_NONE = 0;
    /**
     * This is just -1L, but it is nicer as a symbolic constant.
     */
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    /**
     * Create a Vulkan instance using LWJGL 3.
     * @param debugContext 
     * 
     * @return the VkInstance handle
     */
    public static VKContext createContext(boolean debugContext) {
        if (!glfwVulkanSupported()) {
            throw new AssertionError("GLFW failed to find the Vulkan loader");
        }

        /* Look for instance extensions */
        PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
        if (requiredExtensions == null) {
            throw new AssertionError("Failed to find list of required Vulkan extensions");
        }
        if (debugContext) {
            enabledVulkanLayers = memAllocPointer(1);
            enabledVulkanLayers.put(memUTF8("VK_LAYER_LUNARG_standard_validation"));
        }
        
        VkInstance instance = createInstance(requiredExtensions, debugContext);
        VKContext context = new VKContext(instance);
        if (debugContext) {
            final VkDebugReportCallbackEXT debugCallback = new VkDebugReportCallbackEXT() {
                public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
                    System.err.println("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));
                    return 0;
                }
            };
            context.debugCallbackHandle = setupDebugging(instance, VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback);
        }
        getFirstPhysicalDevice(context);
        createDeviceAndGetGraphicsQueueFamily(context);
        return context;
    }
    public static void createSwapchainFramebuffers(VKContext ctxt) {
        if (ctxt.swapChain.framebuffers != null) {
            for (int i = 0; i < ctxt.swapChain.framebuffers.length; i++)
                vkDestroyFramebuffer(ctxt.device, ctxt.swapChain.framebuffers[i], null);
        }
        LongBuffer attachments = memAllocLong(1);
        VkFramebufferCreateInfo fci = VkFramebufferCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .pAttachments(attachments)
                .flags(VK_FLAGS_NONE)
                .height(ctxt.swapChain.height)
                .width(ctxt.swapChain.width)
                .layers(1)
                .pNext(NULL)
                .renderPass(ctxt.clearRenderPass);
        // Create a framebuffer for each swapchain image
        long[] framebuffers = new long[ctxt.swapChain.images.length];
        LongBuffer pFramebuffer = memAllocLong(1);
        for (int i = 0; i < ctxt.swapChain.images.length; i++) {
            attachments.put(0, ctxt.swapChain.imageViews[i]);
            int err = vkCreateFramebuffer(ctxt.device, fci, null, pFramebuffer);
            long framebuffer = pFramebuffer.get(0);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to create framebuffer: " + VulkanErr.toString(err));
            }
            framebuffers[i] = framebuffer;
        }
        memFree(attachments);
        memFree(pFramebuffer);
        fci.free();
        ctxt.swapChain.framebuffers = framebuffers;
    }
    public static void initContext(VKContext ctxt, long windowSurface, int windowWidth, int windowHeight, boolean vsync) {
        if (windowSurface == 0) {
            throw new AssertionError("Need surface to init vk context");
        }
        if (ctxt.physicalDevice == null) {
            throw new AssertionError("Need physicalDevice to init vk context");
        }
        ctxt.surface = windowSurface;
        getColorFormatAndSpace(ctxt);
        ctxt.setupCommandPool = createCommandPool(ctxt);
        ctxt.renderCommandPool = createCommandPool(ctxt);
        createDeviceQueue(ctxt);
        ctxt.postPresentCommandBuffer = createCommandBuffer(ctxt, ctxt.setupCommandPool);
        createClearRenderPass(ctxt);
        ctxt.init();
        ctxt.updateSwapchain(windowWidth, windowHeight, vsync);
    }
    private static void createDeviceQueue(VKContext ctxt) {
        PointerBuffer pQueue = memAllocPointer(1);
        vkGetDeviceQueue(ctxt.device, ctxt.queueFamilyIndex, 0, pQueue);
        long queue = pQueue.get(0);
        memFree(pQueue);
        ctxt.vkQueue = new VkQueue(queue, ctxt.device);
    }

    private static long createCommandPool(VKContext ctxt) {
        VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .queueFamilyIndex(ctxt.queueFamilyIndex)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
        LongBuffer pCmdPool = memAllocLong(1);
        int err = vkCreateCommandPool(ctxt.device, cmdPoolInfo, null, pCmdPool);
        long commandPool = pCmdPool.get(0);
        cmdPoolInfo.free();
        memFree(pCmdPool);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create command pool: " + VulkanErr.toString(err));
        }
        return commandPool;
    }

    private static void createClearRenderPass(VKContext ctxt) {
        VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1)
                .format(ctxt.colorFormat)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .flags(VK_FLAGS_NONE)
                .pInputAttachments(null)
                .colorAttachmentCount(colorReference.remaining())
                .pColorAttachments(colorReference)
                .pResolveAttachments(null)
                .pDepthStencilAttachment(null)
                .pPreserveAttachments(null);
        

        VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(2);
        VkSubpassDependency subpass0 = subpassDependencies.get(0)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                .dstStageMask (VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask (VK_ACCESS_MEMORY_READ_BIT)
                .dstAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
        VkSubpassDependency subpass1 = subpassDependencies.get(1)
                .srcSubpass(0)
                .dstSubpass(VK_SUBPASS_EXTERNAL)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask (VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                .srcAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .dstAccessMask (VK_ACCESS_MEMORY_READ_BIT)
                .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
        

        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pNext(NULL)
                .pAttachments(attachments)
                .pSubpasses(subpass)
                .pDependencies(subpassDependencies);

        LongBuffer pRenderPass = memAllocLong(1);
        int err = vkCreateRenderPass(ctxt.device, renderPassInfo, null, pRenderPass);
        long renderPass = pRenderPass.get(0);
        memFree(pRenderPass);
        renderPassInfo.free();
        colorReference.free();
        subpass.free();
        attachments.free();
        subpassDependencies.free();
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create clear render pass: " + VulkanErr.toString(err));
        }
        ctxt.clearRenderPass = renderPass;
    }
    public static void createSwapChain(VKContext ctxt, int newWidth, int newHeight, boolean vsync) {
        int err;
        // Get physical device surface properties and formats
        VkSurfaceCapabilitiesKHR surfCaps = VkSurfaceCapabilitiesKHR.calloc();
        err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(ctxt.physicalDevice, ctxt.surface, surfCaps);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get physical device surface capabilities: " + VulkanErr.toString(err));
        }

        IntBuffer pPresentModeCount = memAllocInt(1);
        err = vkGetPhysicalDeviceSurfacePresentModesKHR(ctxt.physicalDevice, ctxt.surface, pPresentModeCount, null);
        int presentModeCount = pPresentModeCount.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get number of physical device surface presentation modes: " + VulkanErr.toString(err));
        }

        IntBuffer pPresentModes = memAllocInt(presentModeCount);
        err = vkGetPhysicalDeviceSurfacePresentModesKHR(ctxt.physicalDevice, ctxt.surface, pPresentModeCount, pPresentModes);
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
            ctxt.swapChain.width = currentWidth;
            ctxt.swapChain.height = currentHeight;
        } else {
            ctxt.swapChain.width = newWidth;
            ctxt.swapChain.height = newHeight;
        }

        int preTransform;
        if ((surfCaps.supportedTransforms() & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0) {
            preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
        } else {
            preTransform = surfCaps.currentTransform();
        }
        surfCaps.free();
        if (ctxt.swapChain.swapchainCI != null)
            ctxt.swapChain.swapchainCI.free();
        int imageUseageFlags = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

        // Set additional usage flag for blitting from the swapchain images if supported
        VkFormatProperties formatProps = VkFormatProperties.calloc();
        vkGetPhysicalDeviceFormatProperties(ctxt.physicalDevice, ctxt.colorFormat, formatProps);
        if ((formatProps.optimalTilingFeatures() & VK_FORMAT_FEATURE_BLIT_DST_BIT) != 0) {
            imageUseageFlags |= VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
        }
        ctxt.swapChain.swapchainCI = VkSwapchainCreateInfoKHR.calloc()
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
                .oldSwapchain(ctxt.swapChain.swapchainHandle)
                .clipped(true)
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
        ctxt.swapChain.swapchainCI.imageExtent()
                .width(ctxt.swapChain.width)
                .height(ctxt.swapChain.height);
        LongBuffer pSwapChain = memAllocLong(1);
        err = vkCreateSwapchainKHR(ctxt.device, ctxt.swapChain.swapchainCI, null, pSwapChain);
        long swapChain = pSwapChain.get(0);
        memFree(pSwapChain);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create swap chain: " + VulkanErr.toString(err));
        }

        // If we just re-created an existing swapchain, we should destroy the old swapchain at this point.
        // Note: destroying the swapchain also cleans up all its associated presentable images once the platform is done with them.
        if (ctxt.swapChain.swapchainHandle != VK_NULL_HANDLE) {
            vkDestroySwapchainKHR(ctxt.device, ctxt.swapChain.swapchainHandle, null);
            ctxt.swapChain.swapchainHandle = VK_NULL_HANDLE;
        }

        IntBuffer pImageCount = memAllocInt(1);
        err = vkGetSwapchainImagesKHR(ctxt.device, swapChain, pImageCount, null);
        int imageCount = pImageCount.get(0);
        
        
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get number of swapchain images: " + VulkanErr.toString(err));
        }

        LongBuffer pSwapchainImages = memAllocLong(imageCount);
        err = vkGetSwapchainImagesKHR(ctxt.device, swapChain, pImageCount, pSwapchainImages);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get swapchain images: " + VulkanErr.toString(err));
        }
        memFree(pImageCount);
        long[] images = new long[imageCount];
        pSwapchainImages.get(images, 0, imageCount);
        long[] imageViews = new long[imageCount];

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
        for (int i = 0; i < imageCount; i++) {
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

        ctxt.swapChain.images = images;
        ctxt.swapChain.imageViews = imageViews;
        ctxt.swapChain.swapchainHandle = swapChain;
    }

    private static VkCommandBuffer createCommandBuffer(VKContext ctxt, long pool) {
        VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(pool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1);
        PointerBuffer pCommandBuffer = memAllocPointer(1);
        int err = vkAllocateCommandBuffers(ctxt.device, cmdBufAllocateInfo, pCommandBuffer);
        cmdBufAllocateInfo.free();
        long commandBuffer = pCommandBuffer.get(0);
        memFree(pCommandBuffer);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to allocate command buffer: " + VulkanErr.toString(err));
        }
        return new VkCommandBuffer(commandBuffer, ctxt.device);
    }
    private static void getColorFormatAndSpace(VKContext ctxt) {
        IntBuffer pQueueFamilyPropertyCount = memAllocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(ctxt.physicalDevice, pQueueFamilyPropertyCount, null);
        int queueCount = pQueueFamilyPropertyCount.get(0);
        VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
        vkGetPhysicalDeviceQueueFamilyProperties(ctxt.physicalDevice, pQueueFamilyPropertyCount, queueProps);
        memFree(pQueueFamilyPropertyCount);

        // Iterate over each queue to learn whether it supports presenting:
        IntBuffer supportsPresent = memAllocInt(queueCount);
        for (int i = 0; i < queueCount; i++) {
            supportsPresent.position(i);
            int err = vkGetPhysicalDeviceSurfaceSupportKHR(ctxt.physicalDevice, i, ctxt.surface, supportsPresent);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to physical device surface support: " + VulkanErr.toString(err));
            }
        }

        // Search for a graphics and a present queue in the array of queue families, try to find one that supports both
        int graphicsQueueNodeIndex = Integer.MAX_VALUE;
        int presentQueueNodeIndex = Integer.MAX_VALUE;
        for (int i = 0; i < queueCount; i++) {
            if ((queueProps.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                if (graphicsQueueNodeIndex == Integer.MAX_VALUE) {
                    graphicsQueueNodeIndex = i;
                }
                if (supportsPresent.get(i) == VK_TRUE) {
                    graphicsQueueNodeIndex = i;
                    presentQueueNodeIndex = i;
                    break;
                }
            }
        }
        queueProps.free();
        if (presentQueueNodeIndex == Integer.MAX_VALUE) {
            // If there's no queue that supports both present and graphics try to find a separate present queue
            for (int i = 0; i < queueCount; ++i) {
                if (supportsPresent.get(i) == VK_TRUE) {
                    presentQueueNodeIndex = i;
                    break;
                }
            }
        }
        memFree(supportsPresent);

        // Generate error if could not find both a graphics and a present queue
        if (graphicsQueueNodeIndex == Integer.MAX_VALUE) {
            throw new AssertionError("No graphics queue found");
        }
        if (presentQueueNodeIndex == Integer.MAX_VALUE) {
            throw new AssertionError("No presentation queue found");
        }
        if (graphicsQueueNodeIndex != presentQueueNodeIndex) {
            throw new AssertionError("Presentation queue != graphics queue");
        }

        // Get list of supported formats
        IntBuffer pFormatCount = memAllocInt(1);
        int err = vkGetPhysicalDeviceSurfaceFormatsKHR(ctxt.physicalDevice, ctxt.surface, pFormatCount, null);
        int formatCount = pFormatCount.get(0);
        memFree(pFormatCount);

        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to query number of physical device surface formats: " + VulkanErr.toString(err));
        }

        VkSurfaceFormatKHR.Buffer surfFormats = VkSurfaceFormatKHR.calloc(formatCount);
        err = vkGetPhysicalDeviceSurfaceFormatsKHR(ctxt.physicalDevice, ctxt.surface, pFormatCount, surfFormats);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to query physical device surface formats: " + VulkanErr.toString(err));
        }

        // If the format list includes just one entry of VK_FORMAT_UNDEFINED, the surface has no preferred format. Otherwise, at least one supported format will
        // be returned.
        int colorFormat;
        if (formatCount == 1 && surfFormats.get(0).format() == VK_FORMAT_UNDEFINED) {
            colorFormat = VK_FORMAT_B8G8R8A8_UNORM;
        } else {
            colorFormat = surfFormats.get(0).format();
        }
        int colorSpace = surfFormats.get(0).colorSpace();
        surfFormats.free();

        ctxt.colorFormat = colorFormat;
        ctxt.colorSpace = colorSpace;
    }
    private static void getFirstPhysicalDevice(VKContext ctxt) {
        IntBuffer pPhysicalDeviceCount = memAllocInt(1);
        int err = vkEnumeratePhysicalDevices(ctxt.vk, pPhysicalDeviceCount, null);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get number of physical devices: " + VulkanErr.toString(err));
        }
        PointerBuffer pPhysicalDevices = memAllocPointer(pPhysicalDeviceCount.get(0));
        err = vkEnumeratePhysicalDevices(ctxt.vk, pPhysicalDeviceCount, pPhysicalDevices);
        long physicalDevice = pPhysicalDevices.get(0);
        memFree(pPhysicalDeviceCount);
        memFree(pPhysicalDevices);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get physical devices: " + VulkanErr.toString(err));
        }
        ctxt.physicalDevice = new VkPhysicalDevice(physicalDevice, ctxt.vk);
    }

    private static void createDeviceAndGetGraphicsQueueFamily(VKContext ctxt) {
        IntBuffer pQueueFamilyPropertyCount = memAllocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(ctxt.physicalDevice, pQueueFamilyPropertyCount, null);
        int queueCount = pQueueFamilyPropertyCount.get(0);
        VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
        vkGetPhysicalDeviceQueueFamilyProperties(ctxt.physicalDevice, pQueueFamilyPropertyCount, queueProps);
        memFree(pQueueFamilyPropertyCount);
        int graphicsQueueFamilyIndex;
        for (graphicsQueueFamilyIndex = 0; graphicsQueueFamilyIndex < queueCount; graphicsQueueFamilyIndex++) {
            if ((queueProps.get(graphicsQueueFamilyIndex).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
                break;
        }
        queueProps.free();
        FloatBuffer pQueuePriorities = memAllocFloat(1).put(0.0f);
        pQueuePriorities.flip();
        VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
                .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(graphicsQueueFamilyIndex)
                .pQueuePriorities(pQueuePriorities);

        PointerBuffer extensions = memAllocPointer(1);
        ByteBuffer VK_KHR_SWAPCHAIN_EXTENSION = memUTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
        extensions.put(VK_KHR_SWAPCHAIN_EXTENSION);
        extensions.flip();

        VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pNext(NULL)
                .pQueueCreateInfos(queueCreateInfo)
                .ppEnabledExtensionNames(extensions);

        if (enabledVulkanLayers != null) {
            deviceCreateInfo.ppEnabledLayerNames(enabledVulkanLayers.rewind());
        }
        PointerBuffer pDevice = memAllocPointer(1);
        int err = vkCreateDevice(ctxt.physicalDevice, deviceCreateInfo, null, pDevice);
        long device = pDevice.get(0);
        memFree(pDevice);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create device: " + VulkanErr.toString(err));
        }

        VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(ctxt.physicalDevice, memoryProperties);

        ctxt.device = new VkDevice(device, ctxt.physicalDevice, deviceCreateInfo);
        ctxt.queueFamilyIndex = graphicsQueueFamilyIndex;
        ctxt.memoryProperties = memoryProperties;

        deviceCreateInfo.free();
        memFree(VK_KHR_SWAPCHAIN_EXTENSION);
        memFree(extensions);
        memFree(pQueuePriorities);
    }
    private static long setupDebugging(VkInstance instance, int flags, VkDebugReportCallbackEXT callback) {
        VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
                .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                .pNext(NULL)
                .pfnCallback(callback)
                .pUserData(NULL)
                .flags(flags);
        LongBuffer pCallback = memAllocLong(1);
        int err = vkCreateDebugReportCallbackEXT(instance, dbgCreateInfo, null, pCallback);
        long callbackHandle = pCallback.get(0);
        memFree(pCallback);
        dbgCreateInfo.free();
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create VkInstance: " + VulkanErr.toString(err));
        }
        return callbackHandle;
    }
    public static VkInstance createInstance(PointerBuffer requiredExtensions, boolean validationLayer) {
        VkApplicationInfo appInfo = VkApplicationInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(memUTF8("GLFW Vulkan Demo"))
                .pEngineName(memUTF8(""))
                .apiVersion(VK_MAKE_VERSION(1, 0, 2));
        PointerBuffer ppEnabledExtensionNames = memAllocPointer(requiredExtensions.remaining() + 1);
        ppEnabledExtensionNames.put(requiredExtensions);
        ByteBuffer VK_EXT_DEBUG_REPORT_EXTENSION = memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
        ppEnabledExtensionNames.put(VK_EXT_DEBUG_REPORT_EXTENSION);
        ppEnabledExtensionNames.flip();
        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(NULL)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppEnabledExtensionNames);
        if (enabledVulkanLayers != null) {
            pCreateInfo.ppEnabledLayerNames(enabledVulkanLayers.rewind());
        }
        PointerBuffer pInstance = memAllocPointer(1);
        int err = vkCreateInstance(pCreateInfo, null, pInstance);
        long instance = pInstance.get(0);
        memFree(pInstance);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create VkInstance: " + VulkanErr.toString(err));
        }
        VkInstance ret = new VkInstance(instance, pCreateInfo);
        pCreateInfo.free();
        memFree(VK_EXT_DEBUG_REPORT_EXTENSION);
        memFree(ppEnabledExtensionNames);
        memFree(appInfo.pApplicationName());
        memFree(appInfo.pEngineName());
        appInfo.free();
        return ret;
    }
    public static long createSemaphore(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) {
            LongBuffer lbuffer = stack.longs(0);
            // Info struct to create a semaphore
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                    .pNext(NULL)
                    .flags(0);
            // Create a semaphore to wait for the swapchain to acquire the next image
            int err = vkCreateSemaphore(ctxt.device, semaphoreCreateInfo, null, lbuffer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to create image acquired semaphore: " + VulkanErr.toString(err));
            }
            return lbuffer.get(0);
        }
    }
}
