package nidefawl.qubes.vulkan;


import static nidefawl.qubes.gl.Engine.vkContext;
import static nidefawl.qubes.vulkan.VulkanInit.UINT64_MAX;
import static nidefawl.qubes.vulkan.VulkanInit.VK_FLAGS_NONE;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.*;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.shader.ShaderSource;
import nidefawl.qubes.shader.ShaderSource.ProcessMode;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.vulkan.spirvloader.SpirvCompiler;
import nidefawl.qubes.vulkan.spirvloader.SpirvCompilerOutput;

public class VKContext {
    static final boolean USE_FENCE_SYNC = true; // FAST
    static final boolean USE_RENDER_COMPLETE_SEMAPHORE = false;
    private static final boolean VK_DEBUG_CTXT = true;
    public static LongBuffer ZERO_OFFSET;
    public static int currentBuffer = 0;
    public static boolean DUMP_SHADER_SRC = false;
    private static VkImageMemoryBarrier.Buffer BARRIER_MEM_IMG;
    public static VkMemoryBarrier.Buffer BARRIER_MEM_ATT_WRITE_SHADER_READ;

    private static VkClearColorValue CLEAR_COLOR;
    private static VkImageSubresourceRange CLEAR_RANGE;
    private static VkClearDepthStencilValue CLEAR_DEPTH;

    public boolean reinitSwapchain = false;
    private boolean isInit;
    private boolean begin;

    public final VkInstance                    vk;
    public VkDevice                            device;
    public VkQueue                             vkQueue;
    public SwapChain                           swapChain           = null;
    private VkPresentInfoKHR                   presentInfo;
    public VkSubmitInfo                        submitInfo;
    public VkDescLayouts                       descLayouts;
    public VkMemoryManager                     memoryManager;
    protected VkPhysicalDeviceMemoryProperties memoryProperties;
    protected VkPhysicalDevice                 physicalDevice;
    public VkPhysicalDeviceProperties          properties;
    public VkPhysicalDeviceFeatures            features;
    public VkPhysicalDeviceLimits              limits;
    
    public int                                 colorFormat;
    public int                                 colorSpace;
    public int                                 depthFormat;
    protected long                             surface                       = VK_NULL_HANDLE;
    private long[]                             fences;
    private long                               freeFence                     = VK_NULL_HANDLE;
    private long                               copyFence                     = VK_NULL_HANDLE;
    public long                                renderCommandPool             = VK_NULL_HANDLE;
    public long                                copyCommandPool               = VK_NULL_HANDLE;
    public long                                descriptorPool                = VK_NULL_HANDLE;
    public long                               samplerLinear                 = VK_NULL_HANDLE;
    public long                               samplerNearest                = VK_NULL_HANDLE;
    
    protected long                             debugCallbackHandle           = VK_NULL_HANDLE;
    protected int                              queueFamilyIndex;
    protected LongBuffer                       psemaphorePresentComplete;
    protected LongBuffer                       psemaphoreRenderComplete;
    protected IntBuffer                        pWaitDstStageMask;
    public PointerBuffer                       pCommandBuffers;
    private IntBuffer                          pImageIndex;
    private LongBuffer                         pSwapchains;

    CommandBuffer[]                            copyCommandBuffers            = null;
    private CommandBuffer[]                    renderCommandBuffers;
    private CommandBuffer                      currentCmdBuffer;
    
    
    private ArrayList<IVkResource> resources = new ArrayList<>();
    ArrayList<RefTrackedResource> orphanedInUse = new ArrayList<>();

    public VKContext(VkInstance vk) {
        this.vk = vk;
    }

    public void syncAllFences() {
        if (VK_DEBUG_CTXT) System.err.println("VKContext.syncAllFences");
        if (!isInit) {
            return;
        }
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
//               vkWaitForFences(device, lFences, true, 1000000L*100L);
               freeFence = -1L; // signal that we have no fence in any queue submitted
            }
            vkQueueWaitIdle(vkQueue);
        } else {
            vkQueueWaitIdle(vkQueue);
        }
    }
    public void shutdown() {
        if (VK_DEBUG_CTXT) System.err.println("VKContext.shutdown");
        if (isInit) {
            syncAllFences();
            vkDeviceWaitIdle(device);
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
        freeCommandBuffers();
        for (int i = 0 ; i < orphanedInUse.size(); i++) {
            RefTrackedResource resource = orphanedInUse.remove(i--);
            resource.destroy();
        }
        ArrayList<IVkResource> list = new ArrayList<>(resources);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).destroy();
        }
        resources.clear();
        swapChain.destroy();
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
        if (samplerLinear != VK_NULL_HANDLE) {
            vkDestroySampler(device, samplerLinear, null);
        }
        if (samplerNearest != VK_NULL_HANDLE) {
            vkDestroySampler(device, samplerNearest, null);
        }

        this.descLayouts.destroy();
        if (descriptorPool != VK_NULL_HANDLE) {
            vkResetDescriptorPool(device, descriptorPool, 0);
            vkDestroyDescriptorPool(device, descriptorPool, null);
        }
        if (this.fences != null) {
            for (int i = 0; i < this.fences.length; i++) {
                vkDestroyFence(this.device, this.fences[i], null);
            }
        }
        vkDestroyFence(this.device, this.copyFence, null);
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

            VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
            LongBuffer pSampler = stack.longs(0);
            {
                sampler.minFilter(VK_FILTER_LINEAR);
                sampler.magFilter(VK_FILTER_LINEAR);
                sampler.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
                int err = vkCreateSampler(vkContext.device, sampler, null, pSampler);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
                }
                samplerLinear = pSampler.get(0);
            }
            {
                sampler.minFilter(VK_FILTER_NEAREST);
                sampler.magFilter(VK_FILTER_NEAREST);
                sampler.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);
                int err = vkCreateSampler(vkContext.device, sampler, null, pSampler);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
                }
                samplerNearest = pSampler.get(0);
            }
        
        }
        pSwapchains.put(0, swapChain.swapchainHandle);
        reinitPerFrameResources(swapChain.numImages);
    }
    private void reinitPerFrameResources(int numImages) {
        if (USE_FENCE_SYNC) {
            initFences(numImages);
        }
        initCommandBuffers(numImages);
        vkResetCommandPool(device, renderCommandPool, VK_FLAGS_NONE);
        int nFrameBuffers = swapChain.numImages;
        try (MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkInitializers.
                    commandBufferAllocInfo(this.renderCommandPool, nFrameBuffers);

            PointerBuffer pCommandBuffer = stack.callocPointer(nFrameBuffers);
            int err = vkAllocateCommandBuffers(this.device, cmdBufAllocateInfo, pCommandBuffer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to allocate render command buffer: " + VulkanErr.toString(err));
            }
            renderCommandBuffers = new CommandBuffer[nFrameBuffers];
            for (int i = 0; i < nFrameBuffers; i++) {
                renderCommandBuffers[i] = new CommandBuffer(pCommandBuffer.get(i), this.device, i);
            }
        }
    }

    private void initCommandBuffers(int length) {
        this.copyCommandBuffers = new CommandBuffer[length];
        this.renderCommandBuffers = new CommandBuffer[length];
        try ( MemoryStack stack = stackPush() ) {
            VkCommandBufferAllocateInfo cmdBufAllocInfo = VkCommandBufferAllocateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(length);
            PointerBuffer pCommandBuffer = stack.callocPointer(length);
            cmdBufAllocInfo.commandPool(this.copyCommandPool);
            vkAllocateCommandBuffers(this.device, cmdBufAllocInfo, pCommandBuffer);
            for (int i = 0; i < length; i++) {
                this.copyCommandBuffers[i] = new CommandBuffer(pCommandBuffer.get(i), device, i);
            }
            cmdBufAllocInfo.commandPool(this.renderCommandPool);
            vkAllocateCommandBuffers(this.device, cmdBufAllocInfo, pCommandBuffer);
            for (int i = 0; i < length; i++) {
                this.renderCommandBuffers[i] = new CommandBuffer(pCommandBuffer.get(i), device, i);
            }
        }
    }
    void freeCommandBuffers() {
        if (VK_DEBUG_CTXT) System.err.println("VKContext.freeCopyCommandBufferFrames");
        if (copyCommandBuffers != null) {
            for (int i = 0; i < copyCommandBuffers.length; i++) {
                vkFreeCommandBuffers(device, copyCommandPool, copyCommandBuffers[i]);
            }
        }
        if (renderCommandBuffers != null) {
            for (int i = 0; i < renderCommandBuffers.length; i++) {
                vkFreeCommandBuffers(device, renderCommandPool, renderCommandBuffers[i]);
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
            vkCreateFence(this.device, fenceCreate, null, pFence);
            this.copyFence = pFence.get(0);
        }
    }
    public void preRender() {
        // Get next image from the swap chain (back/front buffer).
        int err = vkAcquireNextImageKHR(device, swapChain.swapchainHandle, UINT64_MAX, psemaphorePresentComplete.get(0), VK_NULL_HANDLE, pImageIndex);
        currentBuffer = pImageIndex.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to acquire next swapchain image: " + VulkanErr.toString(err));
        }
        swapChain.swapChainAquired = true;
        if (USE_FENCE_SYNC) {
            // Use a fence to wait until the command buffer has finished execution before using it again
            err = vkWaitForFences(device, this.fences[currentBuffer], true, 1000L*1000L*1000L);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkWaitForFences failed: " + VulkanErr.toString(err));
            }

            err = vkResetFences(device, this.fences[currentBuffer]);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkResetFences failed: " + VulkanErr.toString(err));
            }
            if (begin) {
                throw new GameLogicError("cpyCmdBuf is open, error");
            }
            freeFence = this.fences[currentBuffer];
        }
        if (currentCmdBuffer != null) {
            currentCmdBuffer.inUse = false;
        }
        this.currentCmdBuffer = this.renderCommandBuffers[currentBuffer];
        VkTess.beginFrame();
        this.currentCmdBuffer.completeTasks();
        vkResetCommandBuffer(this.currentCmdBuffer, 0);
        updateOrphanedList();
        
    }
    public void postRender() {
        if (begin) {
            throw new GameLogicError("cpyCmdBuf is open, error");
        }
        if (swapChain.swapChainAquired) {

            int err = vkQueuePresentKHR(vkQueue, presentInfo);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to present the swapchain image: " + VulkanErr.toString(err));
            }
            if (!USE_FENCE_SYNC) {
                vkQueueWaitIdle(vkQueue);
            }
        }
    }
    public void submitCommandBuffer() {
        VkTess.endFrame();
        currentCmdBuffer.inUse = true;
        pCommandBuffers.put(0, this.currentCmdBuffer);
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
    public int[] updateSwapchain(GameBase gameBase, boolean vsync) {
        if (!isInit) {
            return new int[] { gameBase.windowWidth, gameBase.windowHeight };
        }
        syncAllFences();
//        vkQueueWaitIdle(vkQueue);
        System.out.println("Reinit swap chain "+gameBase.windowWidth+","+gameBase.windowHeight+",vsync="+vsync);
        // Begin the setup command buffer (the one we will use for swapchain/framebuffer creation)
        int images = this.swapChain.numImages;
        this.swapChain.setup(gameBase.windowWidth, gameBase.windowHeight, vsync);
        pSwapchains.put(0, swapChain.swapchainHandle);
        
        if (images != this.swapChain.numImages) {
            throw new GameLogicError("Attempt to change number of swapchain images at runtime. Unsupported behaviour");
        }
//        this.swapChain.framebuffers = createFramebuffers(device, swapchain, clearRenderPass, width, height);
        // Create render command buffers
        if (VkRenderPasses.isInit()) {
            VkPipelines.init(this);
//            reallocRenderCommandBuffers();
        }
        swapChain.swapChainAquired = false;
        reinitSwapchain = false;
        return new int[] { this.swapChain.width, this.swapChain.height };
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
        if (!reinitSwapchain && VkRenderPasses.isInit()) {
            if (i == 1) {
                syncAllFences();
//                reallocRenderCommandBuffers();
            }
        }
    }
    public VkShader loadShader(AssetManager assetManager, String string, int stage) {
        VkShader shader = assetManager.loadVkShaderBin(this, string, stage);
        shader.buildShader();
        return shader;
    }
    public void writeShaderBin(byte[] data, String string) {
        File out = new File(string);
        System.out.println("write to "+out.getAbsolutePath());
        new File(out.getAbsolutePath()).getParentFile().mkdirs();
//        System.out.println("mkdirs "+out.getParentFile().getAbsolutePath());
        
        OutputStream os = null;
        try {
            os = new FileOutputStream(out);
            os = new DataOutputStream(os);
            ((DataOutputStream)os).write(data);
            os.flush();
            os.close();
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
        return this.loadCompileGLSL(assetManager, string, stage, def, ProcessMode.VULKAN);
        
    }
    public VkShader loadCompileGLSL(AssetManager assetManager, String string, int stage, IShaderDef def, ProcessMode processMode) {
        ShaderSource shaderSource = assetManager.loadVkShaderSource(string, stage, def, processMode);
        if (shaderSource.isEmpty()) {
            throw new GameLogicError("Shader source is empty for "+string);
        }
        String source = shaderSource.getSource();
//        writeShader(source, "preprocessed/"+string);
        int options = 0;
        options |= SpirvCompiler.OptionLinkProgram;
        options |= SpirvCompiler.OptionSpv;
        options |= SpirvCompiler.OptionVulkanRules;
        options |= SpirvCompiler.OptionSuppressWarnings;
//        options |= SpirvCompiler.OptionAutoMapBindings;
//        options |= SpirvCompiler.OptionDumpReflection;
        SpirvCompilerOutput result = SpirvCompiler.compile(source, stage, options);
        if (result == null) {
            throw new GameLogicError("Failed compiling spirv. Expected nonnull return value");
        }
        if (result.get(stage) == null) {
//                System.err.println(source);

            System.err.println("MISSING BINARY "+string+": "+result.log+","+result.status+","+result);
            throw new GameLogicError("Missing shader binary module");
        }
        result.log = result.log.replaceAll("Warning, version 450 is not yet complete; most version-specific features are present, but some are missing.", "").trim();
        if (!result.log.isEmpty())
            System.err.println(result.log.trim());
        if (result.status != 0) {
            return null;
        }
        if (DUMP_SHADER_SRC) {
//            System.out.println(source);
//            writeShaderBin(result.get(stage), string);
            writeShader(source, "preprocessed_shader_src/"+string);
        }
        
        
        VkShader shader = new VkShader(this, stage, shaderSource.getFileName(), result.get(stage));
        shader.buildShader();
        return shader;
    }
    public void finishUpload() {
        //TODO: create unsychronized upload handle using fences and non-blocking vkWaitForFences calls
        if (begin) {
            begin = false;
            copyCommandBuffers[currentBuffer].inUse = true;
            int err = vkEndCommandBuffer(copyCommandBuffers[currentBuffer]);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkEndCommandBuffer failed: " + VulkanErr.toString(err));
            }
//            System.out.println("SUBMIT COPY CMD "+currentBuffer);
            try ( MemoryStack stack = stackPush() ) {
                VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                PointerBuffer pCommandBuffers = stack.pointers(copyCommandBuffers[currentBuffer]);
                submitInfo.pCommandBuffers(pCommandBuffers);

                err = vkResetFences(device, this.copyFence);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkResetFences failed: " + VulkanErr.toString(err));
                }
                
                err = vkQueueSubmit(this.vkQueue, submitInfo, copyFence);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkQueueSubmit failed: " + VulkanErr.toString(err));
                }
                //Wait for upload to complete!
                err = vkWaitForFences(device, this.copyFence, true, 1000L*1000L*6000L);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkWaitForFences failed: " + VulkanErr.toString(err));
                }
                copyCommandBuffers[currentBuffer].inUse = false;
            }
        }
    }
    public CommandBuffer getCopyCommandBuffer() {
        CommandBuffer commandBuffer = copyCommandBuffers[currentBuffer];
        if (commandBuffer.inUse) {
            throw new GameLogicError("CANNOT START ANOTHER CPY BUFFER FOR THIS FRAME");
        }
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

    public void setImageLayout(VkCommandBuffer cmdBuffer, long image, int aspectMask,
            int oldLayout, int newLayout, int srcStageFlags, int destStageFlags) {
        this.setImageLayout(cmdBuffer, image, aspectMask, oldLayout, newLayout, srcStageFlags, destStageFlags, 0, 0);
    }
    public void setImageLayout(VkCommandBuffer cmdBuffer, long image, int aspectMask,
            int oldLayout, int newLayout, int srcStageFlags, int destStageFlags, int srcAccessMask, int dstAccessMask) {
        BARRIER_MEM_IMG.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        BARRIER_MEM_IMG.pNext(NULL);
        BARRIER_MEM_IMG.srcAccessMask(srcAccessMask);
        BARRIER_MEM_IMG.dstAccessMask(dstAccessMask);
        BARRIER_MEM_IMG.oldLayout(oldLayout);
        BARRIER_MEM_IMG.newLayout(newLayout);
        BARRIER_MEM_IMG.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        BARRIER_MEM_IMG.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        BARRIER_MEM_IMG.image(image);
//        BARRIER_MEM_IMG.subresourceRange(subresourceRange);
        BARRIER_MEM_IMG.subresourceRange()
            .aspectMask(aspectMask)
            .baseMipLevel(0)
            .levelCount(1)
            .baseArrayLayer(0)
            .layerCount(1);
        
        // Only sets masks for layouts used in this example
        // For a more complete version that can be used with other layouts see vkTools::setImageLayout

        // Source layouts (old)
        switch (oldLayout)
        {
        case VK_IMAGE_LAYOUT_UNDEFINED:
            // Only valid as initial layout, memory contents are not preserved
            // Can be accessed directly, no source dependency required
            BARRIER_MEM_IMG.srcAccessMask(0);
            break;
        case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL:
            BARRIER_MEM_IMG.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            break;
        case VK_IMAGE_LAYOUT_PREINITIALIZED:
            // Only valid as initial layout for linear images, preserves memory contents
            // Make sure host writes to the image have been finished
            BARRIER_MEM_IMG.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT);
            break;
        case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:
            // Old layout is transfer destination
            // Make sure any writes to the image have been finished
            BARRIER_MEM_IMG.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            break;
        case VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL:
            // Transfer source (copy, blit)
            // Make sure any reads from the image have been finished
            BARRIER_MEM_IMG.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
            break;
        }

        // Target layouts (new)
        switch (newLayout)
        {
        case VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL:
            // Transfer source (copy, blit)
            // Make sure any reads from the image have been finished
            BARRIER_MEM_IMG.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
            break;
        case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:
            // Transfer destination (copy, blit)
            // Make sure any writes to the image have been finished
            BARRIER_MEM_IMG.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            break;
        case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL:
            // Shader read (sampler, input attachment)
            BARRIER_MEM_IMG.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            break;

        case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL:
            BARRIER_MEM_IMG.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            break;

        case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL:
            BARRIER_MEM_IMG.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
            break;
        case VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL:
            BARRIER_MEM_IMG.dstAccessMask(VK_ACCESS_INPUT_ATTACHMENT_READ_BIT | VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT);
            break;
        }

        // Put barrier inside setup command buffer
        vkCmdPipelineBarrier(
            cmdBuffer,
            srcStageFlags, 
            destStageFlags, 
            0, 
            null,
            null,
            BARRIER_MEM_IMG);
    }
    public static void allocStatic() {
        ZERO_OFFSET = memAllocLong(1);
        ZERO_OFFSET.put(0, 0);
        BARRIER_MEM_IMG = VkImageMemoryBarrier.calloc(1);
        BARRIER_MEM_ATT_WRITE_SHADER_READ = VkMemoryBarrier.calloc(1);
        BARRIER_MEM_ATT_WRITE_SHADER_READ.sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER).pNext(0L);
        BARRIER_MEM_ATT_WRITE_SHADER_READ.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
        CLEAR_COLOR = VkClearColorValue.calloc();
        CLEAR_DEPTH = VkClearDepthStencilValue.calloc();
        CLEAR_RANGE = VkImageSubresourceRange.calloc();

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
        BARRIER_MEM_IMG.free();
        BARRIER_MEM_ATT_WRITE_SHADER_READ.free();
        CLEAR_RANGE.free();
        CLEAR_DEPTH.free();
        CLEAR_COLOR.free();
    }
    public CommandBuffer getCurrentCmdBuffer() {
        return this.currentCmdBuffer;
    }

    public void updateOrphanedList() {
        int curFrame = VKContext.currentBuffer;
        for (int i = 0 ; i < orphanedInUse.size(); i++) {
            orphanedInUse.get(i).unflagUse(curFrame);
        }
        for (int i = 0 ; i < orphanedInUse.size(); i++) {
            if (orphanedInUse.get(i).isFree()) {
                RefTrackedResource resource = orphanedInUse.remove(i--);
//                if (resource instanceof BufferPair) {
//                    System.out.print(resource+" "+((BufferPair)resource).tag()+" ON DESTROY ");
//                    boolean[] b = ((BufferPair)resource).inUseBy;
//                    for (int z = 0; z < b.length; z++) {
//                        System.out.print(b[z]);
//                        System.out.print(",");
//                    }
//                    System.out.println();
//                }
                resource.destroy();
            }
        }
    }
    public BufferPair getFreeBuffer() {
        return new BufferPair(this);
    }
    public void orphanResource(RefTrackedResource resource) {
        if (resource != null) {
            this.orphanedInUse.add(resource);
//            if (resource instanceof BufferPair) {
//                System.out.print(resource+" "+((BufferPair)resource).tag()+" ON ORPHAN ");
//                boolean[] b = ((BufferPair)resource).inUseBy;
//                for (int i = 0; i < b.length; i++) {
//                    System.out.print(b[i]);
//                    System.out.print(",");
//                }
//                System.out.println();
//            }
        }
    }
    public void clearImage(VkCommandBuffer commandBuffer, long image, int nLayers, int targetlayout, float r, float g, float b, float a) {

        vkContext.setImageLayout(commandBuffer, image,
                VK_IMAGE_ASPECT_COLOR_BIT, VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,    VK_PIPELINE_STAGE_TRANSFER_BIT, 
                VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT, VK_ACCESS_TRANSFER_WRITE_BIT);
        CLEAR_COLOR.float32(0, r).float32(1, g).float32(2, b).float32(3, a);
        CLEAR_RANGE.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            .baseArrayLayer(0)
            .baseMipLevel(0)
            .layerCount(1)
            .levelCount(1);
        vkCmdClearColorImage(commandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, CLEAR_COLOR, CLEAR_RANGE);
        vkContext.setImageLayout(commandBuffer, image,
                VK_IMAGE_ASPECT_COLOR_BIT, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                targetlayout,
                VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,  VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 
                VK_ACCESS_TRANSFER_WRITE_BIT,                   VK_ACCESS_MEMORY_READ_BIT);
//        System.out.println(""+r+","+g+","+b+","+a);
    }
    public void clearDepthStencilImage(VkCommandBuffer commandBuffer, long image, int targetlayout, float depth, int stencil) {

        vkContext.setImageLayout(commandBuffer, image,
                VK_IMAGE_ASPECT_DEPTH_BIT|VK_IMAGE_ASPECT_STENCIL_BIT, VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,    VK_PIPELINE_STAGE_TRANSFER_BIT, 
                VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK_ACCESS_TRANSFER_WRITE_BIT);
        CLEAR_DEPTH.set(depth, stencil);
        CLEAR_RANGE.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            .baseArrayLayer(0)
            .baseMipLevel(0)
            .layerCount(1)
            .levelCount(1);
        vkCmdClearDepthStencilImage(commandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, CLEAR_DEPTH, CLEAR_RANGE);
        vkContext.setImageLayout(commandBuffer, image,
                VK_IMAGE_ASPECT_DEPTH_BIT|VK_IMAGE_ASPECT_STENCIL_BIT, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                targetlayout,
                VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,  VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 
                VK_ACCESS_TRANSFER_WRITE_BIT,                   VK_ACCESS_MEMORY_READ_BIT);
//        System.out.println(""+r+","+g+","+b+","+a);
    }
}
