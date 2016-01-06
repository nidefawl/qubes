package nidefawl.qubes.gl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL15;

public class GLVBO {

    public int vboId = 0;
    public int vboSize = 0;
    public void bind() {
        if (this.vboId == 0) {
            vboId = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
    }
    public int getVboId() {
        return this.vboId;
    }
    public void upload(ByteBuffer buffer, int len) {
        if (this.vboSize <= len) {
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        } else {
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, len, buffer);
        }
        this.vboSize = len;
    }
    public void release() {
        if (this.vboId != 0) {
            GL15.glDeleteBuffers(this.vboId);
        }
    }
}
