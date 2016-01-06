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
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class GLQuadBuffer {

    private int      vbo;
    private int      vboIndices;
    ReallocIntBuffer vboBuf;
    ReallocIntBuffer vboIdxBuf;
    private int faceCount;
    private int vertexCount;
    /**
     * @param buf
     */
    public void upload(VertexBuffer buf) {
        if (this.vboBuf == null) {
            IntBuffer buff = Engine.glGenBuffers(2);
            this.vbo = buff.get(0);
            this.vboIndices = buff.get(1);
            this.vboBuf = new ReallocIntBuffer(1024);
            this.vboIdxBuf = new ReallocIntBuffer(1024);
        }
        int numInts = buf.putIn(this.vboBuf);
        this.faceCount = buf.faceCount;
        this.vertexCount = buf.vertexCount;

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, numInts * 4L, this.vboBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData ");
        if (Engine.USE_TRIANGLES) {
            numInts = VertexBuffer.createIndex(this.faceCount * 2, this.vboIdxBuf);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndices);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBuffer " + vboIndices);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, numInts * 4, this.vboIdxBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBufferData");
        }
    }

    /**
     */
    public void draw() {
        Engine.bindBuffer(vbo);
        if (Engine.USE_TRIANGLES) {
            Engine.bindIndexBuffer(vboIndices);
            GL11.glDrawElements(GL11.GL_TRIANGLES, this.faceCount * 2 * 3, GL11.GL_UNSIGNED_INT, 0);
        } else {
            GL11.glDrawArrays(GL11.GL_QUADS, 0, this.vertexCount);
        }
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
