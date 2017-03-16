package nidefawl.qubes.vulkan;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;

public class CommandBuffer extends VkCommandBuffer {

    public boolean inUse;
    public int frameIdx;

    public CommandBuffer(long handle, VkDevice device, int i) {
        super(handle, device);
        this.frameIdx = i;
    }


}
