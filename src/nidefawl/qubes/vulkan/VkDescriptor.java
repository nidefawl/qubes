package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.IBufferDynamicOffset;
import nidefawl.qubes.shader.UniformBuffer;

public class VkDescriptor {
    public String tag;
    static class VkDescriptorBinding {
        int type;
        VkDescriptorBufferInfo.Buffer descBufInfo;
        VkDescriptorImageInfo.Buffer descImgInfo;
        public IBufferDynamicOffset dynamicOffset;
    }
    int numBindings = 0;
    VkDescriptorBinding[] bindings = new VkDescriptorBinding[9];
    private final long descriptorSet;
    int[] overrideOffsets = null;
    public VkDescriptor(long descriptorSet) {
        this.descriptorSet = descriptorSet;
        for (int i = 0; i < bindings.length; i++) {
            bindings[i] = new VkDescriptorBinding();
        }
    }

    public void setBindingUniformBuffer(int i, UniformBuffer buffer) {
        this.numBindings = Math.max(this.numBindings, i+1);
        bindings[i].type = buffer.isConstant() ? VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER : VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
        bindings[i].descBufInfo = buffer.getDescriptorBuffer();
        bindings[i].dynamicOffset = null;
        if (!buffer.isConstant()) {
            bindings[i].dynamicOffset = buffer;
        }
    }
    public void setBindingSSBO(int i, VkSSBO vkBlockInfo) {
        this.numBindings = Math.max(this.numBindings, i+1);
        bindings[i].type = vkBlockInfo.isConstant() ? VK_DESCRIPTOR_TYPE_STORAGE_BUFFER: VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC;
        bindings[i].descBufInfo = vkBlockInfo.getDescriptorBuffer();
        bindings[i].dynamicOffset = null;
        if (!vkBlockInfo.isConstant()) {
            bindings[i].dynamicOffset = vkBlockInfo;
        }
    }

    public void setBindingBuffer(int i, int type, VkDescriptorBufferInfo.Buffer descBufInfo) {
        this.numBindings = Math.max(this.numBindings, i+1);
        bindings[i].type = type;
        bindings[i].descBufInfo = descBufInfo;
        bindings[i].dynamicOffset = null;
    }
    
    public void update(VKContext ctxt) {
        try ( MemoryStack stack = stackPush() ) {
            VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.callocStack(this.numBindings, stack);
            for (int i = 0; i < this.numBindings; i++) {
                VkInitializers.writeDescriptorSet(writeDescriptorSet, i, this.descriptorSet, bindings[i].type, i, 
                        bindings[i].descBufInfo);
                writeDescriptorSet.get(i)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(this.descriptorSet)
                .descriptorType(bindings[i].type)
                .dstBinding(i)
                .pImageInfo(bindings[i].descImgInfo)
                .pBufferInfo(bindings[i].descBufInfo);
            }
            vkUpdateDescriptorSets(ctxt.device, writeDescriptorSet, null);
        }
    }

    public void setBindingCombinedImageSampler(int i, long textureView, long sampler, int imageLayout) {
        this.numBindings = Math.max(this.numBindings, i + 1);
        bindings[i].dynamicOffset = null;
        if (bindings[i].descImgInfo != null) {
            bindings[i].descImgInfo.free();
        }
        bindings[i].type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        VkDescriptorImageInfo.Buffer textureDescriptor = VkDescriptorImageInfo.calloc(1);
        textureDescriptor.imageView(textureView);
        textureDescriptor.sampler(sampler);
        textureDescriptor.imageLayout(imageLayout);
        bindings[i].descImgInfo = textureDescriptor;
    }

    public void setBindingStorageImage(int i, long textureView, int imageLayout) {
        this.numBindings = Math.max(this.numBindings, i + 1);
        bindings[i].dynamicOffset = null;
        if (bindings[i].descImgInfo != null) {
            bindings[i].descImgInfo.free();
        }
        bindings[i].type = VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
        VkDescriptorImageInfo.Buffer textureDescriptor = VkDescriptorImageInfo.calloc(1);
        textureDescriptor.imageView(textureView);
        textureDescriptor.sampler(0L);
        textureDescriptor.imageLayout(imageLayout);
        bindings[i].descImgInfo = textureDescriptor;
    }

    public long get() {
        return this.descriptorSet;
    }

    public VkDescriptor tag(String tag) {
        this.tag = tag;
        return this;
    }
    public void addDynamicOffsets(IntBuffer pOffsets) {
        if (this.overrideOffsets != null) {
            pOffsets.put(this.overrideOffsets);
        } else {
            for (int i = 0; i < this.numBindings; i++) {
                if (this.bindings[i].dynamicOffset != null) {
//                    if (Engine.debugflag)
//                    System.out.println("bind set "+i+": "+tag +" at offset "+this.bindings[i].dynamicOffset.getDynamicOffset());
//                    if (this.bindings[i].dynamicOffset == UniformBuffer.uboMatrixShadow) {
//                        int offset = this.bindings[i].dynamicOffset.getDynamicOffset();
//                        int alignRet = -1;
//                        for (int z = 24; z >= 0; z--) {
//                            if ((offset & ((1 << z) - 1)) == 0) {
//                                alignRet = 1 << z;
//                                break;
//                            }
//                        }
//                        System.out.println("bind offset " + this.bindings[i].dynamicOffset.getDynamicOffset()+" align: "+alignRet);
//                    }
                    pOffsets.put(this.bindings[i].dynamicOffset.getDynamicOffset());
                }
            }
        }
    }

    public void setOverrideOffset(int[] ssboOffsets) {
        this.overrideOffsets = ssboOffsets;
    }
    @Override
    public String toString() {
        return "VkDescriptor["+Long.toHexString(this.descriptorSet)+","+this.tag+"]";
    }
}
