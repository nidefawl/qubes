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
    
    public int getGLArrayBuffer() {
        return this.vbo;
    }


    public void upload(VertexBuffer buf) {
        if (this.vbo < 0) {
            IntBuffer buff = Engine.glGenBuffers(1);
            this.vbo = buff.get(0);
        }
        ReallocIntBuffer buffer1 = Engine.getIntBuffer();
        int numInts = buf.storeVertexData(buffer1);
        if ((numInts * 4L) != buffer1.getByteBuf().remaining()) {
            throw new GameError("buffer size is not matching");
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
//        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer1.getByteBuf(), GL15.GL_STATIC_DRAW);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer1.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData ");
        buffer1.setInUse(false);
    }

    /**
     * 
     */
    public void release() {
        if (this.vbo > -1) {
            Engine.deleteBuffers(this.vbo);
            this.vbo = -1;
        }
    }
}
