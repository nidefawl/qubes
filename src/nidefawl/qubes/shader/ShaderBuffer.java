package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.GL_MAP_PERSISTENT_BIT;
import static org.lwjgl.opengl.GL44.GL_MAP_COHERENT_BIT;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Memory;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Stats;

public class ShaderBuffer {
    
    static List<ShaderBuffer> buffers = Lists.newArrayList();
    static int                nextIdx = 0;
    String                    name;
    private int               buffer;
    protected int             len;
    protected int             frameLen;
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
    private boolean framePooled;
    private int framePoolLength;
    private int frameOffset;
    private long[] fences;
    
    public ShaderBuffer(String name) {
        this(name, 0, true);
    }
    ShaderBuffer(String name, int len, boolean isGlobal) {
        if (isGlobal)
            buffers.add(this);
        this.bindingPoint = Engine.getBindingPoint(name);
        this.name = name;
        this.len = len;
    }
    public ShaderBuffer setMakePersistantMapped(boolean persistantMapped) {
        this.makePersistant = persistantMapped;
        return this;
    }
    public void sync() { //TODO: add syncing for disabled framePooling
        if (this.framePooled) { 
            long newSyncObj = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
            this.fences[frameOffset] = newSyncObj;   
        }
    }
    public void nextFrame() {
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("nextFrame pre");
        if (this.framePooled) {
            this.frameOffset = (this.frameOffset+1) % this.framePoolLength;
            if (this.fences[frameOffset] != 0) {
                
                int waitflags = 0;
                boolean b=true;
//                TimingHelper.start(4);
                int max = 12;
                while (b&&max-->0) {
                    int status = GL32.glClientWaitSync(this.fences[frameOffset], waitflags, 5000000);
                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("glClientWaitSync "+status+","+waitflags+","+this.fences[frameOffset]);
                    switch (status) {
                        case GL32.GL_ALREADY_SIGNALED:
//                            System.out.println("ALREADY_SIGNALED");
                            b= false;
                            break;
                        case GL32.GL_CONDITION_SATISFIED:
//                            System.out.println("CONDITION_SATISFIED");
                            b = false;
                            break;
                        case GL32.GL_TIMEOUT_EXPIRED:
//                            System.out.println("TIMEOUT_EXPIRED");
                            break;
                        case GL32.GL_WAIT_FAILED:
//                            System.out.println("WAIT_FAILED");
                            break;
                    }
                    waitflags = GL32.GL_SYNC_FLUSH_COMMANDS_BIT;
                }
//                TimingHelper.end(4);
                if (max == 0) {
                    System.err.println("TIMEOUT !!!");
                }
            }
            if (this.fences[frameOffset] != 0L) {
                GL32.glDeleteSync(this.fences[frameOffset]);
                this.fences[frameOffset] = 0;
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glDeleteSync");
            }
            getBuf().clear().position(this.frameOffset*this.frameLen).limit((this.frameOffset+1)*this.frameLen);
            getIntBuffer().clear().position((this.frameOffset*this.frameLen)>>2).limit(((this.frameOffset+1)*this.frameLen)>>2);
            getFloatBuffer().clear().position((this.frameOffset*this.frameLen)>>2).limit(((this.frameOffset+1)*this.frameLen)>>2);
            glBindBufferRange(GL_SHADER_STORAGE_BUFFER, this.bindingPoint, this.buffer, getBuf().position(), getBuf().remaining());
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBufferRange "+bindingPoint+","+this.buffer+","+getBuf().position()+","+getBuf().remaining());
        } else {
            getBuf().clear();
            getIntBuffer().clear();
            getFloatBuffer().clear();

//            getBuf().clear().position(0).limit(this.len);
//            getIntBuffer().clear().position(0).limit((this.len)>>2);
//            getFloatBuffer().clear().position(0).limit((this.len)>>2);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, this.bindingPoint, this.buffer);
        }
    }
    public ShaderBuffer setSize(int n) {
        return this.setSize(n, 0);
    }
    public ShaderBuffer setSize(int n, int framePoolLength) {
        this.frameLen = n;
        if (framePoolLength > 0) {
            this.fences = new long[framePoolLength];
            this.framePooled = true;
            this.framePoolLength = framePoolLength;
            n *= framePoolLength;
        }
        this.len = n;
        return this;
    }

    public void update() {
        int min;
        int max;
        if (isPersistantMapped) {
            min = this.frameOffset*this.frameLen;
            max = Math.max(buf.limit(), Math.max(this.bufFloat.limit()*4, this.bufInt.limit()*4));
            int sizeData = max - min;
            if (sizeData == 0) {
//                System.err.println("no update :(");
            }
//            System.out.println(sizeData);
//            if (sizeData > 4096) {
//                Thread.dumpStack();
//            }
//            System.out.println("done!");
            //flush?!
            glMemoryBarrier(GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
            return; // done?!
        }
        ByteBuffer buf = this.buf;
        if (!framePooled) {
            min = 0;
            max = Math.max(buf.position(), Math.max(this.bufFloat.position()*4, this.bufInt.position()*4));
//            getBuf().rese
        } else {
            min = this.frameOffset*this.frameLen;
            max = Math.max(buf.limit(), Math.max(this.bufFloat.limit()*4, this.bufInt.limit()*4));
        }
        buf.position(min).limit(max);
        int sizeData = max - min;
        
        
        //System.out.println(buf+"/"+bufFloat+"/"+max+"/"+this.bufInt.get(0));
        //System.out.println(this.shaders.get(0).name+" - "+this.name+" - "+this.bindingPoint + buffers.get(0).bindingPoint);
//        System.out.println("upload "+sizeData);
        
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_SHADER_STORAGE_BUFFER");
        if (this.len < max) {
            System.err.println("INVALID SIZE");
        }
//        GL43.glInvalidateBufferData(this.buffer);
//        GL15.glBufferData(GL_SHADER_STORAGE_BUFFER, this.len, GL15.GL_STATIC_DRAW);

        GL15.glBufferSubData(GL_SHADER_STORAGE_BUFFER, min, buf);
        Stats.uploadBytes+=sizeData;
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferSubData GL_SHADER_STORAGE_BUFFER "+this.name+"/"+this.buffer+"/"+buf+"/"+this.len);
    }
    
    public void setup() {
        this.buffer = Engine.glGenBuffers(1).get();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glGenBuffers");
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer");
        if (makePersistant) {
            GL44.glBufferStorage(GL_SHADER_STORAGE_BUFFER, this.len, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT |GL_MAP_COHERENT_BIT );
            _map(GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT|GL_MAP_COHERENT_BIT );
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
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, buffers.get(i).bindingPoint, buffers.get(i).buffer);
//            for (Shader shader : buffers.get(i).shaders) {
//                if (shader.valid) {
//                    bindBuffers(shader);
//                }
//            }
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
        FloatBuffer buffer = this.bufFloat;
        if (isPersistantMapped) {
            buffer = this.mappedBufferFloat;
        }
        return buffer;
    }
    
    public IntBuffer getIntBuffer() {
        IntBuffer buffer = this.bufInt;
        if (isPersistantMapped) {
            buffer = this.mappedBufferInt;
        }
        return buffer;
    }
    public ByteBuffer getBuf() {
        ByteBuffer buffer = this.buf;
        if (isPersistantMapped) {
            buffer = this.mappedBuffer;
        }
        return buffer;
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
    public int offsetInt() {
        return offset()*4;
    }
    public int offset() {
        return this.framePooled ? (this.frameOffset*this.frameLen) : 0;
    }
    public int getBuffer() {
        return this.buffer;
    }

    public ByteBuffer getMappedBuffer() {
        return this.mappedBuffer;
    }
    public FloatBuffer getMappedBufferFloat() {
        return this.mappedBufferFloat;
    }

}
