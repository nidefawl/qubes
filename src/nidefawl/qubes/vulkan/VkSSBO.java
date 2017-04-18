package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

import java.nio.ByteBuffer;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.IBufferDynamicOffset;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.util.GameMath;

public class VkSSBO implements IBufferDynamicOffset {
    public final VkBuffer buffer;
    public final int      size;
    public final int      dynSize;
    private int           bufferPos;
    private int           bufferSize;
    private int           bufferOffset = 0;
    private String tag;
    private int marker;
    
    public int getBufferPos() {
        return this.bufferPos;
    }

    public VkBuffer getBuffer() {
        return this.buffer;
    }

    public VkSSBO(VKContext ctxt, int size, int dynSize) {
        this.dynSize = dynSize;
        this.size = size;
        this.buffer = new VkBuffer(ctxt);
    }
    public VkSSBO init() {
        this.buffer.create(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, this.size, false);
        VkDescriptorBufferInfo.Buffer desc = this.buffer.getDescriptorBuffer();
        if (isConstant()) {
            desc.range(VK_WHOLE_SIZE);
        } else {
            desc.range(this.dynSize);
        }
        if (dynSize > Engine.vkContext.limits.maxStorageBufferRange()) {
            throw new GameLogicError("Uniform buffer len exceeds maxStorageBufferRange: "+(dynSize)+" > "+(Engine.vkContext.limits.maxStorageBufferRange()));
        }
        return this;
    }
    public VkSSBO tag(String tag) {
        this.buffer.tag(tag);
        this.tag = tag;
        return this;
    }

    public void uploadData(ByteBuffer buf) {
        this.bufferSize = buf.remaining();
        if (this.bufferSize == 0) {
            return;
        }
        long alignedV = Math.max(2048, GameMath.nextPowerOf2(this.dynSize));
        //            System.out.println(BUFFER_SIZE+","+this.bufferOffset+","+this.bufferSize+"  - "+(BUFFER_SIZE-this.bufferOffset)+" < "+this.bufferSize+" = "+(BUFFER_SIZE-this.bufferPos < this.bufferSize));
        if (this.size - this.bufferOffset < alignedV) {
            this.bufferOffset = 0;
//            System.out.println(this.tag+ " wrap around "+this.size+","+alignedV);

        }
        this.bufferPos = this.bufferOffset;
        this.buffer.upload(buf, this.bufferPos);
        this.bufferOffset += alignedV;
        if (Engine.debugflag)
        System.out.println(this.tag+ " Uploaded " + this.bufferSize + " bytes at pos " + this.bufferPos + ", next " + this.bufferOffset);
    }

    public void downloadData(ByteBuffer buf, int offset, int size) {
        this.buffer.download(buf, offset, size);
    }
    public void setPosFromOffset() {
        this.bufferPos = this.bufferOffset;
    }

    public boolean isConstant() {
        return this.dynSize == size;
    }

    public VkDescriptorBufferInfo.Buffer getDescriptorBuffer() {
        return this.buffer.getDescriptorBuffer();
    }

    @Override
    public int getDynamicOffset() {
//        System.out.println(this.tag+ " getoffset "+this.bufferPos);
        return this.bufferPos;
    }

    public void markPos() {
        this.marker = this.bufferOffset;
    }
    /**
     * <b>Caller makes sure buffer size is multiple of framesize!</b>
     * 
     * @param frameSize
     */
    public void seekPos(int frameSize) {
        int nextPos = this.marker+frameSize;
        this.bufferOffset = nextPos;
        if (this.bufferOffset >= this.size) {
            this.bufferOffset = 0;
        }
    }

}