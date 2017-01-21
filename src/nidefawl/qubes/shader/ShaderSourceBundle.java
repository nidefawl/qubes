package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BLOCK;
import static org.lwjgl.opengl.GL43.glGetProgramResourceIndex;
import static org.lwjgl.opengl.GL43.glShaderStorageBlockBinding;

import java.io.IOException;
import java.util.HashMap;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.DebugShaders.Var;

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

    public void load(AssetManager assetManager, String path, String nameVsh, String nameFsh, String nameGsh, String nameCsh, IShaderDef def) throws IOException {
        computeCode.load(assetManager, path, nameCsh + ".csh", def);
        vertCode.load(assetManager, path, nameVsh + ".vsh", def);
        fragCode.load(assetManager, path, nameFsh + ".fsh", def);
        geomCode.load(assetManager, path, nameGsh + ".gsh", def);
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
