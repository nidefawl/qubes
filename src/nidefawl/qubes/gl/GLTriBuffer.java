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
    private int      triCount;
    private int      vertexCount;
    private int idxCount;
    public GLTriBuffer(int usage) {
        this.vbo = new GLVBO(usage);
        this.vboIndices = new GLVBO(usage);
    }


    public int upload(VertexBuffer buf) {
        Engine.checkGLError("upload");
        ReallocIntBuffer buffer1 = Engine.getIntBuffer();
        ReallocIntBuffer buffer2 = Engine.getIntBuffer();
        int numInts = buf.storeVertexData(buffer1);
        int numInts2 = buf.storeIndexData(buffer2);
        this.vbo.upload(GL15.GL_ARRAY_BUFFER, buffer1.getByteBuf(), numInts * 4L);
        this.vboIndices.upload(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer2.getByteBuf(), numInts2 * 4L);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData");
        this.vertexCount = buf.vertexCount;
        this.idxCount = buf.getTriIdxPos();
        this.triCount = this.idxCount/3;
        buffer1.setInUse(false);
        buffer2.setInUse(false);
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
        Engine.bindBuffer(this.vbo);
        Engine.bindIndexBuffer(this.vboIndices);
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.idxCount, GL11.GL_UNSIGNED_INT, 0);
    }

    /**
     * 
     */
    public void release() {
        if (this.vbo != null) {
            this.vbo.release();
        }
        if (this.vboIndices != null) {
            this.vboIndices.release();
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
