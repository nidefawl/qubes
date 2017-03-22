package nidefawl.qubes.render.impl.vk;

import static nidefawl.qubes.gl.Engine.vkContext;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.VkImageCopy;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.render.RenderersGL;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.texture.*;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class WorldRendererVK extends WorldRenderer implements IRenderComponent {

    private VkDescriptor descTextureTerrainNormals;
    private VkDescriptor descTextureTerrain;
    private VkDescriptor descTextureTerrainWater;
    private VkTexture waterNoiseTex;
    private VkImageCopy.Buffer pRegions = VkImageCopy.calloc(1);

    public WorldRendererVK() {
    }

    @Override
    public void init() {
        this.descTextureTerrainNormals = Engine.vkContext.descLayouts.allocDescSetSamplerDouble();
        this.descTextureTerrainWater = Engine.vkContext.descLayouts.allocDescSetSamplerDouble();
        this.descTextureTerrain = Engine.vkContext.descLayouts.allocDescSetSampleSingle();
        this.waterNoiseTex = new VkTexture(Engine.vkContext);
        byte[] data = TextureUtil.genNoise2(256);
        TextureBinMips mips = new TextureBinMips(data, 256, 256);
        this.waterNoiseTex.build(VK_FORMAT_R8G8B8A8_UNORM, mips);
        this.waterNoiseTex.genView();


        pRegions.srcOffset().x(0).y(0).z(0);
        pRegions.dstOffset().x(0).y(0).z(0);
        pRegions.extent().width(1).height(1).depth(1);
        pRegions.srcSubresource()
            .baseArrayLayer(0)
            .mipLevel(0)
            .layerCount(1)
            .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
        pRegions.dstSubresource()
            .baseArrayLayer(0)
            .mipLevel(0)
            .layerCount(1)
            .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
    }

    @Override
    public void initShaders() {
    }

    @Override
    public void tickUpdate() {
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        pRegions.extent().width(displayWidth).height(displayHeight).depth(1);
    }

    boolean first = true;
    public void renderTransparent(World world, float fTime) {        
//        waterShader.enable();
//        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
//        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
//        if (Game.GL_ERROR_CHECKS)
//            Engine.checkGLError("renderSecondPass");
//
//        Shader.disable();

        if (first) {
            first = false;
        } else {

        }
        FramebufferAttachment imageDepthSrc = RenderersVulkan.outRenderer.frameBufferScene.getAtt(4);
        FramebufferAttachment imageDepthDst = RenderersVulkan.outRenderer.frameBufferSceneWater.getAtt(4);
        

        copyDepthBuffer(imageDepthSrc, imageDepthDst);
        
        
        
        FrameBuffer fbScene = RenderersVulkan.outRenderer.frameBufferSceneWater;
        if (fbScene.getWidth() == Engine.fbWidth() && fbScene.getHeight() == Engine.fbHeight())
        {
            Engine.beginRenderPass(VkRenderPasses.passTerrain_Pass1, fbScene, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureTerrainWater);
            Engine.bindPipeline(VkPipelines.water);
            RenderersVulkan.regionRenderer.renderRegions(Engine.getDrawCmdBuffer(), world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
            Engine.endRenderPass();
        } else {
            System.err.println("SKIPPED, framebuffer is not sized");
            System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
                    fbScene.getWidth(), fbScene.getHeight(),
                    Engine.displayWidth, Engine.displayHeight);
        }
    
    }
    private void copyDepthBuffer(FramebufferAttachment imageDepthSrc, FramebufferAttachment imageDepthDst) {
        if (imageDepthSrc.currentLayout != VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {
            System.err.println("Image src isn't in correct layout "+imageDepthSrc.currentLayout+", expected "+VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            
        }
        if (imageDepthDst.currentLayout != imageDepthDst.initialLayout) {
            if (imageDepthDst.currentLayout != VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL) {
                System.err.println("Image dst isn't in correct layout "+imageDepthDst.currentLayout+", expected "+VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
                
            }
            vkContext.setImageLayout(Engine.getDrawCmdBuffer(), imageDepthDst.image,
                    VK_IMAGE_ASPECT_DEPTH_BIT, 
                    imageDepthDst.currentLayout, imageDepthDst.initialLayout,
                    VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT,    VK_PIPELINE_STAGE_TRANSFER_BIT, 
                    VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT, VK_ACCESS_TRANSFER_WRITE_BIT);
        } else {

            System.err.println("First loop, dont transition dst image to initialLayout since renderpass didn't occur!");
        }
        imageDepthDst.currentLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;

        vkCmdCopyImage(Engine.getDrawCmdBuffer(), imageDepthSrc.image, 
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, imageDepthDst.image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pRegions);
        

        vkContext.setImageLayout(Engine.getDrawCmdBuffer(), imageDepthSrc.image,
                VK_IMAGE_ASPECT_DEPTH_BIT, 
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL,
                VK_PIPELINE_STAGE_TRANSFER_BIT,    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 
                VK_ACCESS_SHADER_READ_BIT, VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT|VK_ACCESS_SHADER_READ_BIT);
        imageDepthSrc.currentLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL;
    }

    public void renderWorld(WorldClient world, float fTime) {

        FrameBuffer fbScene = RenderersVulkan.outRenderer.frameBufferScene;
        if (fbScene.getWidth() == Engine.fbWidth() && fbScene.getHeight() == Engine.fbHeight())
        {
            
            Engine.beginRenderPass(VkRenderPasses.passTerrain_Pass0, fbScene, VK_SUBPASS_CONTENTS_INLINE);
//            
            Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureTerrainNormals);
            Engine.bindPipeline(VkPipelines.terrain);
            RenderersVulkan.regionRenderer.renderMain(Engine.getDrawCmdBuffer(), world, fTime);
            rendered = Engine.regionRenderer.rendered;
//            System.out.println("rendered " +rendered);
            Engine.endRenderPass();
        } else {
            System.err.println("SKIPPED, framebuffer is not sized");
            System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
                    fbScene.getWidth(), fbScene.getHeight(),
                    Engine.displayWidth, Engine.displayHeight);

        }
    }

    @Override
    public void onResourceReload() {
        this.descTextureTerrain.setBindingCombinedImageSampler(0, 
                TextureArrays.blockTextureArrayVK.getView(), 
                TextureArrays.blockTextureArrayVK.getSampler(), 
                TextureArrays.blockTextureArrayVK.getImageLayout());
        this.descTextureTerrainNormals.setBindingCombinedImageSampler(0, 
                TextureArrays.blockTextureArrayVK.getView(), 
                TextureArrays.blockTextureArrayVK.getSampler(), 
                TextureArrays.blockTextureArrayVK.getImageLayout());
        this.descTextureTerrainNormals.setBindingCombinedImageSampler(1, 
                TextureArrays.blockNormalMapArrayVK.getView(), 
                TextureArrays.blockNormalMapArrayVK.getSampler(), 
                TextureArrays.blockNormalMapArrayVK.getImageLayout());
        this.descTextureTerrainWater.setBindingCombinedImageSampler(0, 
                TextureArrays.blockTextureArrayVK.getView(), 
                TextureArrays.blockTextureArrayVK.getSampler(), 
                TextureArrays.blockTextureArrayVK.getImageLayout());
        this.descTextureTerrainWater.setBindingCombinedImageSampler(1, 
                this.waterNoiseTex.getView(), 
                Engine.vkContext.samplerLinear, 
                this.waterNoiseTex.getImageLayout());
        this.descTextureTerrainWater.update(Engine.vkContext);
        this.descTextureTerrainNormals.update(Engine.vkContext);
        this.descTextureTerrain.update(Engine.vkContext);
        int n = TextureArrays.blockTextureArrayVK.getNumTextures();
        System.out.println("vk tex array blocks size "+n);
        
        return;
    }
    
    public VkDescriptor getDescTextureTerrain() {
        return this.descTextureTerrain;
    }

    public VkTexture getWaterNoiseTex() {
        return this.waterNoiseTex;
    }

}
