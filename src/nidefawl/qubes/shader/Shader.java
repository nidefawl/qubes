package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBFragmentShader.*;
import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.ARBVertexShader.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public class Shader {
    int fragShader = -1;
    int vertShader = -1;
    int geometryShader = -1;
    int shader = -1;
    final String name;
    private final IntBuffer buffer;
    
    HashMap<String, Integer> locations    = new HashMap<String, Integer>();
    HashMap<String, AbstractUniform> uniforms    = new HashMap<String, AbstractUniform>();
    HashMap<String, Integer> missinglocations    = new HashMap<String, Integer>();
    private final FloatBuffer tmp         = BufferUtils.createFloatBuffer(16);
    private int setProgramUniformCalls;
    private int numUses;
    void incUniformCalls() {
        Stats.uniformCalls++;
    }

    
    public int getUniformLocation(String name) {
        Integer blub = locations.get(name);
        if (blub != null) {
            return blub.intValue();
        }
        blub = missinglocations.get(name);
        if (blub != null) {
            return -1;
        }
        blub = glGetUniformLocationARB(this.shader, name);
        Engine.checkGLError("glGetUniformLocationARB "+name);
        if (blub < 0) {
            missinglocations.put(name, blub);
//            System.err.println("Invalid uniform location for "+this.name+" - "+name+ " ("+blub+")");
        }
        locations.put(name, blub);
        return blub.intValue();
    }
    
    public Shader(String name) {
        this.name = name;
        this.buffer = BufferUtils.createIntBuffer(1);
    }

    public void load(AssetManager assetManager, String path, String fname) throws IOException {

        ShaderSource vertCode = new ShaderSource();
        ShaderSource fragCode = new ShaderSource();
        ShaderSource geomCode = new ShaderSource();
        
        vertCode.load(assetManager, path, fname + ".vsh");
        fragCode.load(assetManager, path, fname + ".fsh");
        geomCode.load(assetManager, path, fname + ".gsh");
        if (vertCode.isEmpty() && fragCode.isEmpty()) {
            throw new GameError("Failed reading shader source: "+name);
        }
//        System.out.println("frag: "+fragCode);
//        System.out.println();
//        System.out.println("vert: "+vertCode);
        if (!fragCode.isEmpty()) {
            this.fragShader = glCreateShaderObjectARB(GL_FRAGMENT_SHADER_ARB);
            Engine.checkGLError("glCreateShaderObjectARB");
            if (fragShader == 0) {
                throw new GameError("Failed creating fragment shader");
            }

            glShaderSourceARB(this.fragShader, fragCode.getSource());
            Engine.checkGLError("glShaderSourceARB");
            glCompileShaderARB(this.fragShader);
            String log = getLog(this.fragShader);
            Engine.checkGLError("getLog");
            if (getStatus(this.fragShader, GL_OBJECT_COMPILE_STATUS_ARB) != 1) {
                Engine.checkGLError("getStatus");
                throw new ShaderCompileError(fragCode, this.name+" fragment", log);
            } else if (!log.isEmpty()) {
                System.out.println(this.name+" fragment");
                System.out.println(log);
            }
        }


        if (!vertCode.isEmpty()) {
            this.vertShader = glCreateShaderObjectARB(GL_VERTEX_SHADER_ARB);
            Engine.checkGLError("glCreateShaderObjectARB");
            if (this.vertShader == 0) {
                throw new GameError("Failed creating vertex shader");
            }

            glShaderSourceARB(this.vertShader, vertCode.getSource());
            Engine.checkGLError("glShaderSourceARB");
            glCompileShaderARB(this.vertShader);
            String log = getLog(this.vertShader);
            Engine.checkGLError("getLog");
            if (getStatus(this.vertShader, GL_OBJECT_COMPILE_STATUS_ARB) != 1) {
                Engine.checkGLError("getStatus");
                throw new ShaderCompileError(vertCode, this.name+" vertex", log);
            } else if (!log.isEmpty()) {
                System.out.println(this.name+" vertex");
                System.out.println(log);
            }
        }


        if (!geomCode.isEmpty()) {
            this.geometryShader = glCreateShaderObjectARB(ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);
            Engine.checkGLError("glCreateShaderObjectARB");
            if (this.geometryShader == 0) {
                throw new GameError("Failed creating geometry shader");
            }

            glShaderSourceARB(this.geometryShader, geomCode.getSource());
            Engine.checkGLError("glShaderSourceARB");
            glCompileShaderARB(this.geometryShader);
            String log = getLog(this.geometryShader);
            Engine.checkGLError("getLog");
            if (getStatus(this.geometryShader, GL_OBJECT_COMPILE_STATUS_ARB) != 1) {
                Engine.checkGLError("getStatus");
                throw new ShaderCompileError(geomCode, this.name+" geometry", log);
            } else if (!log.isEmpty()) {
                System.out.println(this.name+" geometryShader");
                System.out.println(log);
            }
        }
        this.shader = glCreateProgramObjectARB();
        Engine.checkGLError("glCreateProgramObjectARB");
        if (this.fragShader > 0) {
            glAttachObjectARB(this.shader, this.fragShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if (this.vertShader > 0) {
            glAttachObjectARB(this.shader, this.vertShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if (this.geometryShader > 0) {
            glAttachObjectARB(this.shader, this.geometryShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        for (int i = 0; i < Tess.attributes.length; i++) {
            glBindAttribLocationARB(this.shader, i, Tess.attributes[i]);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindAttribLocationARB "+this.name +" ("+this.shader+"): "+Tess.attributes[i]+" = "+i);
        }
        for (int i = 0; i < BlockFaceAttr.attributes.length; i++) {
            glBindAttribLocationARB(this.shader, i+Tess.attributes.length, BlockFaceAttr.attributes[i]);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindAttribLocationARB "+this.name +" ("+this.shader+"): "+BlockFaceAttr.attributes[i]+" = "+i);
        }
        GL30.glBindFragDataLocation(this.shader, 0, "out_Color");
        GL30.glBindFragDataLocation(this.shader, 1, "out_Normal");
        GL30.glBindFragDataLocation(this.shader, 2, "out_Material");
        GL30.glBindFragDataLocation(this.shader, 3, "out_Light");
        linkProgram();
//        final int blockSize = glGetActiveUniformBlocki(this.shader, blockIndex, GL_UNIFORM_BLOCK_DATA_SIZE);
    
    }


    public void bindAttribute(int attr, String attrName) {
        glBindAttribLocationARB(this.shader, attr, attrName);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindAttribLocationARB "+this.name +" ("+this.shader+"): "+attrName+" = "+attr);
    }
    void linkProgram() {
        glLinkProgramARB(this.shader);
        String log = getLog(this.shader);
        Engine.checkGLError("getLog");
        if (getStatus(this.shader, GL_OBJECT_LINK_STATUS_ARB) != 1) {
            Engine.checkGLError("getStatus");
            throw new ShaderCompileError((ShaderSource)null, "Failed linking shader program\n", log);
        }
        glUseProgramObjectARB(this.shader);
        Engine.checkGLError("glUseProgramObjectARB\n"+log);
        glValidateProgramARB(this.shader);
        Engine.checkGLError("glValidateProgramARB\n"+log);
        glUseProgramObjectARB(0);
        Engine.checkGLError("glUseProgramObjectARB 0");
        
        
        UniformBuffer.bindBuffers(this);

    }

    public int getStatus(int obj, int a) {
        buffer.position(0).limit(1);
        GL.glGetObjectParameterivARB(obj, a, buffer);
        return buffer.get();
    }

    public String getLog(int obj) {
        int length = getStatus(obj, GL_OBJECT_INFO_LOG_LENGTH_ARB);
        if (length > 1) {
            ByteBuffer infoLog = BufferUtils.createByteBuffer(length);
            buffer.flip();
            glGetInfoLogARB(obj, buffer, infoLog);
            byte[] infoBytes = new byte[length];
            infoLog.get(infoBytes);
            String out = new String(infoBytes);
            out = out.trim() + "\n";
            out = out.replace("\n\n", "\n");
            out = out.replace("\n\n", "\n");
            return out;
        }
        return "";
    }
    public void enable() {
        if (this.shader >= 0) {
//            if (lastBoundShader != this.shader) {
//                lastBoundShader = this.shader;
                glUseProgramObjectARB(this.shader);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glUseProgramObjectARB "+this.name +" ("+this.shader+")");
                numUses++;
                if (numUses < 2) {
                    reuploadUniforms();
                }
//            }
        }
    }
//    static int lastBoundShader = -1;
    public static void disable() {
//        if (lastBoundShader != 0)
            glUseProgramObjectARB(0);
//        lastBoundShader = 0;
    }
    public void release() {
        if (this.shader >= 0) {
            if (this.vertShader >= 0)
                glDetachObjectARB(this.shader, this.vertShader);
            if (this.fragShader >= 0)
                glDetachObjectARB(this.shader, this.fragShader);
            if (this.vertShader >= 0)
                glDeleteObjectARB(this.vertShader);
            if (this.fragShader >= 0)
                glDeleteObjectARB(this.fragShader);
            glDeleteObjectARB(this.shader);
            this.shader = this.vertShader = this.fragShader = -1;
        }
    }

    public void setProgramUniform1i(String name, int x) {
        Uniform1i uni = getUniform(name, Uniform1i.class);
        if (uni.set(x)) {
            incUniformCalls();
        }
    }

    public void setProgramUniform2i(String name, int x, int y) {
        Uniform2i uni = getUniform(name, Uniform2i.class);
        if (uni.set(x, y)) {
            incUniformCalls();
        }
    }
    public void setProgramUniform2f(String name, float x, float y) {
        Uniform2f uni = getUniform(name, Uniform2f.class);
        if (uni.set(x, y)) {
            incUniformCalls();
        }
    }
    public void setProgramUniform1f(String name, float x) {
        Uniform1f uni = getUniform(name, Uniform1f.class);
        if (uni.set(x)) {
            incUniformCalls();
        }
    }
    public void setProgramUniform3f(String name, float x, float y, float z) {
        Uniform3f uni = getUniform(name, Uniform3f.class);
        if (uni.set(x, y, z)) {
            incUniformCalls();
        }
    }
    public<T extends AbstractUniform> T getUniform(String name, Class<T> type) {
        T uni = (T) uniforms.get(name);
        if (uni == null) {
            int loc = getUniformLocation(name);
            try {
                uni = type.getDeclaredConstructor(String.class, int.class).newInstance(name, loc);
            } catch (Exception e) {
                e.printStackTrace();
            }
            uniforms.put(name, uni);
        }
        return type.cast(uni);
    }
    public void setProgramUniform4f(String name, float x, float y, float z, float w) {
        Uniform4f uni = getUniform(name, Uniform4f.class);
        Lists.newArrayList();
        if (uni.set(x, y, z, w)) {
            incUniformCalls();
        }
    }

    public void setProgramUniform3f(String string, Vector3f position) {
        setProgramUniform3f(string, position.x, position.y, position.z);
    }

    public void setProgramUniform3f(String string, Vector4f position) {
        setProgramUniform3f(string, position.x, position.y, position.z);
    }


    public void setProgramUniformMatrix4ARB(String name, boolean transpose, FloatBuffer matrix, boolean invert) {
        UniformMat4 uni = getUniform(name, UniformMat4.class);
        if (invert) {
            GameMath.invertMat4x(matrix, tmp);
            matrix = tmp;
            matrix.position(0).limit(16);
        }
        if (uni.set(matrix, transpose)) {
            incUniformCalls();
        }
    }
    public void reuploadUniforms() {
        for (AbstractUniform uni : uniforms.values()) {
            uni.set();
        }
    }


    public int getAndResetNumCalls() {
        int calls = setProgramUniformCalls;
        setProgramUniformCalls = 0;
        return calls;
    }

}
        