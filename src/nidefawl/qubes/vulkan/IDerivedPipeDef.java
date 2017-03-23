package nidefawl.qubes.vulkan;

import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;

public interface IDerivedPipeDef {

    int getNumDerived();

    void setPipeDef(int i, VkGraphicsPipelineCreateInfo subPipeline);

}
