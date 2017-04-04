package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.FinalRenderer;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.WorldClient;

public class FinalRendererVK extends FinalRenderer implements IRenderComponent {

    public FrameBuffer frameBufferScene;
    public FrameBuffer frameBufferSceneWater;
    public FrameBuffer frameBufferSceneFirstPerson;
    public FrameBuffer frameBufferDeferred;
    public FrameBuffer frameBufferDeferredPass01;
    public FrameBuffer frameBufferBloom;
    public FrameBuffer frameBufferTonemapped;
    public FrameBuffer frameBufferSSR;
    
    
    public FrameBuffer[] fbLuminanceDownsample;
    public FrameBuffer[] fbLuminanceInterp;

    public VkDescriptor descTextureDeferred0;
    public VkDescriptor descTextureDeferredInput1;
    public VkDescriptor descTextureDeferredInput2;
    public VkDescriptor descTextureDeferredOut;
    public VkDescriptor descTextureFirstPersonOut;
    
    public VkDescriptor descTextureBloomInput;
    public VkDescriptor descTextureBloomOut;

    private VkDescriptor descTextureSSRInput;
    private VkDescriptor descTextureSSROutput;
    private VkDescriptor descTextureSSRCombineInput;
    
    public VkDescriptor[] descTextureDownsampleOutputs = new VkDescriptor[10];
    public VkDescriptor descTextureDownsampleEnd;
    public VkDescriptor[] descLumInterpOutputsVertexInput = new VkDescriptor[2];
    public VkDescriptor[] descLumInterpOutputsCalcd = new VkDescriptor[2];
    
    public VkDescriptor descTextureFinalOut;
    
    public long samplerClamp;
    
    public int frame;
    private int[] ssrSize;
    private long samplerNearest;
    
    @Override
    public void init() {
        VKContext ctxt = Engine.vkContext;
        this.frameBufferScene = new FrameBuffer(ctxt).tag("scene_pass0");
        this.frameBufferScene.fromRenderpass(VkRenderPasses.passTerrain_Pass0, VK_IMAGE_USAGE_SAMPLED_BIT|VK_IMAGE_USAGE_TRANSFER_SRC_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);
        
        this.frameBufferSceneWater = new FrameBuffer(ctxt).tag("scene_pass1");
        this.frameBufferSceneWater.fromRenderpass(VkRenderPasses.passTerrain_Pass1, VK_IMAGE_USAGE_SAMPLED_BIT|VK_IMAGE_USAGE_TRANSFER_DST_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferSceneWater.copyAtt(this.frameBufferScene, 1);
        this.frameBufferSceneWater.copyAtt(this.frameBufferScene, 2);
        this.frameBufferSceneWater.copyAtt(this.frameBufferScene, 3);

        this.frameBufferSceneFirstPerson = new FrameBuffer(ctxt).tag("scene_pass2");
        this.frameBufferSceneFirstPerson.fromRenderpass(VkRenderPasses.passTerrain_Pass2, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);
//        this.frameBufferSceneFirstPerson.copyAtt(this.frameBufferScene, 1);
        
        this.frameBufferDeferred = new FrameBuffer(ctxt).tag("deferred");
        this.frameBufferDeferred.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferDeferredPass01 = new FrameBuffer(ctxt).tag("deferredPass01");
        this.frameBufferDeferredPass01.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferSSR = new FrameBuffer(ctxt).tag("ssr");
        this.frameBufferSSR.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferTonemapped = new FrameBuffer(ctxt).tag("tonemap");
        this.frameBufferTonemapped.fromRenderpass(VkRenderPasses.passTonemap, VK_IMAGE_USAGE_TRANSFER_DST_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);

        this.frameBufferBloom = new FrameBuffer(ctxt).tag("bloom");
        this.frameBufferBloom.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);

        this.fbLuminanceInterp = new FrameBuffer[2];
        for (int i = 0; i < 2; i++) {
            this.fbLuminanceInterp[i] = new FrameBuffer(ctxt).tag("fbLuminanceInterp");
            this.fbLuminanceInterp[i].fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        }

        for (int i = 0; i < descTextureDownsampleOutputs.length; i++) {
            this.descTextureDownsampleOutputs[i] = ctxt.descLayouts.allocDescSetSampleSingle().tag("downsampleOutSingle");
        }
        this.descTextureDownsampleEnd = ctxt.descLayouts.allocDescSetSamplerSingleVertexStage().tag("textureDownsampleEndSingleVert");
        for (int i = 0; i < descLumInterpOutputsVertexInput.length; i++) {
            this.descLumInterpOutputsVertexInput[i] = ctxt.descLayouts.allocDescSetSamplerSingleVertexStage().tag("LumInterpOutputsSingleVert");
        }
        for (int i = 0; i < descLumInterpOutputsCalcd.length; i++) {
            this.descLumInterpOutputsCalcd[i] = ctxt.descLayouts.allocDescSetSampleSingle().tag("LumInterpOutputs");
        }
        this.descTextureDeferredOut = ctxt.descLayouts.allocDescSetSampleSingle().tag("DownsampleInputSingle");
        this.descTextureFirstPersonOut = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureBloomOut = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureBloomInput = ctxt.descLayouts.allocDescSetSampleTriple();
        this.descTextureFinalOut = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureDeferred0 = ctxt.descLayouts.allocDescSetSamplerDeferredPass0();
        this.descTextureDeferredInput1 = ctxt.descLayouts.allocDescSetSamplerDeferredPass1();
        this.descTextureDeferredInput2 = ctxt.descLayouts.allocDescSetSamplerDeferredPass0();
        this.descTextureSSRInput = ctxt.descLayouts.allocDescSetSampleSSR();
        this.descTextureSSROutput = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureSSRCombineInput = ctxt.descLayouts.allocDescSetSamplerDouble();

        try ( MemoryStack stack = stackPush() ) {
            VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
            sampler.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            sampler.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            sampler.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            LongBuffer pSampler = stack.longs(0);
            int err = vkCreateSampler(ctxt.device, sampler, null, pSampler);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
            }
            this.samplerClamp = pSampler.get(0);
            sampler.magFilter(VK_FILTER_NEAREST);
            sampler.minFilter(VK_FILTER_NEAREST);
            sampler.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);
            err = vkCreateSampler(ctxt.device, sampler, null, pSampler);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
            }
            this.samplerNearest = pSampler.get(0);
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
        this.ssrSize = GameMath.downsample(displayWidth, displayHeight, Engine.RENDER_SETTINGS.ssr>2?1:2);
        VKContext ctxt = Engine.vkContext;
        releaseFramebuffers();
        long shadowSampler = samplerNearest;//RenderersVulkan.shadowRenderer.getSampler();
        int[] lumSize = GameMath.downsample(displayWidth, displayHeight, 2);

        int lumW = lumSize[0];
        int lumH = lumSize[1];
        List<FrameBuffer> list = Lists.newArrayList();
        while (true) {
            int idx = list.size();
            FrameBuffer fbLuminance2 = new FrameBuffer(ctxt).tag("fbLuminanceDownsample_"+idx);
            fbLuminance2.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
            fbLuminance2.build(VkRenderPasses.passDeferred, lumW, lumH);
            FramebufferAttachment coloratt = fbLuminance2.getAtt(0);
            this.descTextureDownsampleOutputs[idx].setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
            this.descTextureDownsampleOutputs[idx].update(ctxt);
            
            list.add(fbLuminance2);
            if (lumW == 1 && lumH == 1) {
                this.descTextureDownsampleEnd.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
                this.descTextureDownsampleEnd.update(ctxt);
                break;
            }
            lumW = lumW / 4;
            lumH = lumH / 4;
            if (lumW < 1)
                lumW = 1;
            if (lumH < 1)
                lumH = 1;
        }
        this.fbLuminanceDownsample = list.toArray(new FrameBuffer[list.size()]);
        for (int i = 0; i < fbLuminanceInterp.length; i++) {
            this.fbLuminanceInterp[i].build(VkRenderPasses.passDeferred, 1, 1);
            FramebufferAttachment coloratt = this.fbLuminanceInterp[i].getAtt(0);
            this.descLumInterpOutputsVertexInput[i].setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
            this.descLumInterpOutputsVertexInput[i].update(ctxt);
            this.descLumInterpOutputsCalcd[i].setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
            this.descLumInterpOutputsCalcd[i].update(ctxt);
        }
        
        

        this.frameBufferScene.build(VkRenderPasses.passTerrain_Pass0, displayWidth, displayHeight);
        this.frameBufferSceneWater.build(VkRenderPasses.passTerrain_Pass1, displayWidth, displayHeight);
        this.frameBufferSceneFirstPerson.build(VkRenderPasses.passTerrain_Pass2, displayWidth, displayHeight);
        this.frameBufferDeferred.build(VkRenderPasses.passDeferred, displayWidth, displayHeight);
        this.frameBufferDeferredPass01.build(VkRenderPasses.passDeferred, displayWidth, displayHeight);
        this.frameBufferSSR.build(VkRenderPasses.passDeferred, ssrSize[0], ssrSize[1]);
        this.frameBufferTonemapped.build(VkRenderPasses.passTonemap, displayWidth, displayHeight);
        this.frameBufferBloom.build(VkRenderPasses.passDeferred, displayWidth, displayHeight);
        FramebufferAttachment coloratt;
        
        for (int i = 0; i < 5; i++)
        {
            FramebufferAttachment att = this.frameBufferScene.getAtt(i);
            this.descTextureDeferred0.setBindingCombinedImageSampler(i, att.getView(), i>0&&i!=4?samplerNearest:samplerClamp, att.finalLayout);
        }
        this.descTextureDeferred0.setBindingCombinedImageSampler(5, RenderersVulkan.shadowRenderer.getView(), shadowSampler, RenderersVulkan.shadowRenderer.getLayout());
        this.descTextureDeferred0.setBindingCombinedImageSampler(6, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureDeferred0.update(ctxt);
        for (int i = 0; i < 5; i++)
        {
            FramebufferAttachment att = this.frameBufferSceneWater.getAtt(i);
            this.descTextureDeferredInput1.setBindingCombinedImageSampler(i, att.getView(), i>0&&i!=4?samplerNearest:samplerClamp, att.finalLayout);

        }
        this.descTextureDeferredInput1.setBindingCombinedImageSampler(5, RenderersVulkan.shadowRenderer.getView(), shadowSampler, RenderersVulkan.shadowRenderer.getLayout());
        this.descTextureDeferredInput1.setBindingCombinedImageSampler(6, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());

        FramebufferAttachment depth_att_pass_0 = this.frameBufferScene.getAtt(4);
        this.descTextureDeferredInput1.setBindingCombinedImageSampler(7, depth_att_pass_0.getView(), samplerClamp, depth_att_pass_0.finalLayout);
        VkTexture tex = RenderersVulkan.worldRenderer.getWaterNoiseTex();
        this.descTextureDeferredInput1.setBindingCombinedImageSampler(8, tex.getView(), ctxt.samplerLinear, tex.getImageLayout());
        this.descTextureDeferredInput1.update(ctxt);
        
        coloratt = this.frameBufferDeferredPass01.getAtt(0);
        this.descTextureSSRInput.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);

        for (int i = 1; i < 4; i++)
        {
            int attIdx = i;
            if (i == 3) attIdx = 4;
            FramebufferAttachment att = this.frameBufferSceneWater.getAtt(attIdx);
            this.descTextureSSRInput.setBindingCombinedImageSampler(i, att.getView(), samplerNearest, att.finalLayout);
        }
        coloratt = this.frameBufferScene.getAtt(4);
        this.descTextureSSRInput.setBindingCombinedImageSampler(4, coloratt.getView(), samplerClamp, coloratt.finalLayout);

        this.descTextureSSRInput.update(ctxt);

        coloratt = this.frameBufferSSR.getAtt(0);
        this.descTextureSSROutput.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        this.descTextureSSROutput.update(ctxt);
        
        
        coloratt = this.frameBufferDeferredPass01.getAtt(0);
        this.descTextureSSRCombineInput.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        
        coloratt = this.frameBufferSceneWater.getAtt(2);
        this.descTextureSSRCombineInput.setBindingCombinedImageSampler(1, coloratt.getView(), samplerNearest, coloratt.finalLayout);

        this.descTextureSSRCombineInput.update(ctxt);
        
        
        
        
        for (int i = 0; i < 5; i++)
        {
            FramebufferAttachment att = this.frameBufferSceneFirstPerson.getAtt(i);
            this.descTextureDeferredInput2.setBindingCombinedImageSampler(i, att.getView(), i>0?samplerNearest:samplerClamp, att.finalLayout);
        }
        this.descTextureDeferredInput2.setBindingCombinedImageSampler(5, RenderersVulkan.shadowRenderer.getView(), shadowSampler, RenderersVulkan.shadowRenderer.getLayout());
        this.descTextureDeferredInput2.setBindingCombinedImageSampler(6, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureDeferredInput2.update(ctxt);

        coloratt = this.frameBufferDeferred.getAtt(0);
        this.descTextureDeferredOut.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        this.descTextureDeferredOut.update(ctxt);

        coloratt = this.frameBufferSceneFirstPerson.getAtt(0);
        this.descTextureFirstPersonOut.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        this.descTextureFirstPersonOut.update(ctxt);

        coloratt = this.frameBufferDeferred.getAtt(0);
        this.descTextureBloomInput.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
               
        coloratt = this.frameBufferSceneWater.getAtt(3);
        this.descTextureBloomInput.setBindingCombinedImageSampler(1, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        this.descTextureBloomInput.setBindingCombinedImageSampler(2, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureBloomInput.update(ctxt);
        
        coloratt = this.frameBufferBloom.getAtt(0);
        this.descTextureBloomOut.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        this.descTextureBloomOut.update(ctxt);
        
        coloratt = this.frameBufferTonemapped.getAtt(0);
        this.descTextureFinalOut.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        this.descTextureFinalOut.update(ctxt);
        
        
        VkDescriptor lightComputeImages = RenderersVulkan.lightCompute.descriptorSetImages;

        coloratt = this.frameBufferScene.getAtt(1); //scene normals, pre water
        lightComputeImages.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        coloratt = this.frameBufferScene.getAtt(4); //scene depth, pre water
        lightComputeImages.setBindingCombinedImageSampler(1, coloratt.getView(), samplerClamp, coloratt.finalLayout);
        lightComputeImages.setBindingStorageImage(2, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getLayout());
        lightComputeImages.update(ctxt);
    }
    
    private void releaseFramebuffers() {
        if (this.frameBufferScene != null) {
            this.frameBufferScene.destroy();
        }
        if (this.frameBufferSceneWater != null) {
            this.frameBufferSceneWater.destroy();
        }
        if (this.frameBufferSceneFirstPerson != null) {
            this.frameBufferSceneFirstPerson.destroy();
        }
        if (this.frameBufferDeferred != null) {
            this.frameBufferDeferred.destroy();
        }
        if (this.frameBufferDeferredPass01 != null) {
            this.frameBufferDeferredPass01.destroy();
        }
        if (this.frameBufferTonemapped != null) {
            this.frameBufferTonemapped.destroy();
        }
        if (this.frameBufferBloom != null) {
            this.frameBufferBloom.destroy();
        }

        if (this.frameBufferSSR != null) {
            this.frameBufferSSR.destroy();
        }

        if (this.fbLuminanceDownsample != null) {
            for (int i = 0; i < fbLuminanceDownsample.length; i++) {
                this.fbLuminanceDownsample[i].destroy();
            }
            this.fbLuminanceDownsample = null;
        }
        if (this.fbLuminanceInterp != null) {
            for (int i = 0; i < fbLuminanceInterp.length; i++) {
                this.fbLuminanceInterp[i].destroy();
            }
        }
    }

    @Override
    public void release() {
        releaseFramebuffers();
        if (this.samplerClamp != VK_NULL_HANDLE) {
            vkDestroySampler(Engine.vkContext.device, this.samplerClamp, null);
            this.samplerClamp = VK_NULL_HANDLE;
        }
    }
    public void renderBloom() {
        FrameBuffer fb = this.frameBufferBloom;
        Engine.clearAllDescriptorSets();
        Engine.beginRenderPass(VkRenderPasses.passDeferred, fb, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.DESC0, this.descTextureBloomInput);
            Engine.bindPipeline(VkPipelines.post_bloom);
            Engine.drawFSTri();
        Engine.endRenderPass();
        VkDescriptor blurredBloom = RenderersVulkan.blurRenderer.renderBlur1PassDownsample(this.descTextureBloomOut);
        Engine.beginRenderPass(VkRenderPasses.passDeferred, fb, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.DESC0, this.descTextureDeferredOut);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, blurredBloom);
            Engine.bindPipeline(VkPipelines.post_bloom_combine);
            Engine.drawFSTri();
        Engine.endRenderPass();
    }

    public void calcLum() {
        Engine.clearAllDescriptorSets();
        VkDescriptor inputBuffer = this.descTextureDeferredOut;
        FrameBuffer top = this.fbLuminanceDownsample[0];

        float twoPixelX = 2.0f/(float)frameBufferDeferred.getWidth();
        float twoPixelY = 2.0f/(float)frameBufferDeferred.getHeight();
        Engine.beginRenderPass(VkRenderPasses.passDeferred, top, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setViewport(0, 0, top.getWidth(), top.getHeight()); // 4x downsampled render resolutino
    //        System.out.println("0, 0, "+top.getWidth()+", "+top.getHeight());
            Engine.setDescriptorSet(VkDescLayouts.DESC0, inputBuffer);
            Engine.bindPipeline(VkPipelines.post_downsample_pass0);
    
            PushConstantBuffer buf = PushConstantBuffer.INST;
            buf.setFloat(0, twoPixelX);
            buf.setFloat(1, twoPixelY);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.post_downsample_pass0.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(8));
    
            Engine.drawFSTri();
        Engine.endRenderPass();

        vkCmdPipelineBarrier(Engine.getDrawCmdBuffer(),
                VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 
                VK_DEPENDENCY_BY_REGION_BIT, VKContext.BARRIER_MEM_ATT_WRITE_SHADER_READ, null, null);
        FrameBuffer lastBound = this.fbLuminanceDownsample[0];
        for (int i = 0; i < this.fbLuminanceDownsample.length-1; i++) {
            
//            System.out.println("input w/h "+this.fbLuminanceDownsample[i].getWidth()+", "+this.fbLuminanceDownsample[i].getHeight());
            
            twoPixelX = 2.0f / (float) this.fbLuminanceDownsample[i].getWidth();
            twoPixelY = 2.0f / (float) this.fbLuminanceDownsample[i].getHeight();
            lastBound = this.fbLuminanceDownsample[i+1];
            Engine.beginRenderPass(VkRenderPasses.passDeferred, lastBound, VK_SUBPASS_CONTENTS_INLINE);
//          System.out.println("0, 0, "+lastBound.getWidth()+", "+lastBound.getHeight());
                Engine.setViewport(0, 0, lastBound.getWidth(), lastBound.getHeight()); // 16x downsampled render resolutino
                Engine.setDescriptorSet(VkDescLayouts.DESC0, this.descTextureDownsampleOutputs[i]);
                Engine.bindPipeline(VkPipelines.post_downsample_pass1);
                buf.setFloat(0, twoPixelX);
                buf.setFloat(1, twoPixelY);
                vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.post_downsample_pass1.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(8));
                Engine.drawFSTri();
            Engine.endRenderPass();
//            if (i == 0)
            vkCmdPipelineBarrier(Engine.getDrawCmdBuffer(),
                    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 
                    VK_DEPENDENCY_BY_REGION_BIT, VKContext.BARRIER_MEM_ATT_WRITE_SHADER_READ, null, null);

        }
        int indexIn = this.frame%2;
        int indexOut = 1-indexIn;
        
        FrameBuffer fblumInterp = this.fbLuminanceInterp[indexOut];
        vkCmdPipelineBarrier(Engine.getDrawCmdBuffer(),
                VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, 
                VK_DEPENDENCY_BY_REGION_BIT, VKContext.BARRIER_MEM_ATT_WRITE_SHADER_READ, null, null);
        Engine.beginRenderPass(VkRenderPasses.passDeferred, fblumInterp, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setViewport(0, 0, 1, 1); 
            Engine.setDescriptorSet(VkDescLayouts.DESC0, this.descLumInterpOutputsVertexInput[indexIn]);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureDownsampleEnd);
            Engine.bindPipeline(VkPipelines.post_lum_interp);
            buf.setFloat(0, (Stats.avgFrameTime)/100f);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.post_lum_interp.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(4));
    
            Engine.drawFSTri();
        Engine.endRenderPass();

        
    }
    private void renderSSR() {
        Engine.clearAllDescriptorSets();
        Engine.setViewport(0, 0, this.ssrSize[0], this.ssrSize[1]);
        Engine.beginRenderPass(VkRenderPasses.passDeferred, this.frameBufferSSR, VK_SUBPASS_CONTENTS_INLINE);
        Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);
        Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureSSRInput);
        Engine.setDescriptorSet(VkDescLayouts.DESC2, RenderersVulkan.skyRenderer.descTextureSkyboxCubemap);
        Engine.bindPipeline(VkPipelines.ssr);
        Engine.drawFSTri();  
        Engine.endRenderPass();
        
        
        Engine.setDefaultViewport();
        

        VkDescriptor blurred = RenderersVulkan.blurRenderer.renderBlurSeperate(descTextureSSROutput, 8);
        
        Engine.beginRenderPass(VkRenderPasses.passDeferred, this.frameBufferDeferred, VK_SUBPASS_CONTENTS_INLINE);
        Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);
        Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureSSRCombineInput);
        Engine.setDescriptorSet(VkDescLayouts.DESC2, descTextureSSROutput);
        Engine.bindPipeline(VkPipelines.ssrCombine);
        Engine.drawFSTri();  
        Engine.endRenderPass();
            
    }
    public void render(WorldClient world, float fTime) {
        renderDeferred(fTime);
    }
    public void renderDeferred(float fTime) {
        boolean ssr = Engine.RENDER_SETTINGS.ssr>0;
        FrameBuffer fb = ssr ? frameBufferDeferredPass01 : frameBufferDeferred;
        if (fb.getWidth() == Engine.fbWidth() && fb.getHeight() == Engine.fbHeight())
        {
            
            Engine.beginRenderPass(VkRenderPasses.passDeferred, fb, VK_SUBPASS_CONTENTS_INLINE);
            Engine.clearAllDescriptorSets();
            Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureDeferred0);
            Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboShadow);
            Engine.setDescriptorSet(VkDescLayouts.DESC3, Engine.descriptorSetUboLights);
            Engine.bindPipeline(VkPipelines.deferred_pass0);
            Engine.drawFSTri();            
            Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureDeferredInput1);
            Engine.bindPipeline(VkPipelines.deferred_pass1);
            Engine.drawFSTri();  
            Engine.endRenderPass();
            if (ssr){
                renderSSR();   
            }
            
            calcLum();
            Engine.setDefaultViewport();
            boolean firstPerson = !Game.instance.thirdPerson;
            if (firstPerson) {
                Engine.beginRenderPass(VkRenderPasses.passDeferredNoClear, frameBufferDeferred, VK_SUBPASS_CONTENTS_INLINE);
                Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);
                Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureDeferredInput2);
                Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboShadow);
                Engine.setDescriptorSet(VkDescLayouts.DESC3, Engine.descriptorSetUboLights);
                Engine.bindPipeline(VkPipelines.deferred_pass2);
                Engine.drawFSTri();
                Engine.endRenderPass();
                Engine.clearAllDescriptorSets();
            }
            renderBloom();

            Engine.clearAllDescriptorSets();
            Engine.beginRenderPass(VkRenderPasses.passTonemap, this.frameBufferTonemapped, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureBloomOut);
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descLumInterpOutputsCalcd[(this.frame+1)%2]);
            Engine.bindPipeline(VkPipelines.tonemapDynamic);
            Engine.drawFSTri();
            Engine.clearDescriptorSet(VkDescLayouts.DESC2);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureFirstPersonOut);
            Engine.bindPipeline(VkPipelines.texturedFullscreen);
            Engine.drawFSTri();
            Engine.endRenderPass();
            
            
            Engine.clearAllDescriptorSets();

            
            this.frame++;
        } else {
            System.err.println("SKIPPED, framebuffer is not sized");
            System.err.printf("%dx%d vs %dx%d\n", 
                    fb.getWidth(), fb.getHeight(),
                    Engine.displayWidth, Engine.displayHeight);

        }
        
    }
    
    @Override
    public void onSSRSettingChanged() {
        Engine.vkContext.reinitSwapchain=true;
    }


}
