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

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.vulkan.*;

public class RenderFramebufferCached {
    private FrameBuffer fbDbg;
    private nidefawl.qubes.vulkan.FrameBuffer fbVk;
    private VkDescriptor descTextureGbufferColor;
    private boolean color16Bit;
    private boolean filterLinear;
    private boolean blendEnabled;
    private boolean clearOnUpdate;
    private String tag;

    public RenderFramebufferCached(boolean color16Bit, boolean filterLinear, boolean clearOnUpdate) {
        this.color16Bit = color16Bit;
        this.filterLinear = filterLinear;
        this.clearOnUpdate = clearOnUpdate;
    }

    public void setSize(int w, int h) {
        if (!Engine.isVulkan) {
            if (fbDbg == null || fbDbg.getWidth() != w || fbDbg.getHeight() != h) {
                if (fbDbg != null) fbDbg.destroy();
                fbDbg = new FrameBuffer(w, h);
                fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, color16Bit ? GL_RGBA16 : GL_RGBA8);
                int filter = this.filterLinear ? GL_LINEAR : GL_NEAREST;
                fbDbg.setFilter(GL_COLOR_ATTACHMENT0, filter, filter);
                fbDbg.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
                fbDbg.setHasDepthAttachment();
                fbDbg.setup(null);
            }
        } else {
            if (fbVk == null || (fbVk.getWidth() != w || fbVk.getHeight() != h)) {
                VKContext vkContext = Engine.vkContext;
                vkContext.orphanResource(this.fbVk);
                fbVk = new nidefawl.qubes.vulkan.FrameBuffer(vkContext).tag(this.tag);
                fbVk.fromRenderpass(VkRenderPasses.passFramebuffer, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
                fbVk.build(VkRenderPasses.passFramebuffer, w, h);
                FramebufferAttachment coloratt = fbVk.getAtt(0);
                this.descTextureGbufferColor.setBindingCombinedImageSampler(0, coloratt.getView(), filterLinear?vkContext.samplerLinear:vkContext.samplerNearest, coloratt.finalLayout);
                this.descTextureGbufferColor.update(Engine.vkContext);
            }        

        }
    }
    public void bindTextureDescriptor() {
        if (Engine.isVulkan) {
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureGbufferColor);
        } else {
            GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, fbDbg.getTexture(0));
        }
    }
    public void render() {
        Engine.pxStack.push(0, 0, -20);
        if (Engine.isVulkan) {
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureGbufferColor);
        } else {
            GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, fbDbg.getTexture(0));
        }
        Engine.setPipeStateTextured2D();
        Engine.drawFullscreenQuad();
        Engine.pxStack.pop();
    }
    public void preRender(boolean blend) {
        if (Engine.isVulkan) {
            Engine.beginRenderPass(clearOnUpdate?VkRenderPasses.passFramebuffer:VkRenderPasses.passFramebufferNoClear, this.fbVk);
        } else {
            fbDbg.bind();
            if (clearOnUpdate)
                fbDbg.clearFrameBuffer();
            if (blend) {
                GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                Engine.setBlend(true);
            }
            this.blendEnabled = blend;
        }
    }

    public void postRender() {
        if (Engine.isVulkan) {
            Engine.endRenderPass();
        } else {
            if (this.blendEnabled) {
                GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);    
                this.blendEnabled = false;
            }
            
            FrameBuffer.unbindFramebuffer();
        }
    }

    public void init() {
        if (Engine.isVulkan) {
            VKContext vkContext = Engine.vkContext;
            this.descTextureGbufferColor = vkContext.descLayouts.allocDescSetSampleSingle();
        }
    
    }

    public void clear() {
        if (Engine.isVulkan) {

            Engine.beginRenderPass(VkRenderPasses.passFramebuffer, this.fbVk);
            Engine.endRenderPass();
        } else {
            fbDbg.bind();
            this.fbDbg.clearFrameBuffer();
            FrameBuffer.unbindFramebuffer();
        }
    }
    public void destroy() {
        if (this.fbVk != null) {
            Engine.vkContext.orphanResource(this.fbVk);
            this.fbVk = null;
        }
        if (this.fbDbg != null) {
            this.fbDbg.destroy();
            this.fbDbg = null;
        }
    }

    public RenderFramebufferCached tag(String tag) {
        this.tag = tag;
        return this;
    }
    
}
