package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

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
    VkDescriptorBinding[] bindings = new VkDescriptorBinding[8];
    private final long descriptorSet;
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

    public long get() {
        return this.descriptorSet;
    }

    public VkDescriptor tag(String tag) {
        this.tag = tag;
        return this;
    }

    public void addDynamicOffsets(IntBuffer pOffsets) {
        for (int i = 0; i < this.numBindings; i++) {
            if (this.bindings[i].dynamicOffset != null) {
                pOffsets.put(this.bindings[i].dynamicOffset.getDynamicOffset());
            }
        }
    }
}
