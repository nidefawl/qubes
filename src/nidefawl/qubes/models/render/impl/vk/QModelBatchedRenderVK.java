package nidefawl.qubes.models.render.impl.vk;

import static org.lwjgl.vulkan.VK10.*;
import static nidefawl.qubes.models.render.ModelConstants.*;
import static nidefawl.qubes.vulkan.VkConstants.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.vulkan.*;

public class QModelBatchedRenderVK extends QModelBatchedRender {
    final static int SSBO_DRAW_SIZE_MODEL = SIZE_OF_MAT4*MAX_INSTANCES;
    final static int SSBO_DRAW_SIZE_NORMAL = SIZE_OF_MAT4*MAX_INSTANCES;
    final static int SSBO_DRAW_SIZE_BONES = SIZE_OF_MAT4*MAX_INSTANCES*NUM_BONE_MATRICES;
    final static int SSBO_FRAME_SIZE_MODEL = MAX_DRAWS_FRAME*SSBO_DRAW_SIZE_MODEL;
    final static int SSBO_FRAME_SIZE_NORMAL = MAX_DRAWS_FRAME*SSBO_DRAW_SIZE_NORMAL;
    final static int SSBO_FRAME_SIZE_BONES = MAX_DRAWS_FRAME*SSBO_DRAW_SIZE_BONES;
    final static int SSBO_SIZE_MODEL = MAX_NUM_SWAPCHAIN*SSBO_FRAME_SIZE_MODEL;
    final static int SSBO_SIZE_NORMAL = MAX_NUM_SWAPCHAIN*SSBO_FRAME_SIZE_NORMAL;
    final static int SSBO_SIZE_BONES = MAX_NUM_SWAPCHAIN*SSBO_FRAME_SIZE_BONES;
    private VkSSBO vkNormalMat;
    private VkSSBO vkModelMat;
    private VkSSBO vkBones;
    private VkDescriptor descriptorSetSSBOBatched;
    private ByteBuffer[] buffers;
    private FloatBuffer[] floatbuffers;
    private VkDescriptor descriptorSetSSBOStatic;

    @Override
    public void render(float fTime) {

        if (this.renderer == 1) {
            Engine.setDescriptorSet(VkDescLayouts.DESC4, Engine.descriptorSetUboShadow);
        } else {
            Engine.clearDescriptorSet(VkDescLayouts.DESC4);
        }
        this.vkBones.markPos();
        this.vkModelMat.markPos();
        this.vkNormalMat.markPos();

        for (int i = 0; i < this.subLists.size(); i++) {
            if (i >= MAX_DRAWS_FRAME) {
                System.err.println("MAX_DRAWS_FRAME REACHED!");
                break;
            }
            QModelRenderSubList n = this.subLists.get(i);
            VkPipeline pipe;
            VkDescriptor descriptorSetSSBO;
//          System.out.println(Stats.fpsCounter+","+n.isSkinned+","+n.group.idx);
//            if (n.group.idx == 1)
//                continue;

//            Engine.debugflag = i == 1;
            if (!n.isSkinned) {
                pipe = VkPipelines.model_static[this.renderer];
                descriptorSetSSBO = this.descriptorSetSSBOStatic;
                this.begin();
                n.put(floatbuffers[0], floatbuffers[1]); 
                this.end();
            } else {
                pipe = VkPipelines.model_skinned[this.renderer];
                descriptorSetSSBO = this.descriptorSetSSBOBatched;
                this.begin();
                n.putSkinned(floatbuffers[0], floatbuffers[1], floatbuffers[2]);    
                this.end();
            }
            Engine.setDescriptorSet(VkDescLayouts.DESC2, n.tex.descriptorSetTex);
            Engine.setDescriptorSetF(VkDescLayouts.DESC3, descriptorSetSSBO);
            Engine.bindPipeline(pipe);
            if (this.renderer == 1) {
                PushConstantBuffer buf = PushConstantBuffer.INST;
                buf.setInt(0, this.shadowVP);
                vkCmdPushConstants(Engine.getDrawCmdBuffer(), pipe.getLayoutHandle(), VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf.getBuf(4));
            }
            Engine.debugflag = false;
            n.model.renderRestModel(n.object, n.group, n.instances);
        }
        Engine.clearDescriptorSet(VkDescLayouts.DESC4);
        this.vkBones.seekPos(SSBO_FRAME_SIZE_BONES);
        this.vkModelMat.seekPos(SSBO_FRAME_SIZE_MODEL);
        this.vkNormalMat.seekPos(SSBO_FRAME_SIZE_NORMAL);
    }
    private void end() {
        for (int i = 0; i < this.buffers.length; i++) {
            this.floatbuffers[i].flip();
            this.buffers[i].position(0).limit(this.floatbuffers[i].remaining() * 4);
        }
        this.vkModelMat.uploadData(this.buffers[0]);
        this.vkNormalMat.uploadData(this.buffers[1]);
        this.vkBones.uploadData(this.buffers[2]);
//        System.out.println(Stats.fpsCounter);
    }
    private void begin() {
        for (int i = 0; i < this.buffers.length; i++) {
            this.floatbuffers[i].clear();
        }
    }
    @Override
    public void init() {
        VKContext ctxt = Engine.vkContext;
        this.vkNormalMat = new VkSSBO(ctxt, SSBO_SIZE_NORMAL, SSBO_DRAW_SIZE_NORMAL).tag("ssbo_model_normal_mat");
        this.vkModelMat = new VkSSBO(ctxt, SSBO_SIZE_MODEL, SSBO_DRAW_SIZE_MODEL).tag("ssbo_model_model_mat"); 
        this.vkBones = new VkSSBO(ctxt, SSBO_SIZE_BONES, SSBO_DRAW_SIZE_BONES).tag("ssbo_model_bones");
        descriptorSetSSBOBatched = ctxt.descLayouts.allocDescSetSSBOModelBatched();
        descriptorSetSSBOBatched.setBindingSSBO(0, this.vkModelMat);
        descriptorSetSSBOBatched.setBindingSSBO(1, this.vkNormalMat);
        descriptorSetSSBOBatched.setBindingSSBO(2, this.vkBones);
        descriptorSetSSBOBatched.update(ctxt);
        descriptorSetSSBOStatic = ctxt.descLayouts.allocDescSetSSBOModelStatic();
        descriptorSetSSBOStatic.setBindingSSBO(0, this.vkModelMat);
        descriptorSetSSBOStatic.setBindingSSBO(1, this.vkNormalMat);
        descriptorSetSSBOStatic.update(ctxt);
        this.buffers = new ByteBuffer[3];
        this.floatbuffers = new FloatBuffer[3];
        for (int i = 0; i < this.buffers.length; i++) {
            this.buffers[i] = MemoryUtil.memCalloc(i==2?SSBO_SIZE_BONES:SSBO_SIZE_NORMAL);
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
