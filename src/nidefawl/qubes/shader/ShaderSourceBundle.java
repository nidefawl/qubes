package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

import java.io.*;

import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL43;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.shader.ShaderSource.ProcessMode;

public class ShaderSourceBundle {

    private static final boolean SAVE_PROCESSED_SHADERS = false;
    ShaderSource computeCode = new ShaderSource(this, ProcessMode.OPENGL);
    ShaderSource vertCode = new ShaderSource(this, ProcessMode.OPENGL);
    ShaderSource fragCode = new ShaderSource(this, ProcessMode.OPENGL);
    ShaderSource geomCode = new ShaderSource(this, ProcessMode.OPENGL);
    DebugShaders s;
    private String name;
    public ShaderSourceBundle(String name) {
        this.name = name;
    }

    public void load(AssetManager assetManager, String[] nameFsh, String[] nameVsh, String[] nameGsh, String[] nameCsh, IShaderDef def) throws IOException {
//        String[] extensions = new String[] {"GL_ARB_shading_language_420pack"};
        String[] extensions = new String[] {};
        fragCode.addEnabledExtensions(extensions);
        vertCode.addEnabledExtensions(extensions);
        computeCode.addEnabledExtensions(extensions);
        geomCode.addEnabledExtensions(extensions);
        fragCode.load(assetManager, nameFsh[0], nameFsh[1] + ".fsh", def, GL_FRAGMENT_SHADER);
        vertCode.load(assetManager, nameVsh[0], nameVsh[1] + ".vsh", def, GL_VERTEX_SHADER);
        computeCode.load(assetManager, nameCsh[0], nameCsh[1] + ".csh", def, ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);
        geomCode.load(assetManager, nameGsh[0], nameGsh[1] + ".gsh", def, GL43.GL_COMPUTE_SHADER);
        if (SAVE_PROCESSED_SHADERS) {
            if (!fragCode.isEmpty()) {
                writeShader(fragCode.getSource(), "processed_shaders/"+nameFsh[0]+"/"+nameFsh[1]+".fsh");
            }
            if (!vertCode.isEmpty()) {
                writeShader(vertCode.getSource(), "processed_shaders/"+nameVsh[0]+"/"+nameVsh[1]+".vsh");
            }
        }
    }
    
    private void writeShader(String source, String string) {
//        System.out.println("write to "+string);
        File out = new File(string);
        out.getParentFile().mkdirs();
//        System.out.println("mkdirs "+out.getParentFile().getAbsolutePath());
        
        FileWriter os = null;
        try {
            os = new FileWriter(out);
            BufferedWriter bw = new BufferedWriter(os);
            bw.write(source);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
