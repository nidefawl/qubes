/**
 * 
 */
package nidefawl.qubes.gl;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Game;
import nidefawl.qubes.render.region.MeshedRegion;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class GLTriBuffer {

    private int      vbo;
    private int      vboIndices;
    ReallocIntBuffer vboBuf;
    ReallocIntBuffer vboIdxBuf;
    private int      triCount;
    private int      vertexCount;

    /**
     * @param buf
     */
    public void upload(VertexBuffer buf, int[] vertIdx) {
        if (this.vboBuf == null) {
            IntBuffer buff = Engine.glGenBuffers(2);
            this.vbo = buff.get(0);
            this.vboIndices = buff.get(1);
            this.vboBuf = new ReallocIntBuffer(1024);
            this.vboIdxBuf = new ReallocIntBuffer(1024);
        }
        int numInts = buf.putIn(this.vboBuf);
        this.vertexCount = buf.vertexCount;
        this.triCount = vertIdx.length/3;

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, numInts * 4L, this.vboBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData ");
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndices);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vboIndices);

        this.vboIdxBuf.reallocBuffer(vertIdx.length);
        this.vboIdxBuf.put(vertIdx);
        
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, vertIdx.length * 4, this.vboIdxBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData");
    
    }
    

    /**
     * 
     */
    public void bind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndices);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vboIndices);
    }

    /**
     * @param ptrSetting
     */
    public void draw(int ptrSetting) {
        MeshedRegion.enableVertexPtrs(ptrSetting);
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.triCount * 3, GL11.GL_UNSIGNED_INT, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        MeshedRegion.disableVertexPtrs(ptrSetting);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 
     */
    public void release() {
        if (this.vboBuf != null) {
            Engine.deleteBuffers(this.vbo, this.vboIndices);
            this.vboBuf.release();
            this.vboIdxBuf.release();
            this.vboBuf = null;
            this.vboIdxBuf = null;
        }
    }
}
