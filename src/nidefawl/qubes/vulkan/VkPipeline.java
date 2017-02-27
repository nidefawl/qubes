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

public class VkPipeline {

    VkPipelineInputAssemblyStateCreateInfo inputAssemblyState = pipelineInputAssemblyStateCreateInfo(
                VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                0,
                false);
    VkPipelineRasterizationStateCreateInfo rasterizationState = pipelineRasterizationStateCreateInfo(
                VK_POLYGON_MODE_FILL,
                VK_CULL_MODE_NONE,
                VK_FRONT_FACE_COUNTER_CLOCKWISE,
                0);
    
    VkPipelineColorBlendAttachmentState.Buffer blendAttachmentState = pipelineColorBlendAttachmentState(
                0xf,
                false);
    
    VkPipelineColorBlendStateCreateInfo colorBlendState = pipelineColorBlendStateCreateInfo(
                1, 
                blendAttachmentState);
    VkPipelineMultisampleStateCreateInfo multisampleState = pipelineMultisampleStateCreateInfo(
                VK_SAMPLE_COUNT_1_BIT,
                0);
    
    VkPipelineDepthStencilStateCreateInfo depthStencilState = pipelineDepthStencilStateCreateInfo(
                true,
                true,
                VK_COMPARE_OP_LESS_OR_EQUAL);
    VkViewport.Buffer viewport = VkViewport.calloc(1)
            .height(0)
            .width(0)
            .minDepth(0)
            .maxDepth(1);
    VkRect2D.Buffer scissors = VkRect2D.calloc(1);
    VkPipelineViewportStateCreateInfo viewportState = pipelineViewportStateCreateInfo(1, 1, 0)
            .pViewports(viewport)
            .pScissors(scissors);
    VkPipelineVertexInputStateCreateInfo vertexInputState = pipelineVertexInputStateCreateInfo();


    VkVertexInputBindingDescription.Buffer bindingDescriptions = VkVertexInputBindingDescription.calloc(1);

    ByteBuffer                                 mainMethod            = MemoryUtil.memUTF8("main");
    
    public VkPipelineLayout                    layout                = null;
    private VkShader[]                         shaders;
    private long                               renderpass            = VK_NULL_HANDLE;
    private int                                subpass               = 0;
    public long                                       pipeline              = VK_NULL_HANDLE;

    public void setPipelineLayout(VkPipelineLayout layout) {
        this.layout = layout;
    }
    
    public VkPipeline(VkPipelineLayout pipelineLayoutTextured) {
        this.layout = pipelineLayoutTextured;
        viewportState.pViewports(viewport);
        viewportState.pScissors(scissors);
    }

    public void destroyPipeLine(VKContext vkContext) {
        if (this.shaders != null) {
            for (int i = 0; i < this.shaders.length; i++) {
                this.shaders[i].destroy();
            }
            this.shaders = null;
        }
        if (pipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(vkContext.device, pipeline, null);
            pipeline = VK_NULL_HANDLE;
        }
    }

    public void setShaders(VkShader ...shadersArr) {
        this.shaders = shadersArr;
    }
    public void setVertexDesc(VkVertexDescriptors desc) {
        bindingDescriptions.get(0)
                .binding(0)
                .stride(desc.stride)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        vertexInputState.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(bindingDescriptions)
                .pVertexAttributeDescriptions(desc.attributeDescriptions);
    }
    public void setRenderPass(long renderpass, int subpass) {
        this.renderpass = renderpass;
        this.subpass = subpass;
    }
    public long buildPipeline(VKContext vkContext) {
        if (shaders == null) {
            throw new GameLogicError("MISSING SHADERS");
        }
        try ( MemoryStack stack = stackPush() ) {
            VkPipelineShaderStageCreateInfo.Buffer shaderStageCreateInfo = VkPipelineShaderStageCreateInfo.callocStack(this.shaders.length, stack);

            for (int i = 0; i < this.shaders.length; i++) {
                if (this.shaders[i].getShaderModule() == VK_NULL_HANDLE) {
                    throw new GameLogicError("NULL SHADER MODULE");
                }
                VkPipelineShaderStageCreateInfo shaderstagecreateinfo = shaderStageCreateInfo.get(i);
                shaderstagecreateinfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                shaderstagecreateinfo.stage(this.shaders[i].getStage());
                shaderstagecreateinfo.module(this.shaders[i].getShaderModule());
                shaderstagecreateinfo.pName(mainMethod);
            }
            int nPipelines = 1;
            VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfoBuffer = pipelineCreateInfo(nPipelines, layout.pipelineLayout, renderpass, VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT);
            VkGraphicsPipelineCreateInfo mainPipe = pipelineCreateInfoBuffer.get(0);

            mainPipe.subpass(this.subpass);
            mainPipe.pVertexInputState(vertexInputState);
            
            mainPipe.pInputAssemblyState(inputAssemblyState);
            mainPipe.pRasterizationState(rasterizationState);
            mainPipe.pColorBlendState(colorBlendState);
            mainPipe.pMultisampleState(multisampleState);
            mainPipe.pViewportState(viewportState);
            mainPipe.pDepthStencilState(depthStencilState);
//          mainPipe.pDynamicState(dynamicState);
            mainPipe.pStages(shaderStageCreateInfo);
            for (int i = 1; i < nPipelines; i++) {
                pipelineCreateInfoBuffer.put(i, mainPipe);
                VkGraphicsPipelineCreateInfo subPipeline = pipelineCreateInfoBuffer.get(i);
                subPipeline.flags(VK_PIPELINE_CREATE_DERIVATIVE_BIT);
                subPipeline.basePipelineIndex(0);
            }
            LongBuffer pPipelines = stack.callocLong(nPipelines);
            int err = vkCreateGraphicsPipelines(vkContext.device, VK_NULL_HANDLE, pipelineCreateInfoBuffer, null, pPipelines);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateGraphicsPipelines failed: " + VulkanErr.toString(err));
            }
            pipelineCreateInfoBuffer.free();
            return pPipelines.get(0);
        }
    }


    public static VkPipelineVertexInputStateCreateInfo pipelineVertexInputStateCreateInfo()
    {
        VkPipelineVertexInputStateCreateInfo pipelineVertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc();
        pipelineVertexInputStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
        return pipelineVertexInputStateCreateInfo;
    }

    public static VkPipelineInputAssemblyStateCreateInfo pipelineInputAssemblyStateCreateInfo(
        int topology,
        int flags,
        boolean primitiveRestartEnable)
    {
        VkPipelineInputAssemblyStateCreateInfo pipelineInputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc();
        pipelineInputAssemblyStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
        pipelineInputAssemblyStateCreateInfo.topology ( topology);
        pipelineInputAssemblyStateCreateInfo.flags(flags);
        pipelineInputAssemblyStateCreateInfo.primitiveRestartEnable(primitiveRestartEnable);
        return pipelineInputAssemblyStateCreateInfo;
    }

    public static VkPipelineRasterizationStateCreateInfo pipelineRasterizationStateCreateInfo(
            int polygonMode,
        int cullMode,
        int frontFace,
        int flags)
    {
        VkPipelineRasterizationStateCreateInfo pipelineRasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc();
        pipelineRasterizationStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
        pipelineRasterizationStateCreateInfo.polygonMode(polygonMode);
        pipelineRasterizationStateCreateInfo.cullMode(cullMode);
        pipelineRasterizationStateCreateInfo.frontFace(frontFace);
        pipelineRasterizationStateCreateInfo.flags(flags);
        pipelineRasterizationStateCreateInfo.depthClampEnable(false);
        pipelineRasterizationStateCreateInfo.lineWidth(1.0f);
        return pipelineRasterizationStateCreateInfo;
    }

    public static VkPipelineColorBlendAttachmentState.Buffer pipelineColorBlendAttachmentState(
        int colorWriteMask,
        boolean blendEnable)
    {
        VkPipelineColorBlendAttachmentState.Buffer pipelineColorBlendAttachmentState = VkPipelineColorBlendAttachmentState.calloc(1);
        pipelineColorBlendAttachmentState.colorWriteMask(colorWriteMask);
        pipelineColorBlendAttachmentState.blendEnable(blendEnable);
        return pipelineColorBlendAttachmentState;
    }

    public static VkPipelineColorBlendStateCreateInfo pipelineColorBlendStateCreateInfo(
            int attachmentCount,
            VkPipelineColorBlendAttachmentState.Buffer pAttachments)
    {
        VkPipelineColorBlendStateCreateInfo pipelineColorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc();
        pipelineColorBlendStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
        pipelineColorBlendStateCreateInfo.pAttachments(pAttachments);
        return pipelineColorBlendStateCreateInfo;
    }

    public static VkPipelineDepthStencilStateCreateInfo pipelineDepthStencilStateCreateInfo(
        boolean depthTestEnable,
        boolean depthWriteEnable,
        int depthCompareOp)
    {
        VkPipelineDepthStencilStateCreateInfo pipelineDepthStencilStateCreateInfo = VkPipelineDepthStencilStateCreateInfo.calloc();
        pipelineDepthStencilStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
        pipelineDepthStencilStateCreateInfo.depthTestEnable(depthTestEnable);
        pipelineDepthStencilStateCreateInfo.depthWriteEnable(depthWriteEnable);
        pipelineDepthStencilStateCreateInfo.depthCompareOp(depthCompareOp);
        pipelineDepthStencilStateCreateInfo.back()
            .failOp(VK_STENCIL_OP_KEEP)
            .passOp(VK_STENCIL_OP_KEEP)
            .compareOp(VK_COMPARE_OP_ALWAYS);
        pipelineDepthStencilStateCreateInfo.front(pipelineDepthStencilStateCreateInfo.back());
        return pipelineDepthStencilStateCreateInfo;
    }

    public static VkPipelineViewportStateCreateInfo pipelineViewportStateCreateInfo(
            int viewportCount,
            int scissorCount,
            int flags)
    {
        VkPipelineViewportStateCreateInfo pipelineViewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc();
        pipelineViewportStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
        pipelineViewportStateCreateInfo.viewportCount(viewportCount);
        pipelineViewportStateCreateInfo.scissorCount(scissorCount);
        pipelineViewportStateCreateInfo.flags(flags);
        return pipelineViewportStateCreateInfo;
    }

    public static VkPipelineMultisampleStateCreateInfo pipelineMultisampleStateCreateInfo(
            int rasterizationSamples,
            int flags)
    {
        VkPipelineMultisampleStateCreateInfo pipelineMultisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc();
        pipelineMultisampleStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
        pipelineMultisampleStateCreateInfo.rasterizationSamples(rasterizationSamples);
        return pipelineMultisampleStateCreateInfo;
    }

    public static VkPipelineDynamicStateCreateInfo pipelineDynamicStateCreateInfo(
        IntBuffer pDynamicStates)
    {
        VkPipelineDynamicStateCreateInfo pipelineDynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.calloc();
        pipelineDynamicStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
        pipelineDynamicStateCreateInfo.pDynamicStates(pDynamicStates);
        return pipelineDynamicStateCreateInfo;
    }

    public static VkPipelineTessellationStateCreateInfo.Buffer pipelineTessellationStateCreateInfo(int patchControlPoints)
    {
        VkPipelineTessellationStateCreateInfo.Buffer pipelineTessellationStateCreateInfo = VkPipelineTessellationStateCreateInfo.calloc(1);
        pipelineTessellationStateCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO);
        pipelineTessellationStateCreateInfo.patchControlPoints(patchControlPoints);
        return pipelineTessellationStateCreateInfo;
    }

    public static VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfo(int num, long layout, long renderPass, int flags) {
        VkGraphicsPipelineCreateInfo.Buffer graphicsPipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(num);
        graphicsPipelineCreateInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
        graphicsPipelineCreateInfo.layout(layout);
        graphicsPipelineCreateInfo.renderPass(renderPass);
        graphicsPipelineCreateInfo.flags(flags);
        graphicsPipelineCreateInfo.basePipelineIndex(-1);
        graphicsPipelineCreateInfo.basePipelineHandle(VK_NULL_HANDLE);
        return graphicsPipelineCreateInfo;
    }

    public void destroy(VKContext vkContext) {
        destroyPipeLine(vkContext);
        MemoryUtil.memFree(mainMethod);
        bindingDescriptions.free();
        blendAttachmentState.free();
        colorBlendState.free();
        depthStencilState.free();
        inputAssemblyState.free();
        multisampleState.free();
        rasterizationState.free();
        scissors.free();
        vertexInputState.free();
        viewport.free();
        viewportState.free();
        
    }

    public long getLayoutHandle() {
        return this.layout.pipelineLayout;
    }

    public void setBlend(boolean b) {
        this.blendAttachmentState.blendEnable(b);
        if (b) {
            this.blendAttachmentState.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
            this.blendAttachmentState.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
            this.blendAttachmentState.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
            this.blendAttachmentState.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
        } else {

            this.blendAttachmentState.srcAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
            this.blendAttachmentState.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
            this.blendAttachmentState.srcColorBlendFactor(VK_BLEND_FACTOR_ZERO);
            this.blendAttachmentState.dstColorBlendFactor(VK_BLEND_FACTOR_ZERO);
        }
    }
}
