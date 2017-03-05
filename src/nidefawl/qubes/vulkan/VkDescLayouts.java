package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding.Buffer;

public class VkDescLayouts {

    public static void allocStatic() {
    }

    public static void destroyStatic() {
    }

    private final VKContext ctxt;
    public VkDescriptorSetLayoutBinding.Buffer ubo_scene_bindings = VkDescriptorSetLayoutBinding.calloc(4);
    public VkDescriptorSetLayoutBinding.Buffer ubo_constants_bindings = VkDescriptorSetLayoutBinding.calloc(1);
    public VkDescriptorSetLayoutBinding.Buffer sampler_image_single = VkDescriptorSetLayoutBinding.calloc(1);
    public VkDescriptorSetLayoutBinding.Buffer sampler_image_double = VkDescriptorSetLayoutBinding.calloc(2);
    public VkPushConstantRange.Buffer push_constant_ranges_gui = VkPushConstantRange.calloc(1);
    public VkPushConstantRange.Buffer push_constant_ranges_shadow_solid = VkPushConstantRange.calloc(1);
//    public VkDescriptorSetLayoutBinding.Buffer ubo_shadow_bindings = VkDescriptorSetLayoutBinding.calloc(2);
    public long descSetLayoutUBOScene = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageSingle = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageDouble = VK_NULL_HANDLE;
//    public long descSetLayoutUBOShadow = VK_NULL_HANDLE;
    public VkDescLayouts(VKContext ctxt) {
        this.ctxt = ctxt;
        for (int i = 0; i < ubo_scene_bindings.limit(); i++) {
            ubo_scene_bindings.get(i)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_ALL)
                .binding(i)
                .descriptorCount(1);
        }
        for (int i = 0; i < ubo_constants_bindings.limit(); i++) {
            ubo_constants_bindings.get(i)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .stageFlags(VK_SHADER_STAGE_ALL)
                .binding(i)
                .descriptorCount(1);
        }

//        ubo_shadow_bindings.get(0) //uboMatrix3D
//            .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
//            .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
//            .binding(0)//uboMatrix3D
//            .descriptorCount(1);
//        ubo_shadow_bindings.get(1)
//            .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
//            .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
//            .binding(3)//uboMatrixShadow
//            .descriptorCount(1);
        sampler_image_single.get(0)
            .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            .binding(0)
            .descriptorCount(1);
        sampler_image_double.get(0)
            .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            .binding(0)
            .descriptorCount(1);
        sampler_image_double.get(1)
            .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            .binding(1)
            .descriptorCount(1);
        push_constant_ranges_gui.get(0)
            .stageFlags(VK_SHADER_STAGE_VERTEX_BIT|VK_SHADER_STAGE_FRAGMENT_BIT)
            .offset(0)
            .size(8*4+2*16);
        push_constant_ranges_shadow_solid.get(0)
            .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
            .offset(0)
            .size(64+4);
        descSetLayoutUBOScene = makeSet(ubo_scene_bindings);
//        descSetLayoutUBOShadow = makeSet(ubo_shadow_bindings);
        descSetLayoutSamplerImageSingle = makeSet(sampler_image_single);
        descSetLayoutSamplerImageDouble = makeSet(sampler_image_double);
        VkPipelines.pipelineLayoutMain.build(ctxt, descSetLayoutUBOScene, descSetLayoutSamplerImageDouble);
        VkPipelines.pipelineLayoutTextured.build(ctxt, descSetLayoutUBOScene, descSetLayoutSamplerImageSingle);
        VkPipelines.pipelineLayoutColored.build(ctxt, descSetLayoutUBOScene);
        VkPipelines.pipelineLayoutGUI.build(ctxt, new long[] {descSetLayoutUBOScene}, push_constant_ranges_gui);
//        VkPipelines.pipelineLayoutShadow.build(ctxt, new long[] {descSetLayoutUBOShadow}, push_constant_ranges_shadow_solid);
        VkPipelines.pipelineLayoutShadow.build(ctxt, new long[] {descSetLayoutUBOScene}, push_constant_ranges_shadow_solid);
    }
            
    private long makeSet(VkDescriptorSetLayoutBinding.Buffer... buffers) {
        try ( MemoryStack stack = stackPush() ) {
            int len = 0;
            for (int i = 0; i < buffers.length; i++) {
                len += buffers[i].remaining();
            }
            VkDescriptorSetLayoutBinding.Buffer setLayoutBindings = VkDescriptorSetLayoutBinding.callocStack(len, stack);
            setLayoutBindings.rewind();
            int offset = 0;
            for (int i = 0; i < buffers.length; i++) {
                for (int j = 0; j < buffers[i].remaining(); j++) {
                    setLayoutBindings.put(offset++, buffers[i].get(j));
                }
            }
            setLayoutBindings.rewind();
            VkDescriptorSetLayoutCreateInfo descriptorLayoutCI = VkInitializers.descriptorSetLayoutCreateInfo(setLayoutBindings);
            LongBuffer pDescriptorSetLayout = stack.longs(0);
            int err = vkCreateDescriptorSetLayout(this.ctxt.device, descriptorLayoutCI, null, pDescriptorSetLayout);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateDescriptorSetLayout failed: " + VulkanErr.toString(err));
            }
            return pDescriptorSetLayout.get(0);
        }
    }

    void destroy() {
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutUBOScene, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSamplerImageSingle, null);
        ubo_constants_bindings.free();
        ubo_scene_bindings.free();
        sampler_image_single.free();
    }

    public void init(VKContext ctxt) {
    }

    public long allocDescSetUBOScene() {
        return allocDescSet(descSetLayoutUBOScene);
    }

//    public long allocDescSetUBOShadow() {
//        return allocDescSet(descSetLayoutUBOShadow);
//    }
    public long allocDescSetSampleSingle() {
        return allocDescSet(descSetLayoutSamplerImageSingle);
    }
    public long allocDescSetSamplerDouble() {
        return allocDescSet(descSetLayoutSamplerImageDouble);
    }
    public long allocDescSet(long descSetLayout) {
        try ( MemoryStack stack = stackPush() ) {
            LongBuffer pDescriptorSetLayouts = stack.longs(descSetLayout);
            VkDescriptorSetAllocateInfo allocInfo = VkInitializers.descriptorSetAllocateInfo(this.ctxt.descriptorPool, pDescriptorSetLayouts);
            LongBuffer pDescriptorSet = stack.longs(0);
            int err = vkAllocateDescriptorSets(this.ctxt.device, allocInfo, pDescriptorSet);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkAllocateDescriptorSets failed: " + VulkanErr.toString(err));
            }
            return pDescriptorSet.get(0);
        }
    }
}
