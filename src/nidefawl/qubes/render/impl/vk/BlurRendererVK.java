package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.BlurRenderer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.*;

public class BlurRendererVK extends BlurRenderer implements IRenderComponent {

    private FrameBuffer frameBufferKawase1;
    private FrameBuffer frameBufferKawase2;
    private VkDescriptor descTexture1;
    private VkDescriptor descTexture2;
    private long samplerClamp;

    @Override
    public void init() {
        VKContext ctxt = Engine.vkContext;
        this.frameBufferKawase1 = new FrameBuffer(ctxt).tag("Kawase1");
        this.frameBufferKawase1.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.frameBufferKawase2 = new FrameBuffer(ctxt).tag("Kawase2");
        this.frameBufferKawase2.fromRenderpass(VkRenderPasses.passDeferred, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.descTexture1 = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTexture2 = ctxt.descLayouts.allocDescSetSampleSingle();
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
        }
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        initBlurKawase(displayWidth, displayHeight, 2);
    }

    public void initBlurKawase(int inputWidth, int inputHeight, int blurDownSample) {
        if (this.frameBufferKawase1 != null) {
            VKContext ctxt = Engine.vkContext;
            this.frameBufferKawase1.destroy();
            this.frameBufferKawase2.destroy();
            this.w1 = 1.0f/(float)inputWidth;
            this.h1 = 1.0f/(float)inputHeight;
            int[] blurSize = GameMath.downsample(inputWidth, inputHeight, blurDownSample);
            this.frameBufferKawase1.build(VkRenderPasses.passDeferred, blurSize[0], blurSize[1]);
            this.frameBufferKawase2.build(VkRenderPasses.passDeferred, blurSize[0], blurSize[1]);
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
            Engine.beginRenderPass(VkRenderPasses.passDeferred, buffer, VK_SUBPASS_CONTENTS_INLINE);
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
        }
        return descInput;
    }

}
