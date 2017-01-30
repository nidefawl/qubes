package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.vec.Vector4f;

public class DebugShaders {
    static final int TYPE_FLOAT = 0;
    static final int TYPE_VEC2 = 1;
    static final int TYPE_VEC3 = 2;
    static final int TYPE_VEC4 = 3;
    static final int TYPE_NUM_TYPES = 4;

    static final int MAX_VALS = 1024;
    //size in 4-bytes
    static int getSizeOfType(int type) {
        switch (type) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
            default:
                return 4;
        }
    }
    static int getArrayStartOffset(int type) {
        int offset = 0;
        if (type > TYPE_FLOAT) {
            offset += getSizeOfType(TYPE_FLOAT)*MAX_VALS;
        }
        if (type > TYPE_VEC2) {
            offset += getSizeOfType(TYPE_VEC2)*MAX_VALS;
        }
        if (type > TYPE_VEC3) {
            offset += getSizeOfType(TYPE_VEC3)*MAX_VALS;
        }
        if (type > TYPE_VEC4) {
            offset += getSizeOfType(TYPE_VEC4)*MAX_VALS;
        }
        return offset;
    }
    static int getArrayIdxOffset(int type, int idx) {
        int start = getArrayStartOffset(type);
        return start + getSizeOfType(type)*idx;
    }
    public static int typeFromString(String defType) {
        if (defType.equals("float")) {
            return TYPE_FLOAT;
        }
        if (defType.equals("vec2")) {
            return TYPE_VEC2;
        }
        if (defType.equals("vec3")) {
            return TYPE_VEC3;
        }
        if (defType.equals("vec4")) {
            return TYPE_VEC4;
        }
        return -1;
    }
    public ShaderBuffer        debugOutput;
    public static class Var {
        int idx;
        int type;
        String name;
        public String typeName;
        public final Vector4f value = new Vector4f();
        @Override
        public String toString() {
            return this.name+": "+this.value.toShortString(getSizeOfType(type));
        }
    }
    ArrayList<Var> variables = new ArrayList<>();
    private Shader shader;

    public DebugShaders() {
    }


    public ArrayList<Var> readBack() {
        debugOutput.bind();//skip binding?
        ByteBuffer buf = debugOutput.map(false);
        FloatBuffer fbuf = debugOutput.getMappedBufferFloat();
        for (Var v : variables) {
            int n = getArrayIdxOffset(v.type, v.idx);
//            System.out.println(n);
            v.value.x = fbuf.get(n++);
            if (getSizeOfType(v.type) < 2) {
                v.value.y = 0;
            } else {
                v.value.y = fbuf.get(n++);
            }
            if (getSizeOfType(v.type) < 3) {
                v.value.z = 0;
            } else {
                v.value.z = fbuf.get(n++);
            }
            if (getSizeOfType(v.type) < 4) {
                v.value.w = 0;
            } else {
                v.value.w = fbuf.get(n++);
            }
        }
//        for (int i = 0; i < fbuf.limit(); i++) {
//            if (0 != fbuf.get(i)) {
//                System.out.println(i+"="+fbuf.get(i));
//            }
//        }
        debugOutput.unmap();
        debugOutput.unbind();
//        for (Var v : variables) {
//            System.out.println(v.name+" = "+v.value.toShortString());
//        }
        return variables;
    }

    public String registerDebugVar(String defType, String defName, String defExpr) {
        Var n = new Var();
        n.idx = this.variables.size(); //TODO: index per type
        n.name = defName;
        n.type = typeFromString(defType);
        n.typeName = defType;
        this.variables.add(n);
//        String check = "if (true)\n";
        String check = "if (IS_DEBUG_FRAG(pass_texcoord))\n";
        String copy = "shaderDebugBuffer.v_"+n.typeName+"["+n.idx+"] = "+defExpr+";\n";
//        check = "";
//        copy = "shaderDebugBuffer.v_vec2[0]=floor(pass_texcoord.st*VIEWPORT_SIZE.st);";
        return check+copy;
    }

    public void initDebug(Shader shader) {
        this.shader = shader;
        this.debugOutput = new ShaderBuffer("debugOutput", getArrayStartOffset(TYPE_NUM_TYPES)*4, false);
        this.debugOutput.setup();
        final int blockIndex = glGetProgramResourceIndex(shader.shader, GL_SHADER_STORAGE_BLOCK, "debugBuffer");
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glGetProgramResourceIndex GL_SHADER_STORAGE_BUFFER");
        if (blockIndex != -1) {
//            System.out.println("bind block "+blockIndex+" to binding point "+Engine.getBindingPoint("debugBuffer"));
            glShaderStorageBlockBinding(shader.shader, blockIndex, Engine.getBindingPoint("debugBuffer"));
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glShaderStorageBlockBinding GL_SHADER_STORAGE_BUFFER");
        }
    }

    public void enable() {
        this.debugOutput.nextFrame();
        FloatBuffer buf = this.debugOutput.getFloatBuffer();
        for (int i = 0; i < buf.limit(); i++) {
            buf.put(0);
        }
        this.debugOutput.update();
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, Engine.getBindingPoint("debugBuffer"), this.debugOutput.getBuffer());
        int[] n = Engine.getViewport();
        int mX = n[2]>>1;
        int mY = n[3]>>1;
        if (!Mouse.isGrabbed()) {
            mX = (int) Mouse.getX();
            mY = (int) Mouse.getY();
        }
//        System.out.println(mX+","+mY);
        this.shader.setProgramUniform2f("DEBUG_FRAG_POS", mX, mY);
        this.shader.setProgramUniform2f("VIEWPORT_SIZE", n[2], n[3]);
    }

}
