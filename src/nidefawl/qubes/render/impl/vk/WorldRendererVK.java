package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.WorldClient;

public class WorldRendererVK extends WorldRenderer implements IRenderComponent {

    private VkDescriptor descTextureTerrainNormals;
    private VkDescriptor descTextureTerrain;

    public WorldRendererVK() {
    }

    @Override
    public void init() {
        this.descTextureTerrainNormals = Engine.vkContext.descLayouts.allocDescSetSamplerDouble();
        this.descTextureTerrain = Engine.vkContext.descLayouts.allocDescSetSampleSingle();
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

    public void renderWorld(WorldClient world, float fTime) {

        FrameBuffer fbScene = RenderersVulkan.outRenderer.frameBufferScene;
        if (fbScene.getWidth() == Engine.fbWidth() && fbScene.getHeight() == Engine.fbHeight())
        {
            
            Engine.beginRenderPass(VkRenderPasses.passTerrain, fbScene.get(), VK_SUBPASS_CONTENTS_INLINE);
//            
            Engine.setDescriptorSet(1, this.descTextureTerrainNormals);
            Engine.setDescriptorSet(2, Engine.descriptorSetUboConstants);
            Engine.bindPipeline(VkPipelines.terrain);
            RenderersVulkan.regionRenderer.renderMain(Engine.getDrawCmdBuffer(), world, fTime);
            rendered = Engine.regionRenderer.rendered;
//            System.out.println("rendered " +rendered);
            Engine.endRenderPass();
            Engine.clearDescriptorSet(2);
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
        this.descTextureTerrainNormals.update(Engine.vkContext);
        this.descTextureTerrain.update(Engine.vkContext);
        int n = TextureArrays.blockTextureArrayVK.getNumTextures();
        System.out.println("vk tex array blocks size "+n);
        
        return;
    }
    
    public VkDescriptor getDescTextureTerrain() {
        return this.descTextureTerrain;
    }

}
