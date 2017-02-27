package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BLOCK;
import static org.lwjgl.opengl.GL43.glGetProgramResourceIndex;
import static org.lwjgl.opengl.GL43.glShaderStorageBlockBinding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Memory;
import nidefawl.qubes.shader.DebugShaders.Var;
import nidefawl.qubes.shader.ShaderSource.ProcessMode;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public abstract class Shader implements IManagedResource {
    public static int SHADERS = 0;
    int shader = -1;
    final String name;
    
    HashMap<String, Integer> locations    = new HashMap<String, Integer>();
    HashMap<String, AbstractUniform> uniforms    = new HashMap<String, AbstractUniform>();
    HashMap<String, Integer> missinglocations    = new HashMap<String, Integer>();
    private int setProgramUniformCalls;
    public boolean valid = true;
    private ShaderSourceBundle src;
    private DebugShaders debugVars;
    ShaderBuffer buffer;
    void incUniformCalls() {
        Stats.uniformCalls++;
    }

    protected static int shVertexFullscreenTri = 0;
    public static boolean isGlobalProgram(int shader) {
        return shader == shVertexFullscreenTri;
    }
    public static void init() {
        ShaderSource vertCode = new ShaderSource(null, ProcessMode.OPENGL);
        try {
            vertCode.load(AssetManager.getInstance(), "shaders", "screen_triangle.vsh", null, GL_VERTEX_SHADER);
        } catch (IOException e) {
            throw new GameError("Failed loading fullscreen vertex program");
        }
        if (vertCode.isEmpty()) {
            throw new GameError("Missing source");
        }
        shVertexFullscreenTri = compileShader(GL_VERTEX_SHADER, vertCode, "fullscreen_triangle");
    }
    
    static String typeToString(int shaderTypeConstant) {
        switch (shaderTypeConstant)
        {
            case GL_VERTEX_SHADER:
                return "vertex";
            case GL_FRAGMENT_SHADER:
                return "fragment";
            case ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB:
                return "geometry";
            case GL43.GL_COMPUTE_SHADER:
                return "compute";
        }
        throw new GameLogicError("Invalid shader type");
    }
    static int compileShader(int type, ShaderSource src, String name) {
        int iShader = glCreateShader(type);
        Engine.checkGLError("glCreateShader");
        if (iShader == 0) {
            throw new GameError(String.format("Failed creating %s shader", typeToString(type)));
        }

        glShaderSource(iShader, src.getSource());
        Engine.checkGLError("glShaderSourceARB");
        glCompileShader(iShader);
        String log = getLog(0, iShader);
        Engine.checkGLError("getLog");
        if (getStatus(iShader, GL_COMPILE_STATUS) != 1) {
            Engine.checkGLError("getStatus");
            throw new ShaderCompileError(src, String.format("%s %s", name, typeToString(type)), log);
        } else if (!log.isEmpty()) {
            System.out.println(String.format("%s %s", name, typeToString(type)));
            System.out.println(log);
        }
        return iShader;
    }
    static int buildSpirShader(int type, AssetBinary src, String name) {
        int iShader = glCreateShader(type);
        Engine.checkGLError("glCreateShader");
        if (iShader == 0) {
            throw new GameError(String.format("Failed creating %s shader", typeToString(type)));
        }
        byte[] data = src.getData();
        buf.clear();
        buf.put(iShader).flip();
        ByteBuffer buf2 = BufferUtils.createByteBuffer(data.length);
        buf2.put(data).flip();
        GL41.glShaderBinary(buf, ARBGLSPIRV.GL_SHADER_BINARY_FORMAT_SPIR_V_ARB, buf2);
        Engine.checkGLError("glShaderBinary");
        ARBGLSPIRV.glSpecializeShaderARB(iShader, "main", new int[0], new int[0]);
        System.out.println(getLog(0, iShader));
        Engine.checkGLError("glSpecializeShaderARB");
        String log = getLog(0, iShader);
        Engine.checkGLError("getLog");
        if (getStatus(iShader, GL_COMPILE_STATUS) != 1) {
            Engine.checkGLError("getStatus");
            throw new ShaderCompileError("spir "+src.getName(), String.format("%s %s", name, typeToString(type)), log);
        } else if (!log.isEmpty()) {
            System.out.println(String.format("%s %s", name, typeToString(type)));
            System.out.println(log);
        }
        return iShader;
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
        Engine.checkGLError("glGetUniformLocationARB "+name+", this.shader "+this.shader+", "+lastBoundShader);
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

    public abstract void attach(IShaderDef def);
//    public void attach() throws IOException {}


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
            throw new ShaderCompileError((ShaderSource) null, "Failed linking shader program\n", log);
        }
        glUseProgram(this.shader);
        glValidateProgram(this.shader);
        log = getLog(1, this.shader);
        if (!log.isEmpty())
            System.out.println(this.name +": "+log);
        glUseProgram(0);
        UniformBuffer.bindBuffers(this);
        ShaderBuffer.bindBuffers(this);
    }
    
    static IntBuffer buf = Memory.createIntBuffer(1);
    public static int getStatus(int obj, int a) {
        buf.clear();
        GL.glGetObjectParameterivARB(obj, a, buf);
        return buf.get();
    }

    public static String getLog(int logtype, int obj) {
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
                if (this.debugVars != null) {
                    this.debugVars.enable();
                }
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
    public abstract void release();

    public void setProgramUniform1i(String name, int x) {
        Uniform1i uni = getUniform(name, Uniform1i.class);
        if (uni.set(x)) {
            incUniformCalls();
        }
    }

    public void setProgramUniform1ui(String name, int x) {
        Uniform1ui uni = getUniform(name, Uniform1ui.class);
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
            if (loc < 0) {
                if (Game.GL_ERROR_CHECKS)
                    System.out.println("invalid uniform "+getName()+":"+type.getSimpleName().replaceFirst("Uniform", "")+" "+name);
            }
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
//
//    @Override
//    public int hashCode() {
//        return this.name.hashCode();
//    }
//    @Override
//    public boolean equals(Object obj) {
//        return obj instanceof Shader && ((Shader)obj).name.equals(this.name);
//    }


    public void setSource(ShaderSourceBundle shaderSrc) {
        this.src = shaderSrc;
    }
    public ShaderSourceBundle getSource() {
        return this.src;
    }


    public void initDebug(DebugShaders debugVars) {
        this.debugVars = debugVars;
        if (this.debugVars != null) {
            this.debugVars.initDebug(this);
        }
    }
    public List<Var> readDebugVars() {
        return this.debugVars != null ? this.debugVars.readBack() : Collections.<Var>emptyList();
    }

}
        