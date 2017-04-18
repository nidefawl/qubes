/**
 * 
 */
package nidefawl.qubes.gl;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vulkan.BufferPair;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class GLTriBuffer {

    private GLVBO vbo;
    private GLVBO vboIndices;
    private int      triCount;
    private int      vertexCount;
    private int idxCount;
    private BufferPair bufferPair;
    private boolean isDynamic;
    String tag;
    public GLTriBuffer tag(String tag) {
        this.tag = tag;
        if (Engine.isVulkan) {
            this.bufferPair.tag(tag);
        }
        return this;
    }
    public String tag() {
        return this.tag;
    }
    public GLTriBuffer(boolean isDynamic) {
        this.isDynamic = isDynamic;
        if (Engine.isVulkan) {
            this.bufferPair = Engine.vkContext.getFreeBuffer();
        } else {
            this.vbo = new GLVBO(isDynamic ? GL15.GL_DYNAMIC_DRAW : GL15.GL_STATIC_DRAW);
            this.vboIndices = new GLVBO(isDynamic ? GL15.GL_DYNAMIC_DRAW : GL15.GL_STATIC_DRAW);
        }
    }


    public int upload(VertexBuffer buf) {
        ReallocIntBuffer buffer1 = Engine.getIntBuffer();
        ReallocIntBuffer buffer2 = Engine.getIntBuffer();
        int numInts = buf.storeVertexData(buffer1);
        int numInts2 = buf.storeIndexData(buffer2);
        if (Engine.isVulkan) {
            this.bufferPair.create(numInts*4L, numInts2*4L, !isDynamic);
            this.bufferPair.upload(0, buffer1.getByteBuf(), 0, buffer2.getByteBuf());
            this.bufferPair.setElementCount(numInts2);
        } else {
            this.vbo.upload(GL15.GL_ARRAY_BUFFER, buffer1.getByteBuf(), numInts * 4L);
            this.vboIndices.upload(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer2.getByteBuf(), numInts2 * 4L);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("upload");
        }
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

        if (Engine.isVulkan) {
            this.bufferPair.draw(Engine.getDrawCmdBuffer());
        } else {
            Engine.bindBuffer(this.vbo);
            Engine.bindIndexBuffer(this.vboIndices);
            GL11.glDrawElements(GL11.GL_TRIANGLES, this.idxCount, GL11.GL_UNSIGNED_INT, 0);
        }
    }

    /**
     * 
     */
    public void release() {
        if (this.bufferPair != null) {
            Engine.vkContext.orphanResource(this.bufferPair);
            this.bufferPair = null;
        }
        
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


    public BufferPair getVkBuffer() {
        return this.bufferPair;
    }
}
