package nidefawl.qubes.gui;

import static nidefawl.qubes.gl.Engine.isVulkan;
import static nidefawl.qubes.gl.Engine.vkContext;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.ITess;
import nidefawl.qubes.vulkan.*;

public class LoadingScreen {
    InitCrap initCrap;
    private boolean[] recorded;
    static class InitCrap {
        VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pNext(NULL).flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
        private VkCommandBuffer[] renderCommandBuffers;
        public InitCrap(VKContext vkContext) {
            int nFrameBuffers = vkContext.swapChain.numImages;
            try ( MemoryStack stack = stackPush() ) {
                VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkInitializers.commandBufferAllocInfo(
                        vkContext.renderCommandPool, nFrameBuffers);
            
                PointerBuffer pCommandBuffer = stack.callocPointer(nFrameBuffers);
                int err = vkAllocateCommandBuffers(vkContext.device, cmdBufAllocateInfo, pCommandBuffer);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("Failed to allocate render command buffer: " + VulkanErr.toString(err));
                }
                renderCommandBuffers = new VkCommandBuffer[nFrameBuffers];
                for (int i = 0; i < nFrameBuffers; i++) {
                    renderCommandBuffers[i] = new VkCommandBuffer(pCommandBuffer.get(i), vkContext.device);
                }
            }
        }
        void destroy() {
            cmdBufInfo.free();
        }
    }
    public LoadingScreen() {
    }
    
    final float[] loadProgress = new float[2];

    public boolean render(int step, float f) {
        return setProgress(step, f, "");
    }

    public boolean setProgress(int step, float f, String string) {
        GameBase g = GameBase.baseInstance;
        if (g.isCloseRequested()) {
            g.shutdown();
            return false;
        }
        int tw = Engine.getGuiWidth();
        int th = Engine.getGuiHeight();
        if (step > loadProgress.length - 1) {
            System.err.println("step > loadprogress-1!");
            step = loadProgress.length - 1;
        }
        int oldW = (int) (tw * 0.6f * loadProgress[step]);
        int newW = (int) (tw * 0.6f * (f));
        float fd = Math.abs(newW - oldW);
        if (fd < 15f && f < 1 && f > 0)
            return false;
        loadProgress[step] = f;
        if (isVulkan) {
            if (initCrap == null) {
                initCrap = new InitCrap(vkContext);
            }
            vkContext.finishUpload();
            Engine.preRenderUpdateVK();
            vkContext.preRender();
            long framebuffer = vkContext.swapChain.framebuffers[VKContext.currentBuffer];
            int width = tw = vkContext.swapChain.width;
            int height = th = vkContext.swapChain.height;
            VkCommandBuffer commandBuffer = initCrap.renderCommandBuffers[VKContext.currentBuffer];
            int err = vkBeginCommandBuffer(commandBuffer, initCrap.cmdBufInfo);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to begin render command buffer: " + VulkanErr.toString(err));
            }
            Engine.setViewport(0, 0, width, height);
            Engine.beginRenderPass(commandBuffer, VkRenderPasses.passSubpassSwapchain, framebuffer, VK_SUBPASS_CONTENTS_INLINE);
            vkCmdNextSubpass(commandBuffer, VK_SUBPASS_CONTENTS_INLINE);
            
        }
        boolean b = renderProgress(tw, th, step, f, string);
        if (isVulkan) {
            VkCommandBuffer commandBuffer = initCrap.renderCommandBuffers[VKContext.currentBuffer];
            vkCmdEndRenderPass(commandBuffer);
            
            int err = vkEndCommandBuffer(commandBuffer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to end render command buffer: " + VulkanErr.toString(err));
            }
            vkContext.submitCommandBuffer(commandBuffer);
            vkContext.finishUpload();
            vkContext.postRender();
        }
        return b;
    }
    public boolean renderProgress(int tw, int th, int step, float f, String string) {
        int nzero = 0;
        for (int i = 0; i < loadProgress.length; i++) {
            if (loadProgress[i] == 0)
                nzero++;
            else if (nzero > 0 && loadProgress[i] > 0) {
                throw new GameError("out of order");
            }
        }
        float x = 0;
        float y = 0;
        float l = tw * 0.2f;
        float barsH = 32;
        float barsTop = (th - barsH) / 2.0f;
        Engine.updateOrthoMatrix(tw, th);
        UniformBuffer.updateOrtho();
        if (!Engine.isVulkan) {

            glClearColor(0, 0, 0, 0F);
            glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("loadRender glClear");
        }
        Engine.setPipeStateColored2D();
        ITess tess = Engine.getTess();
        tess.resetState();
        tess.setColor(0, 0xff);
        tess.add(x + tw, y, 0);
        tess.add(x, y, 0);
        tess.add(x, y + th, 0);
        tess.add(x + tw, y + th, 0);
        tess.drawQuads();
        float p = 0;
        for (int i = 0; i < loadProgress.length; i++) {
            p += loadProgress[i];
        }
        p /= (float) loadProgress.length;
        float w = tw * 0.6f * p;
        tess.setColor(0xffffff, 0xff);
        tess.add(x + l + w, y + barsTop - 2, 0);
        tess.add(x + l, y + barsTop - 2, 0);
        tess.add(x + l, y + barsTop + barsH + 2, 0);
        tess.add(x + l + w, y + barsTop + barsH + 2, 0);
        tess.drawQuads();
        FontRenderer font = FontRenderer.get(0, 16, 1);
        if (font != null) {
            Engine.setPipeStateFontrenderer();
            font.drawString(string, x + l + 2, y + barsTop + barsH + 10 + font.getLineHeight(), -1, true, 1.0f);
        }
        //        for (int i = 0; i < loadProgress.length; i++) {
        //            
        //             w = tw*0.6f*loadProgress[i];
        //            tess.setColor(0x666666, 0xff);
        //            tess.add(x + l + w, y + barsTop, 0, 1, 1);
        //            tess.add(x + l, y + barsTop, 0, 0, 1);
        //            tess.add(x + l, y + barsTop+barh, 0, 0, 0);
        //            tess.add(x + l + w, y + barsTop+barh, 0, 1, 0);
        //            tess.drawQuads();
        //            barsTop+=barh+2;
        //        }
        GameBase g = GameBase.baseInstance;
        g.updateDisplay();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("loadRender updateDisplay");
        return true;
    }
}
