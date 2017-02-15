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
    private static VkDebugReportCallbackEXT debugCallback;
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
        {
            int apiVersion = instance.getCapabilities().apiVersion;
            int major = VK_VERSION_MAJOR(apiVersion);
            int minor = VK_VERSION_MINOR(apiVersion);
            int patch = VK_VERSION_PATCH(apiVersion);
            System.out.println("API VERSION "+major+"."+minor+"."+patch);
        }
        VKContext context = new VKContext(instance);
        if (debugContext) {
            debugCallback = new VkDebugReportCallbackEXT() {
                public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
                    System.err.println("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));
                    return 0;
                }
            };
            context.debugCallbackHandle = setupDebugging(instance, VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback);
        }
        getFirstPhysicalDevice(context);
        createDeviceAndQueue(context);
        {
            int apiVersion = context.properties.apiVersion();
            int major = VK_VERSION_MAJOR(apiVersion);
            int minor = VK_VERSION_MINOR(apiVersion);
            int patch = VK_VERSION_PATCH(apiVersion);
            System.out.println("DRIVER VERSION "+major+"."+minor+"."+patch);
        }
        context.memoryManager = new VkMemoryManager(context);
        
        return context;
    }
    public static void createFramebuffers(VKContext ctxt) {
        if (ctxt.swapChain.framebuffers != null) {
            for (int i = 0; i < ctxt.swapChain.framebuffers.length; i++)
                vkDestroyFramebuffer(ctxt.device, ctxt.swapChain.framebuffers[i], null);
        }
        try ( MemoryStack stack = stackPush() ) {
            LongBuffer attachments = stack.longs(0, 0);
            VkFramebufferCreateInfo fci = VkFramebufferCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .pAttachments(attachments)
                    .flags(VK_FLAGS_NONE)
                    .height(ctxt.swapChain.height)
                    .width(ctxt.swapChain.width)
                    .layers(1)
                    .pNext(NULL)
                    .renderPass(ctxt.renderPass);
            // Create a framebuffer for each swapchain image
            long[] framebuffers = new long[ctxt.swapChain.images.length];
            LongBuffer pFramebuffer = stack.longs(0);
            
            // Depth/Stencil attachment is the same for all frame buffers
            attachments.put(1, ctxt.depthStencil.view);
            for (int i = 0; i < ctxt.swapChain.images.length; i++) {
                attachments.put(0, ctxt.swapChain.imageViews[i]);
                int err = vkCreateFramebuffer(ctxt.device, fci, null, pFramebuffer);
                long framebuffer = pFramebuffer.get(0);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("Failed to create framebuffer: " + VulkanErr.toString(err));
                }
                framebuffers[i] = framebuffer;
            }
            ctxt.swapChain.framebuffers = framebuffers;
        }
    }
    public static void initContext(VKContext ctxt, long windowSurface, int windowWidth, int windowHeight, boolean vsync) {
        if (windowSurface == 0) {
            throw new AssertionError("Need surface to init vk context");
        }
        if (ctxt.getPhysicalDevice() == null) {
            throw new AssertionError("Need physicalDevice to init vk context");
        }
        ctxt.surface = windowSurface;
        ctxt.swapChain = new SwapChain(ctxt);
        ctxt.swapChain.width = windowWidth;
        ctxt.swapChain.height = windowHeight;
        getColorFormatAndSpace(ctxt);
        ctxt.copyCommandPool = createCommandPool(ctxt, VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT|VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);;
        ctxt.renderCommandPool = createCommandPool(ctxt, VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
        createDeviceQueue(ctxt);
        // Find a suitable depth format
        if (!getSupportedDepthFormat(ctxt)) {
            throw new AssertionError("No supported depth format");
        }
        createRenderPass(ctxt);
        ctxt.swapChain.setup(windowWidth, windowHeight, vsync);
        createDepthStencilImages(ctxt);
        createFramebuffers(ctxt);
        ctxt.init();
    }
    public static void createDepthStencilImages(VKContext ctxt) {
        if (ctxt.depthStencil.view != VK_NULL_HANDLE)
            vkDestroyImageView(ctxt.device, ctxt.depthStencil.view, null);
        if (ctxt.depthStencil.image != VK_NULL_HANDLE)
            vkDestroyImage(ctxt.device, ctxt.depthStencil.image, null);
        if (ctxt.depthStencil.image != VK_NULL_HANDLE)
            ctxt.memoryManager.releaseImageMemory(ctxt.depthStencil.image);
        try ( MemoryStack stack = stackPush() ) {
            VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .pNext(0)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(ctxt.depthFormat)
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
                    .flags(0);
            VkExtent3D extent = imageCreateInfo.extent();
            extent.width(ctxt.swapChain.width).height(ctxt.swapChain.height).depth(1);
            VkImageViewCreateInfo depthStencilView = VkImageViewCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .pNext(0)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(ctxt.depthFormat)
                    .flags(0);
            depthStencilView.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);
            LongBuffer pImage = stack.longs(0);
            int err = vkCreateImage(ctxt.device, imageCreateInfo, null, pImage);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateImage failed: " + VulkanErr.toString(err));
            }
            ctxt.depthStencil.image = pImage.get(0);
            ctxt.memoryManager.allocateImageMemory(ctxt.depthStencil.image, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, VkConstants.DEPTH_STENCIL_MEMORY);
            depthStencilView.image(ctxt.depthStencil.image);
            LongBuffer pDepthStencilView = stack.longs(0);
            err = vkCreateImageView(ctxt.device, depthStencilView, null, pDepthStencilView);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
            }
            ctxt.depthStencil.view = pDepthStencilView.get(0);
            
        }
    }
    private static boolean getSupportedDepthFormat(VKContext ctxt) {
        // Since all depth formats may be optional, we need to find a suitable depth format to use
        // Start with the highest precision packed format
        int[] depthFormats = new int[] { 
            VK_FORMAT_D32_SFLOAT_S8_UINT, 
            VK_FORMAT_D32_SFLOAT,
            VK_FORMAT_D24_UNORM_S8_UINT, 
            VK_FORMAT_D16_UNORM_S8_UINT, 
            VK_FORMAT_D16_UNORM 
        };

        try ( MemoryStack stack = stackPush() ) {
            for (int format : depthFormats)
            {
                VkFormatProperties formatProps = VkFormatProperties.callocStack(stack);
                vkGetPhysicalDeviceFormatProperties(ctxt.getPhysicalDevice(), format, formatProps);
                // Format must support depth stencil attachment for optimal tiling
                if ((formatProps.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0)
                {
                    System.out.println("using depth format "+format);
                    ctxt.depthFormat = format;
                    return true;
                }
            }
        }
        return false;
    }
    private static void createDeviceQueue(VKContext ctxt) {
        PointerBuffer pQueue = memAllocPointer(1);
        vkGetDeviceQueue(ctxt.device, ctxt.queueFamilyIndex, 0, pQueue);
        long queue = pQueue.get(0);
        memFree(pQueue);
        ctxt.vkQueue = new VkQueue(queue, ctxt.device);
    }

    static long createCommandPool(VKContext ctxt, int flags) {
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

    private static void createRenderPass(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(2, stack);
            attachments.get(0)
                    .format(ctxt.colorFormat)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            attachments.get(1)
                    .format(ctxt.depthFormat)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.callocStack(1, stack)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            VkAttachmentReference depthReference = VkAttachmentReference.callocStack(stack)
                    .attachment(1)
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .flags(VK_FLAGS_NONE)
                    .pInputAttachments(null)
                    .colorAttachmentCount(colorReference.remaining())
                    .pColorAttachments(colorReference)
                    .pDepthStencilAttachment(depthReference)
                    .pResolveAttachments(null)
                    .pPreserveAttachments(null);
            

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
            

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pNext(NULL)
                    .pAttachments(attachments)
                    .pSubpasses(subpass)
                    .pDependencies(subpassDependencies);

            LongBuffer pRenderPass = stack.longs(0);
            int err = vkCreateRenderPass(ctxt.device, renderPassInfo, null, pRenderPass);
            ctxt.renderPass = pRenderPass.get(0);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to create clear render pass: " + VulkanErr.toString(err));
            }
        }
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
        vkGetPhysicalDeviceQueueFamilyProperties(ctxt.getPhysicalDevice(), pQueueFamilyPropertyCount, null);
        int queueCount = pQueueFamilyPropertyCount.get(0);
        VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
        vkGetPhysicalDeviceQueueFamilyProperties(ctxt.getPhysicalDevice(), pQueueFamilyPropertyCount, queueProps);
        memFree(pQueueFamilyPropertyCount);

        // Iterate over each queue to learn whether it supports presenting:
        IntBuffer supportsPresent = memAllocInt(queueCount);
        for (int i = 0; i < queueCount; i++) {
            supportsPresent.position(i);
            int err = vkGetPhysicalDeviceSurfaceSupportKHR(ctxt.getPhysicalDevice(), i, ctxt.surface, supportsPresent);
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
        int err = vkGetPhysicalDeviceSurfaceFormatsKHR(ctxt.getPhysicalDevice(), ctxt.surface, pFormatCount, null);
        int formatCount = pFormatCount.get(0);
        memFree(pFormatCount);

        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to query number of physical device surface formats: " + VulkanErr.toString(err));
        }

        VkSurfaceFormatKHR.Buffer surfFormats = VkSurfaceFormatKHR.calloc(formatCount);
        err = vkGetPhysicalDeviceSurfaceFormatsKHR(ctxt.getPhysicalDevice(), ctxt.surface, pFormatCount, surfFormats);
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

    private static void createDeviceAndQueue(VKContext ctxt) {
        IntBuffer pQueueFamilyPropertyCount = memAllocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(ctxt.getPhysicalDevice(), pQueueFamilyPropertyCount, null);
        int queueCount = pQueueFamilyPropertyCount.get(0);
        VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
        vkGetPhysicalDeviceQueueFamilyProperties(ctxt.getPhysicalDevice(), pQueueFamilyPropertyCount, queueProps);
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
        String[] extension = new String[] {
            VK_KHR_SWAPCHAIN_EXTENSION_NAME, 
            NVGLSLShader.VK_NV_GLSL_SHADER_EXTENSION_NAME
        };
        ByteBuffer[] extensionsByteBuffer = new ByteBuffer[extension.length];
        PointerBuffer extensions = memAllocPointer(extension.length);
        for (int i = 0; i < extension.length; i++) {
            extensionsByteBuffer[i] = memUTF8(extension[i]);
            extensions.put(extensionsByteBuffer[i]);
        }
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
        int err = vkCreateDevice(ctxt.getPhysicalDevice(), deviceCreateInfo, null, pDevice);
        long device = pDevice.get(0);
        memFree(pDevice);
        queueCreateInfo.free();
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create device: " + VulkanErr.toString(err));
        }
        // Store Properties features, limits and properties of the physical device for later use
        // Device properties also contain limits and sparse properties
        ctxt.features = VkPhysicalDeviceFeatures.calloc();
        ctxt.properties = VkPhysicalDeviceProperties.calloc();
        ctxt.memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceFeatures(ctxt.physicalDevice, ctxt.features);
        vkGetPhysicalDeviceProperties(ctxt.physicalDevice, ctxt.properties);
        vkGetPhysicalDeviceMemoryProperties(ctxt.getPhysicalDevice(), ctxt.memoryProperties);
        ctxt.limits = ctxt.properties.limits();


        ctxt.device = new VkDevice(device, ctxt.getPhysicalDevice(), deviceCreateInfo);
        ctxt.queueFamilyIndex = graphicsQueueFamilyIndex;

        deviceCreateInfo.free();
        for (int i = 0; i < extensionsByteBuffer.length; i++) {
            memFree(extensionsByteBuffer[i]);
        }
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
                .pApplicationName(memUTF8("a"))
                .pEngineName(memUTF8("a"))
                .apiVersion(VK_API_VERSION_1_0)
                ;
        PointerBuffer ppEnabledExtensionNames = null;
        ByteBuffer VK_EXT_DEBUG_REPORT_EXTENSION = null;
        int len = requiredExtensions.remaining();
        if (validationLayer) {
            len++;
        }
        ppEnabledExtensionNames = memAllocPointer(len);
        ppEnabledExtensionNames.put(requiredExtensions);
        if (validationLayer) {
            VK_EXT_DEBUG_REPORT_EXTENSION = memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
            ppEnabledExtensionNames.put(VK_EXT_DEBUG_REPORT_EXTENSION);
        }
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
        if (VK_EXT_DEBUG_REPORT_EXTENSION != null)
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
    public static VkBufferCreateInfo bufferCreateInfo(int usageFlags, long size) {
        VkBufferCreateInfo vk = VkBufferCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .usage(usageFlags)
                .size(size);
        return vk;
    }
    public static VkMemoryAllocateInfo memoryAllocateInfo() {
        VkMemoryAllocateInfo vk = VkMemoryAllocateInfo.calloc().sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        return vk;
    }
    
    public static void destroy() {
        if (enabledVulkanLayers != null) {
            for (int i = 0; i < enabledVulkanLayers.limit(); i++) {
                long n = enabledVulkanLayers.get(i);
                nmemFree(n);
            }
            memFree(enabledVulkanLayers);
            enabledVulkanLayers = null;
        }
        if (debugCallback != null) {
            debugCallback.free();
        }
    }
}
