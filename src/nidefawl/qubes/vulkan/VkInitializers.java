package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.*;

public final class VkInitializers {


    public static VkShaderModuleCreateInfo.Buffer shaderModuleCreateInfo() {
        VkShaderModuleCreateInfo.Buffer shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(1);
        shaderModuleCreateInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
        return shaderModuleCreateInfo;
    }

    public static VkPipelineShaderStageCreateInfo pipelineShaderStageCreateInfo() {
        VkPipelineShaderStageCreateInfo pipelineShaderStageCreateInfo = VkPipelineShaderStageCreateInfo.calloc();
        pipelineShaderStageCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        pipelineShaderStageCreateInfo.stage(0);
        pipelineShaderStageCreateInfo.module(0);
        return pipelineShaderStageCreateInfo;
    }
    public static VkDescriptorPoolSize descriptorPoolSize(int type, int descriptorCount) {
        VkDescriptorPoolSize descriptorPoolSize = VkDescriptorPoolSize.callocStack();
        descriptorPoolSize.type(type);
        descriptorPoolSize.descriptorCount(descriptorCount);
        return descriptorPoolSize;
    }
    public static VkDescriptorPoolCreateInfo descriptorPoolSize() {
        VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.callocStack();
        descriptorPoolCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
        return descriptorPoolCreateInfo;
    }
    public static VkDescriptorSetLayoutBinding descriptorSetLayoutBinding(
            int descriptorType,
            int stageFlags,
            int binding,
            int descriptorCount) {
        VkDescriptorSetLayoutBinding descriptorSetLayoutBinding = VkDescriptorSetLayoutBinding.callocStack();
        descriptorSetLayoutBinding.descriptorType(descriptorType);
        descriptorSetLayoutBinding.stageFlags(stageFlags);
        descriptorSetLayoutBinding.binding(binding);
        descriptorSetLayoutBinding.descriptorCount(descriptorCount);
        return descriptorSetLayoutBinding;
    }
    public static VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo(VkDescriptorSetLayoutBinding.Buffer pBindings) {
        VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.callocStack();
        descriptorSetLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
        descriptorSetLayoutCreateInfo.pBindings(pBindings);
        return descriptorSetLayoutCreateInfo;
    }

    public static VkDescriptorPoolCreateInfo descriptorPoolCreateInfo(VkDescriptorPoolSize.Buffer poolSizes, int maxSets) {
        VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.callocStack();
        descriptorPoolCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
        descriptorPoolCreateInfo.pPoolSizes(poolSizes);
        descriptorPoolCreateInfo.maxSets(maxSets);
        return descriptorPoolCreateInfo;
    }

    public static VkDescriptorSetAllocateInfo descriptorSetAllocateInfo(long descriptorPool, LongBuffer pDescriptorSetLayouts) {
        VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.callocStack();
        descriptorSetAllocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
        descriptorSetAllocateInfo.descriptorPool(descriptorPool);
        descriptorSetAllocateInfo.pSetLayouts(pDescriptorSetLayouts);
        return descriptorSetAllocateInfo;
    }

    public static void writeDescriptorSet(VkWriteDescriptorSet.Buffer writeDescriptorSetStructBuffer, int bufferIdx, 
            long dstSet,
            int descriptorType, int dstBinding, org.lwjgl.vulkan.VkDescriptorBufferInfo.Buffer buffer) {
        writeDescriptorSetStructBuffer.get(bufferIdx)
            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            .dstSet(dstSet)
            .descriptorType(descriptorType)
            .dstBinding(dstBinding)
            .pBufferInfo(buffer);
    }
    public static void writeDescriptorSet(VkWriteDescriptorSet.Buffer writeDescriptorSetStructBuffer, int bufferIdx, 
            long dstSet,
            int descriptorType, int dstBinding, VkDescriptorImageInfo.Buffer buffer) {
        writeDescriptorSetStructBuffer.get(bufferIdx)
            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            .dstSet(dstSet)
            .descriptorType(descriptorType)
            .dstBinding(dstBinding)
            .pImageInfo(buffer);
    }

    public static VkCommandBufferAllocateInfo commandBufferAllocInfo(long commandPool, int commandBufferCount) {
        VkCommandBufferAllocateInfo cmdBufAllocInfo = VkCommandBufferAllocateInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            .commandPool(commandPool)
            .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            .commandBufferCount(commandBufferCount);
        return cmdBufAllocInfo;
    }

    public static VkSamplerCreateInfo samplerCreateStack() {
        return VkSamplerCreateInfo.callocStack()
                .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(VK_FILTER_LINEAR)
                .minFilter(VK_FILTER_LINEAR)
                .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .mipLodBias(0.0f)
                .compareOp(VK_COMPARE_OP_NEVER)
                .minLod(0.0f).maxLod(1.0f)
                .maxAnisotropy(1.0f)
                .anisotropyEnable(false)
                .borderColor(VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK);
    }
    
    
}