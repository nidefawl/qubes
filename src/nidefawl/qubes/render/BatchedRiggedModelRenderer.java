package nidefawl.qubes.render;

import java.nio.FloatBuffer;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.vec.Matrix4f;

public class BatchedRiggedModelRenderer extends AbstractRenderer {
    public static final int MAX_INSTANCES = 16*1024;
    public static final int SIZE_OF_MAT4 = 16*4;
    public static final int SIZE_OF_VEC4 = 4*4;
    public static final int NUM_BONE_MATRICES = 64;
    public Shader           shaderModelShadow;
    public Shader          shaderModelBatched;
    public Shader          shaderModelBatchedSkinned;
    private boolean           startup = true;
    FloatBuffer buf;
    private int position;
    private FloatBuffer bufBoneMat;
    private FloatBuffer bufNormalMat;
    private FloatBuffer bufModelMat;
     

    
    public BatchedRiggedModelRenderer() {
    }
    
    public void initShaders() {

        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader newShaderModelBatched = assetMgr.loadShader(this, "model/model_batched");
            Shader newShaderModelBatchedSkinned = assetMgr.loadShader(this, "model/model_batched_skinned");
            Shader modelShadow = assetMgr.loadShader(this, "model/model_batched_skinned", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("SHADOW_PASS".equals(define))
                        return "#define SHADOW_PASS 1";
                    return null;
                }
            });
            popNewShaders();
            this.shaderModelShadow = modelShadow;
            this.shaderModelBatched = newShaderModelBatched;
            this.shaderModelBatchedSkinned = newShaderModelBatchedSkinned;
            this.shaderModelBatched.enable();
            this.shaderModelBatched.setProgramUniform1i("tex0", 0);
            this.shaderModelBatchedSkinned.enable();
            this.shaderModelBatchedSkinned.setProgramUniform1i("tex0", 0);
            this.shaderModelShadow.enable();
            this.shaderModelShadow.setProgramUniform1i("tex0", 0);
            Shader.disable();
            startup = false;
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            throw e;
        }
        startup = false;
    
    }

    public void init() {
        initShaders();
    }

    public void begin() {
        this.position = 0;
//        this.buf = UniformBuffer.BoneMatUBO.getFloatBuffer();
        
//        Engine.boneMatrices.bind();
//        Engine.boneMatrices.map(true);
//        this.buf = Engine.boneMatrices.getMappedBufFloat();

//        Engine.ssbo_model_modelmat.bind();
//        Engine.ssbo_model_modelmat.map(true);
        this.bufModelMat = Engine.ssbo_model_modelmat.getFloatBuffer();
//        Engine.ssbo_model_normalmat.bind();
//        Engine.ssbo_model_normalmat.map(true);
        this.bufNormalMat = Engine.ssbo_model_normalmat.getFloatBuffer();
//        Engine.ssbo_model_bonemat.bind();
//        Engine.ssbo_model_bonemat.map(true);
        this.bufBoneMat = Engine.ssbo_model_bonemat.getFloatBuffer();
        this.bufModelMat.clear();
        this.bufNormalMat.clear();
        this.bufBoneMat.clear();
    }
    public void end() {
//        Engine.boneMatrices.unmap();
//        Engine.boneMatrices.unbind();
//        UniformBuffer.BoneMatUBO.update();
//        Engine.ssbo_model_modelmat.bind();
//        Engine.ssbo_model_modelmat.unmap();
//        Engine.ssbo_model_modelmat.unbind();
//        Engine.ssbo_model_normalmat.bind();
//        Engine.ssbo_model_normalmat.unmap();
//        Engine.ssbo_model_normalmat.unbind();
//        Engine.ssbo_model_bonemat.bind();
//        Engine.ssbo_model_bonemat.unmap();
//      Engine.ssbo_model_bonemat.unbind();
        this.bufModelMat.flip();
        this.bufNormalMat.flip();
        this.bufBoneMat.flip();
      Engine.ssbo_model_modelmat.update();
      Engine.ssbo_model_normalmat.update();
      Engine.ssbo_model_bonemat.update();
    }

    public FloatBuffer getBuf() {
        return this.buf;
    }

    public FloatBuffer getBufModelMat() {
        return this.bufModelMat;
    }

    public FloatBuffer getBufNormalMat() {
        return this.bufNormalMat;
    }

    public FloatBuffer getBufBoneMat() {
        return this.bufBoneMat;
    }

    public void submitModel() {
        this.position++;
//      this.buf.position((this.position*STRUCT_SIZE)>>2);
        this.bufModelMat.position((this.position*SIZE_OF_MAT4)>>2);
        this.bufNormalMat.position((this.position*SIZE_OF_MAT4)>>2);
        this.bufBoneMat.position((this.position*SIZE_OF_MAT4*NUM_BONE_MATRICES)>>2);
    }

    public int getNumModels() {
        return this.position;
    }

    public Shader getShader(int pass, int shadowVP) {
        Shader shader = this.shaderModelBatched;
        if (pass == WorldRenderer.PASS_SHADOW_SOLID) {
            shader = this.shaderModelShadow;
        }
        return shader;
    }

    public boolean isOverCapacity() {
        return this.position+1>=MAX_INSTANCES;
//        return this.buf.remaining()<STRUCT_SIZE;
    }
}
