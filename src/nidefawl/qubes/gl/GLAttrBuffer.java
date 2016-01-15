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
public class GLAttrBuffer {

    private int      vbo = -1;
    ReallocIntBuffer vboBuf;
    
    public int getGLArrayBuffer() {
        return this.vbo;
    }



    public void gen() {
        if (this.vboBuf == null) {
            IntBuffer buff = Engine.glGenBuffers(1);
            this.vbo = buff.get(0);
            this.vboBuf = new ReallocIntBuffer(1024);
        }
    }
    public void upload(VertexBuffer buf) {
        if (this.vboBuf == null) {
            gen();
        }
        int numInts = buf.putIn(this.vboBuf);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, numInts * 4L, this.vboBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData ");
    }

    /**
     * 
     */
    public void release() {
        if (this.vboBuf != null) {
            Engine.deleteBuffers(this.vbo);
            this.vboBuf.release();
            this.vboBuf = null;
        }
    }
}
