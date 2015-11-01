package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.*;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public class Shader implements IManagedResource {
    public static int SHADERS = 0;
    int fragShader = -1;
    int vertShader = -1;
    int geometryShader = -1;
    int computeShader = -1;
    int shader = -1;
    final String name;
    
    HashMap<String, Integer> locations    = new HashMap<String, Integer>();
    HashMap<String, AbstractUniform> uniforms    = new HashMap<String, AbstractUniform>();
    HashMap<String, Integer> missinglocations    = new HashMap<String, Integer>();
    private int setProgramUniformCalls;
    public boolean valid = true;
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
        blub = glGetUniformLocation(this.shader, name);
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
    }
    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    static HashMap<String, Integer> map = new HashMap<>();

    public void load(AssetManager assetManager, String path, String fname, IShaderDef def) throws IOException {

        ShaderSource computeCode = new ShaderSource(this);
        ShaderSource vertCode = new ShaderSource(this);
        ShaderSource fragCode = new ShaderSource(this);
        ShaderSource geomCode = new ShaderSource(this);
        vertCode.load(assetManager, path, fname + ".vsh", def);
        fragCode.load(assetManager, path, fname + ".fsh", def);
        geomCode.load(assetManager, path, fname + ".gsh", def);
        computeCode.load(assetManager, path, fname + ".csh", def);
        if (!computeCode.isEmpty()) {

            this.computeShader = glCreateShader(GL43.GL_COMPUTE_SHADER);
            Engine.checkGLError("glCreateShader");
            if (computeShader == 0) {
                throw new GameError("Failed creating compute shader");
            }

            glShaderSource(this.computeShader, computeCode.getSource());
            Engine.checkGLError("glShaderSourceARB");
            glCompileShader(this.computeShader);
            String log = getLog(0, this.computeShader);
            Engine.checkGLError("getLog");
            if (getStatus(this.computeShader, GL_COMPILE_STATUS) != 1) {
                Engine.checkGLError("getStatus");
                throw new ShaderCompileError(this, computeCode, this.name+" compute", log);
            } else if (!log.isEmpty()) {
                System.out.println(this.name+" compute");
                System.out.println(log);
            }
        } else {

            if (vertCode.isEmpty() && fragCode.isEmpty()) {
                throw new GameError("Failed reading shader source: "+name);
            }
            if (!fragCode.isEmpty()) {
                this.fragShader = glCreateShader(GL_FRAGMENT_SHADER);
                Engine.checkGLError("glCreateShader");
                if (fragShader == 0) {
                    throw new GameError("Failed creating fragment shader");
                }

                glShaderSource(this.fragShader, fragCode.getSource());
                Engine.checkGLError("glShaderSourceARB");
                glCompileShader(this.fragShader);
                String log = getLog(0, this.fragShader);
                Engine.checkGLError("getLog");
                if (getStatus(this.fragShader, GL_COMPILE_STATUS) != 1) {
                    Engine.checkGLError("getStatus");
                    throw new ShaderCompileError(this, fragCode, this.name+" fragment", log);
                } else if (!log.isEmpty()) {
                    System.out.println(this.name+" fragment");
                    System.out.println(log);
                }
            }


            if (!vertCode.isEmpty()) {
                this.vertShader = glCreateShader(GL_VERTEX_SHADER);
                Engine.checkGLError("glCreateShader");
                if (this.vertShader == 0) {
                    throw new GameError("Failed creating vertex shader");
                }

                glShaderSource(this.vertShader, vertCode.getSource());
                Engine.checkGLError("glShaderSourceARB");
                glCompileShader(this.vertShader);
                String log = getLog(0, this.vertShader);
                Engine.checkGLError("getLog");
                if (getStatus(this.vertShader, GL_COMPILE_STATUS) != 1) {
                    Engine.checkGLError("getStatus");
                    throw new ShaderCompileError(this, vertCode, this.name+" vertex", log);
                } else if (!log.isEmpty()) {
                    System.out.println(this.name+" vertex");
                    System.out.println(log);
                }
            }


            if (!geomCode.isEmpty()) {
                this.geometryShader = glCreateShader(ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);
                Engine.checkGLError("glCreateShader");
                if (this.geometryShader == 0) {
                    throw new GameError("Failed creating geometry shader");
                }

                glShaderSource(this.geometryShader, geomCode.getSource());
                Engine.checkGLError("glShaderSourceARB");
                glCompileShader(this.geometryShader);
                String log = getLog(0, this.geometryShader);
                Engine.checkGLError("getLog");
                if (getStatus(this.geometryShader, GL_COMPILE_STATUS) != 1) {
                    Engine.checkGLError("getStatus");
                    throw new ShaderCompileError(this, geomCode, this.name+" geometry", log);
                } else if (!log.isEmpty()) {
                    System.out.println(this.name+" geometryShader");
                    System.out.println(log);
                }
            }   
        }
        this.shader = glCreateProgram();
        SHADERS++;
//        {
//            Integer i = map.get(this.name);
//            if (i == null)
//            {
//                i=0;
//            }
//            if (i > 0)
//            {
//                System.err.println(this.name +" - "+ i);
//            }
//            i++;
//            map.put(this.name, i);
//        }
        Engine.checkGLError("glCreateProgramObjectARB");
        if (this.fragShader > 0) {
            glAttachShader(this.shader, this.fragShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if (this.vertShader > 0) {
            glAttachShader(this.shader, this.vertShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if (this.geometryShader > 0) {
            glAttachShader(this.shader, this.geometryShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if (this.computeShader > 0) {
            glAttachShader(this.shader, this.computeShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if (this.computeShader <= 0) {
            String attr = vertCode.getAttrTypes();
            if ("shadow".equals(attr)) {
                glBindAttribLocation(this.shader, 0, "in_position");
                glBindAttribLocation(this.shader, 1, "in_texcoord");
                glBindAttribLocation(this.shader, 2, "in_blockinfo");
            } else {
                for (int i = 0; i < Tess.attributes.length; i++) {
                    glBindAttribLocation(this.shader, i, Tess.attributes[i]);
                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("glBindAttribLocationARB "+this.name +" ("+this.shader+"): "+Tess.attributes[i]+" = "+i);
                }
                for (int i = 0; i < BlockFaceAttr.attributes.length; i++) {
                    glBindAttribLocation(this.shader, i+Tess.attributes.length, BlockFaceAttr.attributes[i]);
                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("glBindAttribLocationARB "+this.name +" ("+this.shader+"): "+BlockFaceAttr.attributes[i]+" = "+i);
                }
            }
            GL30.glBindFragDataLocation(this.shader, 0, "out_Color");
            GL30.glBindFragDataLocation(this.shader, 1, "out_Normal");
            GL30.glBindFragDataLocation(this.shader, 2, "out_Material");
            GL30.glBindFragDataLocation(this.shader, 3, "out_Light");
        }
        linkProgram();
//        final int blockSize = glGetActiveUniformBlocki(this.shader, blockIndex, GL_UNIFORM_BLOCK_DATA_SIZE);
    
    }


    public void bindAttribute(int attr, String attrName) {
        glBindAttribLocation(this.shader, attr, attrName);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindAttribLocationARB "+this.name +" ("+this.shader+"): "+attrName+" = "+attr);
    }

    void linkProgram() {
        String log = getLog(0, this.shader);
        if (!log.isEmpty())
            System.out.println(this.name +": "+log);
        glLinkProgram(this.shader);
        log = getLog(1, this.shader);
        if (!log.isEmpty())
            System.out.println(this.name +": "+log);
        if (getStatus(this.shader, GL_LINK_STATUS) != 1) {
            Engine.checkGLError("getStatus");
            throw new ShaderCompileError(this, (ShaderSource) null, "Failed linking shader program\n", log);
        }
        glUseProgram(this.shader);
        glValidateProgram(this.shader);
        log = getLog(1, this.shader);
        if (!log.isEmpty())
            System.out.println(this.name +": "+log);
        glUseProgram(0);
        UniformBuffer.bindBuffers(this);

    }
    static IntBuffer buf = Memory.createIntBuffer(1);
    public int getStatus(int obj, int a) {
        buf.clear();
        GL.glGetObjectParameterivARB(obj, a, buf);
        return buf.get();
    }

    public String getLog(int logtype, int obj) {
        int length = getStatus(obj, GL_INFO_LOG_LENGTH);
        if (length > 1) {
            String out = logtype == 0 ? glGetShaderInfoLog(obj, length) : glGetProgramInfoLog(obj, length);
            out = out.trim() + "\n";
            out = out.replace("\n\n", "\n");
            out = out.replace("\n\n", "\n");
            return out.trim();
        }
        return "";
    }
    public void enable() {
        if (this.shader > 0) {
            if (lastBoundShader != this.shader) {
                lastBoundShader = this.shader;
                glUseProgram(this.shader);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glUseProgramObjectARB "+this.name +" ("+this.shader+")");
//                numUses++;
//                if (numUses < 2) {
//                    reuploadUniforms();
//                }
            }
        }
    }
    static int lastBoundShader = -1;
    public static void disable() {
        if (lastBoundShader != 0)
            glUseProgram(0);
        lastBoundShader = 0;
    }
    public void release() {
        this.valid = false;
        if (this.shader > 0) {
            if (this.vertShader > 0)
                glDetachShader(this.shader, this.vertShader);
            if (this.fragShader > 0)
                glDetachShader(this.shader, this.fragShader);
            if (this.geometryShader > 0)
                glDetachShader(this.shader, this.geometryShader);
            if (this.computeShader > 0)
                glDetachShader(this.shader, this.computeShader);
            if (this.fragShader > 0)
                glDeleteShader(this.fragShader);
            if (this.vertShader > 0)
                glDeleteShader(this.vertShader);
            if (this.geometryShader > 0)
                glDeleteShader(this.geometryShader);
            if (this.computeShader > 0)
                glDeleteShader(this.computeShader);
            
            glDeleteProgram(this.shader);
            this.shader = this.vertShader = this.fragShader = -1;
            for (AbstractUniform uni : this.uniforms.values()) {
                uni.release();
            }
            this.uniforms.clear();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("release Shader "+this.name +" ("+this.shader+")");
        }
        SHADERS--;
//        {
//            Integer i = map.get(this.name);
//            if (i == null)
//            {
//                i=0;
//            }
//            if (i > 0) {
//                i--;
//                map.put(this.name, i);
//            }
//        }
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


    public void setProgramUniformMatrix4(String name, boolean transpose, FloatBuffer matrix, boolean invert) {
        UniformMat4 uni = getUniform(name, UniformMat4.class);
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


    /**
     * @return shader program gl name
     */
    public int get() {
        return this.shader;
    }


    @Override
    public EResourceType getType() {
        return EResourceType.SHADER;
    }

}
        