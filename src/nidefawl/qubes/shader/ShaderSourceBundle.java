package nidefawl.qubes.shader;

import java.io.IOException;

import nidefawl.qubes.assets.AssetManager;

public class ShaderSourceBundle {

    ShaderSource computeCode = new ShaderSource(this);
    ShaderSource vertCode = new ShaderSource(this);
    ShaderSource fragCode = new ShaderSource(this);
    ShaderSource geomCode = new ShaderSource(this);
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

}
