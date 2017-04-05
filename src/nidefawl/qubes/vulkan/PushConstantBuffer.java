package nidefawl.qubes.vulkan;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.vec.Vector4f;

public class PushConstantBuffer {
    public final static PushConstantBuffer INST = new PushConstantBuffer();
    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;
    private IntBuffer intBuffer;
    public PushConstantBuffer() {
        this.buffer = MemoryUtil.memAlignedAlloc(8, 256);
        this.floatBuffer = this.buffer.asFloatBuffer();
        this.intBuffer = this.buffer.asIntBuffer();
    }
    public void setMat4(int pos, BufferedMatrix mat) {
        this.floatBuffer.position(pos>>2);
        mat.store(this.floatBuffer);
    }
    public void setInt(int pos, int val) {
        this.intBuffer.put(pos, val);
    }
    public void setFloat(int pos, float val) {
        this.floatBuffer.put(pos, val);
    }
    public void setVec4(int pos, Vector4f vec) {
        this.floatBuffer.position(pos>>2);
        vec.store(this.floatBuffer);
    }
    public void setVec4(int pos, float x, float y, float z, float w) {
        this.floatBuffer.put(pos++, x);
        this.floatBuffer.put(pos++, y);
        this.floatBuffer.put(pos++, z);
        this.floatBuffer.put(pos++, w);
    }
    public ByteBuffer getBuf(int len) {
        this.buffer.position(0).limit(len);
        return this.buffer;
    }

}
