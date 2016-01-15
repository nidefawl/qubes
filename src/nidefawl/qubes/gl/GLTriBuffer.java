/**
 * 
 */
package nidefawl.qubes.gl;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Game;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.util.GameError;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class GLTriBuffer {

    private final GLVBO vbo;
    private final GLVBO vboIndices;
    ReallocIntBuffer vboBuf;
    ReallocIntBuffer vboIdxBuf;
    private int      triCount;
    private int      vertexCount;
    public GLTriBuffer(int usage) {
        this.vbo = new GLVBO(usage);
        this.vboIndices = new GLVBO(usage);
    }


    public void gen() {
        if (this.vboBuf == null) {
            this.vboBuf = new ReallocIntBuffer(1024);
            this.vboIdxBuf = new ReallocIntBuffer(1024);
        }
    }

    public void upload(VertexBuffer buf) {
        if (this.vboBuf == null) {
            gen();
        }
        int numInts = buf.putIn(this.vboBuf);
        this.vertexCount = buf.vertexCount;
        this.triCount = buf.getTriIdxPos()/3;
        this.vboIdxBuf.put(buf.getTriIdxBuffer(), 0, buf.getTriIdxPos());
        this.vbo.upload(GL15.GL_ARRAY_BUFFER, this.vboBuf.getByteBuf(), numInts * 4L);
        this.vboIndices.upload(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIdxBuf.getByteBuf(), buf.getTriIdxPos() * 4);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData");
    
    }
    

    public void drawElements() {
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.triCount * 3, GL11.GL_UNSIGNED_INT, 0);
    }
    public int getElementCount() {
        return this.triCount * 3;
    }
    public void draw() {
        if (this.triCount <= 0) {
            throw new GameError("this.triCount <= 0");
        }
        Engine.bindBuffer(this.vbo.getVboId());
        Engine.bindIndexBuffer(this.vboIndices.getVboId());
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.triCount * 3, GL11.GL_UNSIGNED_INT, 0);
    }

    /**
     * 
     */
    public void release() {
        if (this.vboBuf != null) {
            this.vbo.release();
            this.vboIndices.release();
            this.vboBuf.release();
            this.vboIdxBuf.release();
            this.vboBuf = null;
            this.vboIdxBuf = null;
        }
    }



    public int getGLArrayBuffer() {
        return this.vbo.getVboId();
    }
    public int getGLIndexBuffer() {
        return this.vbo.getVboId();
    }
}
