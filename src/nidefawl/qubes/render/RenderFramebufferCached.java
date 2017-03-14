package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.system.MemoryStack.stackPush;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.util.Renderable;
import nidefawl.qubes.vulkan.*;

public class RenderFramebufferCached {
    private FrameBuffer fbDbg;
    private nidefawl.qubes.vulkan.FrameBuffer fbVk;
    private VkDescriptor descTextureGbufferColor;
    private long sampler;

    public RenderFramebufferCached() {
        
    }

    public void setSize(int w, int h) {
        if (!Engine.isVulkan) {
            if (fbDbg == null || fbDbg.getWidth() != w || fbDbg.getHeight() != h) {
                if (fbDbg != null) fbDbg.release();
                fbDbg = new FrameBuffer(w, h);
                fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16);
                fbDbg.setFilter(GL_COLOR_ATTACHMENT0, GL_NEAREST, GL_NEAREST);
                fbDbg.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
                fbDbg.setHasDepthAttachment();
                fbDbg.setup(null);
            }
        } else {
            if (fbVk != null && (fbVk.getWidth() != w || fbVk.getHeight() != h)) {
                fbVk.destroy();
                fbVk.build(VkRenderPasses.passFramebuffer, w, h);
                FramebufferAttachment coloratt = fbVk.getAtt(1);
                this.descTextureGbufferColor.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.imageLayout);
                this.descTextureGbufferColor.update(Engine.vkContext);
            }        

        }
    }

    public void render() {
        Engine.pxStack.push(0, 0, -20);
        if (Engine.isVulkan) {
            Engine.setDescriptorSet(1, this.descTextureGbufferColor);
        } else {
            GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, fbDbg.getTexture(0));
        }
        Engine.setPipeStateTextured2D();
        Engine.drawFullscreenQuad();
        Engine.pxStack.pop();
    }
    public void preRender(VkCommandBuffer commandBuffer) {
        if (Engine.isVulkan) {
            Engine.beginRenderPass(commandBuffer, VkRenderPasses.passFramebuffer, this.fbVk.get(), VK_SUBPASS_CONTENTS_INLINE);
        } else {
            fbDbg.bind();
            fbDbg.clearFrameBuffer();
            GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            Engine.setBlend(true);
        }
    }

    public void postRender(VkCommandBuffer commandBuffer) {
        if (Engine.isVulkan) {
            Engine.endRenderPass(commandBuffer);
        } else {
            GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            FrameBuffer.unbindFramebuffer();
        }
    }

    public void init() {
        if (Engine.isVulkan) {
            VKContext vkContext = Engine.vkContext;
            fbVk = new nidefawl.qubes.vulkan.FrameBuffer(vkContext);
            fbVk.fromRenderpass(VkRenderPasses.passFramebuffer, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
            try ( MemoryStack stack = stackPush() ) {
                VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
                LongBuffer pSampler = stack.longs(0);
                int err = vkCreateSampler(vkContext.device, sampler, null, pSampler);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
                }
                this.sampler = pSampler.get(0);
            }
            this.descTextureGbufferColor = vkContext.descLayouts.allocDescSetSampleSingle();
        }
    
    }

    public void clearImage(VkCommandBuffer commandbuffer) {
        if (Engine.isVulkan) {
            this.fbVk.clearColorAtt(1, commandbuffer, 0, 0, 0, 1);
        }
    }
    
}
