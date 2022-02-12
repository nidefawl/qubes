package nidefawl.qubes.render.impl.vk;

import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkViewport;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.render.ShadowRenderer;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.World;

public class ShadowRendererVK extends ShadowRenderer {

    private FrameBuffer frameBufferShadow;
    VkViewport.Buffer viewport = VkViewport.calloc(1);
    private long sampler;
    
    @Override
    public void renderShadowPass(World world, float fTime) {
        CommandBuffer commandBuffer = Engine.getDrawCmdBuffer();
        Engine.updateRenderResolution(SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
        Engine.setViewport(0, 0, SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);

        if (this.frameBufferShadow.getWidth() == SHADOW_BUFFER_SIZE&&this.frameBufferShadow.getHeight() == SHADOW_BUFFER_SIZE)
        {
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);
            if (this.renderMode == MULTI_DRAW_TEXUTED) {
////              renderMultiPassTextured(world, fTime);
              renderMultiPass(commandBuffer, world, fTime);
          } else {
              renderMultiPass(commandBuffer, world, fTime);
          }
           
        } else {
            System.err.println("SKIPPED, framebuffer is not sized");
            System.err.printf("%dx%d vs %dx%d\n", 
                    this.frameBufferShadow.getWidth(), this.frameBufferShadow.getHeight(),
                    Engine.displayWidth, Engine.displayHeight);

        }
    }
    
    private void renderMultiPass(CommandBuffer commandBuffer, World world, float fTime) {
        PushConstantBuffer buf = PushConstantBuffer.INST;
        int mapSize = Engine.getShadowMapTextureSize()/2;
        int requiredShadowMode = Game.instance.settings.renderSettings.shadowDrawMode;
        VkPipelineGraphics pipe = requiredShadowMode > 0 ? VkPipelines.shadowTextured : VkPipelines.shadowSolid;
        Engine.beginRenderPass(VkRenderPasses.passShadow, this.frameBufferShadow);
        boolean render = true;
        if (render) {

            Engine.clearAllDescriptorSets();
            Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, RenderersVulkan.worldRenderer.getDescTextureTerrain());
            Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboShadow);
            Engine.bindPipeline(pipe);
            float f = -1.0f;
            vkCmdSetDepthBias(commandBuffer, f*1.15f, 0.0f, f*1.f);
//            vkCmdSetDepthBias(commandBuffer, 0,0,0);
            Engine.setViewport(0, 0, mapSize, mapSize);
            buf.setMat4(0, Engine.getIdentityMatrix());
            buf.setInt(16, 0);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
            RenderersVulkan.regionRenderer.renderRegions(commandBuffer, world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
            QModelBatchedRender modelRender = RenderersVulkan.renderBatched;
            modelRender.setPass(PASS_SHADOW_SOLID, 0);
            modelRender.render(fTime);

            Engine.setViewport(mapSize, 0, mapSize, mapSize);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, RenderersVulkan.worldRenderer.getDescTextureTerrain());
            Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboShadow);
            Engine.clearDescriptorSet(VkDescLayouts.DESC3);
            Engine.bindPipeline(pipe);
            buf.setMat4(0, Engine.getIdentityMatrix());
            buf.setInt(16, 1);
//            vkCmdSetDepthBias(commandBuffer, f*8.15f, 0.f, f*1.45f);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
            RenderersVulkan.regionRenderer.renderRegions(commandBuffer, world, fTime, PASS_SHADOW_SOLID, 2, Frustum.FRUSTUM_INSIDE);
            modelRender.setPass(PASS_SHADOW_SOLID, 1);
            modelRender.render(fTime);
            Engine.setViewport(0, mapSize, mapSize, mapSize);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, RenderersVulkan.worldRenderer.getDescTextureTerrain());
            Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboShadow);
            Engine.clearDescriptorSet(VkDescLayouts.DESC3);
            Engine.bindPipeline(pipe);
            buf.setMat4(0, Engine.getIdentityMatrix());
            buf.setInt(16, 2);
//            vkCmdSetDepthBias(commandBuffer, f*12.15f, 0.0f, f*1.15f);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
            RenderersVulkan.regionRenderer.renderRegions(commandBuffer, world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);
            modelRender.setPass(PASS_SHADOW_SOLID, 2);
            modelRender.render(fTime);
            vkCmdSetDepthBias(commandBuffer, 0, 0, 0);
        }
        Engine.endRenderPass();
//        for (int i = 0; i < 20; i++) {
//            Engine.beginRenderPass(VkRenderPasses.passShadow, this.frameBufferShadow);
//            Engine.endRenderPass();
//        }

    }

    @Override
    public void init() {
        this.frameBufferShadow = new FrameBuffer(Engine.vkContext).tag("shadow");
        this.frameBufferShadow.fromRenderpass(VkRenderPasses.passShadow, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);

        try ( MemoryStack stack = stackPush() ) {

            VkSamplerCreateInfo sampler = VkSamplerCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_LINEAR)
                    .minFilter(VK_FILTER_LINEAR)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .mipLodBias(0.0f)
                    .compareOp(VK_COMPARE_OP_NEVER)
                    .compareEnable(false)
                    .minLod(0.0f)
                    .maxLod(1.0f)
                    .maxAnisotropy(1.0f)
                    .anisotropyEnable(false)
                    .borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE);
            LongBuffer pSampler = stack.longs(0);
            int err = vkCreateSampler(Engine.vkContext.device, sampler, null, pSampler);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
            }
            this.sampler = pSampler.get(0);
            if (GameBase.DEBUG_LAYER) {
                VkDebug.registerSampler(this.sampler);
            }
        }
    }
    @Override
    public void release() {
        super.release();
        if (this.frameBufferShadow != null) {
            this.frameBufferShadow.destroy();
        }
        if (this.sampler != VK_NULL_HANDLE) {
            vkDestroySampler(Engine.vkContext.device, sampler, null);
            this.sampler = VK_NULL_HANDLE;
        }
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        if (this.frameBufferShadow != null) {
            this.frameBufferShadow.destroy();
        }
        this.frameBufferShadow.build(VkRenderPasses.passShadow, SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
    }

    @Override
    public void tickUpdate() {
    }

    public long getView() {
        return this.frameBufferShadow.getAtt(0).getView();
    }
    public int getLayout() {
        return this.frameBufferShadow.getAtt(0).finalLayout;
    }

    public long getSampler() {
        return this.sampler;
    }

    public void onShadowSettingChanged() {
    }
}
