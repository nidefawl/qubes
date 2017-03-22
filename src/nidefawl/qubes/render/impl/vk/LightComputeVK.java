package nidefawl.qubes.render.impl.vk;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.LightCompute;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vulkan.*;

public class LightComputeVK extends LightCompute implements IRenderComponent {


    private long image;
    private long view;
    private long sampler;

    @Override
    public void init() {
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
        release();
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
                        .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
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
        }
        ctxt.clearImage(ctxt.getCopyCommandBuffer(), this.image, getLayout(), 0, 0, 0, 1);

    }
    @Override
    public void release() {
        if (this.image != VK_NULL_HANDLE) {
            VKContext ctxt = Engine.vkContext;
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
        return VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    }

}
