package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

public abstract class VkPipeline {
    public VkPipelineLayout                    layout               = null;
    public long                                pipeline             = VK_NULL_HANDLE;
    protected long[] allPipes;
    ByteBuffer                                 mainMethod            = MemoryUtil.memUTF8("main");

    public VkPipeline(VkPipelineLayout pipelineLayoutTextured) {
        this.layout = pipelineLayoutTextured;
        VkPipelines.registerPipe(this);
    }
    

    public void destroyPipeLine(VKContext vkContext) {
        if (this.allPipes != null) {
            for (int i = 0; i < this.allPipes.length; i++) {
                vkDestroyPipeline(vkContext.device, this.allPipes[i], null);
            }
            this.allPipes = null;
            pipeline = VK_NULL_HANDLE;
        }   
    }
    public void destroy(VKContext vkContext) {
        destroyPipeLine(vkContext);
        MemoryUtil.memFree(mainMethod);
    }

    public void setPipelineLayout(VkPipelineLayout layout) {
        this.layout = layout;
    }

    public long getLayoutHandle() {
        return this.layout.pipelineLayout;
    }
    public long getDerived(int subpipe) {
        return this.allPipes[subpipe];
    }

    public abstract long buildPipeline(VKContext vkContext);
}
