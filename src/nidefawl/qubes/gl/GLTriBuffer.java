/**
 * 
 */
package nidefawl.qubes.gl;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class GLTriBuffer {

    private final GLVBO vbo;
    private final GLVBO vboIndices;
    ReallocIntBuffer vboBuf;
    ReallocIntBuffer vboIdxBuf;
    private int      triCount;
    private int      vertexCount;
    private int idxCount;
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

    public int upload(VertexBuffer buf) {
        if (this.vboBuf == null) {
            gen();
        }
        int numInts = buf.storeVertexData(this.vboBuf);
        int numInts2 = buf.storeIndexData(this.vboIdxBuf);
        this.vbo.upload(GL15.GL_ARRAY_BUFFER, this.vboBuf.getByteBuf(), numInts * 4L);
        this.vboIndices.upload(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIdxBuf.getByteBuf(), numInts2 * 4L);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData");
        this.vertexCount = buf.vertexCount;
        this.idxCount = buf.getTriIdxPos();
        this.triCount = this.idxCount/3;
        return (numInts+numInts2)*4;
    }
    

    public void drawElements() {
//        GL12.glDrawRangeElements(GL11.GL_TRIANGLES, 14, 12, 9, GL11.GL_UNSIGNED_INT, 0);
//        GL11.glDrawElements(GL11.GL_TRIANGLES, this.idxCount-32, GL11.GL_UNSIGNED_INT, 6*3);
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.idxCount, GL11.GL_UNSIGNED_INT, 0);
    }
    public int getTriCount() {
        return this.triCount;
    }
    public void draw() {
        if (this.triCount <= 0) {
            throw new GameError("this.triCount <= 0");
        }
        Stats.modelDrawCalls++;
        Engine.bindBuffer(this.vbo.getVboId());
        Engine.bindIndexBuffer(this.vboIndices.getVboId());
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.idxCount, GL11.GL_UNSIGNED_INT, 0);
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
        return this.vboIndices.getVboId();
    }
    
    public int getVertexCount() {
        return this.vertexCount;
    }
    
    public GLVBO getVbo() {
        return this.vbo;
    }
    public GLVBO getVboIndices() {
        return this.vboIndices;
    }
    public int getIdxCount() {
        return this.idxCount;
    }
}
