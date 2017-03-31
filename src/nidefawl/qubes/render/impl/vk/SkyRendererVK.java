package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.SkyRenderer;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.World;

public class SkyRendererVK extends SkyRenderer implements IRenderComponent {

    private long image;
    private long view;
    private long sampler;
    private FrameBuffer frameBufferCubemap;
    public VkDescriptor descTextureSkyboxCubemap;
    private long[] viewsFrameBuffer = new long[6];

    public VkDescriptor descriptorSetUboScene;
    private BufferPair quad;
    private VkBuffer bufferInstanceData;
    private VkTexture[] texClouds;
    private VkDescriptor descTextureCloudSingle;
    final static int BUFFER_SIZE = 2*1024*1024;
    @Override
    public void init() {
        super.init();
        VKContext ctxt = Engine.vkContext;
        
        {

            ArrayList<AssetTexture> clouds = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/sky/cloud"+i+".png", i>0);
                if (tex == null)
                    break;
                clouds.add(tex);
            }
            this.texClouds = new VkTexture[clouds.size()];
            for (int i = 0; i < clouds.size(); i++) {
                TextureBinMips binMips = new TextureBinMips(clouds.get(i));
                this.texClouds[i] = new VkTexture(ctxt);
                this.texClouds[i].build(VK_FORMAT_R8G8B8A8_UNORM, binMips);
                this.texClouds[i].genView();
            }
            this.numTexturesCloud = this.texClouds.length;
        }
        this.bufferInstanceData = new VkBuffer(ctxt);
        this.bufferInstanceData.create(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, BUFFER_SIZE, false);

        this.quad = ctxt.getFreeBuffer();
        this.quad.create(4*4, 6*4, true);
        try ( MemoryStack stack = stackPush() ) {
            ByteBuffer vertexBufB = stack.calloc(4*4);
            IntBuffer vertexBuf = vertexBufB.asIntBuffer();
            vertexBuf.put(Half.fromFloat(0) << 16 | Half.fromFloat(1));
            vertexBuf.put(Half.fromFloat(1) << 16 | Half.fromFloat(1));
            vertexBuf.put(Half.fromFloat(1) << 16 | Half.fromFloat(0));
            vertexBuf.put(Half.fromFloat(0) << 16 | Half.fromFloat(0));
            vertexBuf.flip();
            ByteBuffer idxBufB = stack.calloc(6*4);
            IntBuffer idxBuf = idxBufB.asIntBuffer();
            idxBuf.put(0);
            idxBuf.put(1);
            idxBuf.put(2);
            idxBuf.put(2);
            idxBuf.put(3);
            idxBuf.put(0);
            idxBuf.flip();
            this.quad.upload(0, vertexBufB, 0, idxBufB);
        }
        descriptorSetUboScene = ctxt.descLayouts.allocDescSetUBOScene();
        descriptorSetUboScene.setBindingUniformBuffer(0, UniformBuffer.uboMatrix3D_Temp);
        descriptorSetUboScene.setBindingUniformBuffer(1, UniformBuffer.uboMatrix2D);
        descriptorSetUboScene.setBindingUniformBuffer(2, UniformBuffer.uboSceneData);
        descriptorSetUboScene.update(ctxt);
        this.descTextureCloudSingle = ctxt.descLayouts.allocDescSetSampleSingle();
        this.descTextureSkyboxCubemap = ctxt.descLayouts.allocDescSetSampleSingle();
        

        this.descTextureCloudSingle.setBindingCombinedImageSampler(0, this.texClouds[0].getView(), ctxt.samplerLinear, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.descTextureCloudSingle.update(Engine.vkContext);
        
        this.frameBufferCubemap = new FrameBuffer(ctxt).tag("sky_cubemap");

        try ( MemoryStack stack = stackPush() ) {
            LongBuffer pImage = stack.longs(0);
    
                VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                        .imageType(VK_IMAGE_TYPE_2D)
                        .format(VK_FORMAT_R16G16B16A16_SFLOAT)
                        .mipLevels(1)
                        .arrayLayers(6)
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                        .tiling(VK_IMAGE_TILING_OPTIMAL)
                        .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT| VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                        .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                        .flags(VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT);
                imageCreateInfo.extent().width(SKYBOX_RES).height(SKYBOX_RES).depth(1);
                int err = vkCreateImage(ctxt.device, imageCreateInfo, null, pImage);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateImage failed: " + VulkanErr.toString(err));
                }
                this.image = pImage.get(0);
                ctxt.memoryManager.allocateImageMemory(pImage.get(0), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, VkConstants.TEXTURE_COLOR_MEMORY);

                LongBuffer pView = stack.longs(0);

                for (int i = 0; i < 6; i++) {

                    VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                            .viewType(VK_IMAGE_VIEW_TYPE_2D)
                            .format(VK_FORMAT_R16G16B16A16_SFLOAT)
                            .components(VkComponentMapping.callocStack(stack));
                    VkImageSubresourceRange viewSubResRange = view.subresourceRange();
                    viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    viewSubResRange.baseMipLevel(0);
                    viewSubResRange.levelCount(1);
                    viewSubResRange.baseArrayLayer(i);
                    viewSubResRange.layerCount(1);
                    view.image(this.image);
                    err = vkCreateImageView(ctxt.device, view, null, pView);
                    if (err != VK_SUCCESS) {
                        throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
                    }
                    this.viewsFrameBuffer[i] = pView.get(0);
                    this.frameBufferCubemap.attachments[i] = new FramebufferAttachment(ctxt, VK_FORMAT_R16G16B16A16_SFLOAT, VK_IMAGE_ASPECT_COLOR_BIT, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                    this.frameBufferCubemap.attachments[i].setFromImage(this.image, this.viewsFrameBuffer[i], SKYBOX_RES, SKYBOX_RES);
                    this.frameBufferCubemap.isReferencedAtt[i] = true;
                }
                this.frameBufferCubemap.build(VkRenderPasses.passSkyGenerateCubemap, SKYBOX_RES, SKYBOX_RES);
                
                
              VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                      .viewType(VK_IMAGE_VIEW_TYPE_CUBE)
                      .format(VK_FORMAT_R16G16B16A16_SFLOAT)
                      .components(VkComponentMapping.callocStack(stack));
              VkImageSubresourceRange viewSubResRange = view.subresourceRange();
              viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
              viewSubResRange.baseMipLevel(0);
              viewSubResRange.levelCount(1);
              viewSubResRange.baseArrayLayer(0);
              viewSubResRange.layerCount(6);
              view.image(this.image);
              err = vkCreateImageView(ctxt.device, view, null, pView);
              if (err != VK_SUCCESS) {
                  throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
              }
              this.view = pView.get(0);

              VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
              sampler
                  .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                  .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                  .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                  .borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE);
              
              LongBuffer pSampler = stack.longs(0);
              err = vkCreateSampler(ctxt.device, sampler, null, pSampler);
              if (err != VK_SUCCESS) {
                  throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
              }
              this.sampler = pSampler.get(0);
              this.descTextureSkyboxCubemap.setBindingCombinedImageSampler(0, this.view, this.sampler, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
              this.descTextureSkyboxCubemap.update(Engine.vkContext);
              
        }
        ctxt.clearImage(ctxt.getCopyCommandBuffer(), this.image, 6, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, 0, 0, 0, 1);
        redraw();
    }
    @Override
    public void release() {
        if (this.image != VK_NULL_HANDLE) {
            VKContext ctxt = Engine.vkContext;
            ctxt.memoryManager.releaseImageMemory(this.image);
            vkDestroyImageView(ctxt.device, this.view, null);
            vkDestroyImage(ctxt.device, this.image, null);
            vkDestroySampler(ctxt.device, this.sampler, null);
            this.view = VK_NULL_HANDLE;
            this.image = VK_NULL_HANDLE;
            this.sampler = VK_NULL_HANDLE;
            for (int i = 0; i < 6; i++) {
                vkDestroyImageView(ctxt.device, this.viewsFrameBuffer[i], null);
            }
        }
    }
    private int bufferPos;
    private int bufferSize;
    private int bufferOffset = 0;
    @Override
    protected void uploadData() {
        this.bufferSize = this.bufMatFloat.remaining()*4;
        long alignedV = Math.max(2048, GameMath.nextPowerOf2(this.bufferSize));
//        System.out.println(BUFFER_SIZE+","+this.bufferOffset+","+this.bufferSize+"  - "+(BUFFER_SIZE-this.bufferOffset)+" < "+this.bufferSize+" = "+(BUFFER_SIZE-this.bufferPos < this.bufferSize));
        if (BUFFER_SIZE-this.bufferOffset < alignedV) {
            this.bufferOffset = 0;
        }
        this.bufferPos = this.bufferOffset;
        this.bufferInstanceData.upload(this.bufMatFloat, this.bufferPos);
        this.bufferOffset+=alignedV;
//        System.out.println("Uploaded "+this.bufferSize+" bytes at pos "+this.bufferPos+", next "+this.bufferOffset);
    }

    @Override
    public void tickUpdate() {
        updateSpritesTick();
    }

    @Override
    public void renderSky(World world, float fTime) {
        this.updateSprites(fTime);
        Engine.clearAllDescriptorSets();
        
        PushConstantBuffer buf = PushConstantBuffer.INST;
        float weatherStr = GameMath.powf(WEATHER*0.9f, 1.6f);
        buf.setFloat(0, weatherStr);
        buf.setFloat(1, 5);
        storeSprites(fTime, 0);

        Engine.beginRenderPass(VkRenderPasses.passSkyGenerateCubemap, this.frameBufferCubemap, VK_SUBPASS_CONTENTS_INLINE);
        Engine.setDescriptorSet(1, this.descTextureCloudSingle);
        Engine.setDescriptorSet(2, Engine.descriptorSetUboLights);
        Engine.clearDescriptorSet(4);
        for (int i = 0; i < 6; i++) {
            cubeMatrix.setupScene(i, Engine.camera.getPosition());
            Engine.setDescriptorSetF(0, descriptorSetUboScene);
            if (i != 0)
                vkCmdNextSubpass(Engine.getDrawCmdBuffer(), VK_SUBPASS_CONTENTS_INLINE);
            Engine.bindPipeline(VkPipelines.skybox_update_background_cubemap, i);
            Engine.drawFSTri();
            Engine.bindPipeline(VkPipelines.skybox_update_sprites, i);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.skybox_update_sprites.getLayoutHandle(), VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf.getBuf(4+4));
//
            drawSprites(Engine.getDrawCmdBuffer());
        }
        Engine.endRenderPass();
    }

    static long[] pointer = new long[2];
    static long[] offset = new long[2];
    private void drawSprites(CommandBuffer commandBuffer) {
        offset[0] = 0;
        offset[1] = this.bufferPos;
        pointer[0] = this.quad.vert.getBuffer();
        pointer[1] = this.bufferInstanceData.getBuffer();
        vkCmdBindVertexBuffers(commandBuffer, 0, pointer, offset);
        vkCmdBindIndexBuffer(commandBuffer, this.quad.idx.getBuffer(), 0, VK_INDEX_TYPE_UINT32);
        vkCmdDrawIndexed(commandBuffer, 6, this.storedSprites, 0, 0, 0);
    }
    @Override
    public void resize(int displayWidth, int displayHeight) {
    }

    public void renderSkybox() {
    }

}