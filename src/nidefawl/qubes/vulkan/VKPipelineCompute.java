package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.util.GameLogicError;

public class VKPipelineCompute extends VkPipeline {

    private VkShader              shader;
    public IDerivedComputePipeDef derivedPipeDef;
    
    public VKPipelineCompute(VkPipelineLayout pipelineLayout) {
        super(pipelineLayout);
    }

    public void destroyPipeLine(VKContext vkContext) {
        if (this.shader != null) {
            this.shader.destroy();
            this.shader = null;
        }
        super.destroyPipeLine(vkContext);
    }
    public void destroy(VKContext vkContext) {
        super.destroy(vkContext);
    }

    public void setShader(VkShader shader) {
        this.shader = shader;
    }
    public long buildPipeline(VKContext vkContext) {
        if (shader == null) {
            throw new GameLogicError("MISSING SHADERS");
        }
        try ( MemoryStack stack = stackPush() ) {
            VkPipelineShaderStageCreateInfo shaderStageCreateInfo = VkPipelineShaderStageCreateInfo.callocStack(stack);

            if (this.shader.getShaderModule() == VK_NULL_HANDLE) {
                throw new GameLogicError("NULL SHADER MODULE");
            }
            shaderStageCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            shaderStageCreateInfo.stage(this.shader.getStage());
            shaderStageCreateInfo.module(this.shader.getShaderModule());
            shaderStageCreateInfo.pName(mainMethod);
            int nPipelines = 1;
            if (this.allPipes == null&&this.derivedPipeDef != null) {
                nPipelines += this.derivedPipeDef.getNumDerived();
            }
            VkComputePipelineCreateInfo.Buffer pipelineCreateInfoBuffer = pipelineCreateInfo(nPipelines, layout.pipelineLayout, VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT);
            VkComputePipelineCreateInfo mainPipe = pipelineCreateInfoBuffer.get(0);

            mainPipe.stage(shaderStageCreateInfo);

            if (this.allPipes == null&&this.derivedPipeDef != null) {
                for (int i = 1; i < nPipelines; i++) {
                    System.out.println(pipelineCreateInfoBuffer+"/"+nPipelines+"/"+i);
                    pipelineCreateInfoBuffer.put(i, mainPipe);
                    VkComputePipelineCreateInfo subPipeline = pipelineCreateInfoBuffer.get(i);
                    subPipeline.flags(VK_PIPELINE_CREATE_DERIVATIVE_BIT);
                    subPipeline.basePipelineIndex(0);
                    this.derivedPipeDef.setPipeDef(i, subPipeline);
                }
            }
            LongBuffer pPipelines = stack.callocLong(nPipelines);
            int err = vkCreateComputePipelines(vkContext.device, VK_NULL_HANDLE, pipelineCreateInfoBuffer, null, pPipelines);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateGraphicsPipelines failed: " + VulkanErr.toString(err));
            }
            pipelineCreateInfoBuffer.free();
            if (this.allPipes == null) {
                this.allPipes = new long[pPipelines.remaining()];
                pPipelines.get(this.allPipes);
                return this.allPipes[0];
            }
            return pPipelines.get(0);
        }
    }



    public static VkComputePipelineCreateInfo.Buffer pipelineCreateInfo(int num, long layout, int flags) {
        VkComputePipelineCreateInfo.Buffer graphicsPipelineCreateInfo = VkComputePipelineCreateInfo.calloc(num);
        graphicsPipelineCreateInfo.sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO);
        graphicsPipelineCreateInfo.layout(layout);
        graphicsPipelineCreateInfo.flags(flags);
        graphicsPipelineCreateInfo.basePipelineIndex(-1);
        graphicsPipelineCreateInfo.basePipelineHandle(VK_NULL_HANDLE);
        return graphicsPipelineCreateInfo;
    }

    public long getLayoutHandle() {
        return this.layout.pipelineLayout;
    }


    public long getDerived(int subpipe) {
        return this.allPipes[subpipe];
    }
}
