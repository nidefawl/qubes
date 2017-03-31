 package nidefawl.qubes.render.impl.vk;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.particle.CubeParticle;
import nidefawl.qubes.render.CubeParticleRenderer;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.util.RenderUtil;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.world.World;

public class CubeParticleRendererVK extends CubeParticleRenderer implements IRenderComponent {

    private BufferPair cube;
    private ByteBuffer bufferBlockInfo;
    private ByteBuffer bufferMat;
    private FloatBuffer bufferFloat;
    private IntBuffer bufferInt;
    private VkSSBO vkBlockInfo;
    private VkSSBO vkMat;
    private VkDescriptor descriptorSetSSBO;

    public void init() {
        super.init();
        VKContext ctxt = Engine.vkContext;
        this.vkBlockInfo = new VkSSBO(ctxt, 10*32*MAX_PARTICLES, 32*MAX_PARTICLES).tag("ssbo_blockinfo"); 
        this.vkMat = new VkSSBO(ctxt, 10*64*MAX_PARTICLES, 64*MAX_PARTICLES).tag("ssbo_mat");
        this.cube = ctxt.getFreeBuffer();
        VertexBuffer buf = new VertexBuffer(1024*1024);
        ReallocIntBuffer bufIntV = new ReallocIntBuffer();
        ReallocIntBuffer bufIntI = new ReallocIntBuffer();
        RenderUtil.makeCube(buf, 1.0f, GLVAO.vaoStaticModel);
        int intlen = buf.storeVertexData(bufIntV);
        int intlenIdx = buf.storeIndexData(bufIntI);
        this.cube.uploadDeviceLocal(bufIntV.getByteBuf(), intlen, bufIntI.getByteBuf(), intlenIdx);
        this.cube.setElementCount(intlenIdx);
        bufIntI.destroy();
        bufIntV.destroy();
        this.bufferBlockInfo = MemoryUtil.memCalloc(32*MAX_PARTICLES);
        this.bufferMat = MemoryUtil.memCalloc(64*MAX_PARTICLES);
        this.bufferInt = this.bufferBlockInfo.asIntBuffer();
        this.bufferFloat = this.bufferMat.asFloatBuffer();
        descriptorSetSSBO = ctxt.descLayouts.allocDescSetSSBOCubes().tag("SSBOCubes");
        descriptorSetSSBO.setBindingSSBO(0, this.vkBlockInfo);
        descriptorSetSSBO.setBindingSSBO(1, this.vkMat);
        descriptorSetSSBO.update(ctxt);
    }

    public void renderParticles(World world, float iPass, float fTime) {
        if (!this.particles.isEmpty()) {
            storeParticles(world, iPass, fTime);
            Engine.setDescriptorSet(VkDescLayouts.DESC1, RenderersVulkan.worldRenderer.getDescTextureTerrainNormals());
            Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboConstants);
            Engine.setDescriptorSet(VkDescLayouts.DESC3, this.descriptorSetSSBO);
            Engine.bindPipeline(VkPipelines.cube_particle);
            drawCubes(Engine.getDrawCmdBuffer());
        }
    }
    static long[] pointer = new long[1];
    static long[] offset = new long[1];
    private void drawCubes(CommandBuffer commandBuffer) {
        offset[0] = 0;
        pointer[0] = this.cube.vert.getBuffer();
        vkCmdBindVertexBuffers(commandBuffer, 0, pointer, offset);
        vkCmdBindIndexBuffer(commandBuffer, this.cube.idx.getBuffer(), 0, VK_INDEX_TYPE_UINT32);
        vkCmdDrawIndexed(commandBuffer, this.cube.elementCount, this.storedSprites, 0, 0, 0);
//        System.out.println(storedSprites+"/"+this.cube.elementCount);
    }

    void storeParticles(World w, float iPass, float fTime) {
        this.bufferInt.clear();
        this.bufferFloat.clear();
        storedSprites = 0;
        int offset=0;
        for (int i = 0; i < particles.size(); i++) {
            if (i >= MAX_PARTICLES) {
                System.err.println("too many particles");
                break;
            }
            CubeParticle cloud = particles.get(i);
            storedSprites+=cloud.store(offset, this.bufferFloat, this.bufferInt);
            offset++;
        }
        this.bufferFloat.flip();
        this.bufferInt.flip();
        this.bufferBlockInfo.position(0).limit(this.bufferInt.remaining()*4);
        this.bufferMat.position(0).limit(this.bufferFloat.remaining()*4);
        this.vkBlockInfo.uploadData(this.bufferBlockInfo);
        this.vkMat.uploadData(this.bufferMat);
    }
}
