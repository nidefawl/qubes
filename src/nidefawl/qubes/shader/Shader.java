package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBFragmentShader.GL_FRAGMENT_SHADER_ARB;
import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.ARBVertexShader.GL_VERTEX_SHADER_ARB;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;

public class Shader {
    int fragShader = -1;
    int vertShader = -1;
    int shader = -1;
    final String name;
    private final IntBuffer buffer;
    
    HashMap<String, Integer> locations    = new HashMap<String, Integer>();
    HashMap<String, AbstractUniform> uniforms    = new HashMap<String, AbstractUniform>();
    private final FloatBuffer tmp         = BufferUtils.createFloatBuffer(16);
    private int setProgramUniformCalls;

    HashMap<String, Integer> missinglocations    = new HashMap<String, Integer>();
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
    public void load(InputStream streamfsh, InputStream streamvsh) throws IOException {
        BufferedReader readerfsh = streamfsh != null ? new BufferedReader(new InputStreamReader(streamfsh)) : null;
        BufferedReader readervsh = streamvsh != null ? new BufferedReader(new InputStreamReader(streamvsh)) : null;
        try {

            String fragCode = "";
            String vertCode = "";
            String line;
            while (readerfsh != null && (line = readerfsh.readLine()) != null) {
                fragCode += line + "\r\n";
            }
            while (readervsh != null && (line = readervsh.readLine()) != null) {
                vertCode += line + "\r\n";
            }
            if (vertCode.isEmpty() && fragCode.isEmpty()) {
                throw new GameError("Failed reading shader source: "+name);
            }
//            System.out.println("frag: "+fragCode);
//            System.out.println();
//            System.out.println("vert: "+vertCode);
            if (!fragCode.isEmpty()) {
                this.fragShader = glCreateShaderObjectARB(GL_FRAGMENT_SHADER_ARB);
                Engine.checkGLError("glCreateShaderObjectARB");
                if (fragShader == 0) {
                    throw new GameError("Failed creating fragment shader");
                }

                glShaderSourceARB(this.fragShader, fragCode);
                Engine.checkGLError("glShaderSourceARB");
                glCompileShaderARB(this.fragShader);
                String log = getLog(this.fragShader);
                Engine.checkGLError("getLog");
                if (getStatus(this.fragShader, GL_OBJECT_COMPILE_STATUS_ARB) != 1) {
                    Engine.checkGLError("getStatus");
                    throw new ShaderCompileError(this.name+" fragment", log);
                }
            }


            if (!vertCode.isEmpty()) {
                this.vertShader = glCreateShaderObjectARB(GL_VERTEX_SHADER_ARB);
                Engine.checkGLError("glCreateShaderObjectARB");
                if (this.vertShader == 0) {
                    throw new GameError("Failed creating vertex shader");
                }

                glShaderSourceARB(this.vertShader, vertCode);
                Engine.checkGLError("glShaderSourceARB");
                glCompileShaderARB(this.vertShader);
                String log = getLog(this.vertShader);
                Engine.checkGLError("getLog");
                if (getStatus(this.vertShader, GL_OBJECT_COMPILE_STATUS_ARB) != 1) {
                    Engine.checkGLError("getStatus");
                    throw new ShaderCompileError(this.name+" vertex", log);
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
            glLinkProgramARB(this.shader);
            String log = getLog(this.shader);
            Engine.checkGLError("getLog");
            if (getStatus(this.shader, GL_OBJECT_LINK_STATUS_ARB) != 1) {
                Engine.checkGLError("getStatus");
                throw new GameError("Failed linking shader program\n"+log);
            }
            glValidateProgramARB(this.shader);
            Engine.checkGLError("glValidateProgramARB\n"+log);
        } finally {
            if (readerfsh != null)
                readerfsh.close();
            if (readervsh != null)
                readervsh.close();
        }
        
    }
    public int getStatus(int obj, int a) {
        buffer.position(0).limit(1);
        glGetObjectParameterARB(obj, a, buffer);
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
            glUseProgramObjectARB(this.shader);
        }
    }
    public static void disable() {
        glUseProgramObjectARB(0);
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
        Uniform1i uni = (Uniform1i) uniforms.get(name);
        if (uni == null) {
            int loc = getUniformLocation(name);
            uni = new Uniform1i(name, loc);
        }
        if (uni.set(x)) {
            setProgramUniformCalls++;
        }
    }

    public void setProgramUniform2i(String name, int x, int y) {
        Uniform2i uni = (Uniform2i) uniforms.get(name);
        if (uni == null) {
            int loc = getUniformLocation(name);
            uni = new Uniform2i(name, loc);
        }
        if (uni.set(x, y)) {
            setProgramUniformCalls++;
        }
    }
    public void setProgramUniform2f(String name, float x, float y) {
        Uniform2f uni = (Uniform2f) uniforms.get(name);
        if (uni == null) {
            int loc = getUniformLocation(name);
            uni = new Uniform2f(name, loc);
        }
        if (uni.set(x, y)) {
            setProgramUniformCalls++;
        }
    }
    public void setProgramUniform1f(String name, float x) {
        Uniform1f uni = (Uniform1f) uniforms.get(name);
        if (uni == null) {
            int loc = getUniformLocation(name);
            uni = new Uniform1f(name, loc);
        }
        if (uni.set(x)) {
            setProgramUniformCalls++;
        }
    }
    public void setProgramUniform3f(String name, float x, float y, float z) {
        Uniform3f uni = (Uniform3f) uniforms.get(name);
        if (uni == null) {
            int loc = getUniformLocation(name);
            uni = new Uniform3f(name, loc);
        }
        if (uni.set(x, y, z)) {
            setProgramUniformCalls++;
        }
    }

    public void setProgramUniform3f(String string, Vector3f position) {
        setProgramUniform3f(string, position.x, position.y, position.z);
    }


    public void setProgramUniformMatrix4ARB(String name, boolean transpose, FloatBuffer matrix, boolean invert) {
        UniformMat4 uni = (UniformMat4) uniforms.get(name);
        if (uni == null) {
            int loc = getUniformLocation(name);
            uni = new UniformMat4(name, loc);
        }
        if (invert) {
            GameMath.invertMat4x(matrix, tmp);
            matrix = tmp;
            matrix.position(0).limit(16);
        }
        if (uni.set(matrix, transpose)) {
            setProgramUniformCalls++;
        }
    }

    public int getAndResetNumCalls() {
        int calls = setProgramUniformCalls;
        setProgramUniformCalls = 0;
        return calls;
    }
}
        