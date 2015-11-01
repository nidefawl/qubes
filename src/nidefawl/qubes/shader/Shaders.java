package nidefawl.qubes.shader;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.SimpleResourceManager;
import nidefawl.qubes.vec.Vector3f;

public class Shaders {
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();

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
        textured.setProgramUniform3f("in_offset", 0, 0, 0);
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
    public static Shader model;
    public static Shader renderUINT;
    public static Shader singleblock;
    public static Shader gui;


    public static void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_depthBufShader = assetMgr.loadShader(newshaders, "debug/renderdepth");
            Shader new_normals = assetMgr.loadShader(newshaders, "debug/visnormals");
            Shader new_wireframe = assetMgr.loadShader(newshaders, "debug/wireframe");
            Shader new_uint = assetMgr.loadShader(newshaders, "debug/render_uint_texture");
            Shader new_textured = assetMgr.loadShader(newshaders, "textured");
            Shader new_colored = assetMgr.loadShader(newshaders, "colored");
            Shader new_colored3D = assetMgr.loadShader(newshaders, "colored_3D");
            Shader new_model = assetMgr.loadShader(newshaders, "model/model");
            Shader new_singleblock = assetMgr.loadShader(newshaders, "singleblock");
            Shader new_gui = assetMgr.loadShader(newshaders, "gui");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            Shaders.wireframe = new_wireframe;
            Shaders.depthBufShader = new_depthBufShader;
            Shaders.normals = new_normals;
            Shaders.textured = new_textured;
            Shaders.colored = new_colored;
            Shaders.colored3D = new_colored3D;
            Shaders.renderUINT = new_uint;
            Shaders.singleblock = new_singleblock;
            Shaders.gui = new_gui;
            Shaders.model = new_model;
            Shaders.colored.enable();
            Shaders.colored.setProgramUniform3f("offset", Vector3f.ZERO);
            Shaders.gui.enable();
            Shaders.gui.setProgramUniform3f("offset", Vector3f.ZERO);
            singleblock.enable();
            singleblock.setProgramUniform3f("in_offset", Vector3f.ZERO);
            singleblock.setProgramUniform1f("in_scale", 1);
            singleblock.setProgramUniform1i("blockTextures", 0);
            singleblock.setProgramUniform1i("waterNormals", 1);
            Shaders.colored3D.enable();
            Shaders.colored3D.setProgramUniform3f("offset", Vector3f.ZERO);
            Shaders.wireframe.enable();
            Shaders.wireframe.setProgramUniform3f("offset", Vector3f.ZERO);
            Shader.disable();
        } catch (ShaderCompileError e) {
            newshaders.release();
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
