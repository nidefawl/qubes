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

    public void load(AssetManager assetManager, String path, String fname, IShaderDef def) throws IOException {
        computeCode.load(assetManager, path, fname + ".csh", def);
        vertCode.load(assetManager, path, fname + ".vsh", def);
        fragCode.load(assetManager, path, fname + ".fsh", def);
        geomCode.load(assetManager, path, fname + ".gsh", def);
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

}
