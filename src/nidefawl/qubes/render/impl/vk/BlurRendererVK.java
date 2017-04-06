package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.BlurRenderer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.*;

public class BlurRendererVK extends BlurRenderer implements IRenderComponent {

    private FrameBuffer fbSSRBlurredX;
    private FrameBuffer fbSSRBlurredY;
    private FrameBuffer frameBufferKawase1;
    private FrameBuffer frameBufferKawase2;
    private VkDescriptor descTexture1;
    private VkDescriptor descTexture2;
    private long samplerClamp;
    private VkDescriptor descTextureBlurredX;
    private VkDescriptor descTextureBlurredY;

    @Override
    public void init() {
        VKContext ctxt = Engine.vkContext;
        this.frameBufferKawase1 = new FrameBuffer(ctxt).tag("Kawase1");
        this.frameBufferKawase1.fromRenderpass(VkRenderPasses.passPostRGBA16F, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferKawase2 = new FrameBuffer(ctxt).tag("Kawase2");
        this.frameBufferKawase2.fromRenderpass(VkRenderPasses.passPostRGBA16F, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.fbSSRBlurredX = new FrameBuffer(ctxt).tag("SSRBlurredX");
        this.fbSSRBlurredX.fromRenderpass(VkRenderPasses.passPostRGBA16F, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.fbSSRBlurredY = new FrameBuffer(ctxt).tag("SSRBlurredY");
        this.fbSSRBlurredY.fromRenderpass(VkRenderPasses.passPostRGBA16F, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.descTexture1 = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTexture2 = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureBlurredX = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureBlurredY = ctxt.descLayouts.allocDescSetSampleSingle();
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
            if (GameBase.DEBUG_LAYER) {
                VkDebug.registerSampler(this.samplerClamp);
            }
            
        }
    }
    @Override
    public void release() {
        if (this.samplerClamp != VK_NULL_HANDLE) {
            VKContext ctxt = Engine.vkContext;
            vkDestroySampler(ctxt.device, this.samplerClamp, null);
            this.samplerClamp = VK_NULL_HANDLE;
        }
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        initBlurKawase(displayWidth, displayHeight, 2);
        initBlurSeperate(displayWidth, displayHeight, 2);
    }
    public void initBlurSeperate(int displayWidth, int displayHeight, int blurDownSample) {
        if (this.fbSSRBlurredX != null) {
            VKContext ctxt = Engine.vkContext;
            this.fbSSRBlurredX.build(VkRenderPasses.passPostRGBA16F, displayWidth, displayHeight);
            this.fbSSRBlurredY.build(VkRenderPasses.passPostRGBA16F, displayWidth, displayHeight);
            FramebufferAttachment coloratt;
            coloratt = this.fbSSRBlurredX.getAtt(0);
            this.descTextureBlurredX.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
            this.descTextureBlurredX.update(ctxt);
            coloratt = this.fbSSRBlurredY.getAtt(0);
            this.descTextureBlurredY.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
            this.descTextureBlurredY.update(ctxt);
        }
    }

    public void initBlurKawase(int inputWidth, int inputHeight, int blurDownSample) {
        if (this.frameBufferKawase1 != null) {
            VKContext ctxt = Engine.vkContext;
            this.frameBufferKawase1.destroy();
            this.frameBufferKawase2.destroy();
            this.w1 = 1.0f/(float)inputWidth;
            this.h1 = 1.0f/(float)inputHeight;
            int[] blurSize = GameMath.downsample(inputWidth, inputHeight, blurDownSample);
            this.frameBufferKawase1.build(VkRenderPasses.passPostRGBA16F, blurSize[0], blurSize[1]);
            this.frameBufferKawase2.build(VkRenderPasses.passPostRGBA16F, blurSize[0], blurSize[1]);
            FramebufferAttachment coloratt;
            coloratt = this.frameBufferKawase1.getAtt(0);
            this.descTexture1.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
            this.descTexture1.update(ctxt);
            coloratt = this.frameBufferKawase2.getAtt(0);
            this.descTexture2.setBindingCombinedImageSampler(0, coloratt.getView(), samplerClamp, coloratt.finalLayout);
            this.descTexture2.update(ctxt);
        }
    }

    public VkDescriptor renderBlur1PassDownsample(VkDescriptor input) {
        Engine.clearAllDescriptorSets();
        FrameBuffer buffer = this.frameBufferKawase1; // 4x downsampled
        VkDescriptor descInput = input;
        
        int kawaseKernSizeSetting = 2;
        int[] kawaseKernPasses = kawaseKernelSizePasses[kawaseKernSizeSetting];
        for (int p = 0; p < kawaseKernPasses.length; p++) {
            Engine.beginRenderPass(VkRenderPasses.passPostRGBA16F, buffer);
                Engine.setViewport(0, 0, buffer.getWidth(), buffer.getHeight());
                Engine.setDescriptorSet(VkDescLayouts.DESC0, descInput);
                Engine.bindPipeline(VkPipelines.filter_blur_kawase);
                PushConstantBuffer buf = PushConstantBuffer.INST;
                buf.setFloat(0, w1);
                buf.setFloat(1, h1);
                buf.setFloat(2, kawaseKernPasses[p]);
                vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.filter_blur_kawase.getLayoutHandle(), VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf.getBuf(12));
                Engine.drawFSTri();
            Engine.endRenderPass();
            descInput = buffer == this.frameBufferKawase1 ? this.descTexture1 : this.descTexture2;
            buffer = buffer == this.frameBufferKawase1 ? this.frameBufferKawase2 : this.frameBufferKawase1;
            //TODO: pipeline barrier
        }
        return descInput;
    }

    public VkDescriptor renderBlurSeperate(VkDescriptor descInput, int i) {
        float maxBlurRadius = i;
        Engine.clearAllDescriptorSets();
        Engine.beginRenderPass(VkRenderPasses.passPostRGBA16F, fbSSRBlurredX);
        Engine.setDescriptorSet(VkDescLayouts.DESC0, descInput);
        Engine.bindPipeline(VkPipelines.filter_blur_seperate);
        PushConstantBuffer buf = PushConstantBuffer.INST;
        buf.setFloat(0, maxBlurRadius / (float)fbSSRBlurredX.getWidth());
        buf.setFloat(1, 0f);
        vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.filter_blur_seperate.getLayoutHandle(), VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf.getBuf(8));
        Engine.drawFSTri();
        Engine.endRenderPass();
        //TODO: pipeline barrier
        Engine.beginRenderPass(VkRenderPasses.passPostRGBA16F, fbSSRBlurredY);
        Engine.setDescriptorSet(VkDescLayouts.DESC0, descTextureBlurredX);
        Engine.bindPipeline(VkPipelines.filter_blur_seperate);
        buf.setFloat(0, 0f);
        buf.setFloat(1, maxBlurRadius / (float)fbSSRBlurredY.getHeight());
        vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.filter_blur_seperate.getLayoutHandle(), VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf.getBuf(8));
        Engine.drawFSTri();
        Engine.endRenderPass();
        //TODO: pipeline barrier
        return descTextureBlurredY;
    }
}
