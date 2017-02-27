package nidefawl.qubes.vulkan;


import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkImageMemoryBarrier.Buffer;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetManagerClient;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.shader.ShaderSource;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.vulkan.spirvloader.SpirvCompiler;
import nidefawl.qubes.vulkan.spirvloader.SpirvCompilerOutput;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static nidefawl.qubes.vulkan.VulkanInit.*;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class VKContext {
    static final boolean USE_FENCE_SYNC = true; // FAST
    static final boolean USE_RENDER_COMPLETE_SEMAPHORE = false;
    public static LongBuffer ZERO_OFFSET;
    public static int currentBuffer = 0;
    public static final class DepthStencil {
        long image = VK_NULL_HANDLE;
        long view = VK_NULL_HANDLE;
    }

    public SwapChain                           swapChain           = null;
    public final DepthStencil                  depthStencil        = new DepthStencil();
    public final VkInstance                    vk;
    public VkDevice                            device;
    public VkQueue                             vkQueue;
    public int                                 colorFormat;
    public int                                 colorSpace;
    public int                                 depthFormat;
    public long                                renderPass = VK_NULL_HANDLE;
    public long                                renderPassSubpasses = VK_NULL_HANDLE;
    public long                                renderCommandPool = VK_NULL_HANDLE;
    public long                                copyCommandPool = VK_NULL_HANDLE;

    protected long                             surface;
    protected long                             debugCallbackHandle = VK_NULL_HANDLE;
    protected VkPhysicalDevice                 physicalDevice;
    protected VkPhysicalDeviceMemoryProperties memoryProperties;
    protected int                              queueFamilyIndex;
    protected LongBuffer                       psemaphorePresentComplete;
    protected LongBuffer                       psemaphoreRenderComplete;
    protected IntBuffer                       pWaitDstStageMask;
    VkCommandBuffer[] copyCommandBuffers = null;

    public VKContext(VkInstance vk) {
        this.vk = vk;
    }

    public VkSubmitInfo submitInfo;
    private VkPresentInfoKHR presentInfo;
    
    public boolean reinitSwapchain = false;
    public VkPhysicalDeviceProperties properties;
    public VkPhysicalDeviceFeatures features;
    public VkPhysicalDeviceLimits limits;

    public PointerBuffer pCommandBuffers;
    private IntBuffer pImageIndex;
    private LongBuffer pSwapchains;
    
    
    private boolean isInit;
    private ArrayList<IVkResource> resources = new ArrayList<>();
    private long[] fences;
    public VkMemoryManager memoryManager;
    private boolean begin;
    private long freeFence;
    public long descriptorPool;
    public VkDescLayouts descLayouts;
    public void syncAllFences() {
        if (USE_FENCE_SYNC) {
            if (freeFence != -1L) {
               long[] lFences = this.fences;
                // if we are inside a frame then skip the current fence that isn't submitted
               if (freeFence > 0) { 
                   lFences = new long[this.fences.length-1];
                   int idx = 0;
                   for (int i = 0; i < this.fences.length; i++) {
                       if (fences[i] != freeFence) {
                           lFences[idx++] = fences[i];
                       }
                   }
               }
               vkWaitForFences(device, lFences, true, 1000000L*2000L);
               freeFence = -1L; // signal that we have no fence in any queue submitted
            }
        } else {
            vkQueueWaitIdle(vkQueue);
        }
    }
    public void shutdown() {
        if (isInit) {
            if (USE_FENCE_SYNC) {
                if (this.fences != null) {
                    vkWaitForFences(device, this.fences, true, 1000000L*2000L);    
                }
            } else {
                vkDeviceWaitIdle(device);
            }
            vkDestroySemaphore(device, psemaphorePresentComplete.get(0), null);
            vkDestroySemaphore(device, psemaphoreRenderComplete.get(0), null);
            memFree(pCommandBuffers);
            memFree(pImageIndex);
            memFree(pSwapchains);
            memFree(psemaphorePresentComplete);
            memFree(psemaphoreRenderComplete);
            memFree(pWaitDstStageMask);
            submitInfo.free();
            presentInfo.free();
        }
        freeCopyCommandBufferFrames();
        ArrayList<IVkResource> list = new ArrayList<>(resources);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).destroy();
        }
        resources.clear();
        if (swapChain.swapchainHandle != VK_NULL_HANDLE)
        {
            for (int i = 0; i < swapChain.images.length; i++)
            {
                vkDestroyImageView(device, swapChain.imageViews[i], null);
            }
            vkDestroySwapchainKHR(device, swapChain.swapchainHandle, null);
            swapChain.swapchainCI.free();
        }
        if (swapChain.framebuffers != null) {
            for (int i = 0; i < swapChain.framebuffers.length; i++)
                vkDestroyFramebuffer(device, swapChain.framebuffers[i], null);
        }
        if (surface != VK_NULL_HANDLE)
        {
            vkDestroySurfaceKHR(vk, surface, null);
        }
        if (renderCommandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(device, renderCommandPool, null);
        }
        if (copyCommandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(device, copyCommandPool, null);
        }
        if (renderPass != VK_NULL_HANDLE) {
            vkDestroyRenderPass(device, renderPass, null);
        }
        if (renderPassSubpasses != VK_NULL_HANDLE) {
            vkDestroyRenderPass(device, renderPassSubpasses, null);
        }
        if (depthStencil.view != VK_NULL_HANDLE) {
            vkDestroyImageView(device, depthStencil.view, null);
        }
        if (depthStencil.image != VK_NULL_HANDLE) {
            this.memoryManager.releaseImageMemory(depthStencil.image);
            vkDestroyImage(device, depthStencil.image, null);
        }
        this.descLayouts.destroy();
        if (descriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(device, descriptorPool, null);
        }
        if (this.fences != null) {
            for (int i = 0; i < this.fences.length; i++) {
                vkDestroyFence(this.device, this.fences[i], null);
            }
        }
        if (device != null) {
            vkDeviceWaitIdle(device);
            this.memoryManager.shudown();
            vkDestroyDevice(device, null);
        }
        if (debugCallbackHandle != VK_NULL_HANDLE) {
//            vkDestroyDebugReportCallbackEXT(vk, debugCallbackHandle, null);
        }
        vkDestroyInstance(vk, null);
        if (properties != null) {
            properties.free();
        }
        if (features != null) {
            features.free();
        }
        if (memoryProperties != null) {
            memoryProperties.free();
        }
    }
    public void init() {
        isInit = true;
        pCommandBuffers = memAllocPointer(1);
        pImageIndex = memAllocInt(1);
        pSwapchains = memAllocLong(1);
        psemaphorePresentComplete = memAllocLong(1);
        psemaphoreRenderComplete = memAllocLong(1);
        pWaitDstStageMask = memAllocInt(1);
        try ( MemoryStack stack = stackPush() ) {
            psemaphorePresentComplete.put(0, VulkanInit.createSemaphore(this));
            psemaphoreRenderComplete.put(0, VulkanInit.createSemaphore(this));
            pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);

            // Info struct to submit a command buffer which will wait on the semaphore
            submitInfo = VkSubmitInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pNext(NULL)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(psemaphorePresentComplete)
                    .pWaitDstStageMask(pWaitDstStageMask)
                    .pCommandBuffers(pCommandBuffers);
            if (USE_RENDER_COMPLETE_SEMAPHORE) {
                submitInfo.pSignalSemaphores(psemaphoreRenderComplete);
            }

            // Info struct to present the current swapchain image to the display
            presentInfo = VkPresentInfoKHR.calloc()
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pNext(NULL)
                    .swapchainCount(pSwapchains.remaining())
                    .pSwapchains(pSwapchains)
                    .pImageIndices(pImageIndex)
                    .pResults(null);
            if (USE_RENDER_COMPLETE_SEMAPHORE) {
                presentInfo.pWaitSemaphores(psemaphoreRenderComplete);
            }
        }
        pSwapchains.put(0, swapChain.swapchainHandle);
        reinitPerFrameResources(swapChain.numImages);
    }
    private void reinitPerFrameResources(int numImages) {
        if (USE_FENCE_SYNC) {
            initFences(numImages);
        }
        initCopyCommandPools(numImages);
    }

    private void initCopyCommandPools(int length) {
        freeCopyCommandBufferFrames();
        try ( MemoryStack stack = stackPush() ) {
            this.copyCommandBuffers = new VkCommandBuffer[length];
            for (int i = 0; i < this.copyCommandBuffers.length; i++) {
                this.copyCommandBuffers[i] = makeCopyCommandBuffer();
            }
        }
    }
    private void freeCopyCommandBufferFrames() {
        if (copyCommandBuffers != null) {
            for (int i = 0; i < copyCommandBuffers.length; i++) {
                vkFreeCommandBuffers(device, copyCommandPool, copyCommandBuffers[i]);
            }
        }
        copyCommandBuffers = null;
    }
    private void initFences(int length) {
        try ( MemoryStack stack = stackPush() ) {
            if (this.fences != null) {
                for (int i = 0; i < this.fences.length; i++) {
                    vkDestroyFence(this.device, this.fences[i], null);
                }
            }
            VkFenceCreateInfo fenceCreate = VkFenceCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT);
            this.fences = new long[length];
            LongBuffer pFence = stack.longs(1);
            for (int i = 0; i < this.fences.length; i++) {
                vkCreateFence(this.device, fenceCreate, null, pFence);
                this.fences[i] = pFence.get(0);
            }
        }
    }
    public void preRender() {
        // Get next image from the swap chain (back/front buffer).
        int err = vkAcquireNextImageKHR(device, swapChain.swapchainHandle, UINT64_MAX, psemaphorePresentComplete.get(0), VK_NULL_HANDLE, pImageIndex);
        currentBuffer = pImageIndex.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to acquire next swapchain image: " + VulkanErr.toString(err));
        }
        if (USE_FENCE_SYNC) {
            // Use a fence to wait until the command buffer has finished execution before using it again
            err = vkWaitForFences(device, this.fences[currentBuffer], true, UINT64_MAX);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkWaitForFences failed: " + VulkanErr.toString(err));
            }

            vkResetFences(device, this.fences[currentBuffer]);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkResetFences failed: " + VulkanErr.toString(err));
            }
            freeFence = this.fences[currentBuffer];
        }
    }
    public void postRender() {
        int err = vkQueuePresentKHR(vkQueue, presentInfo);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to present the swapchain image: " + VulkanErr.toString(err));
        }
        if (!USE_FENCE_SYNC) {
            vkQueueWaitIdle(vkQueue);
        }
    }
    public void submitCommandBuffer(VkCommandBuffer commandBuffer) {
        pCommandBuffers.put(0, commandBuffer);
        long fence = VK_NULL_HANDLE;
        if (USE_FENCE_SYNC) {
            fence = this.fences[currentBuffer];
            freeFence = 0;
        }
        int err = vkQueueSubmit(this.vkQueue, submitInfo, fence);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to submit command buffer: " + VulkanErr.toString(err));
        }
    }
    public void updateSwapchain(int width, int height, boolean vsync) {
        if (!isInit) {
            return;
        }
        syncAllFences();
        System.out.println("Reinit swap chain "+width+","+height+",vsync="+vsync);
        // Begin the setup command buffer (the one we will use for swapchain/framebuffer creation)
        int images = this.swapChain.numImages;
        this.swapChain.setup(width, height, vsync);
        pSwapchains.put(0, swapChain.swapchainHandle);
        
        VulkanInit.createDepthStencilImages(this);
        VulkanInit.createFramebuffers(this);
        if (images != this.swapChain.numImages) {
            throw new GameLogicError("Attempt to change number of swapchain images at runtime. Unsupported behaviour");
        }
//        this.swapChain.framebuffers = createFramebuffers(device, swapchain, clearRenderPass, width, height);
        // Create render command buffers
        if (getMainRenderPass() != VK_NULL_HANDLE) {
            VkPipelines.init(this);
            GameBase.baseInstance.rebuildRenderCommands();    
        }
        
        reinitSwapchain = false;
    }

    public void resetRenderCommandPool() {
        vkResetCommandPool(device, renderCommandPool, VK_FLAGS_NONE);
    }
    public VkBuffer createBuffer(int usageFlags, long size, boolean deviceLocalMemory, String tag) {
        VkBuffer buffer = new VkBuffer(this).tag(tag);
        buffer.create(usageFlags, size, deviceLocalMemory);
        return buffer;
    }
    
    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }
    public void addResource(IVkResource vkShader) {
        this.resources.add(vkShader);
    }
    public void removeResource(IVkResource vkShader) {
        this.resources.remove(vkShader);
    }

    public void lateInit(int i) {
        if (!reinitSwapchain && getMainRenderPass() != VK_NULL_HANDLE) {
            if (i == 1) {
                GameBase.baseInstance.rebuildRenderCommands();
            }
        }
    }
    public VkShader loadShader(AssetManager assetManager, String string, int stage) {
        VkShader shader = assetManager.loadVkShaderBin(this, string, stage);
        shader.buildShader();
        return shader;
    }

    
    private void writeShader(String source, String string) {
        System.out.println("write to "+string);
        File out = new File(string);
        out.getParentFile().mkdirs();
//        System.out.println("mkdirs "+out.getParentFile().getAbsolutePath());
        
        FileWriter os = null;
        try {
            os = new FileWriter(out);
            BufferedWriter bw = new BufferedWriter(os);
            bw.write(source);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public VkShader loadCompileGLSL(AssetManager assetManager, String string, int stage, IShaderDef def) {
        ShaderSource shaderSource = assetManager.loadVkShaderSource(string, stage, def);
        if (shaderSource.isEmpty()) {
            throw new GameLogicError("Shader source is empty for "+string);
        }
        String source = shaderSource.getSource();
//        writeShader(source, "preprocessed/"+string);
        int options = 0;
        options |= SpirvCompiler.OptionLinkProgram;
        options |= SpirvCompiler.OptionSpv;
        options |= SpirvCompiler.OptionVulkanRules;
//        options |= SpirvCompiler.OptionAutoMapBindings;
//        options |= SpirvCompiler.OptionDumpReflection;
        SpirvCompilerOutput result = SpirvCompiler.compile(source, stage, options);
        if (result == null) {
            throw new GameLogicError("Failed compiling spirv. Expected nonnull return value");
        }
        System.out.println("-- Compiled "+string+"="+result.status+" --");
        System.out.println(result.log.trim());
        if (result.status != 0) {
            return null;
        }
        
        
        VkShader shader = new VkShader(this, stage, shaderSource.getFileName(), result.get(stage));
        shader.buildShader();
        return shader;
    }
    public VkCommandBuffer makeCopyCommandBuffer() {
        try ( MemoryStack stack = stackPush() ) {
            
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack();
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(copyCommandPool);
            allocInfo.commandBufferCount(1);
            PointerBuffer pCommandBuffer = stack.pointers(0);
            vkAllocateCommandBuffers(this.device, allocInfo, pCommandBuffer);
            return new VkCommandBuffer(pCommandBuffer.get(0), this.device);
        }
    }
    public void finishUpload() {
        if (begin) {
            begin = false;
            int err = vkEndCommandBuffer(copyCommandBuffers[currentBuffer]);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkEndCommandBuffer failed: " + VulkanErr.toString(err));
            }
            try ( MemoryStack stack = stackPush() ) {
                VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                PointerBuffer pCommandBuffers = stack.pointers(copyCommandBuffers[currentBuffer]);
                submitInfo.pCommandBuffers(pCommandBuffers);
                err = vkQueueSubmit(this.vkQueue, submitInfo, VK_NULL_HANDLE);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkQueueSubmit failed: " + VulkanErr.toString(err));
                }
            }
        }
    }
    public VkCommandBuffer getCopyCommandBuffer() {
        VkCommandBuffer commandBuffer = copyCommandBuffers[currentBuffer];
        if (!begin) {
            begin = true;
            try ( MemoryStack stack = stackPush() ) {
                VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                        .pNext(NULL).flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                int err = vkBeginCommandBuffer(commandBuffer, cmdBufInfo);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkBeginCommandBuffer failed: " + VulkanErr.toString(err));
                }
            }
        }
        return commandBuffer;
    }
    public boolean isResource(IVkResource vkResource) {
        return this.resources.contains(vkResource);
    }
    public static void allocStatic() {
        ZERO_OFFSET = memAllocLong(1);
        ZERO_OFFSET.put(0, 0);
        VkMemoryManager.allocStatic();
        VkDescLayouts.allocStatic();
        VkTexture.allocStatic();
        VkShader.allocStatic();
    }
    public static void destroyStatic() {
        VkShader.destroyStatic();
        VkTexture.destroyStatic();
        VkDescLayouts.destroyStatic();
        VkMemoryManager.destroyStatic();
        memFree(ZERO_OFFSET);
    }
    public long getMainRenderPass() {
        return this.renderPassSubpasses;
    }
}
