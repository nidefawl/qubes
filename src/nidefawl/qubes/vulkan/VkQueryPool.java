package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.util.Arrays;

public class VkQueryPool {

    private long pool;
    private int size;
    int pos = 0;
    public boolean[] queried;
    public boolean inUse;

    public VkQueryPool(long l, int i) {
        this.pool = l;
        this.size = i;
        this.queried = new boolean[i];
    }

    public void reset(CommandBuffer buffer) {
//        System.err.println("reset "+this.pool);
        vkCmdResetQueryPool(buffer, this.pool, 0, this.size);
        Arrays.fill(this.queried, false);
        this.pos = 0;
    }

    public int getQueries(int i) {
        int j = pos;
        pos += i;
        return j;
    }
    
    public int getPos() {
        return this.pos;
    }

    public long get() {
        return this.pool;
    }
    
    public void query(CommandBuffer commandBuffer, int vkPipeStage, int idx) {
        this.queried[idx] = true;
        vkCmdWriteTimestamp(commandBuffer, vkPipeStage, this.pool, idx);

    }

}
