/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class ModelQModel {

    public final ModelLoaderQModel loader;
    public VertexBuffer buf;
//    public VertexBuffer shadowBuf = new VertexBuffer(1024*16);
    public GLTriBuffer gpuBuf = null;
//    public GLTriBuffer gpuShadowBuf = null;
    public boolean needsDraw = true;
    public long reRender=0;
    /**
     * @param loader2
     */
    public ModelQModel(ModelLoaderQModel loader) {
        this.loader = loader;
    }



    public void release() {
        if (this.gpuBuf != null) {
            this.gpuBuf.release();
            this.gpuBuf = null;
//            this.gpuShadowBuf.release();
//            this.gpuShadowBuf = null;
            this.buf = null;
//            this.shadowBuf = null;
        }
        for (int i = 0; i < this.loader.listTextures.size(); i++) {
            this.loader.listTextures.get(i).release();
        }
    }
    public abstract QModelType getType();


    /**
     * @param i
     * @param f
     */
    public void animate(int i, float f) {
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
    public abstract void render(float fTime);


    public void bindTextures() {
        if (this.loader.listTextures.isEmpty()) {
//            System.out.println("no tex "+this.loader.getModelName());
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, 0);
            return;
        }
        //TODO: multitexturing (requires shader changes)
        for (int i = 0; i < this.loader.listTextures.size(); i++) {
            QModelTexture texture = this.loader.listTextures.get(i);
            GL.bindTexture(GL_TEXTURE0+i, GL_TEXTURE_2D, texture.get());
        }
    }
}
