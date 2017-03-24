package nidefawl.qubes.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.util.GameError;


public class VulkanInit {

    static PointerBuffer enabledVulkanLayers = null;
    static PointerBuffer enabledInstanceExtension = null;
    static PointerBuffer enabledDeviceExtension = null;
    private static VkDebugReportCallbackEXT debugCallback;
    

    public static void initStatic(String[] activeLayers, String[] instanceextension, String[] deviceextensions, boolean installdebugcallback) {

        /* Look for instance extensions */
        PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
        if (requiredExtensions == null) {
            throw new AssertionError("Failed to find list of required Vulkan extensions");
        }
        
        
        enabledVulkanLayers = memAllocPointer(activeLayers.length);
        enabledInstanceExtension = memAllocPointer(requiredExtensions.remaining()+instanceextension.length);
        enabledDeviceExtension = memAllocPointer(1+deviceextensions.length);
        for (int i = 0; i < activeLayers.length; i++) {
            enabledVulkanLayers.put(memUTF8(activeLayers[i]));
        }
        while (requiredExtensions.remaining() > 0) {
            enabledInstanceExtension.put(requiredExtensions.get());
        }
        for (int i = 0; i < instanceextension.length; i++) {
            enabledInstanceExtension.put(memUTF8(instanceextension[i]));
        }
        for (int i = 0; i < deviceextensions.length; i++) {
            enabledDeviceExtension.put(memUTF8(deviceextensions[i]));
        }
        enabledVulkanLayers.flip();
        enabledInstanceExtension.flip();
        enabledDeviceExtension.flip();
        if (installdebugcallback) {
            debugCallback = new VkDebugReportCallbackEXT() {
                public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
                    System.err.println("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));

                    GameBase.baseInstance.setException(new GameError("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage)));
                    return 0;
                }
            };
        }
        VKContext.allocStatic();
    }
    
    public static void destroyStatic() {
        VKContext.destroyStatic();
        for (int i = 0; i < enabledVulkanLayers.limit(); i++) {
            long n = enabledVulkanLayers.get(i);
            nmemFree(n);
        }
        memFree(enabledVulkanLayers);
        for (int i = 0; i < enabledDeviceExtension.limit(); i++) {
            long n = enabledDeviceExtension.get(i);
            nmemFree(n);
        }
        memFree(enabledDeviceExtension);
        enabledDeviceExtension = null;
        if (debugCallback != null)
            debugCallback.free();
    }
    
    public static final int VK_FLAGS_NONE = 0;
    /**
     * This is just -1L, but it is nicer as a symbolic constant.
     */
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    public static final int MAX_NUM_SWAPCHAIN = 5;
    /**
     * Create a Vulkan instance using LWJGL 3.
     * @param debugContext 
     * 
     * @return the VkInstance handle
     */
    public static VKContext createContext() {
        
        VkInstance instance = createInstance();
        {
            int apiVersion = instance.getCapabilities().apiVersion;
            int major = VK_VERSION_MAJOR(apiVersion);
            int minor = VK_VERSION_MINOR(apiVersion);
            int patch = VK_VERSION_PATCH(apiVersion);
            System.out.println("API VERSION "+major+"."+minor+"."+patch);
        }
        VKContext context = new VKContext(instance);
        if (debugCallback != null) {
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
        getSurfaceColorFormatAndSpace(ctxt);
        ctxt.descriptorPool = createDescriptorPool(ctxt);
        ctxt.copyCommandPool = createCommandPool(ctxt, VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT|VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);;
        ctxt.renderCommandPool = createCommandPool(ctxt, VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
        createDeviceQueue(ctxt);
        ctxt.descLayouts = new VkDescLayouts(ctxt);
        // Find a suitable depth format
        if (!getSupportedDepthFormat(ctxt)) {
            throw new AssertionError("No supported depth format");
        }
        VkRenderPasses.init(ctxt);
//        createRenderPass(ctxt);
//        createRenderPassSubpasses(ctxt);
        ctxt.swapChain.setup(windowWidth, windowHeight, vsync);
        ctxt.init();
    }
    private static long createDescriptorPool(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) {
            int max = 1*1024;
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(4, stack);
            poolSizes.put(VkInitializers.descriptorPoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, max));
            poolSizes.put(VkInitializers.descriptorPoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, max));
            poolSizes.put(VkInitializers.descriptorPoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, max));
            poolSizes.put(VkInitializers.descriptorPoolSize(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC, max));
            poolSizes.flip();
            VkDescriptorPoolCreateInfo descriptorPoolInfo = VkInitializers.descriptorPoolCreateInfo(poolSizes, max);
            LongBuffer pDescriptorPool = stack.longs(0);
            int err = vkCreateDescriptorPool(ctxt.device, descriptorPoolInfo, null, pDescriptorPool);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateDescriptorPool failed: " + VulkanErr.toString(err));
            }
            return pDescriptorPool.get(0);
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

//    private static void createRenderPass(VKContext ctxt) {
//        try ( MemoryStack stack = stackPush() ) {
//            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(2, stack);
//            attachments.get(0)
//                    .format(ctxt.colorFormat)
//                    .samples(VK_SAMPLE_COUNT_1_BIT)
//                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
//                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
//                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
//                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
//                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
//                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
//
//            attachments.get(1)
//                    .format(ctxt.depthFormat)
//                    .samples(VK_SAMPLE_COUNT_1_BIT)
//                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
//                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
//                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
//                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
//                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
//                    .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
//
//            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.callocStack(1, stack)
//                    .attachment(0)
//                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
//            VkAttachmentReference depthReference = VkAttachmentReference.callocStack(stack)
//                    .attachment(1)
//                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
//
//            VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack)
//                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
//                    .flags(VK_FLAGS_NONE)
//                    .pInputAttachments(null)
//                    .colorAttachmentCount(colorReference.remaining())
//                    .pColorAttachments(colorReference)
//                    .pDepthStencilAttachment(depthReference)
//                    .pResolveAttachments(null)
//                    .pPreserveAttachments(null);
//            
//
//            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.callocStack(2, stack);
//            subpassDependencies.get(0)
//                    .srcSubpass(VK_SUBPASS_EXTERNAL)
//                    .dstSubpass(0)
//                    .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
//                    .dstStageMask (VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
//                    .srcAccessMask (VK_ACCESS_MEMORY_READ_BIT)
//                    .dstAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
//                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
//            subpassDependencies.get(1)
//                    .srcSubpass(0)
//                    .dstSubpass(VK_SUBPASS_EXTERNAL)
//                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
//                    .dstStageMask (VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
//                    .srcAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
//                    .dstAccessMask (VK_ACCESS_MEMORY_READ_BIT)
//                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
//            
//
//            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
//                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
//                    .pNext(NULL)
//                    .pAttachments(attachments)
//                    .pSubpasses(subpass)
//                    .pDependencies(subpassDependencies);
//
//            LongBuffer pRenderPass = stack.longs(0);
//            int err = vkCreateRenderPass(ctxt.device, renderPassInfo, null, pRenderPass);
//            ctxt.renderPass = pRenderPass.get(0);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to create clear render pass: " + VulkanErr.toString(err));
//            }
//        }
//    }
//    
//
//    private static void createRenderPassSubpasses(VKContext ctxt) {
//        try ( MemoryStack stack = stackPush() ) {
//            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(2, stack);
//            attachments.get(0)
//                    .format(ctxt.colorFormat)
//                    .samples(VK_SAMPLE_COUNT_1_BIT)
//                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
//                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
//                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
//                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
//                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
//                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
//
//            attachments.get(1)
//                    .format(ctxt.depthFormat)
//                    .samples(VK_SAMPLE_COUNT_1_BIT)
//                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
//                    .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
//                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
//                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
//                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
//                    .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
//
//            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.callocStack(1, stack)
//                    .attachment(0)
//                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
//            VkAttachmentReference depthReference = VkAttachmentReference.callocStack(stack)
//                    .attachment(1)
//                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
//            VkAttachmentReference.Buffer colorReference2 = VkAttachmentReference.callocStack(1, stack)
//                    .attachment(0)
//                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
//
//            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.callocStack(2, stack);
//            VkSubpassDescription subpass0 = subpasses.get(0)
//                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
//                    .flags(VK_FLAGS_NONE)
//                    .pInputAttachments(null)
//                    .colorAttachmentCount(colorReference.remaining())
//                    .pColorAttachments(colorReference)
//                    .pDepthStencilAttachment(depthReference)
//                    .pResolveAttachments(null)
//                    .pPreserveAttachments(null);
//            VkSubpassDescription subpass1 = subpasses.get(1)
//                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
//                    .flags(VK_FLAGS_NONE)
//                    .pInputAttachments(null)
//                    .colorAttachmentCount(colorReference2.remaining())
//                    .pColorAttachments(colorReference2)
//                    .pDepthStencilAttachment(null)
//                    .pResolveAttachments(null)
//                    .pPreserveAttachments(null);
//            
//
//            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.callocStack(3, stack);
//            subpassDependencies.get(0)
//                    .srcSubpass(VK_SUBPASS_EXTERNAL)
//                    .dstSubpass(0)
//                    .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
//                    .dstStageMask (VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
//                    .srcAccessMask (VK_ACCESS_MEMORY_READ_BIT)
//                    .dstAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
//                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
//            subpassDependencies.get(1)
//                    .srcSubpass(0)
//                    .dstSubpass(1)
//                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
//                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
//                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
//                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
//                    .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
//            subpassDependencies.get(2)
//                    .srcSubpass(1)
//                    .dstSubpass(VK_SUBPASS_EXTERNAL)
//                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
//                    .dstStageMask (VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
//                    .srcAccessMask (VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
//                    .dstAccessMask (VK_ACCESS_MEMORY_READ_BIT)
//                    .dependencyFlags (VK_DEPENDENCY_BY_REGION_BIT);
//            
//
//            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
//                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
//                    .pNext(NULL)
//                    .pAttachments(attachments)
//                    .pSubpasses(subpasses)
//                    .pDependencies(subpassDependencies);
//
//            LongBuffer pRenderPass = stack.longs(0);
//            int err = vkCreateRenderPass(ctxt.device, renderPassInfo, null, pRenderPass);
//            ctxt.renderPassSubpasses = pRenderPass.get(0);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to create clear render pass: " + VulkanErr.toString(err));
//            }
//        }
//    }
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
    private static void getSurfaceColorFormatAndSpace(VKContext ctxt) {
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
        // Store Properties features, limits and properties of the physical device for later use
        // Device properties also contain limits and sparse properties
        ctxt.features = VkPhysicalDeviceFeatures.calloc();
        ctxt.properties = VkPhysicalDeviceProperties.calloc();
        ctxt.memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceFeatures(ctxt.physicalDevice, ctxt.features);
        vkGetPhysicalDeviceProperties(ctxt.physicalDevice, ctxt.properties);
        vkGetPhysicalDeviceMemoryProperties(ctxt.getPhysicalDevice(), ctxt.memoryProperties);
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
        VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc();
        setupReqFeatures(features);
        VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pNext(NULL)
                .pQueueCreateInfos(queueCreateInfo)
                .ppEnabledExtensionNames(enabledDeviceExtension)
                .ppEnabledLayerNames(enabledVulkanLayers.rewind())
                .pEnabledFeatures(features);

        PointerBuffer pDevice = memAllocPointer(1);
        int err = vkCreateDevice(ctxt.getPhysicalDevice(), deviceCreateInfo, null, pDevice);
        long device = pDevice.get(0);
        memFree(pDevice);
        queueCreateInfo.free();
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create device: " + VulkanErr.toString(err));
        }
        ctxt.limits = ctxt.properties.limits();


        ctxt.device = new VkDevice(device, ctxt.getPhysicalDevice(), deviceCreateInfo);
        ctxt.queueFamilyIndex = graphicsQueueFamilyIndex;

        deviceCreateInfo.free();
        memFree(pQueuePriorities);
    }
    private static void setupReqFeatures(VkPhysicalDeviceFeatures features) {
        features.robustBufferAccess(false);
        features.fullDrawIndexUint32(false);
        features.imageCubeArray(false);
        features.independentBlend(true);
        features.geometryShader(false);
        features.tessellationShader(false);
        features.sampleRateShading(false);
        features.dualSrcBlend(false);
        features.logicOp(false);
        features.multiDrawIndirect(true);
        features.drawIndirectFirstInstance(true);
        features.depthClamp(false);
        features.depthBiasClamp(false);
        features.fillModeNonSolid(false);
        features.depthBounds(false);
        features.wideLines(false);
        features.largePoints(false);
        features.alphaToOne(false);
        features.multiViewport(false);
        features.samplerAnisotropy(true);
        features.textureCompressionETC2(false);
        features.textureCompressionASTC_LDR(false);
        features.textureCompressionBC(false);
        features.occlusionQueryPrecise(false);
        features.pipelineStatisticsQuery(false);
        features.vertexPipelineStoresAndAtomics(false);
        features.fragmentStoresAndAtomics(false);
        features.shaderTessellationAndGeometryPointSize(false);
        features.shaderImageGatherExtended(false);
        features.shaderStorageImageExtendedFormats(false);
        features.shaderStorageImageMultisample(false);
        features.shaderStorageImageReadWithoutFormat(false);
        features.shaderStorageImageWriteWithoutFormat(false);
        features.shaderUniformBufferArrayDynamicIndexing(false);
        features.shaderSampledImageArrayDynamicIndexing(false);
        features.shaderStorageBufferArrayDynamicIndexing(false);
        features.shaderStorageImageArrayDynamicIndexing(false);
        features.shaderClipDistance(false);
        features.shaderCullDistance(false);
        features.shaderFloat64(false);
        features.shaderInt64(false);
        features.shaderInt16(false);
        features.shaderResourceResidency(false);
        features.shaderResourceMinLod(false);
        features.sparseBinding(false);
        features.sparseResidencyBuffer(false);
        features.sparseResidencyImage2D(false);
        features.sparseResidencyImage3D(false);
        features.sparseResidency2Samples(false);
        features.sparseResidency4Samples(false);
        features.sparseResidency8Samples(false);
        features.sparseResidency16Samples(false);
        features.sparseResidencyAliased(false);
        features.variableMultisampleRate(false);
        features.inheritedQueries(false);
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
    public static VkInstance createInstance() {
        VkApplicationInfo appInfo = VkApplicationInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(memUTF8("a"))
                .pEngineName(memUTF8("a"))
                .apiVersion(VK_API_VERSION_1_0);
        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(NULL)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(enabledInstanceExtension)
                .ppEnabledLayerNames(enabledVulkanLayers);
        
        PointerBuffer pInstance = memAllocPointer(1);
        int err = vkCreateInstance(pCreateInfo, null, pInstance);
        long instance = pInstance.get(0);
        memFree(pInstance);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create VkInstance: " + VulkanErr.toString(err));
        }
        VkInstance ret = new VkInstance(instance, pCreateInfo);
        pCreateInfo.free();
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
}
