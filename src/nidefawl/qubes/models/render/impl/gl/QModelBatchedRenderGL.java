package nidefawl.qubes.models.render.impl.gl;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.nio.FloatBuffer;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.render.ModelConstants;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.models.render.QModelRender;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.Matrix4f;

public class QModelBatchedRenderGL extends QModelBatchedRender {
    public final static ShaderBuffer        ssbo_model_modelmat         = new ShaderBuffer("QModel_mat_model")
            .setSize(ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES);
    
    public final static ShaderBuffer        ssbo_model_normalmat         = new ShaderBuffer("QModel_mat_normal")
            .setSize(ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES);

    public final static ShaderBuffer        ssbo_model_bonemat         = new ShaderBuffer("QModel_mat_bone")
            .setSize(ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES*ModelConstants.NUM_BONE_MATRICES);
    @Override
    public void preinit() {
//        System.out.println(ssbo_model_bonemat.getSize());
    }
    
    public final Shader[] shaderSkinned = new Shader[4];
    public final Shader[] shader = new Shader[4];
    private boolean           startup = true;
    private FloatBuffer bufBoneMat;
    private FloatBuffer bufNormalMat;
    private FloatBuffer bufModelMat;


    @Override
    public void render(float fTime) {
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("renderModel");
//        System.out.println(renderer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("modelbatchedssboupdate.render");
        for (QModelRenderSubList n : this.subLists) {
            if (!n.isSkinned) {
                this.begin();
                FloatBuffer bufModel = this.getBufModelMat();
                FloatBuffer bufNormal = this.getBufNormalMat();
                n.put(bufModel, bufNormal); 
                this.end();
                Shader shader = this.shader[this.renderer];
                shader.enable();
                shader.setProgramUniform1i("shadowSplit", this.shadowVP);
                Engine.bindVAO(GLVAO.vaoModelGPUSkinned);
                GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, n.tex.get());
                n.model.renderRestModel(n.object, n.group, n.instances);
                this.sync();
            } else {
                this.begin();
                FloatBuffer bufModel = this.getBufModelMat();
                FloatBuffer bufNormal = this.getBufNormalMat();
                FloatBuffer bufBones = this.getBufBoneMat();
                n.putSkinned(bufModel, bufNormal, bufBones);    
                this.end();
                Shader shader = this.shaderSkinned[this.renderer];
                shader.enable();
                shader.setProgramUniform1i("shadowSplit", this.shadowVP);
                Engine.bindVAO(GLVAO.vaoModelGPUSkinned);
                GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, n.tex.get());
                n.model.renderRestModel(n.object, n.group, n.instances);
                this.sync();
            }
        }
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
    }
    @Override
    public void upload(float fTime) {
    }
    public void initShaders() {

        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader[] newShader = new Shader[4];
            Shader[] newShaderSkinned = new Shader[4];
            for (int i = 0; i < 4; i++) {
                final int iRENDER = i;
                newShader[i] = assetMgr.loadShader(this, "model/model_fragment", "model/model_batched", null, null, new IShaderDef() {
                    @Override
                    public String getDefinition(String define) {
                        if ("RENDERER".equals(define))
                            return "#define RENDERER "+iRENDER;
                        return null;
                    }
                });
                newShaderSkinned[i] = assetMgr.loadShader(this, "model/model_fragment", "model/model_batched_skinned", null, null, new IShaderDef() {
                    @Override
                    public String getDefinition(String define) {
                        if ("RENDERER".equals(define))
                            return "#define RENDERER "+iRENDER;
                        if ("MAX_MODEL_MATS".equals(define))
                            return "#define MAX_MODEL_MATS "+ModelConstants.MAX_INSTANCES;
                        if ("MAX_NORMAL_MATS".equals(define))
                            return "#define MAX_NORMAL_MATS "+ModelConstants.MAX_INSTANCES;
                        if ("MAX_BONES".equals(define))
                            return "#define MAX_BONES "+(ModelConstants.MAX_INSTANCES*ModelConstants.NUM_BONE_MATRICES);
                        return null;
                    }
                });
            }
            popNewShaders();
            for (int i = 0; i < 4; i++) {
                shader[i] = newShader[i];
                shaderSkinned[i] = newShaderSkinned[i];
            }
            for (int i = 0; i < 4; i++) {
                shader[i].enable();
                shader[i].setProgramUniform1i("tex0", 0);
                shaderSkinned[i].enable();
                shaderSkinned[i].setProgramUniform1i("tex0", 0);
            }
            Shader.disable();
            startup = false;
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            throw e;
        }
        startup = false;
    
    }

    public void begin() {
        ssbo_model_modelmat.nextFrame();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("modelbatchedssboupdate.nextFrame1");
        ssbo_model_normalmat.nextFrame();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("modelbatchedssboupdate.nextFrame2");
        ssbo_model_bonemat.nextFrame();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("modelbatchedssboupdate.nextFrame3");
        this.bufModelMat = ssbo_model_modelmat.getFloatBuffer();
        this.bufNormalMat = ssbo_model_normalmat.getFloatBuffer();
        this.bufBoneMat = ssbo_model_bonemat.getFloatBuffer();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("modelbatchedssboupdate.begin");
    }

    public void end() {
        ssbo_model_modelmat.update();
        ssbo_model_normalmat.update();
        ssbo_model_bonemat.update();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("modelbatchedssboupdate.end");
    }

    public void sync() {
        ssbo_model_modelmat.sync();
        ssbo_model_normalmat.sync();
        ssbo_model_bonemat.sync();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("modelbatchedssboupdate.sync");
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
    
    public void init() {
        initShaders();
    }
    
    public void setForwardRenderMVP(BufferedMatrix mvp) {
        this.mvp=mvp;
        shader[RENDERER_SCREEN_MODELVIEWER].enable();
        shader[RENDERER_SCREEN_MODELVIEWER].setProgramUniformMatrix4("mvp", false, this.mvp.get(), false);
        shaderSkinned[RENDERER_SCREEN_MODELVIEWER].enable();
        shaderSkinned[RENDERER_SCREEN_MODELVIEWER].setProgramUniformMatrix4("mvp", false, this.mvp.get(), false);
    }

}
