package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.FinalRenderer;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.WorldClient;

public class FinalRendererVK extends FinalRenderer implements IRenderComponent {

    public FrameBuffer frameBufferScene;
    public FrameBuffer frameBufferSceneWater;
    public FrameBuffer frameBufferDeferred;
    public VkDescriptor descTextureFinalOut;
    public VkDescriptor descTextureDeferred0;
    public VkDescriptor descTextureDeferred1;
    public long sampler;
    public FrameBuffer frameBufferTonemapped;
    public VkDescriptor descTextureTonemap;
//    public FrameBuffer frameBufferSceneColorOnly;
    @Override
    public void init() {
        VKContext ctxt = Engine.vkContext;
        this.frameBufferScene = new FrameBuffer(ctxt).tag("scene_pass0");
        this.frameBufferScene.fromRenderpass(VkRenderPasses.passTerrain_Pass0, VK_IMAGE_USAGE_SAMPLED_BIT|VK_IMAGE_USAGE_TRANSFER_SRC_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);
//        this.frameBufferSceneColorOnly = new FrameBuffer(ctxt).tag("scene_pass0_att0");
//        this.frameBufferSceneColorOnly.copyAtt(frameBufferScene, 0);

        this.frameBufferSceneWater = new FrameBuffer(ctxt).tag("scene_pass1");
        this.frameBufferSceneWater.fromRenderpass(VkRenderPasses.passTerrain_Pass1, VK_IMAGE_USAGE_SAMPLED_BIT|VK_IMAGE_USAGE_TRANSFER_DST_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferDeferred = new FrameBuffer(ctxt).tag("deferred");
        this.frameBufferDeferred.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferTonemapped = new FrameBuffer(ctxt).tag("tonemap");
        this.frameBufferTonemapped.fromRenderpass(VkRenderPasses.passFramebufferNoDepth, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.descTextureFinalOut = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureDeferred0 = ctxt.descLayouts.allocDescSetSamplerDeferredPass0();
        this.descTextureDeferred1 = ctxt.descLayouts.allocDescSetSamplerDeferredPass1();
        this.descTextureTonemap = ctxt.descLayouts.allocDescSetSamplerDouble();
        try ( MemoryStack stack = stackPush() ) {
            VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
            LongBuffer pSampler = stack.longs(0);
            int err = vkCreateSampler(ctxt.device, sampler, null, pSampler);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
            }
            this.sampler = pSampler.get(0);
        }
    }

    @Override
    public void onAASettingChanged() {
    }

    @Override
    public void setSSR(int id) {
    }

    @Override
    public void onAOSettingUpdated() {
    }

    @Override
    public void aoReinit() {
    }

    @Override
    public void onVRModeChanged() {
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        if (this.frameBufferScene != null) {
            this.frameBufferScene.destroy();
        }
//        if (this.frameBufferSceneColorOnly != null) {
//            this.frameBufferSceneColorOnly.destroy();
//        }
        if (this.frameBufferSceneWater != null) {
            this.frameBufferSceneWater.destroy();
        }
        if (this.frameBufferDeferred != null) {
            this.frameBufferDeferred.destroy();
        }
        if (this.frameBufferTonemapped != null) {
            this.frameBufferTonemapped.destroy();
        }
        this.frameBufferScene.build(VkRenderPasses.passTerrain_Pass0, displayWidth, displayHeight);
//        this.frameBufferSceneColorOnly.build(VkRenderPasses.passSkySample, displayWidth, displayHeight);
        this.frameBufferSceneWater.build(VkRenderPasses.passTerrain_Pass1, displayWidth, displayHeight);
        this.frameBufferDeferred.build(VkRenderPasses.passDeferred, displayWidth, displayHeight);
        this.frameBufferTonemapped.build(VkRenderPasses.passFramebufferNoDepth, displayWidth, displayHeight);
        for (int i = 0; i < 5; i++)
        {
            FramebufferAttachment att = this.frameBufferScene.getAtt(i);
            this.descTextureDeferred0.setBindingCombinedImageSampler(i, att.getView(), sampler, att.finalLayout);

        }
        this.descTextureDeferred0.setBindingCombinedImageSampler(5, RenderersVulkan.shadowRenderer.getView(), RenderersVulkan.shadowRenderer.getSampler(), RenderersVulkan.shadowRenderer.getLayout());
        this.descTextureDeferred0.setBindingCombinedImageSampler(6, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureDeferred0.update(Engine.vkContext);
        for (int i = 0; i < 5; i++)
        {
            FramebufferAttachment att = this.frameBufferSceneWater.getAtt(i);
            this.descTextureDeferred1.setBindingCombinedImageSampler(i, att.getView(), sampler, att.finalLayout);

        }
        this.descTextureDeferred1.setBindingCombinedImageSampler(5, RenderersVulkan.shadowRenderer.getView(), RenderersVulkan.shadowRenderer.getSampler(), RenderersVulkan.shadowRenderer.getLayout());
        this.descTextureDeferred1.setBindingCombinedImageSampler(6, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());

        FramebufferAttachment depth_att_pass_0 = this.frameBufferScene.getAtt(4);
        this.descTextureDeferred1.setBindingCombinedImageSampler(7, 
                depth_att_pass_0.getView(), sampler, depth_att_pass_0.finalLayout);
        VkTexture tex = RenderersVulkan.worldRenderer.getWaterNoiseTex();
        this.descTextureDeferred1.setBindingCombinedImageSampler(8, 
                tex.getView(), Engine.vkContext.samplerLinear, tex.getImageLayout());
        this.descTextureDeferred1.update(Engine.vkContext);
        
        FramebufferAttachment coloratt = this.frameBufferDeferred.getAtt(0);
        this.descTextureTonemap.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
        this.descTextureTonemap.setBindingCombinedImageSampler(1, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureTonemap.update(Engine.vkContext);
        
        coloratt = this.frameBufferTonemapped.getAtt(0);
        this.descTextureFinalOut.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
        this.descTextureFinalOut.update(Engine.vkContext);
    }
    
    @Override
    public void release() {
        if (this.frameBufferScene != null) {
            this.frameBufferScene.destroy();
        }
        if (this.frameBufferDeferred != null) {
            this.frameBufferDeferred.destroy();
        }
        if (this.frameBufferTonemapped != null) {
            this.frameBufferTonemapped.destroy();
        }
        if (this.sampler != VK_NULL_HANDLE) {
            vkDestroySampler(Engine.vkContext.device, this.sampler, null);
            this.sampler = VK_NULL_HANDLE;
        }
    }

    public void render(WorldClient world, float fTime) {
        renderDeferred(fTime);
    }
    public void renderDeferred(float fTime) {
        FrameBuffer fb = frameBufferDeferred;
        if (fb.getWidth() == Engine.fbWidth() && fb.getHeight() == Engine.fbHeight())
        {
            
            Engine.beginRenderPass(VkRenderPasses.passDeferred, fb, VK_SUBPASS_CONTENTS_INLINE);
//            
            Engine.setDescriptorSet(VkDescLayouts.DESC3, Engine.descriptorSetUboShadow);
            Engine.setDescriptorSet(VkDescLayouts.DESC4, Engine.descriptorSetUboLights);
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureDeferred0);
            Engine.bindPipeline(VkPipelines.deferred_pass0);
            Engine.drawFSTri();            
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureDeferred1);
            Engine.bindPipeline(VkPipelines.deferred_pass1);
            Engine.drawFSTri();
            Engine.endRenderPass();
            Engine.clearDescriptorSet(VkDescLayouts.DESC3);
            Engine.clearDescriptorSet(VkDescLayouts.DESC4);
            Engine.beginRenderPass(VkRenderPasses.passFramebufferNoDepth, this.frameBufferTonemapped, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureTonemap);
            Engine.bindPipeline(VkPipelines.tonemapDynamic);
            Engine.drawFSTri();
            Engine.endRenderPass();
            
        } else {
            System.err.println("SKIPPED, framebuffer is not sized");
            System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
                    fb.getWidth(), fb.getHeight(),
                    Engine.displayWidth, Engine.displayHeight);

        }
        
    }

}
