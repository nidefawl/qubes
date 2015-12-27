package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.opengl.ARBMapBufferRange;
import org.lwjgl.opengl.GL15;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Memory;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.WorldClient;

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
        
//        System.out.println(buf+"/"+bufFloat+"/"+max+"/"+this.bufInt.get(0));
//        System.out.println(this.shaders.get(0).name+" - "+this.name+" - "+this.bindingPoint + buffers.get(0).bindingPoint);
        buf.position(0).limit(max);
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_SHADER_STORAGE_BUFFER");
        GL15.glBufferData(GL_SHADER_STORAGE_BUFFER, this.len, GL15.GL_DYNAMIC_DRAW);
//        System.out.println(buf+"/"+this.len);
        
        GL15.glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, buf);
        
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

    public static void bindBuffers(Shader shader) {
        for (int i = 0; i < buffers.size(); i++) {
            ShaderBuffer buffer = buffers.get(i);
            final int blockIndex = glGetProgramResourceIndex(shader.shader, GL_SHADER_STORAGE_BLOCK, buffer.name);
            if (blockIndex != -1) {
                System.out.println("buffer "+buffer.name+" is at "+blockIndex+ " on shader "+shader.name+", linking it over binding point "+buffer.bindingPoint);
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
    ByteBuffer readbuf;
    public ByteBuffer map(boolean write) {
       long offset = 0;
       long length = this.len;

       if (GL.getCaps().OpenGL30) {
          int flags = write ? GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT : GL_MAP_READ_BIT;
          readbuf = glMapBufferRange(GL_SHADER_STORAGE_BUFFER, offset, length, flags, readbuf);
          Engine.checkGLError("glMapBufferRange");
          return readbuf;
       }

       if (GL.getCaps().GL_ARB_map_buffer_range) {
          int flags = write ? ARBMapBufferRange.GL_MAP_WRITE_BIT | ARBMapBufferRange.GL_MAP_UNSYNCHRONIZED_BIT : ARBMapBufferRange.GL_MAP_READ_BIT;
          readbuf = ARBMapBufferRange.glMapBufferRange(GL_SHADER_STORAGE_BUFFER, offset, length, flags, readbuf);
          return readbuf;
       }
       
       readbuf = GL15.glMapBuffer(GL_SHADER_STORAGE_BUFFER, GL_READ_ONLY , readbuf);
       return readbuf;
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


}
