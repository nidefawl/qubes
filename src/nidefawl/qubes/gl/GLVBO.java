package nidefawl.qubes.gl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL15;

import nidefawl.qubes.GameBase;

public class GLVBO {
    static final int MIN_BUF_SIZE = 1<<14;
    public int vboId = 0;
    public long vboSize = 0;
    private final int usage;
    public GLVBO(int usage) {
        this.usage = usage;
    }
    public int getVboId() {
        if (this.vboId == 0) {
            vboId = GL15.glGenBuffers();
        }
        return this.vboId;
    }
    public void allocate(int type, long len) {
        GL15.glBindBuffer(type, getVboId());
        len = Math.max(MIN_BUF_SIZE, len);
        GL15.glBufferData(type, len, this.usage);
        this.vboSize = len;
    }
    public void upload(int type, ByteBuffer buffer, long len) {
        GL15.glBindBuffer(type, getVboId());
        if (this.vboSize  < MIN_BUF_SIZE && len < MIN_BUF_SIZE) {
            GL15.glBufferData(type, MIN_BUF_SIZE, this.usage);
            this.vboSize = MIN_BUF_SIZE;
        }
        if (this.vboSize < len) {
            this.vboSize = len;
            GL15.glBufferData(type, len, buffer, this.usage);
        } else {
            GL15.glBufferSubData(type, 0, len, buffer);
        }
        if (GameBase.GL_ERROR_CHECKS)
            Engine.checkGLError("vbo update");
    }
    public void release() {
        if (this.vboId != 0) {
            GL15.glDeleteBuffers(this.vboId);
            this.vboId = 0;
        }
    }
}
