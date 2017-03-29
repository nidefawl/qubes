/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.vulkan.VK10.*;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import com.google.common.collect.Lists;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.util.RenderUtil;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vulkan.BufferPair;
import nidefawl.qubes.vulkan.CommandBuffer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class ModelQModel {

    public final ModelLoaderQModel loader;
    private ModelRenderObject[] objects;
    Vector3f tmpVec = new Vector3f();
    Vector3f tmpVec2 = new Vector3f();
    public Matrix4f tmpMat1 = new Matrix4f();
    Matrix4f tmpMat2 = new Matrix4f();
    
    public static class ModelRenderGroup {
        public GLTriBuffer gpuBufRest = null;
        public GLTriBuffer gpuBuf = null;
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

        
        Stats.modelDrawCalls++;

        if (Engine.isVulkan) {
            if (rGroup.gpuBufRest.getTriCount()*3 > 0) {
                CommandBuffer commandBuffer = Engine.getDrawCmdBuffer();
                BufferPair bufferPair = rGroup.gpuBufRest.getVkBuffer();
                offset[0] = 0;
                pointer[0] = bufferPair.vert.getBuffer();
                vkCmdBindVertexBuffers(commandBuffer, 0, pointer, offset);
                vkCmdBindIndexBuffer(commandBuffer, bufferPair.idx.getBuffer(), 0, VK_INDEX_TYPE_UINT32);
                vkCmdDrawIndexed(commandBuffer, rGroup.gpuBufRest.getTriCount()*3, instances, 0, 0, 0);
                bufferPair.flagUse(commandBuffer.frameIdx);
            } else {
//                System.err.println("attempt to draw empty buffer");
            }
        } else {
            if ("axe".equals(this.getName())) {
                System.err.println(rGroup.gpuBufRest.getTriCount()*3);
            }
            Engine.bindBuffer(rGroup.gpuBufRest.getVbo());
            Engine.bindIndexBuffer(rGroup.gpuBufRest.getVboIndices());
            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, rGroup.gpuBufRest.getTriCount()*3, GL11.GL_UNSIGNED_INT, 0, instances);

        }
    }

    static long[] pointer = new long[1];
    static long[] offset = new long[1];

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
    public QModelTexture getQModelTexture(int texIdx) {
        return this.loader.listTextures.get(texIdx);
    }


    public ModelRenderObject getGroup(int object) {
        if (object >= this.objects.length) {
            System.err.println("Requested model object "+object+" but model only has "+this.objects.length+" objects");
            return null;
        }
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

    public abstract void draw();


}
