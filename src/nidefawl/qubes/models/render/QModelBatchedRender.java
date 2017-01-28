package nidefawl.qubes.models.render;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.nio.FloatBuffer;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.vec.Matrix4f;

public class QModelBatchedRender extends QModelRender {
    public final static int NUM_FRAMES = 1000;
//    public final static ShaderBuffer        ssbo_model_modelmat         = new ShaderBuffer("QModel_mat_model")
//            .setSize(ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES, NUM_FRAMES).setMakePersistantMapped(true);
//    
//    public final static ShaderBuffer        ssbo_model_normalmat         = new ShaderBuffer("QModel_mat_normal")
//            .setSize(ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES, NUM_FRAMES).setMakePersistantMapped(true);
//
//    public final static ShaderBuffer        ssbo_model_bonemat         = new ShaderBuffer("QModel_mat_bone")
//            .setSize(ModelConstants.SIZE_OF_MAT4*ModelConstants.MAX_INSTANCES*ModelConstants.NUM_BONE_MATRICES, NUM_FRAMES).setMakePersistantMapped(true);
    
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
    
    public static final int MAX_INSTANCES = 16*1024;
    public static final int SIZE_OF_MAT4 = 16*4;
    public static final int NUM_BONE_MATRICES = 64;
    public static final int RENDERER_WORLD_MAIN = 0;
    public static final int RENDERER_WORLD_SHADOW = 1;
    public static final int RENDERER_WORLD_MODELVIEWER = 2;
    public static final int RENDERER_SCREEN_MODELVIEWER = 3;
    public final Shader[] shaderSkinned = new Shader[4];
    public final Shader[] shader = new Shader[4];
    private boolean           startup = true;
    private FloatBuffer bufBoneMat;
    private FloatBuffer bufNormalMat;
    private FloatBuffer bufModelMat;
    QModelRenderSubList[] tmpLists = new QModelRenderSubList[256];
    public List<QModelRenderSubList> subLists = Lists.newArrayList();
    public static Matrix4f tmpMat1 = new Matrix4f();

    private int nxtIdx;
    

    private int pass = 0;
    private int shadowVP = 0;
    private int renderer;
    private BufferedMatrix mvp;
    public void setPass(int pass, int shadowVP) {
        this.pass = pass;
        this.shadowVP = shadowVP;
        this.renderer = RENDERER_WORLD_MAIN;
        if (pass == WorldRenderer.PASS_SHADOW_SOLID) {
            this.renderer = RENDERER_WORLD_SHADOW;
        }
    }
    public void setRenderer(int renderer) {
        this.renderer = renderer;
    }

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

    
    static class ReallocFloatBuf {
        public int pos;
        public float[] matBuf = new float[0];
        public int realloc(int newSize) {
            if(matBuf.length!=0)
            System.out.println("realloc buffer to length "+newSize);
            float newBuffer[] = new float[newSize];
            System.arraycopy(this.matBuf, 0, newBuffer, 0, this.pos);
            this.matBuf = newBuffer;
            return this.matBuf.length;
        }

        public int left() {
            return this.matBuf.length-this.pos;
        }
        public void grow() {
            realloc(this.matBuf.length+32*16);
        }

        public void store(Matrix4f modelMat) {
            if (this.left() < 16) {
                realloc(this.matBuf.length+(32*16));
            }
            modelMat.store(this.matBuf, this.pos);
            this.pos+=16;
        }
        
    }
    static Matrix4f tmpMatrix1 = new Matrix4f();
    static Matrix4f tmpMatrix2 = new Matrix4f();
    static public class QModelRenderSubList {
        public boolean isSkinned;
        public ModelQModel     model;
        public QModelObject    object;
        public QModelGroup     group;
        public QModelMaterial  material;
        public QModelTexture   tex;
        public ReallocFloatBuf buf1 = new ReallocFloatBuf();
        public ReallocFloatBuf buf2 = new ReallocFloatBuf();
        public ReallocFloatBuf buf3 = new ReallocFloatBuf();
        
        public int instances;
        public boolean matches(ModelQModel model, QModelObject object, QModelGroup grp) {
            return this.model == model &&  
            this.object == object &&
            this.group == grp &&
            this.material == grp.material &&
            this.tex == grp.material.getBoundTexture() &&
            this.isSkinned == grp.isSkinned;
        }
        public void set(ModelQModel model, QModelObject object, QModelGroup grp) {
            this.model = model;
            this.object = object;
            this.group = grp;
            this.material = grp.material;
            this.tex = grp.material.getBoundTexture();
            this.isSkinned = grp.isSkinned;
        }

        public void add(QModelRender rend, QModelObject model, QModelGroup grp) {
            tmpMatrix1.setIdentity();
            tmpMatrix2.setIdentity();
            QModelBone bone = model.getAttachmentBone();
            QModelAbstractNode node = model.getAttachementNode();
            if (bone != null) {
                tmpMatrix1.translate(bone.posebone.getTailLocal());
                tmpMatrix1.mulMat(bone.posebone.matDeform);
                tmpMatrix2.mulMat(bone.posebone.matDeformNormal);
            }
            if (node != null) {
                tmpMatrix1.mulMat(node.getMatDeform());
                tmpMatrix2.mulMat(node.getMatDeformNormal());
            }
            tmpMatrix1.mulMat(rend.modelMat);
            tmpMatrix2.mulMat(rend.normalMat);
            this.buf1.store(tmpMatrix1);
            this.buf2.store(tmpMatrix2);
            if (model.isSkinned) {
                for (int j = 0; j < ModelConstants.NUM_BONE_MATRICES; j++) {
                    if (j < model.listBones.size()) {
                        QModelBone b1 = model.listBones.get(j);
                        QModelPoseBone jt = b1.posebone;
                        tmpMat1.load(b1.matRestInv);
                        tmpMat1.mulMat(jt.matDeform);
                    } else {
                        tmpMat1.setIdentity();
                    }
                    this.buf3.store(tmpMat1);
                }
            }
            this.instances++;
        }

        
        public void put(FloatBuffer bufModel, FloatBuffer bufNormal) {
            bufModel.put(this.buf1.matBuf, 0, this.buf1.pos);
            bufNormal.put(this.buf2.matBuf, 0, this.buf2.pos);
        }
        public void putSkinned(FloatBuffer bufModel, FloatBuffer bufNormal, FloatBuffer bufBones) {
            bufModel.put(this.buf1.matBuf, 0, this.buf1.pos);
            bufNormal.put(this.buf2.matBuf, 0, this.buf2.pos);
            bufBones.put(this.buf3.matBuf, 0, this.buf3.pos);
        }
        public void reset() {
            this.instances = 0;
            this.group = null;
            this.material = null;
            this.tex = null;
            this.buf1.pos = 0;
            this.buf2.pos = 0;
            this.buf3.pos = 0;
        }
    }

    public void reset() {
        for (int i = 0; i < this.nxtIdx; i++) {
            this.tmpLists[i].reset();
        }
        this.nxtIdx = 0;
        this.subLists.clear();
    }

    @Override
    public void addObject(QModelObject model) {
        for (QModelGroup grp : model.listGroups) {
            QModelRenderSubList s = getSubList(model, grp);
            s.add(this, model, grp);
        }
    }

    private QModelRenderSubList getSubList(QModelObject model, QModelGroup grp) {
        for (int i = 0; i < this.subLists.size(); i++) {
            QModelRenderSubList s = this.subLists.get(i);
            if (s.matches(this.model, model, grp)) {
                return s;
            }
        }
        int idx = nxtIdx++;
        QModelRenderSubList s = tmpLists[idx];
        if (s == null) {
            s = new QModelRenderSubList();
            tmpLists[idx] = s;
        }
        s.set(this.model, model, grp);
        this.subLists.add(s);
        return s;
    }


    public boolean isOverCapacity() {
        return false;
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
