package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.GL_MAP_PERSISTENT_BIT;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
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
    boolean makePersistant = false;
    boolean isPersistantMapped = false;
    private ByteBuffer buf;
    private FloatBuffer bufFloat;
    private IntBuffer bufInt;
    
    
    private ByteBuffer mappedBuffer;
    private FloatBuffer mappedBufferFloat;
    private IntBuffer mappedBufferInt;
    
    public ShaderBuffer(String name) {
        this(name, 0);
    }
    ShaderBuffer(String name, int len) {
        buffers.add(this);
        this.bindingPoint = Engine.getBindingPoint(name);
        this.name = name;
        this.len = len;
    }
    public ShaderBuffer setMakePersistantMapped(boolean persistantMapped) {
        this.makePersistant = persistantMapped;
        return this;
    }
    public ShaderBuffer setSize(int n) {
        this.len=n;
        return this;
    }
    public void update() {
        if (isPersistantMapped) {
//            System.out.println("done!");
            //flush?!
            glMemoryBarrier(GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
            return; // done?!
        }
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
        this.buffer = Engine.glGenBuffers(1).get();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glGenBuffers");
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer");
        if (makePersistant) {
            GL44.glBufferStorage(GL_SHADER_STORAGE_BUFFER, this.len, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT );
            _map(GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT );
            this.buf = this.mappedBuffer;
            this.bufFloat = this.mappedBufferFloat;
            this.bufInt = this.mappedBufferInt;
            this.isPersistantMapped = true;
        } else {
            this.buf = Memory.createByteBufferAligned(64, this.len);
            this.bufFloat = this.buf.asFloatBuffer();
            this.bufInt = this.buf.asIntBuffer();
            GL15.glBufferData(GL_SHADER_STORAGE_BUFFER, this.len, GL15.GL_DYNAMIC_DRAW);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBufferData");
        }
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
        if (isPersistantMapped) {
            return this.mappedBufferFloat;
        }
        return this.bufFloat;
    }
    
    public IntBuffer getIntBuffer() {
        if (isPersistantMapped) {
            return this.mappedBufferInt;
        }
        return this.bufInt;
    }
    public ByteBuffer getBuf() {
        if (isPersistantMapped) {
            return this.mappedBuffer;
        }
        return this.buf;
    }
    public ByteBuffer map(boolean write) {
        if (isPersistantMapped) {
            return this.mappedBuffer;
        }
        _map(write ? GL_MAP_WRITE_BIT : GL_MAP_READ_BIT);
        if (this.mappedBuffer.limit() != this.len) {
            throw new GameError("expected buffer length "+this.len+", got "+this.mappedBuffer.limit());
        }
        if (this.mappedBufferFloat.limit()*4 != this.len) {
            throw new GameError("expected float buffer length "+this.len+", got "+(this.mappedBufferFloat.limit()*4));
        }
        if (this.mappedBufferInt.limit()*4 != this.len) {
            throw new GameError("expected float buffer length "+this.len+", got "+(this.mappedBufferInt.limit()*4));
        }
        return this.mappedBuffer;
    }
    private void _map(int flags) {
        long offset = 0;
        long length = this.len;
        ByteBuffer cur = this.mappedBuffer;
        mappedBuffer = glMapBufferRange(GL_SHADER_STORAGE_BUFFER, offset, length, flags, mappedBuffer);
        Engine.checkGLError("glMapBufferRange");
        if (cur != mappedBuffer) {
            this.mappedBufferFloat = mappedBuffer.asFloatBuffer();
            this.mappedBufferInt = mappedBuffer.asIntBuffer();
        }
        this.mappedBufferFloat.position(0).limit(this.len>>2);
        this.mappedBufferInt.position(0).limit(this.len>>2);
    }
    public ByteBuffer getMappedBuf() {
        return this.mappedBuffer;
    }
    public FloatBuffer getMappedBufFloat() {
        return this.mappedBufferFloat;
    }
    public IntBuffer getMappedBufInt() {
        return this.mappedBufferInt;
    }

    public void unmap() {
       GL45.glUnmapNamedBuffer(this.buffer);
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
    public void clearBuffers() {
        this.bufFloat.clear();
        this.bufInt.clear();
    }


}
