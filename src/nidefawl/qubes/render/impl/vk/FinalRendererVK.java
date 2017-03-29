package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkMemoryBarrier;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.GLDebugTextures;
import nidefawl.qubes.perf.GPUProfiler;
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
    public VkDescriptor descTextureDownsampleInput;
    public VkDescriptor[] descTextureDownsampleOutputs = new VkDescriptor[10];
    public VkDescriptor descTextureDownsampleEnd;
    public VkDescriptor[] descLumInterpOutputsVertexInput = new VkDescriptor[2];
    public VkDescriptor[] descLumInterpOutputsCalcd = new VkDescriptor[2];
    public VkDescriptor descTextureDbg;
    public VkDescriptor descTextureFinalOut;
    public VkDescriptor descTextureDeferred0;
    public VkDescriptor descTextureDeferred1;
    public VkDescriptor descTextureDeferred2;
    public long sampler;
    public FrameBuffer frameBufferTonemapped;
    public FrameBuffer[] fbLuminanceDownsample;
    public VkDescriptor descTextureTonemap;
    private FrameBuffer[] fbLuminanceInterp;
    private int frame;
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
        this.frameBufferSceneFirstPerson = new FrameBuffer(ctxt).tag("scene_pass2");
        this.frameBufferSceneFirstPerson.fromRenderpass(VkRenderPasses.passTerrain_Pass2, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferDeferred = new FrameBuffer(ctxt).tag("deferred");
        this.frameBufferDeferred.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferTonemapped = new FrameBuffer(ctxt).tag("tonemap");
        this.frameBufferTonemapped.fromRenderpass(VkRenderPasses.passFramebufferNoDepth, 0, VK_IMAGE_USAGE_SAMPLED_BIT);

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
        this.descTextureDownsampleInput = ctxt.descLayouts.allocDescSetSampleSingle().tag("DownsampleInputSingle");
        this.descTextureFinalOut = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureDeferred0 = ctxt.descLayouts.allocDescSetSamplerDeferredPass0();
        this.descTextureDeferred1 = ctxt.descLayouts.allocDescSetSamplerDeferredPass1();
        this.descTextureDeferred2 = ctxt.descLayouts.allocDescSetSamplerDeferredPass0();
        this.descTextureTonemap = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureDbg = ctxt.descLayouts.allocDescSetSampleSingle();
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
        VKContext ctxt = Engine.vkContext;
        if (this.frameBufferScene != null) {
            this.frameBufferScene.destroy();
        }
//        if (this.frameBufferSceneColorOnly != null) {
//            this.frameBufferSceneColorOnly.destroy();
//        }
        if (this.frameBufferSceneWater != null) {
            this.frameBufferSceneWater.destroy();
        }
        if (this.frameBufferSceneFirstPerson != null) {
            this.frameBufferSceneFirstPerson.destroy();
        }
        if (this.frameBufferDeferred != null) {
            this.frameBufferDeferred.destroy();
        }
        if (this.frameBufferTonemapped != null) {
            this.frameBufferTonemapped.destroy();
        }
        
        if (this.fbLuminanceDownsample != null) {
            for (int i = 0; i < fbLuminanceDownsample.length; i++) {
                this.fbLuminanceDownsample[i].destroy();
            }
            this.fbLuminanceDownsample = null;
        }

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
            this.descTextureDownsampleOutputs[idx].setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
            this.descTextureDownsampleOutputs[idx].update(ctxt);
            
            list.add(fbLuminance2);
            if (lumW == 1 && lumH == 1) {
                this.descTextureDownsampleEnd.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
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
            this.descLumInterpOutputsVertexInput[i].setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
            this.descLumInterpOutputsVertexInput[i].update(ctxt);
            this.descLumInterpOutputsCalcd[i].setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
            this.descLumInterpOutputsCalcd[i].update(ctxt);
        }
        
        
        
        this.frameBufferScene.build(VkRenderPasses.passTerrain_Pass0, displayWidth, displayHeight);
//        this.frameBufferSceneColorOnly.build(VkRenderPasses.passSkySample, displayWidth, displayHeight);
        this.frameBufferSceneWater.build(VkRenderPasses.passTerrain_Pass1, displayWidth, displayHeight);
        this.frameBufferSceneFirstPerson.build(VkRenderPasses.passTerrain_Pass2, displayWidth, displayHeight);
        this.frameBufferDeferred.build(VkRenderPasses.passDeferred, displayWidth, displayHeight);
        this.frameBufferTonemapped.build(VkRenderPasses.passFramebufferNoDepth, displayWidth, displayHeight);
        for (int i = 0; i < 5; i++)
        {
            FramebufferAttachment att = this.frameBufferScene.getAtt(i);
            this.descTextureDeferred0.setBindingCombinedImageSampler(i, att.getView(), sampler, att.finalLayout);
        }
        this.descTextureDeferred0.setBindingCombinedImageSampler(5, RenderersVulkan.shadowRenderer.getView(), RenderersVulkan.shadowRenderer.getSampler(), RenderersVulkan.shadowRenderer.getLayout());
        this.descTextureDeferred0.setBindingCombinedImageSampler(6, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureDeferred0.update(ctxt);
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
                tex.getView(), ctxt.samplerLinear, tex.getImageLayout());
        this.descTextureDeferred1.update(ctxt);
        
        for (int i = 0; i < 5; i++)
        {
            FramebufferAttachment att = this.frameBufferSceneFirstPerson.getAtt(i);
            this.descTextureDeferred2.setBindingCombinedImageSampler(i, att.getView(), sampler, att.finalLayout);
        }
        this.descTextureDeferred2.setBindingCombinedImageSampler(5, RenderersVulkan.shadowRenderer.getView(), RenderersVulkan.shadowRenderer.getSampler(), RenderersVulkan.shadowRenderer.getLayout());
        this.descTextureDeferred2.setBindingCombinedImageSampler(6, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureDeferred2.update(ctxt);
        FramebufferAttachment coloratt;
        
        this.descTextureDbg.setBindingCombinedImageSampler(0, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureDbg.update(ctxt);
        
        coloratt = this.frameBufferDeferred.getAtt(0);
        this.descTextureDownsampleInput.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
        this.descTextureDownsampleInput.update(ctxt);
        
        coloratt = this.frameBufferDeferred.getAtt(0);
        this.descTextureTonemap.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
        this.descTextureTonemap.update(ctxt);
        
        coloratt = this.frameBufferTonemapped.getAtt(0);
        this.descTextureFinalOut.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
        this.descTextureFinalOut.update(ctxt);
    }
    
    @Override
    public void release() {
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
        if (this.frameBufferTonemapped != null) {
            this.frameBufferTonemapped.destroy();
        }
        if (this.sampler != VK_NULL_HANDLE) {
            vkDestroySampler(Engine.vkContext.device, this.sampler, null);
            this.sampler = VK_NULL_HANDLE;
        }
    }

    public void calcLum() {
        Engine.unbindSceneDescriptorSets();
        VkDescriptor inputBuffer = this.descTextureDownsampleInput;
        FrameBuffer top = this.fbLuminanceDownsample[0];

        float twoPixelX = 2.0f/(float)frameBufferDeferred.getWidth();
        float twoPixelY = 2.0f/(float)frameBufferDeferred.getHeight();
        Engine.beginRenderPass(VkRenderPasses.passDeferred, top, VK_SUBPASS_CONTENTS_INLINE);
            Engine.clearAllDescriptorSets();
            Engine.setDescriptorSet(VkDescLayouts.DESC0, inputBuffer);
            Engine.setViewport(0, 0, top.getWidth(), top.getHeight()); // 4x downsampled render resolutino
    //        System.out.println("0, 0, "+top.getWidth()+", "+top.getHeight());
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

        Engine.rebindSceneDescriptorSets();
        Engine.setDefaultViewport();
        
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
            boolean firstPerson = !Game.instance.thirdPerson;
            if (firstPerson) {
                Engine.endRenderPass();
            }
            calcLum();
            if (firstPerson) {
                Engine.beginRenderPass(VkRenderPasses.passDeferredNoClear, fb, VK_SUBPASS_CONTENTS_INLINE);
                Engine.setDescriptorSet(VkDescLayouts.DESC3, Engine.descriptorSetUboShadow);
                Engine.setDescriptorSet(VkDescLayouts.DESC4, Engine.descriptorSetUboLights);
                Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureDeferred2);
                Engine.bindPipeline(VkPipelines.deferred_pass2);
                Engine.drawFSTri();
                Engine.endRenderPass();
            }
            
            Engine.clearDescriptorSet(VkDescLayouts.DESC3);
            Engine.clearDescriptorSet(VkDescLayouts.DESC4);
            Engine.beginRenderPass(VkRenderPasses.passFramebufferNoDepth, this.frameBufferTonemapped, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureTonemap);
            Engine.setDescriptorSet(VkDescLayouts.DESC3, this.descLumInterpOutputsCalcd[(this.frame+1)%2]);
            Engine.bindPipeline(VkPipelines.tonemapDynamic);
            Engine.drawFSTri();
            Engine.clearDescriptorSet(VkDescLayouts.DESC3);
//            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureDownsampleOutputs[0]);
//            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureDbg);
//            Engine.bindPipeline(VkPipelines.textured2d, 1);
//            int prX = 20;
//            int prY = prX;
//            int pw = 1024;
//            int ph = pw;
//            ITess tess = Engine.getTess(); 
//            tess.setColorF(-1, 1);
//            tess.add(prX, prY+ph, 0, 0, 1);
//            tess.add(prX+pw, prY+ph, 0, 1, 1);
//            tess.add(prX+pw, prY, 0, 1.0f, 0.0f);
//            tess.add(prX, prY, 0, 0.0f, 0.0f);
//            tess.drawQuads();
            

            Engine.endRenderPass();

            this.frame++;
        } else {
            System.err.println("SKIPPED, framebuffer is not sized");
            System.err.printf("%dx%d vs %dx%d\n", 
                    fb.getWidth(), fb.getHeight(),
                    Engine.displayWidth, Engine.displayHeight);

        }
        
    }

}
