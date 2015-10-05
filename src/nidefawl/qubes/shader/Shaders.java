package nidefawl.qubes.shader;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.vec.Vector3f;

public class Shaders {
    private static boolean startup = true;

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
    public static Shader colored3D;
    public static Shader renderUINT;


    public static void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();

            Shader new_depthBufShader = assetMgr.loadShader("shaders/renderdepth");
            Shader new_normals = assetMgr.loadShader("shaders/visnormals");
            Shader new_textured = assetMgr.loadShader("shaders/textured");
            Shader new_colored = assetMgr.loadShader("shaders/colored");
            Shader new_colored3D = assetMgr.loadShader("shaders/colored_3D");
            Shader new_wireframe = assetMgr.loadShader("shaders/wireframe");
            Shader new_uint = assetMgr.loadShader("shaders/render_uint_texture");
            if (Shaders.depthBufShader != null)
                Shaders.depthBufShader.release();
            if (Shaders.normals != null)
                Shaders.normals.release();
            if (Shaders.textured != null)
                Shaders.textured.release();
            if (Shaders.colored != null)
                Shaders.colored.release();
            if (Shaders.colored3D != null)
                Shaders.colored3D.release();
            if (Shaders.wireframe != null)
                Shaders.wireframe.release();
            if (Shaders.renderUINT != null)
                Shaders.renderUINT.release();
            Shaders.wireframe = new_wireframe;
            Shaders.depthBufShader = new_depthBufShader;
            Shaders.normals = new_normals;
            Shaders.textured = new_textured;
            Shaders.colored = new_colored;
            Shaders.colored3D = new_colored3D;
            Shaders.renderUINT = new_uint;
            Shaders.colored.enable();
            Shaders.colored.setProgramUniform3f("offset", Vector3f.ZERO);
            Shaders.colored3D.enable();
            Shaders.colored3D.setProgramUniform3f("offset", Vector3f.ZERO);
            Shaders.wireframe.enable();
            Shaders.wireframe.setProgramUniform3f("offset", Vector3f.ZERO);
            Shader.disable();
        } catch (ShaderCompileError e) {
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
    }
}
