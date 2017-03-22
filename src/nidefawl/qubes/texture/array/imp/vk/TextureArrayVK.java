package nidefawl.qubes.texture.array.imp.vk;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.Arrays;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.texture.array.TextureArray;
import nidefawl.qubes.vulkan.VKContext;
import nidefawl.qubes.vulkan.VkTexture;
import nidefawl.qubes.vulkan.VulkanErr;

public abstract class TextureArrayVK extends TextureArray {
    public VkTexture    texture;
    private long sampler;
    private long textureView;

    public TextureArrayVK(int maxTextures) {
        super(maxTextures);
        this.internalFormat = VK_FORMAT_R8G8B8A8_UNORM;
    }
    protected void unload() {
        if (!firstInit) {
            texture.destroy();
            this.texNameToAssetMap.clear();
            this.blockIDToAssetList.clear();
            this.slotTextureMap.clear();
            Arrays.fill(this.textures, 0);
        }
    }
    public void init() {
        texture = new VkTexture(Engine.vkContext);
    }

    @Override
    protected void uploadTextures() {
    }

    @Override
    protected void collectTextures(AssetManager mgr) {
    }

    @Override
    protected void postUpload() {
        
        if (firstInit) {
            VKContext ctxt = Engine.vkContext;
            try ( MemoryStack stack = stackPush() ) {
                
                VkSamplerCreateInfo sampler = VkSamplerCreateInfo.callocStack(stack)
                        .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                        .magFilter(VK_FILTER_LINEAR)
                        .minFilter(VK_FILTER_LINEAR)
                        .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                        .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                        .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                        .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                        .mipLodBias(0.0f)
                        .compareOp(VK_COMPARE_OP_NEVER)
                        .minLod(0.0f);
                // Set max level-of-detail to mip level count of the texture
                sampler.maxLod((float)this.texture.getNumMips());
                // Enable anisotropic filtering
                // This feature is optional, so we must check if it's supported on the device
                if (anisotropicFiltering > 0 && ctxt.features.samplerAnisotropy())
                {
                    float anisotropicLevel = Math.min(anisotropicFiltering, ctxt.limits.maxSamplerAnisotropy());
                    sampler.maxAnisotropy(anisotropicLevel);
                    sampler.anisotropyEnable(true);
                } else {
                    sampler.maxAnisotropy(1.0f);
                    sampler.anisotropyEnable(false);
                }
                sampler.borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE);
                LongBuffer pSampler = stack.longs(0);
                int err = vkCreateSampler(ctxt.device, sampler, null, pSampler);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
                }
                this.sampler = pSampler.get(0);
                
                VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                        .viewType(VK_IMAGE_VIEW_TYPE_2D_ARRAY)
                        .format(this.internalFormat)
                        .components(VkComponentMapping.callocStack(stack));
                VkImageSubresourceRange viewSubResRange = view.subresourceRange();
                viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                viewSubResRange.baseMipLevel(0);
                viewSubResRange.baseArrayLayer(0);
                viewSubResRange.layerCount(this.texture.getNumLayers());
                // Linear tiling usually won't support mip maps
                // Only set mip map count if optimal tiling is used
                viewSubResRange.levelCount(this.texture.getNumMips());
                // The view will be based on the texture's image
                view.image(this.texture.getImage());
                LongBuffer pView = stack.longs(0);
                err = vkCreateImageView(ctxt.device, view, null, pView);
                if (err != VK_SUCCESS) {
                    throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
                }
                this.textureView = pView.get(0);
            }
        }
    } 
    @Override
    protected void initStorage() {
////      System.err.println(glid+"/"+numMipmaps+"/"+this.tileSize+"/"+this.numTextures);
//      GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);
//      Engine.checkGLError("pre glTexStorage3D");
//      nidefawl.qubes.gl.GL.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, numMipmaps, 
//              this.internalFormat,              //Internal format
//              this.tileSize, this.tileSize,   //width,height
//              this.numTextures       //Number of layers
//      );
//      glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, numMipmaps-1);
//      Engine.checkGLError("glTexStorage3D");
  }
    public long getView() {
        return this.textureView;
    }
    public long getSampler() {
        return this.sampler;
    }
    public int getImageLayout() {
        return this.texture.getImageLayout();
    }
    
    public void destroy() {
        unload();
        if (this.sampler != VK_NULL_HANDLE) {
            vkDestroySampler(Engine.vkContext.device, this.sampler, null);
            this.sampler = VK_NULL_HANDLE;
        }
        if (this.textureView != VK_NULL_HANDLE) {
            vkDestroyImageView(Engine.vkContext.device, this.textureView, null);
            this.textureView = VK_NULL_HANDLE;
        }
    }

}
