package nidefawl.qubes.vulkan;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;

public class CommandBuffer extends VkCommandBuffer {

    public boolean inUse;
    public int frameIdx;
    PostRenderTask[] tasks = new PostRenderTask[256];
    int nTasks = 0;

    public CommandBuffer(long handle, VkDevice device, int i) {
        super(handle, device);
        this.frameIdx = i;
    }

    public void addPostRenderTask(PostRenderTask postRenderTask) {
        tasks[nTasks++] = postRenderTask;
    }

    public void completeTasks() {
        for (int i = 0; i < nTasks; i++) {
            tasks[i].onComplete();
        }
        nTasks = 0;
    }


}
