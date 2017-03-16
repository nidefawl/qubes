package nidefawl.qubes.render.impl.vk;

import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkViewport;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.RenderersGL;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.render.ShadowRenderer;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class ShadowRendererVK extends ShadowRenderer {

    private FrameBuffer frameBufferShadow;
    VkViewport.Buffer viewport = VkViewport.calloc(1);
    
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
        
        Engine.beginRenderPass(VkRenderPasses.passShadow, this.frameBufferShadow.get(), VK_SUBPASS_CONTENTS_INLINE);

        Engine.clearDescriptorSet(1);
        Engine.bindPipeline(VkPipelines.shadowSolid);
        float f = -1.0f;
        vkCmdSetDepthBias(commandBuffer, f*1.0f, f*0.2f, f*0.2f);
//        vkCmdSetDepthBias(commandBuffer, 0,0,0);
        Engine.setViewport(0, 0, mapSize, mapSize);
        buf.setMat4(0, Engine.getIdentityMatrix());
        buf.setInt(16, 0);
        vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
        RenderersVulkan.regionRenderer.renderRegions(commandBuffer, world, fTime, PASS_SHADOW_SOLID, 1, Frustum.FRUSTUM_INSIDE);
        RenderersVulkan.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, 0); //TODO: FRUSTUM CULLING

        Engine.setViewport(mapSize, 0, mapSize, mapSize);
        buf.setMat4(0, Engine.getIdentityMatrix());
        buf.setInt(16, 1);
        vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
        RenderersVulkan.regionRenderer.renderRegions(commandBuffer, world, fTime, PASS_SHADOW_SOLID, 2, Frustum.FRUSTUM_INSIDE);
        RenderersVulkan.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, 1); //TODO: FRUSTUM CULLING
        Engine.setViewport(0, mapSize, mapSize, mapSize);
        buf.setMat4(0, Engine.getIdentityMatrix());
        buf.setInt(16, 2);
        vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
        RenderersVulkan.regionRenderer.renderRegions(commandBuffer, world, fTime, PASS_SHADOW_SOLID, 3, Frustum.FRUSTUM_INSIDE);
        RenderersVulkan.worldRenderer.renderEntities(world, PASS_SHADOW_SOLID, fTime, 2); //TODO: FRUSTUM CULLING
        Engine.endRenderPass();
    }

    private void renderMultiPassTextured(World world, float fTime) {
    }

    @Override
    public void init() {
        super.init();
        this.frameBufferShadow = new FrameBuffer(Engine.vkContext);
        this.frameBufferShadow.fromRenderpass(VkRenderPasses.passShadow, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);

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

}
