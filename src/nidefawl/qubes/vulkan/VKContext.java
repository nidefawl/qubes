package nidefawl.qubes.vulkan;


import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkImageMemoryBarrier.Buffer;

import nidefawl.qubes.GameBase;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static nidefawl.qubes.vulkan.VulkanInit.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class VKContext {
    public static final class SwapChain {
        
        public long[]          images = null;
        public long[]          imageViews = null;
        public long            swapchainHandle = VK_NULL_HANDLE;
        public int             width = 0;
        public int             height = 0;
        public long[] framebuffers;
        public VkSwapchainCreateInfoKHR swapchainCI;
        public boolean isVsync() {
            if (swapchainCI!=null&&swapchainCI.presentMode() == VK_PRESENT_MODE_FIFO_KHR) {
                return true;
            }
            return false;
        }
    }

    public final SwapChain                     swapChain = new SwapChain();
    public final VkInstance                    vk;
    public VkDevice                            device;
    public VkQueue                             vkQueue;
    public int                                 colorFormat;
    public int                                 colorSpace;
    public long                                clearRenderPass;
    public long                                renderCommandPool;

    protected long                             surface;
    protected long                             debugCallbackHandle;
    protected VkPhysicalDevice                 physicalDevice;
    protected VkPhysicalDeviceMemoryProperties memoryProperties;
    protected int                              queueFamilyIndex;
    protected long                             setupCommandPool;

    public VKContext(VkInstance vk) {
        this.vk = vk;
    }

    public int currentBuffer = 0;
    private IntBuffer pImageIndex;
    public PointerBuffer pCommandBuffers;
    private LongBuffer pSwapchains;
    public VkSubmitInfo submitInfo;
    private VkPresentInfoKHR presentInfo;
    protected VkCommandBuffer postPresentCommandBuffer;
    public boolean reinitSwapchain = false;
    private LongBuffer psemaphorePresentComplete;
    private LongBuffer psemaphoreRenderComplete;

    public void init() {
        psemaphorePresentComplete = memAllocLong(1);
        psemaphoreRenderComplete = memAllocLong(1);
        
        pCommandBuffers = memAllocPointer(1);
        pImageIndex = memAllocInt(1);
        pCommandBuffers = memAllocPointer(1);
        pSwapchains = memAllocLong(1);
        
        MemoryStack stack = stackGet(); int stackPointer = stack.getPointer();

        // Pre-allocate everything needed in the render loop
        long semaphorePresentComplete = VulkanInit.createSemaphore(this);
        long semaphoreRenderComplete = VulkanInit.createSemaphore(this);
        psemaphorePresentComplete.put(0, semaphorePresentComplete);
        psemaphoreRenderComplete.put(0, semaphoreRenderComplete);
        // Info struct to submit a command buffer which will wait on the semaphore
        submitInfo = VkSubmitInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pNext(NULL)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(psemaphorePresentComplete)
                .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                .pCommandBuffers(pCommandBuffers)
                .pSignalSemaphores(psemaphoreRenderComplete);

        // Info struct to present the current swapchain image to the display
        presentInfo = VkPresentInfoKHR.calloc()
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pNext(NULL)
                .pWaitSemaphores(psemaphoreRenderComplete)
                .swapchainCount(pSwapchains.remaining())
                .pSwapchains(pSwapchains)
                .pImageIndices(pImageIndex)
                .pResults(null);

        stack.setPointer(stackPointer);
    }
    public void preRender() {

        // Get next image from the swap chain (back/front buffer).
        // This will setup the imageAquiredSemaphore to be signalled when the operation is complete
        int err = vkAcquireNextImageKHR(device, swapChain.swapchainHandle, UINT64_MAX, psemaphorePresentComplete.get(0), VK_NULL_HANDLE, pImageIndex);
        currentBuffer = pImageIndex.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to acquire next swapchain image: " + VulkanErr.toString(err));
        }
    }
    public void postRender() {
        // Present the current buffer to the swap chain
        // This will display the image
        pSwapchains.put(0, swapChain.swapchainHandle);
        int err = vkQueuePresentKHR(vkQueue, presentInfo);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to present the swapchain image: " + VulkanErr.toString(err));
        }

        // Create and submit post present barrier
        vkQueueWaitIdle(vkQueue);

        // Destroy this semaphore (we will create a new one in the next frame)
//        submitPostPresentBarrier(swapChain.images[currentBuffer], postPresentCommandBuffer, vkQueue);
        
    }
    public void submitCommandBuffer(VkCommandBuffer commandBuffer) {
        if (commandBuffer == null || commandBuffer.address() == NULL)
            return;
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
        PointerBuffer pCommandBuffers = memAllocPointer(1)
                .put(commandBuffer)
                .flip();
        submitInfo.pCommandBuffers(pCommandBuffers);
        int err = vkQueueSubmit(this.vkQueue, submitInfo, VK_NULL_HANDLE);
        memFree(pCommandBuffers);
        submitInfo.free();
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to submit command buffer: " + VulkanErr.toString(err));
        }
    }
    public void updateSwapchain(int width, int height, boolean vsync) {
        System.out.println("Reinit swap chain "+width+","+height+",vsync="+vsync);
        // Begin the setup command buffer (the one we will use for swapchain/framebuffer creation)
        VulkanInit.createSwapChain(this, width, height, vsync);
        vkQueueWaitIdle(this.vkQueue);

        VulkanInit.createSwapchainFramebuffers(this);
//        this.swapChain.framebuffers = createFramebuffers(device, swapchain, clearRenderPass, width, height);
        // Create render command buffers
        GameBase.baseInstance.rebuildRenderCommands();
        reinitSwapchain = false;
    }

    public void resetRenderCommandPool() {
        vkResetCommandPool(device, renderCommandPool, VK_FLAGS_NONE);
    }
}
