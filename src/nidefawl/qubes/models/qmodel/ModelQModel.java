/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import com.google.common.collect.Lists;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.qmodel.ModelQModel.ModelRenderGroup;
import nidefawl.qubes.models.qmodel.ModelQModel.ModelRenderObject;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.util.RenderUtil;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class ModelQModel {

    public final ModelLoaderQModel loader;
    private ModelRenderObject[] objects;
    protected VertexBuffer vbuf;
    Vector3f tmpVec = new Vector3f();
    Vector3f tmpVec2 = new Vector3f();
    public Matrix4f tmpMat1 = new Matrix4f();
    Matrix4f tmpMat2 = new Matrix4f();
    public boolean needsDraw = true;
    
    public static class ModelRenderGroup {
        public GLTriBuffer gpuBufRest = null;
        public GLTriBuffer gpuBuf = null;
        public long reRender=0;
        public void release() {
            if (this.gpuBufRest != null) {
                this.gpuBufRest.release();
                this.gpuBufRest = null;
            }
            if (this.gpuBuf != null) {
                this.gpuBuf.release();
                this.gpuBuf = null;
            }
        }
    }
    public static class ModelRenderObject {
        List<ModelRenderGroup> list = Lists.newArrayList();
        public void release() {
            for (ModelRenderGroup g : list) {
                g.release();
            }
            list.clear();
        }
        public ModelRenderGroup getGroup(int g) {
            while (list.size() <= g) {
                list.add(new ModelRenderGroup());
            }
            return list.get(g);
        }
    }
    /**
     * @param loader2
     */
    public ModelQModel(ModelLoaderQModel loader) {
        this.loader = loader;
        this.objects = new ModelRenderObject[loader.listObjects.size()];
    }

    public void release() {
        for (int i = 0; this.objects != null && i < objects.length; i++) {
            if (this.objects[i] != null) {
                this.objects[i].release();
                this.objects[i] = null;
            }
        }
        for (int i = 0; i < this.loader.listTextures.size(); i++) {
            this.loader.listTextures.get(i).release();
        }
        this.loader.listTextures.clear();
    }


    public abstract QModelType getType();


    /**
     * @param i
     * @param f
     */
    public void animate(QModelProperties properties, float fabs, float f) {
    }
    /**
     * @param i
     * @param f
     */
    public void animateNodes(QModelProperties properties, float fabs, float f) {
    }


    /**
     * @param angle
     * @param angle2
     */
    public void setHeadOrientation(float angle, float angle2) {
    }


    /**
     * @param f
     */
    public abstract void render(int object, int group, float f);
    public void renderRestModel(QModelObject obj, QModelGroup grp, int instances) {
        ModelRenderObject rObj = this.getGroup(obj.idx);
        ModelRenderGroup rGroup = rObj.getGroup(grp.idx);
        if (rGroup.gpuBufRest == null /*|| (System.currentTimeMillis()-rGroup.reRender>1000)*/) {
            if (rGroup.gpuBufRest != null) {
                rGroup.gpuBufRest.release();
            }
            rGroup.reRender = System.currentTimeMillis();
            if (this.vbuf == null)
                this.vbuf = new VertexBuffer(1024*64);
            this.vbuf.reset();
            int vPosI = 0;
//            int[] vPos = new int[this.loader.listTri.size()*3];
//            Arrays.fill(vPos, -1);
            for (QModelTriangle triangle : grp.listTri) {
                for (int i = 0; i < 3; i++) {
                    int idx = triangle.vertIdx[i];
//                    if (vPos[idx] < 0) { // shared vertices require per vertex UVs -> requires exporter to be adjusted
                    // but also gives worse performance
//                        vPos[idx] =
//                                vPosI++;
                        QModelVertex v = obj.listVertex.get(idx);
                        vbuf.put(Float.floatToRawIntBits(v.x));
                        vbuf.put(Float.floatToRawIntBits(v.y));
                        vbuf.put(Float.floatToRawIntBits(v.z));
                        tmpVec.set(triangle.normal[i]);
                        vbuf.put(RenderUtil.packNormal(tmpVec));
                        vbuf.put(Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i])));
                        int bones03 = 0;
                        int bones47 = 0;
                        for (int w = 0; w < 4; w++) {
                            int boneIdx = (0 + w) >= v.numBones ? 0xFF : v.bones[0 + w];
                            int boneIdx2 = (4 + w) >= v.numBones ? 0xFF : v.bones[4 + w];
                            bones03 |= (boneIdx) << (w * 8);
                            bones47 |= (boneIdx2) << (w * 8);
                        }
                        vbuf.put(bones03);
                        vbuf.put(bones47);
                        for (int w = 0; w < 4; w++) {
                            vbuf.put(Half.fromFloat(v.weights[w * 2 + 1]) << 16 | (Half.fromFloat(v.weights[w * 2 + 0])));
                        }
                        vbuf.increaseVert();
//                    } else {
//                        System.out.println("reuse vert");
//                    }
                    vbuf.putIdx(vPosI++);
                }
            }
            rGroup.gpuBufRest = new GLTriBuffer(GL15.GL_DYNAMIC_DRAW);

            int bytes = rGroup.gpuBufRest.upload(vbuf);
//            System.out.println("byte size upload "+bytes+", "+rGroup.gpuBufRest.getVertexCount());
//            System.out.println(""+rGroup.gpuBufRest.getVertexCount()+" vertices, "+rGroup.gpuBufRest.getTriCount()+" tris, "+rGroup.gpuBufRest.getIdxCount()+" indexes");

        }
//        this.gpuBufRest.draw();
        Stats.modelDrawCalls++;

//        if (GPUProfiler.PROFILING_ENABLED)
//            GPUProfiler.start("render_"+this.loader.getModelName()+"_"+obj.name+"_"+grp.name);
        Engine.bindBuffer(rGroup.gpuBufRest.getVbo());
        Engine.bindIndexBuffer(rGroup.gpuBufRest.getVboIndices());
//        System.out.println(instances);
        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, rGroup.gpuBufRest.getTriCount()*3, GL11.GL_UNSIGNED_INT, 0, instances);

//        if (GPUProfiler.PROFILING_ENABLED)
//            GPUProfiler.end();
    }

    public void bindTextures(int texIdx) {
        if (this.loader.listTextures.isEmpty()) {
//            System.out.println("no tex "+this.loader.getModelName());
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, 0);
            return;
        }
        if (texIdx < 0 || texIdx >= this.loader.listTextures.size()) {
            //TODO: multitexturing (requires shader changes)
            for (int i = 0; i < this.loader.listTextures.size(); i++) {
                QModelTexture texture = this.loader.listTextures.get(i);
                GL.bindTexture(GL_TEXTURE0+i, GL_TEXTURE_2D, texture.get());
            }
            return;
        }
        QModelTexture texture = this.loader.listTextures.get(texIdx);
        GL.bindTexture(GL_TEXTURE0+texIdx, GL_TEXTURE_2D, texture.get());
    }


    public ModelRenderObject getGroup(int object) {
        ModelRenderObject rgroup = this.objects[object];
        if (rgroup == null) {
            rgroup = this.objects[object] = new ModelRenderObject();
        }
        return rgroup;
    }

    public List<QModelObject> getObjects() {
        return this.loader.listObjects;
    }

    public String getName() {
        return this.loader.getModelName();
    }

}
