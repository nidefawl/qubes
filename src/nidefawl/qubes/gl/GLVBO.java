package nidefawl.qubes.gl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL15;

import nidefawl.qubes.GameBase;

public class GLVBO {
    public static int ALLOC_VBOS = 0;
    public static int ALLOC_VBOS_TERRAIN = 0;
    static final int MIN_BUF_SIZE = 1<<14;
    public int vboId = 0;
    public long vboSize = 0;
    private final int usage;
    public GLVBO(int usage) {
        this.usage = usage;
    }
    boolean isTerrain = false;
    public void setTerrain(boolean isTerrain) {
        this.isTerrain = isTerrain;
    }
    public boolean isTerrain() {
        return this.isTerrain;
    }
    
    public int getVboId() {
        if (this.vboId == 0) {
            vboId = GL15.glGenBuffers();
            ALLOC_VBOS++;
            if (this.isTerrain) {
                ALLOC_VBOS_TERRAIN++;
            }
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
        if (this.usage == GL15.GL_DYNAMIC_DRAW) {
            if (this.vboSize  < MIN_BUF_SIZE && len < MIN_BUF_SIZE) {
                GL15.glBufferData(type, MIN_BUF_SIZE, this.usage);
                this.vboSize = MIN_BUF_SIZE;
            }
//            GL15.glBufferData(type, 0, this.usage);
            if (this.vboSize < len) {
                this.vboSize = len;
                GL15.glBufferData(type, len, this.usage);
                GL15.glBufferData(type, len, buffer, this.usage);
            } else {
                GL15.glBufferSubData(type, 0, len, buffer);
            }
        } else {
            this.vboSize = len;
            GL15.glBufferData(type, len, this.usage);
            GL15.glBufferData(type, len, buffer, this.usage);
        }
        if (GameBase.GL_ERROR_CHECKS)
            Engine.checkGLError("vbo update");
    }
    public void release() {
        if (this.vboId != 0) {
            GL15.glDeleteBuffers(this.vboId);
            this.vboId = 0;
            ALLOC_VBOS--;
            if (this.isTerrain) {
                ALLOC_VBOS_TERRAIN--;
            }
        }
    }
}
