package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.util.GameMath;

public class VkShader {

    static VkShaderModuleCreateInfo.Buffer moduleCreateInfo = VkInitializers.shaderModuleCreateInfo();
    static ByteBuffer directBufShaderCode = MemoryUtil.memCalloc(1024*1024*5);
    static final LongBuffer pShaderModule = MemoryUtil.memAllocLong(1);
    private String name;
    public byte[] bin;
    private long shaderModule = VK_NULL_HANDLE;
    private VKContext ctxt;
    private int stage;

    public VkShader(VKContext ctxt, int stage, String name, byte[] bin) {
        this.ctxt = ctxt;
        this.stage = stage;
        this.name = name;
        this.bin = bin;
    }
    
    public String getName() {
        return this.name;
    }

    public long getShaderModule() {
        return this.shaderModule;
    }

    public void buildShader() {
        directBufShaderCode.clear();
        if (directBufShaderCode.capacity() < bin.length) {
            directBufShaderCode = MemoryUtil.memRealloc(directBufShaderCode, (int) GameMath.nextPowerOf2(bin.length));
        }
        directBufShaderCode.put(bin);
        directBufShaderCode.flip();
        moduleCreateInfo.pCode(directBufShaderCode);
        int err = vkCreateShaderModule(this.ctxt.device, moduleCreateInfo.get(0), null, pShaderModule);
        this.shaderModule = pShaderModule.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("vkCreateDescriptorSetLayout failed: " + VulkanErr.toString(err));
        }
        this.ctxt.addShader(this);
        
    }

    public void destroy() {
        _destroy(false);
    }
    protected void destroyShutdown() {
        _destroy(true);
    }

    private void _destroy(boolean isshutdown) {
        if (shaderModule != VK_NULL_HANDLE) {
            vkDestroyShaderModule(ctxt.device, this.shaderModule, null);
            if (!isshutdown) {
                ctxt.removeShader(this);
            }
        }
    }
    public int getStage() {
        return this.stage;
    }

    public static void destroyStatic() {
        moduleCreateInfo.free();
        memFree(VkShader.directBufShaderCode);
        memFree(VkShader.pShaderModule);
    }

}
