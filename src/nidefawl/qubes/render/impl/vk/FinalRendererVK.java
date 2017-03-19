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
    private FrameBuffer frameBufferDeferred;
    public VkDescriptor descTextureGbufferColor;
    public VkDescriptor descTextureDeferred;
    public long sampler;
    private FrameBuffer frameBufferTonemapped;
    private VkDescriptor descTextureTonemap;
    @Override
    public void init() {
        VKContext ctxt = Engine.vkContext;
        this.frameBufferScene = new FrameBuffer(ctxt);
        this.frameBufferScene.fromRenderpass(VkRenderPasses.passTerrain, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferDeferred = new FrameBuffer(ctxt);
        this.frameBufferDeferred.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferTonemapped = new FrameBuffer(ctxt);
        this.frameBufferTonemapped.fromRenderpass(VkRenderPasses.passFramebufferNoDepth, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.descTextureGbufferColor = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureDeferred = ctxt.descLayouts.allocDescSetSamplerDeferred();
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
        if (this.frameBufferDeferred != null) {
            this.frameBufferDeferred.destroy();
        }
        if (this.frameBufferTonemapped != null) {
            this.frameBufferTonemapped.destroy();
        }
        this.frameBufferScene.build(VkRenderPasses.passTerrain, displayWidth, displayHeight);
        this.frameBufferDeferred.build(VkRenderPasses.passDeferred, displayWidth, displayHeight);
        this.frameBufferTonemapped.build(VkRenderPasses.passFramebufferNoDepth, displayWidth, displayHeight);
        for (int i = 0; i < 5; i++)
        {
            FramebufferAttachment att = this.frameBufferScene.getAtt(i);
            this.descTextureDeferred.setBindingCombinedImageSampler(i, att.getView(), sampler, att.imageLayout);

        }
        this.descTextureDeferred.setBindingCombinedImageSampler(5, RenderersVulkan.shadowRenderer.getView(), RenderersVulkan.shadowRenderer.getSampler(), RenderersVulkan.shadowRenderer.getLayout());
        this.descTextureDeferred.setBindingCombinedImageSampler(6, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureDeferred.update(Engine.vkContext);
        FramebufferAttachment coloratt = this.frameBufferDeferred.getAtt(0);
        this.descTextureTonemap.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.imageLayout);
        this.descTextureTonemap.setBindingCombinedImageSampler(1, RenderersVulkan.lightCompute.getView(), RenderersVulkan.lightCompute.getSampler(), RenderersVulkan.lightCompute.getLayout());
        this.descTextureTonemap.update(Engine.vkContext);
        coloratt = this.frameBufferTonemapped.getAtt(0);
        this.descTextureGbufferColor.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.imageLayout);
        this.descTextureGbufferColor.update(Engine.vkContext);
    }
    
    @Override
    public void release() {
        super.release();
        if (this.sampler != VK_NULL_HANDLE) {
            vkDestroySampler(Engine.vkContext.device, this.sampler, null);
            this.sampler = VK_NULL_HANDLE;
        }
    }

    public void render(WorldClient world, float fTime, int i) {
        renderDeferred(fTime, i);
    }
    public void renderDeferred(float fTime, int pass) {
        FrameBuffer fb = frameBufferDeferred;
        if (fb.getWidth() == Engine.fbWidth() && fb.getHeight() == Engine.fbHeight())
        {
            
            Engine.beginRenderPass(VkRenderPasses.passDeferred, fb, VK_SUBPASS_CONTENTS_INLINE);
//            
            Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureDeferred);
            Engine.setDescriptorSet(VkDescLayouts.UBO_CONSTANTS_DESC_IDX, Engine.descriptorSetUboShadow);
            Engine.setDescriptorSet(VkDescLayouts.UBO_LIGHTS_DESC_IDX, Engine.descriptorSetUboLights);
            Engine.bindPipeline(VkPipelines.deferred);
            Engine.drawFSTri();
            Engine.endRenderPass();
            Engine.clearDescriptorSet(VkDescLayouts.UBO_CONSTANTS_DESC_IDX);
            Engine.clearDescriptorSet(VkDescLayouts.UBO_LIGHTS_DESC_IDX);
            Engine.beginRenderPass(VkRenderPasses.passFramebufferNoDepth, this.frameBufferTonemapped, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureTonemap);
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
