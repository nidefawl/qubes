package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.vulkan.VkMemoryManager.MemoryChunk;

public class VkTexture implements IVkResource {
    private final VKContext ctxt;
    private String tag;
    
    int width;
    int height;
    int mipLevels;
    public long image = VK_NULL_HANDLE;
    public int imageLayout;
    private long view;
    private int layers;

    public VkTexture(VKContext ctxt) {
        this.ctxt = ctxt;
        this.ctxt.addResource(this);
    }
    public void build(int vkFormat, TextureBinMips... texture2dData) {
        try ( MemoryStack stack = stackPush() ) {
            
        VkFormatProperties formatProperties = VkFormatProperties.callocStack(stack);
        vkGetPhysicalDeviceFormatProperties(this.ctxt.getPhysicalDevice(), vkFormat, formatProperties);
        boolean useStaging = true;
        boolean forceLinearTiling = false;
        this.width = texture2dData[0].w[0];
        this.height = texture2dData[0].h[0];
        this.mipLevels = texture2dData[0].mips;
        int totalSize = 0;
        int arrLen = texture2dData.length;
        int totalLayers = 0;
        for (int i = 0; i < arrLen; i++) {
            if (texture2dData[i] != null) {
                totalLayers++;
                totalSize += texture2dData[i].totalSize;
                if (texture2dData[i].w[0] != this.width || texture2dData[i].h[0] != this.height) {
                    throw new GameLogicError("Invalid texture size, array textures are expected to have equal dimensions on all layers");
                }
            }
        }
        this.layers = totalLayers;
        
        ByteBuffer dataDirect = ByteBuffer.allocateDirect(totalSize).order(ByteOrder.nativeOrder());
        dataDirect.clear();
        for (int i = 0; i < arrLen; i++) {
            if (texture2dData[i] != null) {
                dataDirect.put(texture2dData[i].data);
            }
        }
        dataDirect.flip();
//      for (int a = 0; a < texture2dData.data.length; a++) {
//          System.out.printf("0x%02X, ",texture2dData.data[a]);
//          if (a%16==0)
//              System.out.println();
//          if (a > 30)
//              break;
//      }
        // Only use linear tiling if forced
        if (forceLinearTiling)
        {
            // Don't use linear if format is not supported for (linear) shader sampling
            useStaging = (formatProperties.linearTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT) == 0;
        }


            if (useStaging)
            {
                LongBuffer stagingBuffer = stack.longs(0);
                LongBuffer pImage = stack.longs(0);
                VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                        .usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
                        .size(totalSize);
                bufferCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
                int err = vkCreateBuffer(this.ctxt.device, bufferCreateInfo, null, stagingBuffer);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateBuffer failed: " + VulkanErr.toString(err));
                }

                MemoryChunk memChunk = this.ctxt.memoryManager.allocateBufferMemory(stagingBuffer.get(0), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
                long ptr = memChunk.map();
                System.out.println("copy from "+memAddress(dataDirect)+" to "+ptr+", size="+totalSize);
                memCopy(memAddress(dataDirect), ptr, totalSize);
                memChunk.unmap();
                
                int offset = 0;
                System.out.println("ALLOC "+(this.mipLevels*this.layers));
                VkBufferImageCopy.Buffer bufferCopyRegions = VkBufferImageCopy.calloc(this.mipLevels*this.layers);
                for (int l = 0; l < this.layers; l++)
                {
                    for (int i = 0; i < this.mipLevels; i++)
                    {
                        VkBufferImageCopy bufferCopyRegion = bufferCopyRegions.get(l*this.mipLevels+i);
                        VkImageSubresourceLayers bufferCopyRegionSubresource = bufferCopyRegion.imageSubresource();
                        bufferCopyRegionSubresource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                        bufferCopyRegionSubresource.mipLevel(i);
                        bufferCopyRegionSubresource.baseArrayLayer(l);
                        bufferCopyRegionSubresource.layerCount(1);
                        VkExtent3D imageExtend = bufferCopyRegion.imageExtent();
                        imageExtend.width(texture2dData[l].w[i]).height(texture2dData[l].h[i]).depth(1);
                        bufferCopyRegion.bufferOffset(offset);
                        offset += texture2dData[l].sizes[i];
                    }
                }
    
                // Create optimal tiled target image
                VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                        .imageType(VK_IMAGE_TYPE_2D)
                        .format(vkFormat)
                        .mipLevels(this.mipLevels)
                        .arrayLayers(this.layers)
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .tiling(VK_IMAGE_TILING_OPTIMAL)
                        .usage(VK_IMAGE_USAGE_SAMPLED_BIT)
                        .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                        // Set initial layout of the image to undefined;
                        .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                        .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
                imageCreateInfo.extent().width(this.width).height(this.height).depth(1);
                err = vkCreateImage(this.ctxt.device, imageCreateInfo, null, pImage);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateImage failed: " + VulkanErr.toString(err));
                }
                this.ctxt.memoryManager.allocateImageMemory(pImage.get(0), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, VkConstants.TEXTURE_COLOR_MEMORY);
                VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                        .commandPool(this.ctxt.copyCommandPool)
                        .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                        .commandBufferCount(1);
                PointerBuffer pCommandBuffer = stack.pointers(0);
                err = vkAllocateCommandBuffers(this.ctxt.device, cmdBufAllocateInfo, pCommandBuffer);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("Failed to allocate command buffer: " + VulkanErr.toString(err));
                }
                VkCommandBuffer copyCmd = new VkCommandBuffer(pCommandBuffer.get(0), this.ctxt.device);
                // Create the command buffer begin structure
                VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                        .pNext(NULL);
                vkBeginCommandBuffer(copyCmd, cmdBufInfo);
    
                // Image barrier for optimal image
                
                // The sub resource range describes the regions of the image we will be transition
                VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.callocStack(stack)
                        // Image only contains color data
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        // Start at first mip level
                        .baseMipLevel(0)
                        .baseArrayLayer(0)
                        // We will transition on all mip levels
                        .levelCount(this.mipLevels)
                        .layerCount(this.layers);
                // Optimal image will be used as destination for the copy, so we must transfer from our
                // initial undefined image layout to the transfer destination layout
                setImageLayout(copyCmd, 
                        pImage.get(0), 
                        VK_IMAGE_ASPECT_COLOR_BIT, 
                        VK_IMAGE_LAYOUT_UNDEFINED, 
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 
                        subresourceRange);
                // Clean up staging resources
    
                // Copy mip levels from staging buffer
                vkCmdCopyBufferToImage(
                    copyCmd,
                    stagingBuffer.get(0),
                    pImage.get(0), 
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    bufferCopyRegions);
    
                // Change texture image layout to shader read after all mip levels have been copied
                setImageLayout(
                    copyCmd,
                    pImage.get(0),
                    VK_IMAGE_ASPECT_COLOR_BIT,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    subresourceRange);
                this.image = pImage.get(0);
                this.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
                vkEndCommandBuffer(copyCmd);
                VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                
                PointerBuffer pCommandBuffers = stack.pointers(copyCmd);
                submitInfo.pCommandBuffers(pCommandBuffers);
                err = vkQueueSubmit(this.ctxt.vkQueue, submitInfo, VK_NULL_HANDLE);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkQueueSubmit failed: " + VulkanErr.toString(err));
                }
                err = vkQueueWaitIdle(this.ctxt.vkQueue);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkQueueWaitIdle failed: " + VulkanErr.toString(err));
                }
                vkFreeCommandBuffers(this.ctxt.device, this.ctxt.copyCommandPool, pCommandBuffers);
                System.out.println("release staging buffer memory");
                this.ctxt.memoryManager.releaseBufferMemory(stagingBuffer.get(0));
                vkDestroyBuffer(this.ctxt.device, stagingBuffer.get(0), null);
                bufferCopyRegions.free();
            }
        }
    
    }

    @Override
    public void destroy() {
        this.ctxt.removeResource(this);
        if (this.image != VK_NULL_HANDLE) {
            vkDestroyImage(this.ctxt.device, this.image, null);
            this.ctxt.memoryManager.releaseImageMemory(this.image);
            this.image = VK_NULL_HANDLE;
        }
    }

    private void setImageLayout(VkCommandBuffer cmdBuffer, long l, int aspectMask,
            int oldLayout, int newLayout, VkImageSubresourceRange subresourceRange) {
        VkImageMemoryBarrier.Buffer imageMemoryBarrier = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .image(l).subresourceRange(subresourceRange);

        // Only sets masks for layouts used in this example
        // For a more complete version that can be used with other layouts see vkTools::setImageLayout

        // Source layouts (old)
        switch (oldLayout)
        {
        case VK_IMAGE_LAYOUT_UNDEFINED:
            // Only valid as initial layout, memory contents are not preserved
            // Can be accessed directly, no source dependency required
            imageMemoryBarrier.srcAccessMask(0);
            break;
        case VK_IMAGE_LAYOUT_PREINITIALIZED:
            // Only valid as initial layout for linear images, preserves memory contents
            // Make sure host writes to the image have been finished
            imageMemoryBarrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT);
            break;
        case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:
            // Old layout is transfer destination
            // Make sure any writes to the image have been finished
            imageMemoryBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            break;
        }

        // Target layouts (new)
        switch (newLayout)
        {
        case VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL:
            // Transfer source (copy, blit)
            // Make sure any reads from the image have been finished
            imageMemoryBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
            break;
        case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:
            // Transfer destination (copy, blit)
            // Make sure any writes to the image have been finished
            imageMemoryBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            break;
        case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL:
            // Shader read (sampler, input attachment)
            imageMemoryBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            break;
        }
        // Put barrier on top of pipeline
        int srcStageFlags = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        int destStageFlags = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;

        // Put barrier inside setup command buffer
        vkCmdPipelineBarrier(
            cmdBuffer,
            srcStageFlags, 
            destStageFlags, 
            0, 
            null,
            null,
            imageMemoryBarrier);
    }
    
    public VkTexture tag(String string) {
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
    public int getNumMips() {
        return this.mipLevels;
    }
    public int getNumLayers() {
        return this.layers;
    }
}
