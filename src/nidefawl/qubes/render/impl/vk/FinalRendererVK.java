package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.FinalRenderer;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.*;

public class FinalRendererVK extends FinalRenderer implements IRenderComponent {

    public FrameBuffer frameBufferScene;
    public VkDescriptor descTextureGbufferColor;
    public long sampler;
    @Override
    public void init() {
        this.frameBufferScene = new FrameBuffer(Engine.vkContext);
        this.frameBufferScene.fromRenderpass(VkRenderPasses.passTerrain, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
        this.descTextureGbufferColor = Engine.vkContext.descLayouts.allocDescSetSampleSingle();
        try ( MemoryStack stack = stackPush() ) {
            VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
            LongBuffer pSampler = stack.longs(0);
            int err = vkCreateSampler(Engine.vkContext.device, sampler, null, pSampler);
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
        this.frameBufferScene.build(VkRenderPasses.passTerrain, displayWidth, displayHeight);
        FramebufferAttachment coloratt = this.frameBufferScene.getAtt(0);
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

}
