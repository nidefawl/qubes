package nidefawl.qubes.render.impl.vk;

import static nidefawl.qubes.gl.Engine.vkContext;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.VkImageCopy;

import nidefawl.qubes.Game;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.input.DigController;
import nidefawl.qubes.input.Selection;
import nidefawl.qubes.item.*;
import nidefawl.qubes.models.ItemModel;
import nidefawl.qubes.models.qmodel.QModelTexture;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vr.VR;
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
        byte[] data = TextureUtil.genNoise2(256, true);
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

    private void copyDepthBuffers() {

        // copy depth buffer before water is rendered 

        FramebufferAttachment imageDepthSrc = RenderersVulkan.outRenderer.frameBufferScene.getAtt(4);
        FramebufferAttachment imageDepthDst = RenderersVulkan.outRenderer.frameBufferSceneWater.getAtt(4);
        FramebufferAttachment imageDepthDst2 = RenderersVulkan.outRenderer.frameBufferTonemapped.getAtt(1);


        if (imageDepthSrc.currentLayout != VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {
            System.err.println("Image src isn't in correct layout "+imageDepthSrc.currentLayout+", expected "+VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            
        }
        //TODO: make the transition automatic
//        if (imageDepthDst.initialLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
//            System.err.println("incorrect initialLayout "+imageDepthDst.initialLayout+", expected "+VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
//        }
        if (imageDepthDst.currentLayout != VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
            if (imageDepthDst.currentLayout != VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL) {
                System.err.println("Image dst isn't in correct layout "+imageDepthDst.currentLayout+", expected "+VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
                
            }
            vkContext.setImageLayout(Engine.getDrawCmdBuffer(), imageDepthDst.image,
                    VK_IMAGE_ASPECT_DEPTH_BIT, 
                    imageDepthDst.currentLayout, 
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, 
                    VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT,
                    VK_ACCESS_TRANSFER_WRITE_BIT);
            imageDepthDst.currentLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        } else {

//            System.err.println("Renderpass occured, we don't have to manually setImageLayout before transfer");
        }

        vkCmdCopyImage(Engine.getDrawCmdBuffer(), imageDepthSrc.image, 
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, imageDepthDst.image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pRegions);
        vkCmdCopyImage(Engine.getDrawCmdBuffer(), imageDepthSrc.image, 
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, imageDepthDst2.image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pRegions);


        vkContext.setImageLayout(Engine.getDrawCmdBuffer(), imageDepthSrc.image,
                VK_IMAGE_ASPECT_DEPTH_BIT,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL,
                VK_PIPELINE_STAGE_TRANSFER_BIT, 
                VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                VK_ACCESS_SHADER_READ_BIT,
                VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_SHADER_READ_BIT);
        imageDepthSrc.currentLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL;
    }

    public void renderWorld(WorldClient world, float fTime) {
        FrameBuffer fbScene = RenderersVulkan.outRenderer.frameBufferScene;
        FrameBuffer fbSceneWater = RenderersVulkan.outRenderer.frameBufferSceneWater;
        FrameBuffer fbSceneFirstPerson = RenderersVulkan.outRenderer.frameBufferSceneFirstPerson;


        if (fbScene.getWidth() == Engine.fbWidth() && fbScene.getHeight() == Engine.fbHeight())
        {
            Engine.clearAllDescriptorSets();
            Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);

            Engine.beginRenderPass(VkRenderPasses.passTerrain_Pass0, fbScene);
                Engine.setDescriptorSet(VkDescLayouts.DESC1, RenderersVulkan.skyRenderer.descTextureSkyboxCubemap);
                Engine.bindPipeline(VkPipelines.skybox_sample);
                Engine.drawFSTri();

                Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureTerrainNormals);
                Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboConstants);
                Engine.bindPipeline(VkPipelines.terrain);
                RenderersVulkan.regionRenderer.renderMainTrav(Engine.getDrawCmdBuffer(), world, fTime);
                rendered = Engine.regionRenderer.rendered;
        //        System.out.println("rendered " +rendered);
                RenderersVulkan.particleRenderer.renderParticles(world, PASS_SOLID, fTime);

                QModelBatchedRender modelRender = RenderersVulkan.renderBatched;
                modelRender.setPass(PASS_SOLID, 0);
                modelRender.render(fTime);
            Engine.endRenderPass();

            

            copyDepthBuffers();

            RenderersVulkan.lightCompute.render(world, fTime, 0);
            
            
            Engine.beginRenderPass(VkRenderPasses.passTerrain_Pass1, fbSceneWater);
                Engine.setDescriptorSet(VkDescLayouts.DESC1, this.descTextureTerrainWater);
                Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboConstants);
                Engine.bindPipeline(VkPipelines.water);
                RenderersVulkan.regionRenderer.renderRegions(Engine.getDrawCmdBuffer(), world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
            Engine.endRenderPass();

            boolean firstPerson = !Game.instance.thirdPerson;
            if (firstPerson) {
                Engine.beginRenderPass(VkRenderPasses.passTerrain_Pass2, fbSceneFirstPerson);
                    renderFirstPerson(world, fTime);
                Engine.endRenderPass();
            }
            
        

        } else {
            System.err.println("SKIPPED, framebuffer is not sized");
            System.err.printf("%dx%d vs %dx%d\n", 
                    fbScene.getWidth(), fbScene.getHeight(),
                    Engine.displayWidth, Engine.displayHeight);

        }
        
    }

    public void renderFirstPerson(World world, float fTime) {
        Player p = Game.instance.getPlayer();
        if (p == null) {
            return;
        }
        float modelScale = 1 / 2.7f;
        float f1=0;
        DigController dig = Game.instance.dig;
        float roffsetPArm = p.armOffsetPitchPrev+(p.armOffsetPitch-p.armOffsetPitchPrev)*fTime;
        float roffsetYArm = p.armOffsetYawPrev+(p.armOffsetYaw-p.armOffsetYawPrev)*fTime;
        float swingF = p.getSwingProgress(fTime);

        BaseStack stack = p.getActiveItem(0);
        BlockStack bstack = Game.instance.selBlock;
        if (true) { //TODO: fix me (Normal mat)

            BufferedMatrix mat2 = Engine.getTempMatrix2();
            mat2.load(Engine.getMatSceneV());
            mat2.transpose();
            mat2.update();
            UniformBuffer.setNormalMat(mat2.get());
        }
        if (stack != null) {
            ItemStack itemstack = (ItemStack) stack;
            Item item = itemstack.getItem();
            ItemModel model = item.getItemModel();
            if (model != null) {
                //        mat.translate(0, 3, -4);
                BufferedMatrix mat = Engine.getTempMatrix();
                mat.setIdentity();
                Engine.camera.addCameraShake(mat);
                float angleX = -110;
                float angleY = 180;
                float angleZ = 0;
                float swingProgress = dig.getSwingProgress(fTime);
                float f17 = GameMath.sin(GameMath.PI*swingProgress);
                float f23 = GameMath.sin(GameMath.sqrtf(swingProgress)*GameMath.PI);
                mat.translate(-f23*0.25f, f17*0.1f+ GameMath.sin(GameMath.sqrtf(swingProgress)*GameMath.PI*2.0f)*0.3f, f17*-0.11f);
                float f7 = 0.8f;
                mat.translate(0.7F * f7, -0.55F * f7 - (1.0F - f1) * 0.6F, -1.2F * f7);
                float f18 = GameMath.sin(swingProgress*swingProgress*GameMath.PI);
                float f24 = GameMath.sin(GameMath.sqrtf(swingProgress)*GameMath.PI);
                mat.rotate(angleY * GameMath.PI_OVER_180, 0, 1, 0);
                mat.rotate(angleZ * GameMath.PI_OVER_180, 0, 0, 1);
                mat.rotate(angleX * GameMath.PI_OVER_180, 1, 0, 0);
                mat.rotate(f18*-17f * GameMath.PI_OVER_180, 0, 1, 0);
                mat.rotate(f24*16f * GameMath.PI_OVER_180, 0, 0, 1);
                mat.rotate(f24*40f * GameMath.PI_OVER_180, 1, 0, 0);
                mat.scale(modelScale);
//                mat.translate(0, -1, -4);
                mat.rotate(90 * GameMath.PI_OVER_180, 1, 0, 0);
                mat.rotate(-180 * GameMath.PI_OVER_180, 0, 1, 0);
                mat.update();

                if (Game.VR_SUPPORT) {
                    mat.setIdentity();
                    mat.load(Engine.getMatSceneV());
                    Matrix4f.mul(mat, VR.poseMatrices[3], mat);
//                    mat.translate(t,t,t);
                    mat.scale(modelScale);
                    mat.scale(0.4f);
                    mat.translate(0, 0, 2);
                    mat.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
//                    mat.rotate(-180 * GameMath.PI_OVER_180, 0, 1, 0);
                    
                    mat.update();
                    BufferedMatrix mat2 = Engine.getTempMatrix2();
                    mat2.load(mat);
//                    mat2.clearTranslation();
                    mat2.invert().transpose();
                    mat2.update();
                    UniformBuffer.setNormalMat(mat2.get());
                }
                QModelTexture tex = model.loadedModels[0].getQModelTexture(0);
                

                Engine.setDescriptorSet(VkDescLayouts.DESC1, tex.descriptorSetTex);
                Engine.clearDescriptorSet(VkDescLayouts.DESC2);
                Engine.clearDescriptorSet(VkDescLayouts.DESC3);
                Engine.bindPipeline(VkPipelines.model_firstperson);
                PushConstantBuffer buf = PushConstantBuffer.INST;
                buf.setMat4(0, mat);//TODO: push normal mat
                vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.model_firstperson.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64));
//              shaderModelfirstPerson.enable();
//              shaderModelfirstPerson.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
//              model.loadedModels[0].bindTextures(0);
//                Engine.bindVAO(GLVAO.vaoModel);
                model.loadedModels[0].render(0, 0, Game.ticksran+fTime);
            }
            return;
        } else if (bstack!=null&&bstack.id>0) {
            BufferedMatrix mat = Engine.getTempMatrix();
            mat.setIdentity();
            Engine.camera.addCameraShake(mat);
            mat.rotate((p.pitch - roffsetPArm) * 0.12f * GameMath.PI_OVER_180, 1, 0, 0);
            mat.rotate((p.yaw - roffsetYArm) * 0.12f * GameMath.PI_OVER_180, 0, 1, 0);
            float fsqrt = GameMath.sqrtf(swingF) * GameMath.PI;
            if (swingF > 0) {
                float scale = 1.5f;
                mat.translate(
                        scale*-GameMath.sin(fsqrt) * 0.4F, 
                        scale*GameMath.sin(fsqrt * 2.0F) * 0.17F, 
                        scale*-GameMath.sin(swingF * GameMath.PI) * 0.2F
                        );
            }
            mat.translate(0.8f, -0.6f, -1f);
            mat.rotate(50F * GameMath.PI_OVER_180, 0.0F, 1.0F, 0.0F);
            if (swingF > 0) {
                float f18 = GameMath.sin(swingF * swingF * GameMath.PI);
                float f24 = GameMath.sin(fsqrt);
                mat.rotate(f18 * 18F * GameMath.PI_OVER_180, 0.0F, 1.0F, 0.0F);
                mat.rotate(f24 * 15f * GameMath.PI_OVER_180, 0.0F, 0.0F, 1.0F);
                mat.rotate(-GameMath.sin(fsqrt) * -33F * GameMath.PI_OVER_180, 1.0F, 0.0F, 0.0F);
            }
            mat.scale(0.5f);
            mat.update();
            if (Game.VR_SUPPORT) {
                mat.setIdentity();
                mat.load(Engine.getMatSceneV());
                Matrix4f.mul(mat, VR.poseMatrices[3], mat);
//                mat.translate(t,t,t);
                mat.scale(0.4f);
                
                mat.update();
                BufferedMatrix mat2 = Engine.getTempMatrix2();
                mat2.load(mat);
//                mat2.clearTranslation();
                mat2.invert().transpose();
                mat2.update();
                UniformBuffer.setNormalMat(mat2.get());
            }

            Engine.setDescriptorSet(VkDescLayouts.DESC1, RenderersVulkan.worldRenderer.getDescTextureTerrainNormals());
            Engine.clearDescriptorSet(VkDescLayouts.DESC2);
            Engine.clearDescriptorSet(VkDescLayouts.DESC3);
            Engine.bindPipeline(VkPipelines.singleblock_3D);
            PushConstantBuffer buf = PushConstantBuffer.INST;
            buf.setMat4(0, mat);//TODO: push normal mat
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.singleblock_3D.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64));
            Engine.blockDraw.doRender(bstack.getBlock(), 0, null);
        }
        UniformBuffer.setNormalMat(Engine.getMatSceneNormal().get());
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
    public VkDescriptor getDescTextureTerrainNormals() {
        return this.descTextureTerrainNormals;
    }

    public void renderOverlays(WorldClient world, Selection leftSelection, Selection rightSelection, DigController dig, float fTime) {

        FrameBuffer fb = RenderersVulkan.outRenderer.frameBufferTonemapped;
        
        Engine.beginRenderPass(VkRenderPasses.passPostTonemapOverlays, fb);
        Engine.clearAllDescriptorSets();
        Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);
        Engine.setDescriptorSet(VkDescLayouts.DESC1, Engine.descriptorSetUboTransform);
        leftSelection.renderBlockHighlight(world, fTime);
        if (VR.controllerDeviceIndex[1]>0)
            rightSelection.renderBlockHighlight(world, fTime);
        dig.renderDigging(world, fTime);
        Engine.endRenderPass();
        
        
        
    }
    @Override
    public void onNormalMapSettingChanged() {
        Engine.vkContext.reinitSwapchain=true;
    }

}
