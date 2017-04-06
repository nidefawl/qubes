package nidefawl.qubes.render.impl.vk;
import static nidefawl.qubes.vulkan.VkConstants.MAX_NUM_SWAPCHAIN;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.*;
import java.util.Arrays;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.LightCompute;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.util.ITess;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.WorldClient;

public class LightComputeVK extends LightCompute implements IRenderComponent {
    public final static boolean DEBUG_LIGHTS = false;
    final static int SSBO_FRAME_SIZE_DBG = 4096*4;
    final static int SSBO_SIZE_DBG = MAX_NUM_SWAPCHAIN*SSBO_FRAME_SIZE_DBG;
    public static final int MAX_DRAWS_FRAME = 2;
    final static int SSBO_DRAW_SIZE_LIGHT = Engine.MAX_LIGHTS * SIZE_OF_STRUCT_LIGHT;
    final static int SSBO_FRAME_SIZE_LIGHT = MAX_DRAWS_FRAME*SSBO_DRAW_SIZE_LIGHT;
    final static int SSBO_SIZE_LIGHT = MAX_NUM_SWAPCHAIN*SSBO_FRAME_SIZE_LIGHT;

    private long image;
    private long view;
    private long sampler;

    private VkSSBO vkSSBOLights;
    private VkSSBO vkSSBOLightsDbg;
    private FloatBuffer bufferFloat;
    private ByteBuffer bufferByte;
    protected VkDescriptor descriptorSetSSBOLights;
    protected VkDescriptor descriptorSetSSBOLightsDbg;
    protected VkDescriptor descriptorSetImages;
    private ByteBuffer bufferByteDownload;
    private IntBuffer bufferInt;
    private static IntBuffer pOffsets;
    private static LongBuffer pDescriptorSets;

    PostRenderDownload[] tasks = new PostRenderDownload[10];
    private boolean hasLights;
    public static class PostRenderDownload extends PostRenderTask {
        public int frame;
        public int offset;
        public LightComputeVK lightCompute;

        public PostRenderDownload() {
        }

        @Override
        public void onComplete() {
            lightCompute.loadDebugData(frame, offset);
        }
    }
    @Override
    public void init() {
        VKContext ctxt = Engine.vkContext;
        this.vkSSBOLights = new VkSSBO(ctxt, SSBO_SIZE_LIGHT, SSBO_FRAME_SIZE_LIGHT).tag("ssbo_lights");

        this.bufferByte = MemoryUtil.memCalloc(SSBO_SIZE_LIGHT);
        this.bufferFloat = this.bufferByte.asFloatBuffer();
        descriptorSetSSBOLights = ctxt.descLayouts.allocDescSetSSBOPointLights().tag("SSBOPointLights");
        descriptorSetSSBOLights.setBindingSSBO(0, this.vkSSBOLights);
        descriptorSetSSBOLights.update(ctxt);
        descriptorSetImages = ctxt.descLayouts.allocDescSetImagesComputeLight().tag("ImagesComputeLight");
        pDescriptorSets = memAllocLong(5);
        pOffsets = memAllocInt(32);
        if (DEBUG_LIGHTS) {
            for (int i = 0; i < tasks.length; i++) {
                tasks[i] = new PostRenderDownload();
            }
            
            this.bufferByteDownload = MemoryUtil.memCalloc(SSBO_FRAME_SIZE_DBG);
            this.bufferInt = this.bufferByteDownload.asIntBuffer();
            this.vkSSBOLightsDbg = new VkSSBO(ctxt, SSBO_SIZE_DBG, SSBO_FRAME_SIZE_DBG).tag("ssbo_lights_dbg");
            descriptorSetSSBOLightsDbg = ctxt.descLayouts.allocDescSetSSBOPointLights().tag("SSBOPointLightsDbg");
            descriptorSetSSBOLightsDbg.setBindingSSBO(0, this.vkSSBOLightsDbg);
            descriptorSetSSBOLightsDbg.update(ctxt);
        }

    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        release();
        super.resize(displayWidth, displayHeight);
        VKContext ctxt = Engine.vkContext;
        try ( MemoryStack stack = stackPush() ) {
            LongBuffer pImage = stack.longs(0);
    
                VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                        .imageType(VK_IMAGE_TYPE_2D)
                        .format(VK_FORMAT_R16G16B16A16_SFLOAT)
                        .mipLevels(1)
                        .arrayLayers(1)
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                        .tiling(VK_IMAGE_TILING_OPTIMAL)
                        .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT)
                        .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                imageCreateInfo.extent().width(displayWidth).height(displayHeight).depth(1);
                int err = vkCreateImage(ctxt.device, imageCreateInfo, null, pImage);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateImage failed: " + VulkanErr.toString(err));
                }
                this.image = pImage.get(0);
                ctxt.memoryManager.allocateImageMemory(pImage.get(0), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, VkConstants.TEXTURE_COLOR_MEMORY);

              VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                      .viewType(VK_IMAGE_VIEW_TYPE_2D)
                      .format(VK_FORMAT_R16G16B16A16_SFLOAT)
                      .components(VkComponentMapping.callocStack(stack));
              VkImageSubresourceRange viewSubResRange = view.subresourceRange();
              viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
              viewSubResRange.baseMipLevel(0);
              viewSubResRange.levelCount(1);
              viewSubResRange.baseArrayLayer(0);
              viewSubResRange.layerCount(1);
              view.image(this.image);
              LongBuffer pView = stack.longs(0);
              err = vkCreateImageView(ctxt.device, view, null, pView);
              if (err != VK_SUCCESS) {
                  throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
              }
              this.view = pView.get(0);
//              System.err.println("NEW VIEW HDL "+RenderersVulkan.lightCompute.getView());

              VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
              LongBuffer pSampler = stack.longs(0);
              err = vkCreateSampler(ctxt.device, sampler, null, pSampler);
              if (err != VK_SUCCESS) {
                  throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
              }
              this.sampler = pSampler.get(0);
              if (GameBase.DEBUG_LAYER) {
                  VkDebug.registerSampler(this.sampler);
              }
        }
        ctxt.clearImage(ctxt.getGraphicsCopyCommandBuffer(), this.image, 1, getLayout(), 0, 0, 0, 1);

    }
    
    public void render(WorldClient world, float fTime, int pass) {

        if (this.numLights > 0) {
            CommandBuffer commandBuffer = Engine.getDrawCmdBuffer();
            VkPipeline pipe = VkPipelines.computeLight;
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipe.pipeline);
            if (DEBUG_LIGHTS) {
                this.vkSSBOLightsDbg.markPos();
                this.vkSSBOLightsDbg.setPosFromOffset();
                PostRenderDownload task = this.tasks[commandBuffer.frameIdx];
                task.lightCompute = this;
                task.frame = commandBuffer.frameIdx;
                task.offset = this.vkSSBOLightsDbg.getDynamicOffset();
                commandBuffer.addPostRenderTask(task);
            }
            
            pDescriptorSets.clear();
            pDescriptorSets.put(Engine.descriptorSetUboScene.get());
            pDescriptorSets.put(descriptorSetImages.get());
            pDescriptorSets.put(descriptorSetSSBOLights.get());
            if (DEBUG_LIGHTS) {
                pDescriptorSets.put(descriptorSetSSBOLightsDbg.get());
            }
            pDescriptorSets.flip();
            pOffsets.clear();
            Engine.descriptorSetUboScene.addDynamicOffsets(pOffsets);
            descriptorSetImages.addDynamicOffsets(pOffsets);
            descriptorSetSSBOLights.addDynamicOffsets(pOffsets);
            if (DEBUG_LIGHTS) {
                descriptorSetSSBOLightsDbg.addDynamicOffsets(pOffsets);
            }
            pOffsets.flip();
            Stats.callsBindDescSets++;
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipe.getLayoutHandle(), 0, pDescriptorSets, pOffsets);
            

            PushConstantBuffer buf = PushConstantBuffer.INST;
            buf.setInt(0, this.numLights);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), pipe.getLayoutHandle(), VK_SHADER_STAGE_COMPUTE_BIT, 0, buf.getBuf(4));

            
            vkCmdDispatch(commandBuffer, this.lightTiles[0], this.lightTiles[1], 1);
            if (DEBUG_LIGHTS) {
                this.vkSSBOLights.seekPos(SSBO_FRAME_SIZE_DBG);
            }
            hasLights = true;
        } else {
            if (hasLights) {
                hasLights = false;
                if (DEBUG_LIGHTS) {
                    Arrays.fill(this.debugResults, 0);
                }
                Engine.vkContext.clearImage(Engine.getDrawCmdBuffer(), this.image, 1, getLayout(), 0, 0, 0, 1);
            }
            
        }
    }
    public void loadDebugData(int frame, int offset) {
        if (this.numLights > 0) {
            this.vkSSBOLightsDbg.downloadData(this.bufferByteDownload, offset, SSBO_FRAME_SIZE_DBG);
            bufferInt.position(16);
            if (this.debugResults == null || this.debugResults.length != bufferInt.remaining()) {
                this.debugResults = new int[bufferInt.remaining()]; 
            }
            bufferInt.get(this.debugResults);
        }
    }
    public void renderDebug() {
        if (this.debugResults == null) 
            return;
        ITess t = Engine.getTess();
        Engine.clearDescriptorSet(VkDescLayouts.DESC2);
        Engine.clearDescriptorSet(VkDescLayouts.DESC3);
        Engine.clearDescriptorSet(VkDescLayouts.DESC4);
//        Engine.clearDescriptorSet(VkDescLayouts.DESC5);
        
        float chunkWPx = 32.0f;
        float screenX = 0;
        float screenZ = 0;
        float border = 1;
        for (int x = 0; x < this.lightTiles[0]; x++) {
            for (int y = 0; y < this.lightTiles[1]; y++) {
                int idx = (this.lightTiles[1]-1-y) * this.lightTiles[0] + x;
                t.setOffset((screenX + chunkWPx * x), (screenZ + chunkWPx * y), 0);

                int n = this.debugResults[idx];
                if (n <= 0) {
                    t.setColorF(0xff0000, 0.3f);
                } else
                {
                    int r = 0x777777;
                    t.setColorF(r, 0.5f);
                }
                t.add(0, chunkWPx);
                t.add(border, chunkWPx);
                t.add(border, 0);
                t.add(0, 0);
                t.add(chunkWPx, border);
                t.add(chunkWPx, 0);
                t.add(0, 0);
                t.add(0, border);
            }

        }
        Engine.bindPipeline(VkPipelines.colored2D);
        t.drawQuads();
        FontRenderer font = FontRenderer.get(0, 12, 1);
        for (int x = 0; x < this.lightTiles[0]; x++) {
            for (int y = 0; y < this.lightTiles[1]; y++) {
                int idx = (this.lightTiles[1]-1-y) * this.lightTiles[0] + x;
                int n = this.debugResults[idx];
                float screenY = (screenZ + chunkWPx * y);
                font.drawString("" + n, screenX + chunkWPx * x + chunkWPx / 3, screenY + font.getLineHeight() / 2.0f + chunkWPx / 2.0f, -1, true, 0.7f);
            }
        }
        Engine.clearDescriptorSet(VkDescLayouts.DESC2);
        
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
        }
    }

    public long getView() {
        return this.view;
    }

    public long getSampler() {
        return this.sampler;
    }

    public int getLayout() {
        return VK_IMAGE_LAYOUT_GENERAL;
    }

    @Override
    public void updateLights(WorldClient world, float f) {
        this.vkSSBOLights.markPos();
        this.bufferFloat.clear();
        updateAndStoreLights(world, f, this.bufferFloat);
        this.bufferFloat.flip();
        this.bufferByte.position(0).limit(this.bufferFloat.remaining()*4);
        this.vkSSBOLights.uploadData(this.bufferByte);
        this.vkSSBOLights.seekPos(SSBO_FRAME_SIZE_LIGHT);
    }

}
