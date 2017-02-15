package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public abstract class AbstractVkTesselatorState {
    public int           vertexcount;

    public boolean       useColorPtr;
    public boolean       useTexturePtr;
    public boolean       useNormalPtr;
    public boolean       useUINTPtr;
    public int idxCount;
    
    public void copyTo(AbstractVkTesselatorState out) {
        out.vertexcount = this.vertexcount;
        out.useColorPtr = this.useColorPtr;
        out.useTexturePtr = this.useTexturePtr;
        out.useNormalPtr = this.useNormalPtr;
        out.useUINTPtr = this.useUINTPtr;
        out.idxCount = this.idxCount;
    }
    public abstract VkBuffer getVertexBuffer();
    public abstract VkBuffer getIndexBuffer();


    public int getIdx(int v) {
        return getVSize() * v;
    }

    public int getVSize() {
        int stride = 4;
        if (useColorPtr)
            stride++;
        if (useNormalPtr)
            stride++;
        if (useTexturePtr)
            stride+=2;
        if (useUINTPtr)
            stride+=2;
        return stride;
    }

    public int getSetting() {
        int s = 0;
        if (this.useNormalPtr)
            s|=1;
        if (this.useTexturePtr)
            s|=2;
        if (this.useColorPtr)
            s|=4;
        if (this.useUINTPtr)
            s|=8;
        return s;
    }
    long[] pointer = new long[1];
    long[] offset = new long[1];
    public void bindAndDraw(VkCommandBuffer commandBuffer, int bufferoffset) {
        pointer[0] = getVertexBuffer().getBuffer();
        offset[0] = bufferoffset;
        vkCmdBindVertexBuffers(commandBuffer, 0, pointer, offset);
        vkCmdBindIndexBuffer(commandBuffer, getIndexBuffer().getBuffer(), bufferoffset, VK_INDEX_TYPE_UINT32);
        vkCmdDrawIndexed(commandBuffer, this.idxCount, 1, 0, 0, 0);
    }
}
