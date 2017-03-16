package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.ReallocIntBuffer;

public class BufferPair {
    public VkBuffer vert;
    public VkBuffer idx;
    public boolean[] inUseBy = new boolean[4];
    public int elementCount;
    public BufferPair() {
        this.vert = new VkBuffer(Engine.vkContext).tag("region_vertex");
        this.idx = new VkBuffer(Engine.vkContext).tag("region_index");
    }
    public void destroy() {
        this.vert.destroy();
        this.idx.destroy();
    }
    public boolean isFree() {
        for (int i = 0 ; i < inUseBy.length; i++) {
            if (inUseBy[i]) {
                return false;
            }
        }
        return true;
    }
    public void uploadStreaming(ByteBuffer bufVert, long sizeVert, ByteBuffer bufIdx, long sizeIdx) {
        this.vert.create(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, sizeVert*4L, false);
        this.idx.create(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, sizeIdx*4L, false);
        this.vert.upload(bufVert, 0);
        this.idx.upload(bufIdx, 0);
        this.elementCount = (int) sizeIdx;
    }
    public void uploadDeviceLocal(ByteBuffer bufVert, long sizeVert, ByteBuffer bufIdx, long sizeIdx) {
        this.vert.create(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, sizeVert*4L, true);
        this.idx.create(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, sizeIdx*4L, true);
        this.vert.upload(bufVert, 0);
        this.idx.upload(bufIdx, 0);
        this.elementCount = (int) sizeIdx;
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
            inUseBy[commandBuffer.frameIdx] = true;
        } else {
            System.err.println("attempt to draw empty buffer");
        }
    }
}