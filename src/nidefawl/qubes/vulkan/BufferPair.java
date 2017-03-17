package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.ReallocIntBuffer;

public class BufferPair implements RefTrackedResource {
    public VkBuffer vert;
    public VkBuffer idx;
    public boolean[] inUseBy = new boolean[VulkanInit.MAX_NUM_SWAPCHAIN];
    public int elementCount;
    public BufferPair(VKContext ctxt) {
        this.vert = new VkBuffer(ctxt);
        this.idx = new VkBuffer(ctxt);
    }
    public BufferPair tag(String s) {
        this.vert.tag(s+"_vertex");
        this.idx.tag(s+"_idx");
        return this;
    }
    public void destroy() {
        this.vert.destroy();
        this.idx.destroy();
    }
    public void uploadStreaming(ByteBuffer bufVert, long sizeVert, ByteBuffer bufIdx, long sizeIdx) {
        create(sizeVert*4L, sizeIdx*4L, false);
        upload(0, bufVert, 0, bufIdx);
    }
    public void uploadDeviceLocal(ByteBuffer bufVert, long sizeVert, ByteBuffer bufIdx, long sizeIdx) {
        create(sizeVert*4L, sizeIdx*4L, true);
        upload(0, bufVert, 0, bufIdx);
    }
    public void upload(int offsetvert, ByteBuffer bufVert, int offsetIdx, ByteBuffer bufIdx) {
        this.vert.upload(bufVert, offsetvert);
        this.idx.upload(bufIdx, offsetIdx);
    }
    public void setElementCount(int l) {
        this.elementCount = l;
    }
    static long[] pointer = new long[1];
    static long[] offset = new long[1];
    public void draw(CommandBuffer commandBuffer) {
        if (this.elementCount > 0) {
            offset[0] = 0;
            pointer[0] = vert.getBuffer();
            vkCmdBindVertexBuffers(commandBuffer, 0, pointer, offset);
            vkCmdBindIndexBuffer(commandBuffer, this.idx.getBuffer(), 0, VK_INDEX_TYPE_UINT32);
            vkCmdDrawIndexed(commandBuffer, this.elementCount, 1, 0, 0, 0);
            flagUse(commandBuffer.frameIdx);
        } else {
            System.err.println("attempt to draw empty buffer");
        }
    }
    public void create(long sizeVert, long sizeIdx, boolean deviceLocal) {
        this.vert.create(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, sizeVert, deviceLocal);
        this.idx.create(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, sizeIdx, deviceLocal);
    }
    @Override
    public void flagUse(int idx) {
        this.inUseBy[idx] = true;
    }
    @Override
    public void unflagUse(int idx) {
        this.inUseBy[idx] = false;
    }
    @Override
    public boolean isFree() {
        for (int i = 0 ; i < inUseBy.length; i++) {
            if (inUseBy[i]) {
                return false;
            }
        }
        return true;
    }
}