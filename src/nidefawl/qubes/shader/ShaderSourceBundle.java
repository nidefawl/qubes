package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BLOCK;
import static org.lwjgl.opengl.GL43.glGetProgramResourceIndex;
import static org.lwjgl.opengl.GL43.glShaderStorageBlockBinding;

import java.io.IOException;
import nidefawl.qubes.assets.AssetManager;

public class ShaderSourceBundle {

    ShaderSource computeCode = new ShaderSource(this);
    ShaderSource vertCode = new ShaderSource(this);
    ShaderSource fragCode = new ShaderSource(this);
    ShaderSource geomCode = new ShaderSource(this);
    DebugShaders s;
    private String name;
    public ShaderSourceBundle(String name) {
        this.name = name;
    }

    public void load(AssetManager assetManager, String[] nameFsh, String[] nameVsh, String[] nameGsh, String[] nameCsh, IShaderDef def) throws IOException {
        fragCode.load(assetManager, nameFsh[0], nameFsh[1] + ".fsh", def);
        vertCode.load(assetManager, nameVsh[0], nameVsh[1] + ".vsh", def);
        computeCode.load(assetManager, nameCsh[0], nameCsh[1] + ".csh", def);
        geomCode.load(assetManager, nameGsh[0], nameGsh[1] + ".gsh", def);
    }
    
    public Shader compileShader() {
        if (!computeCode.isEmpty()) {
            return new ComputeShader(this.name, this.computeCode);
            
        }
        return new GraphicShader(this.name, this.vertCode, this.fragCode, this.geomCode);
    }

    public String getName() {
        return this.name;
    }

    public ShaderSource getFragment() {
        return this.fragCode;
    }

    public String addDebugVar(String defType, String defName, String defExpr) {
        if (this.s == null) {
            this.s = new DebugShaders();
        }
        return this.s.registerDebugVar(defType, defName, defExpr);
    }
    public DebugShaders getDebugVars() {
        return this.s;
    }

}
