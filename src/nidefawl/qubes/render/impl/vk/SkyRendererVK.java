package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.render.SkyRenderer;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.World;

public class SkyRendererVK extends SkyRenderer implements IRenderComponent {

    private long image;
    private long view;
    private long sampler;
    private FrameBuffer frameBufferCubemap;
    private FrameBuffer frameBufferSingle;
    public VkDescriptor descTextureSkyboxSingle;
    private long[] viewsFrameBuffer;

    @Override
    public void init() {
        super.init();
        VKContext ctxt = Engine.vkContext;
        this.descTextureSkyboxSingle = ctxt.descLayouts.allocDescSetSampleSingle();
        this.frameBufferCubemap = new FrameBuffer(ctxt).tag("sky_cubemap");
        this.frameBufferSingle = new FrameBuffer(ctxt).tag("sky_single");
        this.frameBufferSingle.fromRenderpass(VkRenderPasses.passSkyGenerate, 0, VK_IMAGE_USAGE_SAMPLED_BIT);

        try ( MemoryStack stack = stackPush() ) {
            LongBuffer pImage = stack.longs(0);
    
                VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                        .imageType(VK_IMAGE_TYPE_2D)
                        .format(VK_FORMAT_R16G16B16A16_SFLOAT)
                        .mipLevels(1)
                        .arrayLayers(6)
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                        .tiling(VK_IMAGE_TILING_OPTIMAL)
                        .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                        .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                        .flags(VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT);
                imageCreateInfo.extent().width(SKYBOX_RES).height(SKYBOX_RES).depth(1);
                int err = vkCreateImage(ctxt.device, imageCreateInfo, null, pImage);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateImage failed: " + VulkanErr.toString(err));
                }
                this.image = pImage.get(0);
                ctxt.memoryManager.allocateImageMemory(pImage.get(0), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, VkConstants.TEXTURE_COLOR_MEMORY);

                LongBuffer pView = stack.longs(0);

                for (int i = 0; i < 6; i++) {

                    VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                            .viewType(VK_IMAGE_VIEW_TYPE_2D)
                            .format(VK_FORMAT_R16G16B16A16_SFLOAT)
                            .components(VkComponentMapping.callocStack(stack));
                    VkImageSubresourceRange viewSubResRange = view.subresourceRange();
                    viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    viewSubResRange.baseMipLevel(0);
                    viewSubResRange.levelCount(1);
                    viewSubResRange.baseArrayLayer(i);
                    viewSubResRange.layerCount(1);
                    view.image(this.image);
                    err = vkCreateImageView(ctxt.device, view, null, pView);
                    if (err != VK_SUCCESS) {
                        throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
                    }
                    this.viewsFrameBuffer[i] = pView.get(0);
                    this.frameBufferCubemap.attachments[i] = new FramebufferAttachment(ctxt, VK_FORMAT_R16G16B16A16_SFLOAT, VK_IMAGE_ASPECT_COLOR_BIT, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                    this.frameBufferCubemap.attachments[i].setFromImage(this.image, this.viewsFrameBuffer[i], SKYBOX_RES, SKYBOX_RES);
                    this.frameBufferCubemap.isReferencedAtt[i] = true;
                }
                this.frameBufferCubemap.build(VkRenderPasses.passSkyGenerateCubemap, SKYBOX_RES, SKYBOX_RES);
                
                
              VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                      .viewType(VK_IMAGE_VIEW_TYPE_CUBE)
                      .format(VK_FORMAT_R16G16B16A16_SFLOAT)
                      .components(VkComponentMapping.callocStack(stack));
              VkImageSubresourceRange viewSubResRange = view.subresourceRange();
              viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
              viewSubResRange.baseMipLevel(0);
              viewSubResRange.levelCount(1);
              viewSubResRange.baseArrayLayer(0);
              viewSubResRange.layerCount(6);
              view.image(this.image);
              err = vkCreateImageView(ctxt.device, view, null, pView);
              if (err != VK_SUCCESS) {
                  throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
              }
              this.view = pView.get(0);

              VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
              sampler
                  .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                  .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                  .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                  .borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE);
              
              LongBuffer pSampler = stack.longs(0);
              err = vkCreateSampler(ctxt.device, sampler, null, pSampler);
              if (err != VK_SUCCESS) {
                  throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
              }
              this.sampler = pSampler.get(0);
              
        }
        ctxt.clearImage(ctxt.getCopyCommandBuffer(), this.image, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, 0, 0, 0, 1);

    }
    @Override
    public void release() {
        if (this.image != VK_NULL_HANDLE) {
            VKContext ctxt = Engine.vkContext;
            vkDestroyImageView(ctxt.device, this.view, null);
            vkDestroyImage(ctxt.device, this.image, null);
            vkDestroySampler(ctxt.device, this.sampler, null);
            this.view = VK_NULL_HANDLE;
            this.image = VK_NULL_HANDLE;
            this.sampler = VK_NULL_HANDLE;
            for (int i = 0; i < 6; i++) {
                vkDestroyImageView(ctxt.device, this.viewsFrameBuffer[i], null);
            }
        }
    }
    @Override
    protected void uploadData() {
    }

    @Override
    public void tickUpdate() {
    }

    @Override
    public void renderSky(World world, float fTime) {
        FrameBuffer fbScene = this.frameBufferSingle;
        
        if (fbScene.getWidth() == Engine.fbWidth() && fbScene.getHeight() == Engine.fbHeight())
        {
            Engine.beginRenderPass(VkRenderPasses.passSkyGenerate, fbScene, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(2, Engine.descriptorSetUboLights);
            Engine.clearDescriptorSet(3);
            Engine.clearDescriptorSet(4);
            Engine.bindPipeline(VkPipelines.skybox_update_background);
            Engine.drawFSTri();
            Engine.endRenderPass();
            Engine.clearDescriptorSet(2);
            
            
            
            Engine.beginRenderPass(VkRenderPasses.passSkyGenerateCubemap, this.frameBufferCubemap, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(2, Engine.descriptorSetUboLights);
            Engine.clearDescriptorSet(3);
            Engine.clearDescriptorSet(4);
            for (int i = 0; i < 6; i++) {
                if (i != 0)
                    vkCmdNextSubpass(Engine.getDrawCmdBuffer(), VK_SUBPASS_CONTENTS_INLINE);
                Engine.bindPipeline(VkPipelines.skybox_update_background_cubemap);
                Engine.drawFSTri();
            }
            Engine.endRenderPass();
            Engine.clearDescriptorSet(2);
        }
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        if (this.frameBufferSingle != null) {
            this.frameBufferSingle.destroy();
        }
        if (this.frameBufferCubemap != null) {
            this.frameBufferCubemap.destroy();
        }
        this.frameBufferSingle.build(VkRenderPasses.passSkyGenerate, displayWidth, displayHeight);

        FramebufferAttachment coloratt = this.frameBufferSingle.getAtt(0);
        this.descTextureSkyboxSingle.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
        this.descTextureSkyboxSingle.update(Engine.vkContext);
    }

    public void renderSkybox() {
    }

}
