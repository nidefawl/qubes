package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.EXTDebugReport.*;       
import static org.lwjgl.vulkan.KHRDisplaySwapchain.*;     
import static org.lwjgl.vulkan.KHRSurface.*;      
import static org.lwjgl.vulkan.KHRSwapchain.*;        
import static org.lwjgl.vulkan.VK10.*;

public class VulkanErr {

    /**
     * Translates a Vulkan {@code VkResult} value to a String describing the result.
     * 
     * @param result
     *            the {@code VkResult} value
     * 
     * @return the result description
     */
    public static String toString(int result) {
        switch (result) {
        // Success codes
        case VK_SUCCESS:
            return "Command successfully completed.";
        case VK_NOT_READY:
            return "A fence or query has not yet completed.";
        case VK_TIMEOUT:
            return "A wait operation has not completed in the specified time.";
        case VK_EVENT_SET:
            return "An event is signaled.";
        case VK_EVENT_RESET:
            return "An event is unsignaled.";
        case VK_INCOMPLETE:
            return "A return array was too small for the result.";
        case VK_SUBOPTIMAL_KHR:
            return "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

            // Error codes
        case VK_ERROR_OUT_OF_HOST_MEMORY:
            return "A host memory allocation has failed.";
        case VK_ERROR_OUT_OF_DEVICE_MEMORY:
            return "A device memory allocation has failed.";
        case VK_ERROR_INITIALIZATION_FAILED:
            return "Initialization of an object could not be completed for implementation-specific reasons.";
        case VK_ERROR_DEVICE_LOST:
            return "The logical or physical device has been lost.";
        case VK_ERROR_MEMORY_MAP_FAILED:
            return "Mapping of a memory object has failed.";
        case VK_ERROR_LAYER_NOT_PRESENT:
            return "A requested layer is not present or could not be loaded.";
        case VK_ERROR_EXTENSION_NOT_PRESENT:
            return "A requested extension is not supported.";
        case VK_ERROR_FEATURE_NOT_PRESENT:
            return "A requested feature is not supported.";
        case VK_ERROR_INCOMPATIBLE_DRIVER:
            return "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
        case VK_ERROR_TOO_MANY_OBJECTS:
            return "Too many objects of the type have already been created.";
        case VK_ERROR_FORMAT_NOT_SUPPORTED:
            return "A requested format is not supported on this device.";
        case VK_ERROR_SURFACE_LOST_KHR:
            return "A surface is no longer available.";
        case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
            return "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
        case VK_ERROR_OUT_OF_DATE_KHR:
            return "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
                    + "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue" + "presenting to the surface.";
        case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR:
            return "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an" + " image.";
        case VK_ERROR_VALIDATION_FAILED_EXT:
            return "A validation layer found an error.";
        default:
            return String.format("%s [%d]", "Unknown", Integer.valueOf(result));
        }
    }

    public static String memFlagsToStr(int memtypeflags) {
        String memtypeflagsStr = "";
        for (int j = 0; j < 8; j++) {
            if ((memtypeflags & (1<<j)) != 0) {
                if (!memtypeflagsStr.isEmpty()) {
                    memtypeflagsStr+=",";
                }
                if (j==0) {
                    memtypeflagsStr+="VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT";
                } else if (j==1) {
                    memtypeflagsStr+="VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT";
                } else if (j==2) {
                    memtypeflagsStr+="VK_MEMORY_PROPERTY_HOST_COHERENT_BIT";
                } else if (j==3) {
                    memtypeflagsStr+="VK_MEMORY_PROPERTY_HOST_CACHED_BIT";
                } else if (j==4) {
                    memtypeflagsStr+="VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT";
                } else 
                memtypeflagsStr+=Integer.toHexString((1<<j));
            }
        }
        return memtypeflagsStr;
    }

    public static String imageLayoutToStr(int layout) {
        switch (layout) {
            case VK_IMAGE_LAYOUT_UNDEFINED:
                return "VK_IMAGE_LAYOUT_UNDEFINED";
            case VK_IMAGE_LAYOUT_GENERAL:
                return "VK_IMAGE_LAYOUT_GENERAL";
            case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL:
                return "VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL";
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL:
                return "VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL";
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL:
                return "VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL";
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL:
                return "VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL";
            case VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL:
                return "VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL";
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:
                return "VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL";
            case VK_IMAGE_LAYOUT_PREINITIALIZED:
                return "VK_IMAGE_LAYOUT_PREINITIALIZED";
            default:
                return "UNKNOWN";
        }
    }
}
