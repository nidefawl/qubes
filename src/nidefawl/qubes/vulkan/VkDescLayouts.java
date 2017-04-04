package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.render.impl.vk.LightComputeVK;

public class VkDescLayouts {

    public static final int DESC0 = 0;
    public static final int DESC1 = 1;
    public static final int DESC2 = 2;
    public static final int DESC3 = 3;
    public static final int DESC4 = 4;

    public static void allocStatic() {
    }

    public static void destroyStatic() {
    }

    private final VKContext ctxt;
    public long descSetLayoutUBOScene = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageSingle = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageDouble = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageTriple = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageSSR = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageSingleVertexStage = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageDeferredPass0 = VK_NULL_HANDLE;
    public long descSetLayoutSamplerImageDeferredPass1 = VK_NULL_HANDLE;
    public long descSetLayoutUBOConstants = VK_NULL_HANDLE;
    public long descSetLayoutUBOTransform = VK_NULL_HANDLE;
    public long descSetLayoutUBOShadow = VK_NULL_HANDLE;
    public long descSetLayoutUBOLightInfo = VK_NULL_HANDLE;
    public long descSetLayoutSSBOCubes = VK_NULL_HANDLE;
    public long descSetLayoutSSBOModelBatched = VK_NULL_HANDLE;
    public long descSetLayoutSSBOModelStatic = VK_NULL_HANDLE;
    public long descSetLayoutSSBOPointLights = VK_NULL_HANDLE;
    public long descSetLayoutImagesCompute = VK_NULL_HANDLE;
    
    public VkDescLayouts(VKContext ctxt) {
        this.ctxt = ctxt;
        try ( MemoryStack stack = stackPush() ) 
        {

            VkDescriptorSetLayoutBinding.Buffer ubo_scene_bindings = VkDescriptorSetLayoutBinding.calloc(3);
            VkDescriptorSetLayoutBinding.Buffer ubo_transform_stack_bindings = VkDescriptorSetLayoutBinding.calloc(1);
            VkDescriptorSetLayoutBinding.Buffer ubo_shadow_bindings = VkDescriptorSetLayoutBinding.calloc(1);
            VkDescriptorSetLayoutBinding.Buffer ubo_constants_bindings = VkDescriptorSetLayoutBinding.calloc(2);
            VkDescriptorSetLayoutBinding.Buffer ubo_lightinfo_bindings = VkDescriptorSetLayoutBinding.calloc(1);
            VkDescriptorSetLayoutBinding.Buffer ssbo_cubes_bindings = VkDescriptorSetLayoutBinding.calloc(2);
            VkDescriptorSetLayoutBinding.Buffer ssbo_model_batched_bindings = VkDescriptorSetLayoutBinding.calloc(3);
            VkDescriptorSetLayoutBinding.Buffer ssbo_model_static_bindings = VkDescriptorSetLayoutBinding.calloc(2);
            VkDescriptorSetLayoutBinding.Buffer ssbo_point_lights = VkDescriptorSetLayoutBinding.calloc(1);
            VkDescriptorSetLayoutBinding.Buffer sampler_image_single = VkDescriptorSetLayoutBinding.calloc(1);
            VkDescriptorSetLayoutBinding.Buffer sampler_image_double = VkDescriptorSetLayoutBinding.calloc(2);
            VkDescriptorSetLayoutBinding.Buffer sampler_image_triple = VkDescriptorSetLayoutBinding.calloc(3);
            VkDescriptorSetLayoutBinding.Buffer sampler_image_single_vertex_stage = VkDescriptorSetLayoutBinding.calloc(1);
            VkDescriptorSetLayoutBinding.Buffer sampler_image_deferred_pass0 = VkDescriptorSetLayoutBinding.calloc(7);
            VkDescriptorSetLayoutBinding.Buffer sampler_image_deferred_pass1 = VkDescriptorSetLayoutBinding.calloc(9);
            VkDescriptorSetLayoutBinding.Buffer sampler_image_ssr = VkDescriptorSetLayoutBinding.calloc(5);
            VkDescriptorSetLayoutBinding.Buffer sampler_images_compute_light = VkDescriptorSetLayoutBinding.calloc(3);


            
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
            
            ubo_transform_stack_bindings.get(0) //uboTransformStack
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT|VK_SHADER_STAGE_FRAGMENT_BIT)
                .binding(0)//uboMatrixShadow
                .descriptorCount(1);
            ubo_shadow_bindings.get(0) //uboMatrixShadow
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT|VK_SHADER_STAGE_FRAGMENT_BIT)
                .binding(0)//uboMatrixShadow
                .descriptorCount(1);
            
            ubo_lightinfo_bindings.get(0) //LightInfo
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT|VK_SHADER_STAGE_FRAGMENT_BIT)
                .binding(0)//SkyLight
                .descriptorCount(1);
            
            sampler_image_single.get(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                .binding(0)
                .descriptorCount(1);
            for (int i = 0; i < 3; i++)
                sampler_image_double.get(i)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .binding(i)
                    .descriptorCount(1);

            for (int i = 0; i < 3; i++)
                sampler_image_triple.get(i)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .binding(i)
                    .descriptorCount(1);
            
            sampler_image_single_vertex_stage.get(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT|VK_SHADER_STAGE_FRAGMENT_BIT)
                .binding(0)
                .descriptorCount(1);

            for (int i = 0; i < 5; i++)
                sampler_image_ssr.get(i)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .binding(i)
                    .descriptorCount(1);
            
            ssbo_cubes_bindings.get(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .binding(0)
                .descriptorCount(1);
            ssbo_cubes_bindings.get(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .binding(1)
                .descriptorCount(1);
            ssbo_model_batched_bindings.get(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .binding(0)
                .descriptorCount(1);
            ssbo_model_batched_bindings.get(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .binding(1)
                .descriptorCount(1);
            ssbo_model_batched_bindings.get(2)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .binding(2)
                .descriptorCount(1);
            
            ssbo_model_static_bindings.get(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .binding(0)
                .descriptorCount(1);
            ssbo_model_static_bindings.get(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .binding(1)
                .descriptorCount(1);
            
            ssbo_point_lights.get(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC)
                .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                .binding(0)
                .descriptorCount(1);
            
            for (int i = 0; i < sampler_image_deferred_pass0.limit(); i++) {
                sampler_image_deferred_pass0.get(i)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .binding(i)
                    .descriptorCount(1);
            }
            for (int i = 0; i < sampler_image_deferred_pass1.limit(); i++) {
                sampler_image_deferred_pass1.get(i)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .binding(i)
                    .descriptorCount(1);
            }
            sampler_images_compute_light.get(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                .binding(0)
                .descriptorCount(1);
            sampler_images_compute_light.get(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                .binding(1)
                .descriptorCount(1);
            sampler_images_compute_light.get(2)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                .binding(2)
                .descriptorCount(1);
            descSetLayoutUBOScene = makeSet(ubo_scene_bindings);
            descSetLayoutUBOTransform = makeSet(ubo_transform_stack_bindings);
            descSetLayoutUBOConstants = makeSet(ubo_constants_bindings);
            descSetLayoutUBOShadow = makeSet(ubo_shadow_bindings);
            descSetLayoutUBOLightInfo = makeSet(ubo_lightinfo_bindings);
            descSetLayoutSSBOCubes = makeSet(ssbo_cubes_bindings);
            descSetLayoutSSBOModelBatched = makeSet(ssbo_model_batched_bindings);
            descSetLayoutSSBOModelStatic = makeSet(ssbo_model_static_bindings);
            descSetLayoutSSBOPointLights = makeSet(ssbo_point_lights);
            descSetLayoutSamplerImageSingle = makeSet(sampler_image_single);
            descSetLayoutSamplerImageDouble = makeSet(sampler_image_double);
            descSetLayoutSamplerImageTriple = makeSet(sampler_image_triple);
            descSetLayoutSamplerImageSSR = makeSet(sampler_image_ssr);
            descSetLayoutSamplerImageSingleVertexStage = makeSet(sampler_image_single_vertex_stage);
            descSetLayoutSamplerImageDeferredPass0 = makeSet(sampler_image_deferred_pass0);
            descSetLayoutSamplerImageDeferredPass1 = makeSet(sampler_image_deferred_pass1);
            descSetLayoutImagesCompute = makeSet(sampler_images_compute_light);
            
            VkPushConstantRange.Buffer push_constant_ranges_gui = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_shadow_solid = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_single_block = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_single_block_3d = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_sprites = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_shadow_split = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_vec2 = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_float = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_blur_seperate = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_blur_kawase = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_colored_3d = VkPushConstantRange.calloc(1);
            VkPushConstantRange.Buffer push_constant_ranges_wireframe = VkPushConstantRange.calloc(2);
            VkPushConstantRange.Buffer push_constant_ranges_compute_light = VkPushConstantRange.calloc(1);

            push_constant_ranges_gui.get(0)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT|VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(8*4+2*16);
            push_constant_ranges_shadow_solid.get(0)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size(64+4);
            push_constant_ranges_sprites.get(0)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(4+4);
            push_constant_ranges_shadow_split.get(0)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size(4);
            push_constant_ranges_single_block.get(0)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size((4*4)*4*2);
            push_constant_ranges_single_block_3d.get(0)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size((4*4)*4);
            push_constant_ranges_vec2.get(0)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size(4+4);
            push_constant_ranges_float.get(0)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size(4);
            push_constant_ranges_blur_seperate.get(0)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(4+4);   
            push_constant_ranges_blur_kawase.get(0)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(4+4+4);
            push_constant_ranges_colored_3d.get(0)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(4+4*4);

            push_constant_ranges_wireframe.get(0)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size(84);

            push_constant_ranges_wireframe.get(1)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(84)
                .size(8);
            
            push_constant_ranges_compute_light.get(0)
                .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
                .offset(0)
                .size(4);
            
            VkPipelines.pipelineLayoutTexturedFullscreen.build(ctxt, 
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle);
            VkPipelines.pipelineLayoutTextured.build(ctxt, 
                    descSetLayoutUBOScene, descSetLayoutUBOTransform, descSetLayoutSamplerImageSingle);
            VkPipelines.pipelineLayoutColored.build(ctxt, 
                    descSetLayoutUBOScene, descSetLayoutUBOTransform);
            VkPipelines.pipelineLayoutColored3D.build(ctxt, new long[] { 
                    descSetLayoutUBOScene, descSetLayoutUBOTransform}, push_constant_ranges_colored_3d);
            VkPipelines.pipelineLayoutWireframe.build(ctxt, new long[] { 
                    descSetLayoutUBOScene, descSetLayoutUBOTransform}, push_constant_ranges_wireframe);
            VkPipelines.pipelineLayoutGUI.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutUBOTransform}, push_constant_ranges_gui);
            VkPipelines.pipelineLayoutSingleBlock.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutUBOTransform, descSetLayoutUBOConstants, descSetLayoutSamplerImageSingle}, push_constant_ranges_single_block);

            
            VkPipelines.pipelineLayoutTerrain.build(ctxt, 
                    descSetLayoutUBOScene, descSetLayoutSamplerImageDouble, descSetLayoutUBOConstants);
            VkPipelines.pipelineLayoutMain.build(ctxt, 
                    descSetLayoutUBOScene, descSetLayoutSamplerImageDouble, descSetLayoutUBOShadow);
            VkPipelines.pipelineLayoutShadow.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle, descSetLayoutUBOShadow}, push_constant_ranges_shadow_solid);
            VkPipelines.pipelineLayoutPostDownsample.build(ctxt, new long[] {descSetLayoutSamplerImageSingle}, push_constant_ranges_vec2);
            VkPipelines.pipelineLayoutPostLumInterp.build(ctxt, new long[] {descSetLayoutSamplerImageSingleVertexStage, descSetLayoutSamplerImageSingleVertexStage}, push_constant_ranges_float);
            VkPipelines.pipelineLayoutBlurKawase.build(ctxt, new long[] {descSetLayoutSamplerImageSingle}, push_constant_ranges_blur_kawase);
            VkPipelines.pipelineLayoutBlurSeperate.build(ctxt, new long[] {descSetLayoutSamplerImageSingle}, push_constant_ranges_blur_seperate);
            VkPipelines.pipelineLayoutBloom.build(ctxt, new long[] {descSetLayoutSamplerImageTriple});
            VkPipelines.pipelineLayoutBloomCombine.build(ctxt, new long[] {descSetLayoutSamplerImageSingle, descSetLayoutSamplerImageSingle});
            VkPipelines.pipelineLayoutDeferredPass0.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageDeferredPass0, descSetLayoutUBOShadow, descSetLayoutUBOLightInfo});
            VkPipelines.pipelineLayoutDeferredPass1.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageDeferredPass1, descSetLayoutUBOShadow, descSetLayoutUBOLightInfo});
            VkPipelines.pipelineLayoutTonemapDynamic.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle, descSetLayoutSamplerImageSingle});
            VkPipelines.pipelineLayoutSkybox.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle, descSetLayoutUBOLightInfo});
            VkPipelines.pipelineLayoutSkyboxSprites.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle, descSetLayoutUBOLightInfo}, push_constant_ranges_sprites);
            VkPipelines.pipelineLayoutParticleCube.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageDouble, descSetLayoutUBOConstants, descSetLayoutSSBOCubes});
            VkPipelines.pipelineLayoutSingleBlock3D.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageDouble}, push_constant_ranges_single_block_3d);
            VkPipelines.pipelineLayoutModelFirstPerson.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle}, push_constant_ranges_single_block_3d);
            VkPipelines.pipelineLayoutModelStaticGbuffer.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle, descSetLayoutSSBOModelStatic});
            VkPipelines.pipelineLayoutModelStaticShadow.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle, descSetLayoutSSBOModelStatic, descSetLayoutUBOShadow}, push_constant_ranges_shadow_split);
            VkPipelines.pipelineLayoutModelBatchedGbuffer.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle, descSetLayoutSSBOModelBatched});
            VkPipelines.pipelineLayoutModelBatchedShadow.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSingle, descSetLayoutSSBOModelBatched, descSetLayoutUBOShadow}, push_constant_ranges_shadow_split);

            VkPipelines.pipelineLayoutSSR.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageSSR, descSetLayoutSamplerImageSingle});
            VkPipelines.pipelineLayoutSSRCombine.build(ctxt, new long[] {
                    descSetLayoutUBOScene, descSetLayoutSamplerImageDouble, descSetLayoutSamplerImageSingle});
            if (LightComputeVK.DEBUG_LIGHTS) {
                VkPipelines.pipelineLayoutComputeLight.build(ctxt, new long[] {
                        descSetLayoutUBOScene, descSetLayoutImagesCompute, descSetLayoutSSBOPointLights, descSetLayoutSSBOPointLights}, push_constant_ranges_compute_light);
            } else {
                VkPipelines.pipelineLayoutComputeLight.build(ctxt, new long[] {
                        descSetLayoutUBOScene, descSetLayoutImagesCompute, descSetLayoutSSBOPointLights}, push_constant_ranges_compute_light);

            }
            
        }


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
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutUBOConstants, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutUBOTransform, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutUBOShadow, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutUBOLightInfo, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSamplerImageTriple, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSamplerImageSSR, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSamplerImageSingle, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSamplerImageDouble, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSamplerImageSingleVertexStage, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSamplerImageDeferredPass0, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSamplerImageDeferredPass1, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSSBOCubes, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSSBOModelBatched, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSSBOModelStatic, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutSSBOPointLights, null);
        vkDestroyDescriptorSetLayout(this.ctxt.device, this.descSetLayoutImagesCompute, null);
    }

    public void init(VKContext ctxt) {
    }

    public VkDescriptor allocDescSetUBOScene() {
        return new VkDescriptor(allocDescSet(descSetLayoutUBOScene)).tag("UBOScene");
    }

    public VkDescriptor allocDescSetUBOConstants() {
        return new VkDescriptor(allocDescSet(descSetLayoutUBOConstants)).tag("UBOConstants");
    }
    
    public VkDescriptor allocDescSetSSBOCubes() {
        return new VkDescriptor(allocDescSet(descSetLayoutSSBOCubes)).tag("SSBOCubes");
    }

    public VkDescriptor allocDescSetSSBOModelBatched() {
        return new VkDescriptor(allocDescSet(descSetLayoutSSBOModelBatched)).tag("SSBOModelBatched");
    }
    
    public VkDescriptor allocDescSetSSBOModelStatic() {
        return new VkDescriptor(allocDescSet(descSetLayoutSSBOModelStatic)).tag("SSBOModelStatic");
    }
    
    public VkDescriptor allocDescSetSSBOPointLights() {
        return new VkDescriptor(allocDescSet(descSetLayoutSSBOPointLights)).tag("SSBOPointLights");
    }
    
    public VkDescriptor allocDescSetImagesComputeLight() {
        return new VkDescriptor(allocDescSet(descSetLayoutImagesCompute)).tag("ImagesComputeLight");
    }

    public VkDescriptor allocDescSetUBOShadow() {
        return new VkDescriptor(allocDescSet(descSetLayoutUBOShadow)).tag("UBOShadow");
    }
    public VkDescriptor allocDescSetUBOTransform() {
        return new VkDescriptor(allocDescSet(descSetLayoutUBOTransform)).tag("UBOTransform");
    }
    public VkDescriptor allocDescSetUBOLightInfo() {
        return new VkDescriptor(allocDescSet(descSetLayoutUBOLightInfo)).tag("UBOLightInfo");
    }
    public VkDescriptor allocDescSetSampleSSR() {
        return new VkDescriptor(allocDescSet(descSetLayoutSamplerImageSSR)).tag("samplerSSR");
    }
    public VkDescriptor allocDescSetSampleSingle() {
        return new VkDescriptor(allocDescSet(descSetLayoutSamplerImageSingle)).tag("samplerSingle");
    }
    public VkDescriptor allocDescSetSampleTriple() {
        return new VkDescriptor(allocDescSet(descSetLayoutSamplerImageTriple)).tag("samplerTriple");
    }
    public VkDescriptor allocDescSetSamplerDouble() {
        return new VkDescriptor(allocDescSet(descSetLayoutSamplerImageDouble)).tag("samplerDouble");
    }
    public VkDescriptor allocDescSetSamplerSingleVertexStage() {
        return new VkDescriptor(allocDescSet(descSetLayoutSamplerImageSingleVertexStage)).tag("samplerSingleVertexStage");
    }
    public VkDescriptor allocDescSetSamplerDeferredPass0() {
        return new VkDescriptor(allocDescSet(descSetLayoutSamplerImageDeferredPass0)).tag("descSetLayoutSamplerImageDeferredPass0");
    }
    public VkDescriptor allocDescSetSamplerDeferredPass1() {
        return new VkDescriptor(allocDescSet(descSetLayoutSamplerImageDeferredPass1)).tag("descSetLayoutSamplerImageDeferredPass1");
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
