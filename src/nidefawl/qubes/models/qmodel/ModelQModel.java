/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class ModelQModel {

    public final ModelLoaderQModel loader;
    public VertexBuffer buf = new VertexBuffer(1024*64);
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


    /**
     * @param vector3f
     * @return
     */
    protected int packNormal(Vector3f v) {
        byte byte0 = (byte)(int)(v.x * 127F);
        byte byte1 = (byte)(int)(v.y * 127F);
        byte byte2 = (byte)(int)(v.z * 127F);
        int normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
        return normal;
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
}
