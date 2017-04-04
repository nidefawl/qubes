package nidefawl.qubes.vulkan;

import org.lwjgl.vulkan.VkComputePipelineCreateInfo;

public interface IDerivedComputePipeDef {

    int getNumDerived();

    void setPipeDef(int i, VkComputePipelineCreateInfo subPipeline);

}
