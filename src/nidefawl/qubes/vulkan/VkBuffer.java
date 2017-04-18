package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.*;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.util.UnsafeHelper;
import nidefawl.qubes.vulkan.VkMemoryManager.MemoryChunk;

public class VkBuffer implements IVkResource {
    private final VKContext ctxt;

    private LongBuffer pStagingBuffer;
    private LongBuffer pBuffer;
    private VkDescriptorBufferInfo descriptor;
    private VkDescriptorBufferInfo.Buffer descriptorBuffer;
    private MemoryChunk memory;
    private long size;
    private long ptr;
    private MemoryChunk memoryStaging;
    private boolean isDeviceLocal;
    private String tag;
    
    public VkBuffer(VKContext ctxt) {
        this.ctxt = ctxt;
    }

    public void create(int usageFlags, long size, boolean isDeviceLocal) {
        this.ctxt.addResource(this);
        this.size = size;
        this.isDeviceLocal = isDeviceLocal;
        if (this.tag == null) {
            Thread.dumpStack();
        }
        if (this.isDeviceLocal)
        {
            pStagingBuffer = memAllocLong(1);
            VkBufferCreateInfo bufferCreateInfo = VulkanInit.bufferCreateInfo(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, size);
            int err = vkCreateBuffer(ctxt.device, bufferCreateInfo, null, pStagingBuffer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateBuffer failed: " + VulkanErr.toString(err));
            }
            this.memoryStaging = ctxt.memoryManager.allocateBufferMemory(pStagingBuffer.get(0), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, this.tag+"_staging");
            bufferCreateInfo.free();
            usageFlags |= VK_BUFFER_USAGE_TRANSFER_DST_BIT;
        }
        
        {
            pBuffer = memAllocLong(1);
            VkBufferCreateInfo bufferCreateInfo = VulkanInit.bufferCreateInfo(usageFlags, size);
            int err = vkCreateBuffer(ctxt.device, bufferCreateInfo, null, pBuffer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateBuffer failed: " + VulkanErr.toString(err));
            }
            int memoryPropertyFlags = isDeviceLocal ? VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT : (VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
//            ctxt.memoryManager.wholeBlock = (usageFlags & VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT) != 0;
            this.memory = ctxt.memoryManager.allocateBufferMemory(pBuffer.get(0), memoryPropertyFlags, this.tag);
//            ctxt.memoryManager.wholeBlock  = false;
            bufferCreateInfo.free();
        }
        this.descriptorBuffer = VkDescriptorBufferInfo.calloc(1);
        this.descriptor = this.descriptorBuffer.get(0);
        this.descriptor.offset(0);
        this.descriptor.buffer(pBuffer.get(0));
        this.descriptor.range(VK_WHOLE_SIZE);
    }

    public void mapStaging() {
        this.ptr = memoryStaging.map();
    }
    

    public void unmapStaging() {
        memoryStaging.unmap();
    }

    public void unmap() {
        memory.unmap();
    }

    public long getMappedPtr() {
        return ptr;
    }
    public void upload(FloatBuffer floatBuffer, int offset) {
        _upload(memAddress(floatBuffer), floatBuffer.remaining()*4, offset);
    }
    public void upload(IntBuffer intBuffer, int offset) {
        _upload(memAddress(intBuffer), intBuffer.remaining()*4, offset);
    }
    public void upload(ByteBuffer byteBuffer, int offset) {
        _upload(memAddress(byteBuffer), byteBuffer.remaining(), offset);
    }
    public void download(ByteBuffer byteBuffer, int offset, int size) {
        if (isDeviceLocal) {
            throw new GameLogicError("Cannot download from device local memory");
        }
        mapRange(offset, size);
        memCopy(ptr, memAddress(byteBuffer), size);
        unmap();
    }

    public void mapRange(long offset, long size) {
        this.ptr = memory.mapRange(offset, size);
        if (Engine.debugflag&&this.tag != null && this.tag.contains("ssbo")) {
            System.out.println("mapped "+tag+" to "+this.ptr);
        }
    }
    private void _upload(long addr, int size, int offset) {
        if (isDeviceLocal) {
            mapStaging();
            memCopy(addr, ptr+offset, size);
        } else {
            mapRange(offset, size);
            long alignedLoc = ptr-offset;
//            Thread.dumpStack();
//            System.out.println(ptr-offset);
//            int alignRet = -1;
//            for (int i = 16; i >= 0; i--) {
//                if ((alignedLoc&((1<<i)-1))==0) {
//                    alignRet = 1<<i;
//                    break;
//                }
//            }
//            System.out.println("ALIGN REQ "+alignRet);
            memCopy(addr, ptr, size);
        }
        if (isDeviceLocal) {
            unmapStaging();
            copy();
        } else {
            unmap();
//            Thread.dumpStack();
        }
        Stats.uploadBytes+=size;
    }
    
    private void copy() {
        try ( MemoryStack stack = stackPush() ) {
            VkCommandBuffer commandBuffer = ctxt.getTransferCopyCommandBuffer();
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.srcOffset(0);
            copyRegion.dstOffset(0);
            copyRegion.size(this.size);
            vkCmdCopyBuffer(commandBuffer, this.pStagingBuffer.get(0), this.pBuffer.get(0), copyRegion);
        }


    }
    
    public void destroy() {
        ctxt.removeResource(this);
        if (pBuffer != null) {
            ctxt.memoryManager.releaseBufferMemory(pBuffer.get(0));
            vkDestroyBuffer(ctxt.device, pBuffer.get(0), null);
            memFree(pBuffer);
            if (isDeviceLocal && pStagingBuffer != null) {
                ctxt.memoryManager.releaseBufferMemory(pStagingBuffer.get(0));
                vkDestroyBuffer(ctxt.device, pStagingBuffer.get(0), null);
                memFree(pStagingBuffer);
            }
            descriptor.free();
            pStagingBuffer = null;
            pBuffer = null;
        }
    }

    public VkDescriptorBufferInfo.Buffer getDescriptorBuffer() {
        return descriptorBuffer;
    }
    public VkDescriptorBufferInfo getDescriptor() {
        return descriptor;
    }

    public LongBuffer getBufferP() {
        return pBuffer;
    }

    public long getBuffer() {
        return this.pBuffer.get(0);
    }

    public long getSize() {
        return this.size;
    }

    public boolean isDeviceLocal() {
        return this.isDeviceLocal;
    }

    public VkBuffer tag(String string) {
        this.tag = string;
        return this;
    }

    @Override
    public String toString() {
        return super.toString()+(this.tag!= null?" "+this.tag:"");
    }
    public String tag() {
        return this.tag;
    }

}
