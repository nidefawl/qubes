package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

public class VkPipeline {
    public VkPipelineLayoutCreateInfo pipelineLayoutCI = pipelineLayoutCreateInfo();

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

    VkPipelineShaderStageCreateInfo.Buffer shaderStageCreateInfo = VkPipelineShaderStageCreateInfo.calloc(5);

    VkVertexInputBindingDescription.Buffer bindingDescriptions = VkVertexInputBindingDescription.calloc(1);
    
    private long                               pipeline               = VK_NULL_HANDLE;
    private long                               newDescriptorSetLayout = VK_NULL_HANDLE;
    public long                                pipelineLayout         = VK_NULL_HANDLE;
    private long                               renderpass             = VK_NULL_HANDLE;

    private LongBuffer pipelineLayoutCIDescPtr;
    ByteBuffer mainMethod = MemoryUtil.memUTF8("main");


    public void setDescriptorSetLayout(long descriptorSetLayout) {
        this.newDescriptorSetLayout = descriptorSetLayout;
    }
    
    public VkPipeline() {
        viewportState.pViewports(viewport);
        viewportState.pScissors(scissors);
        pipelineLayoutCIDescPtr = memAllocLong(1);
        pipelineLayoutCI.pSetLayouts(pipelineLayoutCIDescPtr);
        pipelineLayoutCI.pSetLayouts().put(0, VK_NULL_HANDLE);
    }

    public void destroyPipeLine(VKContext vkContext) {
        if (pipeline != VK_NULL_HANDLE)
            vkDestroyPipeline(vkContext.device, pipeline, null);
        pipeline = VK_NULL_HANDLE;
        pipelineLayoutCIDescPtr.clear();
//        pipelineLayoutCI.pSetLayouts(pipelineLayoutCIDescPtr);
//        pipelineLayoutCI.pSetLayouts().put(0, VK_NULL_HANDLE);
    }


    public long getPtr() {
        return this.pipeline;
    }
    public void setShaders(VkShader ...shadersArr) {
        for (int i = 0; i < shadersArr.length; i++) {
            VkPipelineShaderStageCreateInfo shaderstagecreateinfo = shaderStageCreateInfo.get(i);
            shaderstagecreateinfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            shaderstagecreateinfo.stage(shadersArr[i].getStage());
            shaderstagecreateinfo.module(shadersArr[i].getShaderModule());
            shaderstagecreateinfo.pName(mainMethod);
        }
        shaderStageCreateInfo.limit(shadersArr.length);
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
    public void setRenderPass(long renderpass) {
        this.renderpass = renderpass;
    }
    public void buildPipeline(VKContext vkContext) {
        try ( MemoryStack stack = stackPush() ) {
            long prev = this.pipelineLayoutCI.pSetLayouts().get(0);
            if (prev != newDescriptorSetLayout) {
                System.out.println("newDescriptorSetLayout "+newDescriptorSetLayout+", prev "+prev);
                this.pipelineLayoutCI.pSetLayouts().put(0, newDescriptorSetLayout);
                if (this.pipelineLayout != VK_NULL_HANDLE) {
                    vkDestroyPipelineLayout(vkContext.device, this.pipelineLayout, null);
                }
                LongBuffer pPipelineLayout = stack.callocLong(1);
                int err = vkCreatePipelineLayout(vkContext.device, pipelineLayoutCI, null, pPipelineLayout);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreatePipelineLayout failed: " + VulkanErr.toString(err));
                }
                this.pipelineLayout = pPipelineLayout.get(0);
                System.out.println("pipeline layout "+pipelineLayout);
            }
            
            int nPipelines = 1;
            VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfoBuffer = pipelineCreateInfo(nPipelines, pipelineLayout, renderpass, VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT);
            VkGraphicsPipelineCreateInfo mainPipe = pipelineCreateInfoBuffer.get(0);
            
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
            this.pipeline = pPipelines.get(0);
            pipelineCreateInfoBuffer.free();
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

    public static VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo() {
        VkPipelineLayoutCreateInfo descriptorSetLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc();
        descriptorSetLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
        return descriptorSetLayoutCreateInfo;
    }
    public void destroy(VKContext vkContext) {
        if (pipeline != VK_NULL_HANDLE)
            vkDestroyPipeline(vkContext.device, pipeline, null);
        if (this.pipelineLayout != VK_NULL_HANDLE)
            vkDestroyPipelineLayout(vkContext.device, this.pipelineLayout, null);
        MemoryUtil.memFree(pipelineLayoutCIDescPtr);
        MemoryUtil.memFree(mainMethod);
        bindingDescriptions.free();
        blendAttachmentState.free();
        colorBlendState.free();
        depthStencilState.free();
        inputAssemblyState.free();
        multisampleState.free();
        pipelineLayoutCI.free();
        rasterizationState.free();
        scissors.free();
        shaderStageCreateInfo.free();
        vertexInputState.free();
        viewport.free();
        viewportState.free();
        
    }

}
