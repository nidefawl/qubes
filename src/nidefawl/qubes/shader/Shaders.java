package nidefawl.qubes.shader;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;

public class Shaders {

    public static void reinit() {
        Shaders.depthBufShader = null;
        Shaders.normals = null;
        Shaders.textured = null;
        Shaders.colored = null;
        Shaders.wireframe = null;
        initShaders();
    }

    public static void init() {
        Shaders.colored.enable();
        colored.setProgramUniform3f("in_offset", 0, 0, 0);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("setProgramUniform3f");
        Shaders.textured.enable();
        textured.setProgramUniform1i("tex0", 0);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("setProgramUniform1i");
        Shader.disable();
    }

    public static Shader depthBufShader;
    public static Shader normals;
    public static Shader wireframe;
    public static Shader textured;
    public static Shader colored;


    public static void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();

            Shader new_depthBufShader = assetMgr.loadShader("shaders/renderdepth");
            Shader new_normals = assetMgr.loadShader("shaders/visnormals");
            Shader new_textured = assetMgr.loadShader("shaders/textured");
            Shader new_colored = assetMgr.loadShader("shaders/colored");
            Shader new_wireframe = assetMgr.loadShader("shaders/wireframe");
            if (Shaders.depthBufShader != null)
                Shaders.depthBufShader.release();
            if (Shaders.normals != null)
                Shaders.normals.release();
            if (Shaders.textured != null)
                Shaders.textured.release();
            if (Shaders.colored != null)
                Shaders.colored.release();
            if (Shaders.wireframe != null)
                Shaders.wireframe.release();
            Shaders.wireframe = new_wireframe;
            Shaders.depthBufShader = new_depthBufShader;
            Shaders.normals = new_normals;
            Shaders.textured = new_textured;
            Shaders.colored = new_colored;
        } catch (ShaderCompileError e) {
            Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
    }
}
