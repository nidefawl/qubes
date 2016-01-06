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

    private int      vbo = -1;
    private int      vboIndices = -1;
    ReallocIntBuffer vboBuf;
    ReallocIntBuffer vboIdxBuf;
    private int      triCount;
    private int      vertexCount;
    
    public int getGLArrayBuffer() {
        return this.vbo;
    }
    public int getGLIndexBuffer() {
        return this.vboIndices;
    }


    public void gen() {
        if (this.vboBuf == null) {
            IntBuffer buff = Engine.glGenBuffers(2);
            this.vbo = buff.get(0);
            this.vboIndices = buff.get(1);
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

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, numInts * 4L, this.vboBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData ");
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndices);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vboIndices);

        this.vboIdxBuf.put(buf.getTriIdxBuffer(), 0, buf.getTriIdxPos());
        
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buf.getTriIdxPos() * 4, this.vboIdxBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData");
    
    }
    

    /**
     * 
     */
    public void bind() {
        if (this.vbo == -1) {
            throw new GameError("VBO == -1");
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndices);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vboIndices);
    }
    public void unbind() {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
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
        Engine.bindBuffer(this.vbo);
        Engine.bindIndexBuffer(this.vboIndices);
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.triCount * 3, GL11.GL_UNSIGNED_INT, 0);
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
