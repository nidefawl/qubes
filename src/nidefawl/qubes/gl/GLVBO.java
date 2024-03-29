package nidefawl.qubes.gl;

import static org.lwjgl.opengl.NVShaderBufferLoad.GL_BUFFER_GPU_ADDRESS_NV;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.NVShaderBufferLoad;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.util.GameError;

public class GLVBO {
    public static int ALLOC_VBOS = 0;
    public static int ALLOC_VBOS_TERRAIN = 0;
    static final int MIN_BUF_SIZE = 1<<14;
    public int vboId = 0;
    public long vboSize = 0;
    private final int usage;
    public long        addr;
    public long        size;
    public boolean canUseBindless;
    
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
        upload(type, buffer, len, true);
    }
    public void upload(int type, ByteBuffer buffer, long len, boolean bindless) {
        if (len != buffer.remaining()) {
            System.err.println("buffer remaining ("+buffer.remaining()+") is not matching len " + len);
            throw new GameError("Fix your buffer");
        }
        if (GameBase.GL_ERROR_CHECKS)
            Engine.checkGLError("pre vbo update");

        boolean newBuffer = false;
        GL15.glBindBuffer(type, getVboId());

        if (this.usage == GL15.GL_DYNAMIC_DRAW) {
            if (this.vboSize < MIN_BUF_SIZE && len < MIN_BUF_SIZE) {
                GL15.glBufferData(type, MIN_BUF_SIZE, this.usage);
                this.vboSize = MIN_BUF_SIZE;
            }
            //          GL15.glBufferData(type, 0, this.usage);
            if (this.vboSize < len) {
                this.vboSize = len;
                GL15.glBufferData(type, len, this.usage);//invalidate previous buffer ('handoff' to driver as explained by some guru) 
                GL15.glBufferData(type, buffer, this.usage);
                newBuffer = true;
            } else {
                GL15.glBufferSubData(type, 0, buffer);
            }
        } else {
            this.vboSize = len;
            GL15.glBufferData(type, len, this.usage);//invalidate previous buffer ('handoff' to driver as explained by some guru)
            GL15.glBufferData(type, buffer, this.usage);
            newBuffer = true;
        }
        if (GameBase.GL_ERROR_CHECKS)
            Engine.checkGLError("vbo update");
        makeResident(type, bindless, newBuffer);
        
    }
    public void makeResident(int type, boolean bindless, boolean newBuffer) {
        this.canUseBindless = false;
        this.addr=0L;
        if (bindless && GL.isBindlessSuppported() && this.usage != GL15.GL_DYNAMIC_DRAW) {
            if (!NVShaderBufferLoad.glIsNamedBufferResidentNV(this.vboId)) {
                NVShaderBufferLoad.glMakeNamedBufferResidentNV(this.vboId, GL15.GL_READ_ONLY);
            }
            if (GameBase.GL_ERROR_CHECKS)
                Engine.checkGLError("make resident");
            this.addr = NVShaderBufferLoad.glGetBufferParameterui64NV(type, GL_BUFFER_GPU_ADDRESS_NV);
            if (GameBase.GL_ERROR_CHECKS)
                Engine.checkGLError("GL_BUFFER_GPU_ADDRESS_NV");
            this.size = GL15.glGetBufferParameteri(type, GL15.GL_BUFFER_SIZE);
            if (GameBase.GL_ERROR_CHECKS)
                Engine.checkGLError("GL_BUFFER_SIZE");
            this.canUseBindless = true;
        }
    }
    public void release() {
        if (this.vboId != 0) {
            this.size = 0L;
            this.addr = 0L;
            this.canUseBindless = false;
            GL15.glDeleteBuffers(this.vboId);
            this.vboId = 0;
            ALLOC_VBOS--;
            if (this.isTerrain) {
                ALLOC_VBOS_TERRAIN--;
            }
        }
    }
    public int getBufferSize(int type) {
        return GL15.glGetBufferParameteri(type, GL15.GL_BUFFER_SIZE);
    }
}
