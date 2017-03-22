package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.vulkan.VK10.*;

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
    }

    @Override
    public void initShaders() {
    }

    @Override
    public void tickUpdate() {
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
    }

    public void renderTransparent(World world, float fTime) {        
//        waterShader.enable();
//        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
//        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
//        if (Game.GL_ERROR_CHECKS)
//            Engine.checkGLError("renderSecondPass");
//
//        Shader.disable();
        FrameBuffer fbScene = RenderersVulkan.outRenderer.frameBufferSceneWater;
        if (fbScene.getWidth() == Engine.fbWidth() && fbScene.getHeight() == Engine.fbHeight())
        {
            Engine.beginRenderPass(VkRenderPasses.passTerrain, fbScene, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureTerrainWater);
            Engine.bindPipeline(VkPipelines.water);
            RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
            Engine.endRenderPass();
        } else {
            System.err.println("SKIPPED, framebuffer is not sized");
            System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
                    fbScene.getWidth(), fbScene.getHeight(),
                    Engine.displayWidth, Engine.displayHeight);
        }
    
    }
    public void renderWorld(WorldClient world, float fTime) {

        FrameBuffer fbScene = RenderersVulkan.outRenderer.frameBufferScene;
        if (fbScene.getWidth() == Engine.fbWidth() && fbScene.getHeight() == Engine.fbHeight())
        {
            
            Engine.beginRenderPass(VkRenderPasses.passTerrain, fbScene, VK_SUBPASS_CONTENTS_INLINE);
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
