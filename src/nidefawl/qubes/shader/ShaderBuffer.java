package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.opengl.ARBMapBufferRange;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Memory;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;

public class ShaderBuffer {
    static List<ShaderBuffer> buffers = Lists.newArrayList();
    static int                nextIdx = 0;
    String                    name;
    private int               buffer;
    protected int             len;
    private int               bindingPoint;
    List<Shader>              shaders = Lists.newArrayList();
    private ByteBuffer buf;
    private FloatBuffer bufFloat;
    private IntBuffer bufInt;
    ByteBuffer readBuf;
    FloatBuffer readBufFloat;
    
    public ShaderBuffer(String name) {
        this(name, 0);
    }
    ShaderBuffer(String name, int len) {
        buffers.add(this);
        this.bindingPoint = Engine.getBindingPoint(name);
        this.name = name;
        this.len = len;
    }
    public ShaderBuffer setSize(int n) {
        this.len=n;
        return this;
    }
    public void update() {
        int max = Math.max(this.buf.limit(), Math.max(this.bufFloat.limit()*4, this.bufInt.limit()*4));
        
        //System.out.println(buf+"/"+bufFloat+"/"+max+"/"+this.bufInt.get(0));
        //System.out.println(this.shaders.get(0).name+" - "+this.name+" - "+this.bindingPoint + buffers.get(0).bindingPoint);
        
        buf.position(0).limit(max);
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_SHADER_STORAGE_BUFFER");
        if (this.len < max) {
            System.err.println("INVALID SIZE");
        }
//        GL43.glInvalidateBufferData(this.buffer);
//        GL15.glBufferData(GL_SHADER_STORAGE_BUFFER, this.len, GL15.GL_STATIC_DRAW);

        //System.out.println(buf+"/"+this.len);
        
        GL15.glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, buf);
        Stats.uploadBytes+=max;
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferSubData GL_SHADER_STORAGE_BUFFER "+this.name+"/"+this.buffer+"/"+buf+"/"+this.len);
        bufFloat.position(0).limit(0);
        bufInt.position(0).limit(0);
        buf.position(0).limit(0);
    }
    public void setup() {
        this.buf = Memory.createByteBufferAligned(64, this.len);
        this.bufFloat = this.buf.asFloatBuffer();
        this.bufInt = this.buf.asIntBuffer();
        Engine.checkGLError("glGenBuffers");
        this.buffer = Engine.glGenBuffers(1).get();
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("UBO Engine.glGenBuffers");
        GL15.glBufferData(GL_SHADER_STORAGE_BUFFER, this.len, GL15.GL_DYNAMIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("UBO Matrix");
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        bufFloat.position(0).limit(0);
        bufInt.position(0).limit(0);
        buf.position(0).limit(0);
    }
    
    public static void rebindShaders() {
        for (int i = 0; i < buffers.size(); i++) {
            for (Shader shader : buffers.get(i).shaders) {
                if (shader.valid) {
                    bindBuffers(shader);
                }
            }
        }
    }
    public static void init() {
        for (int i = 0; i < buffers.size(); i++) {
            buffers.get(i).setup();
            buffers.get(i).update();
        }
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }
    public static void bindBuffers(Shader shader) {
        for (int i = 0; i < buffers.size(); i++) {
            ShaderBuffer buffer = buffers.get(i);
            final int blockIndex = glGetProgramResourceIndex(shader.shader, GL_SHADER_STORAGE_BLOCK, buffer.name);
            if (blockIndex != -1) {
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, buffer.bindingPoint, buffer.buffer);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glBindBufferBase GL_SHADER_STORAGE_BUFFER");
                glShaderStorageBlockBinding(shader.shader, blockIndex, buffer.bindingPoint);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glUniformBlockBinding blockIndex "+blockIndex);
                buffer.addShader(shader);
            }
        }
    }

    /**
     * @param shader
     */
    private void addShader(Shader shader) {
        if (this.shaders.contains(shader)) return;
        for (int i = 0; i < this.shaders.size(); i++) {
            Shader s = this.shaders.get(i);
            if (!s.valid) {
                this.shaders.remove(i--);
            }
        }
        this.shaders.add(shader);
    }
    public FloatBuffer getFloatBuffer() {
        return this.bufFloat;
    }
    
    public IntBuffer getIntBuffer() {
        return this.bufInt;
    }
    public ByteBuffer map(boolean write) {
        ByteBuffer cur = this.readBuf;
        ByteBuffer buf = _map(write);
        if (cur != readBuf) {
            this.readBufFloat = readBuf.asFloatBuffer();
        }
        if (this.readBuf.limit() != this.len) {
            throw new GameError("expected buffer length "+this.len+", got "+this.readBuf.limit());
        }
        if (this.readBufFloat.limit()*4 != this.len) {
            throw new GameError("expected float buffer length "+this.len+", got "+(this.readBufFloat.limit()*4));
        }
        this.readBufFloat.position(0).limit(this.len>>2);
        return buf;
    }
    private ByteBuffer _map(boolean write) {

        long offset = 0;
        long length = this.len;

        int flags = write ? GL_MAP_WRITE_BIT : GL_MAP_READ_BIT;
        if (GL.getCaps().OpenGL30) {
           readBuf = glMapBufferRange(GL_SHADER_STORAGE_BUFFER, offset, length, flags, readBuf);
           Engine.checkGLError("glMapBufferRange");
           return readBuf;
        }

        if (GL.getCaps().GL_ARB_map_buffer_range) {
           readBuf = ARBMapBufferRange.glMapBufferRange(GL_SHADER_STORAGE_BUFFER, offset, length, flags, readBuf);
           return readBuf;
        }
        flags = write ? GL15.GL_WRITE_ONLY : GL_READ_ONLY;
        readBuf = GL15.glMapBuffer(GL_SHADER_STORAGE_BUFFER, GL_READ_ONLY , readBuf);
        return readBuf;
     
    }
    public ByteBuffer getMappedBuf() {
        return this.readBuf;
    }
    public FloatBuffer getMappedBufFloat() {
        if (this.readBufFloat.limit()*4!=this.len) {
            throw new GameError("buffer size missmatch "+(this.readBufFloat.limit()*4)+" != "+this.len);
        }
        return this.readBufFloat;
    }

    public void unmap() {
       glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
    }
    public void unbind() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }
    public void bind() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer);
    }
    public int getSize() {
        return this.len;
    }


}
