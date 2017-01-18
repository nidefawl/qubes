/**
 * 
 */
package nidefawl.qubes.gl;

import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Game;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class GLAttrBuffer {

    private final GLVBO vbo;
    private int vertexCount;

    public GLAttrBuffer() {
        this.vbo = new GLVBO(GL15.GL_STATIC_DRAW);
    }
    public int getGLArrayBuffer() {
        return this.vbo.getVboId();
    }


    public int upload(VertexBuffer buf) {
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("preupload");
        ReallocIntBuffer buffer1 = Engine.getIntBuffer();
        int numInts = buf.storeVertexData(buffer1);
        this.vbo.upload(GL15.GL_ARRAY_BUFFER, buffer1.getByteBuf(), numInts * 4L);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData");
        this.vertexCount = buf.vertexCount;
        buffer1.setInUse(false);
        return (numInts)*4;
    }

    /**
     * 
     */
    public void release() {
        if (this.vbo != null) {
            this.vbo.release();
        }
    }


    public GLVBO getVbo() {
        return this.vbo;
    }
}
