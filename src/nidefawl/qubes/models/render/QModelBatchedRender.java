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
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.Matrix4f;

public abstract class QModelBatchedRender extends QModelRender {
    public final static int NUM_FRAMES = 1000;
    public static final int MAX_INSTANCES = 16*1024;
    public static final int SIZE_OF_MAT4 = 16*4;
    public static final int NUM_BONE_MATRICES = 64;
    public static final int RENDERER_WORLD_MAIN = 0;
    public static final int RENDERER_WORLD_SHADOW = 1;
    public static final int RENDERER_WORLD_MODELVIEWER = 2;
    public static final int RENDERER_SCREEN_MODELVIEWER = 3;
    static Matrix4f tmpMatrix1 = new Matrix4f();
    static Matrix4f tmpMatrix2 = new Matrix4f();
    public static Matrix4f tmpMat1 = new Matrix4f();
    @Override
    public void preinit() {
//        System.out.println(ssbo_model_bonemat.getSize());
    }
    
    protected boolean           startup = true;
    protected QModelRenderSubList[] tmpLists = new QModelRenderSubList[256];
    public List<QModelRenderSubList> subLists = Lists.newArrayList();

    protected int nxtIdx;
    protected int pass = 0;
    protected int shadowVP = 0;
    protected int renderer;
    protected BufferedMatrix mvp;
    
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

    public abstract void render(float fTime);

    public abstract void begin();
    public abstract void end();
    public abstract void sync();


    
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
    public abstract void setForwardRenderMVP(BufferedMatrix temp);
}
