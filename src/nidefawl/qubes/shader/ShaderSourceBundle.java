package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BLOCK;
import static org.lwjgl.opengl.GL43.glGetProgramResourceIndex;
import static org.lwjgl.opengl.GL43.glShaderStorageBlockBinding;

import java.io.IOException;

import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL43;

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
        fragCode.load(assetManager, nameFsh[0], nameFsh[1] + ".fsh", def, GL_FRAGMENT_SHADER);
        vertCode.load(assetManager, nameVsh[0], nameVsh[1] + ".vsh", def, GL_VERTEX_SHADER);
        computeCode.load(assetManager, nameCsh[0], nameCsh[1] + ".csh", def, ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);
        geomCode.load(assetManager, nameGsh[0], nameGsh[1] + ".gsh", def, GL43.GL_COMPUTE_SHADER);
    }
    
    public Shader compileShader(IShaderDef def) {
        if (!computeCode.isEmpty()) {
            return new ComputeShader(this.name, this.computeCode, def);
            
        }
        return new GraphicShader(this.name, this.vertCode, this.fragCode, this.geomCode, def);
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
