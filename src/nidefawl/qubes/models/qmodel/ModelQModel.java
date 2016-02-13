/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.qmodel.ModelQModel.ModelRenderObject;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class ModelQModel {

    public final ModelLoaderQModel loader;
    private ModelRenderObject[] objects;
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
