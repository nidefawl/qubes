package nidefawl.qubes.models.render.impl.vk;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.render.ModelConstants;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.models.render.QModelBatchedRender.QModelRenderSubList;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.vulkan.*;

public class QModelBatchedRenderVK extends QModelBatchedRender {

    private VkSSBO vkNormalMat;
    private VkSSBO vkModelMat;
    private VkSSBO vkBones;
    private VkDescriptor descriptorSetSSBO;
    private ByteBuffer[] buffers;
    private FloatBuffer[] floatbuffers;

    @Override
    public void render(float fTime) {
        for (QModelRenderSubList n : this.subLists) {
            VkPipeline pipe;
            if (!n.isSkinned) {
                pipe = VkPipelines.model_static[this.renderer];
                this.begin();
                n.put(floatbuffers[0], floatbuffers[1]); 
                this.end();
            } else {
                pipe = VkPipelines.model_skinned[this.renderer];
                this.begin();
                n.putSkinned(floatbuffers[0], floatbuffers[1], floatbuffers[2]);    
                this.end();
            }
            Engine.bindPipeline(pipe);
            if (this.renderer == 1) {
                PushConstantBuffer buf = PushConstantBuffer.INST;
                buf.setInt(0, this.shadowVP);
                vkCmdPushConstants(Engine.getDrawCmdBuffer(), pipe.getLayoutHandle(), VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf.getBuf(4));
            }
            n.model.renderRestModel(n.object, n.group, n.instances);
        }
    }
    private void end() {
        for (int i = 0; i < this.buffers.length; i++) {
            this.floatbuffers[i].flip();
            this.buffers[i].position(0).limit(this.floatbuffers[i].remaining() * 4);
        }
        this.vkModelMat.uploadData(this.buffers[0]);
        this.vkNormalMat.uploadData(this.buffers[1]);
        this.vkBones.uploadData(this.buffers[2]);
    }
    private void begin() {
        for (int i = 0; i < this.buffers.length; i++) {
            this.floatbuffers[i].clear();
        }
    }
    @Override
    public void init() {
        VKContext ctxt = Engine.vkContext;
        int sizeNormals = ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES;
        int sizeModelMat = ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES;
        int sizeBones = ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES*ModelConstants.NUM_BONE_MATRICES;
        this.vkNormalMat = new VkSSBO(ctxt, 10*sizeNormals, sizeNormals).tag("ssbo_model_normal_mat");
        this.vkModelMat = new VkSSBO(ctxt, 10*sizeModelMat, sizeModelMat).tag("ssbo_model_model_mat"); 
        this.vkBones = new VkSSBO(ctxt, 10*sizeBones, sizeBones).tag("ssbo_mat");
        descriptorSetSSBO = ctxt.descLayouts.allocDescSetSSBOModelBatched().tag("SSBOModel");
        descriptorSetSSBO.setBindingSSBO(0, this.vkNormalMat);
        descriptorSetSSBO.setBindingSSBO(1, this.vkModelMat);
        descriptorSetSSBO.setBindingSSBO(2, this.vkBones);
        descriptorSetSSBO.update(ctxt);
        this.buffers = new ByteBuffer[3];
        for (int i = 0; i < this.buffers.length; i++) {
            this.buffers[i] = MemoryUtil.memCalloc(sizeBones);
            this.floatbuffers[i] = this.buffers[i].asFloatBuffer();
        }
    }

    @Override
    public void initShaders() {
    }

    @Override
    public void setForwardRenderMVP(BufferedMatrix mvp) {
        this.mvp=mvp;
    }

}
