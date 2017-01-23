package nidefawl.qubes.shader;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.util.SimpleResourceManager;

public class Shaders {
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();

    private static boolean startup = true;

    public static void init() {
        initShaders();
    }
    
    public static Shader depthBufShader;
    public static Shader normals;
    public static Shader wireframe;
    public static Shader texturedAlphaTest;
    public static Shader textured;
    public static Shader textured_to_srgb;
    public static Shader textured_to_lin;
    public static Shader colored;
    public static Shader colored3D;
    public static Shader textured3D;
    public static Shader textured3DAlphaTest;
    public static Shader textured3DMipMap;
    public static Shader renderUINT;
    public static Shader singleblock;
    public static Shader singleblock3D;
    public static Shader gui;
    public static Shader item;
    public static Shader tonemap;


    public static void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_depthBufShader = assetMgr.loadShader(newshaders, "debug/renderdepth");
            Shader new_normals = null;
            if (GL.getCaps().GL_ARB_geometry_shader4) {
                new_normals = assetMgr.loadShader(newshaders, "debug/visnormals");
            }
            Shader new_wireframe = assetMgr.loadShader(newshaders, "debug/wireframe");
            Shader new_uint = assetMgr.loadShader(newshaders, "debug/render_uint_texture");
            Shader new_textured_alpha_test = assetMgr.loadShader(newshaders, "textured", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("ALPHA_TEST".equals(define)) {
                        return "#define ALPHA_TEST 1";
                    }
                    return null;
                }
            });
            Shader new_textured_srgb_to_lin = assetMgr.loadShader(newshaders, "textured", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("SAMPLER_CONVERT_GAMMA".equals(define)) {
                        return "#define SAMPLER_SRGB_TO_LIN 1";
                    }
                    return null;
                }
            });
            Shader new_textured_lin_to_srgb = assetMgr.loadShader(newshaders, "textured", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("SAMPLER_CONVERT_GAMMA".equals(define)) {
                        return "#define SAMPLER_LIN_TO_SRGB 1";
                    }
                    return null;
                }
            });
            Shader new_textured = assetMgr.loadShader(newshaders, "textured");
            Shader new_textured3D = assetMgr.loadShader(newshaders, "textured_3D");
            Shader new_textured3D_alpha_test = assetMgr.loadShader(newshaders, "textured_3D", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("ALPHA_TEST".equals(define)) {
                        return "#define ALPHA_TEST 1";
                    }
                    return null;
                }
            });
            Shader new_textured3dMipMap = assetMgr.loadShader(newshaders, "textured_3D", new IShaderDef() {
                
                @Override
                public String getDefinition(String define) {
                    if ("ALPHA_TEST".equals(define)) {
                        return "#define ALPHA_TEST 1";
                    }
                    if ("EXPLICIT_LOD".equals(define)) {
                        return "#define EXPLICIT_LOD 4";
                    }
                    return null;
                }
            });
            Shader new_colored = assetMgr.loadShader(newshaders, "colored");
            Shader new_colored3D = assetMgr.loadShader(newshaders, "colored_3D");
            Shader new_singleblock = assetMgr.loadShader(newshaders, "singleblock");
            Shader new_singleblock3D = assetMgr.loadShader(newshaders, "singleblock_3D");
            Shader new_gui = assetMgr.loadShader(newshaders, "gui");
            Shader new_item = assetMgr.loadShader(newshaders, "item");
            Shader new_tonemap = assetMgr.loadShader(newshaders, "post/finalstage");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            Shaders.wireframe = new_wireframe;
            Shaders.depthBufShader = new_depthBufShader;
            Shaders.normals = new_normals;
            Shaders.textured = new_textured;
            Shaders.textured_to_srgb = new_textured_lin_to_srgb;
            Shaders.textured_to_lin = new_textured_srgb_to_lin;
            Shaders.texturedAlphaTest = new_textured_alpha_test;
            Shaders.textured3D = new_textured3D;
            Shaders.textured3DMipMap = new_textured3dMipMap;
            Shaders.textured3DAlphaTest = new_textured3D_alpha_test;
            Shaders.colored = new_colored;
            Shaders.colored3D = new_colored3D;
            Shaders.renderUINT = new_uint;
            Shaders.singleblock = new_singleblock;
            Shaders.singleblock3D = new_singleblock3D;
            Shaders.tonemap = new_tonemap;
            Shaders.gui = new_gui;
            Shaders.item = new_item;
            Shaders.colored.enable();
            Shaders.item.enable();
            Shaders.gui.enable();
            Shaders.gui.setProgramUniform1i("colorwheel", 0);
            Shaders.gui.setProgramUniform1f("valueH", 0.5f);
            Shaders.gui.setProgramUniform1f("valueS", 1f);
            Shaders.gui.setProgramUniform1f("valueL", 0.5f);
            Shaders.gui.setProgramUniform1f("fade", 0.3f);
            singleblock.enable();
//          singleblock.setProgramUniform1f("in_scale", 1);
          singleblock.setProgramUniformMatrix4("in_modelMatrix", false, Engine.getIdentityMatrix().get(), false);
          singleblock.setProgramUniform1i("blockTextures", 0);
          singleblock3D.enable();
//        singleblock.setProgramUniform1f("in_scale", 1);
          singleblock3D.setProgramUniformMatrix4("in_modelMatrix", false, Engine.getIdentityMatrix().get(), false);
        singleblock3D.setProgramUniform1i("blockTextures", 0);
//            singleblock.setProgramUniform1i("waterNormals", 1);
            Shaders.colored3D.enable();
            Shaders.colored3D.setProgramUniform1f("color_brightness", 0.1f);

            Shaders.textured3D.enable();
            Shaders.textured3D.setProgramUniform1f("color_brightness", 0.1f);
            Shaders.textured3DMipMap.enable();
            Shaders.textured3DMipMap.setProgramUniform1f("color_brightness", 0.1f);
            Shaders.textured3DAlphaTest.enable();
            Shaders.textured3DAlphaTest.setProgramUniform1f("color_brightness", 0.1f);
//            Shaders.textured3D.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
            Shaders.tonemap.enable();
            Shaders.tonemap.setProgramUniform1i("texColor", 0);
            Shaders.tonemap.setProgramUniform1f("constexposure", 660);
            Shaders.wireframe.enable();
            Shaders.wireframe.setProgramUniform1i("num_vertex", 4);
            Shaders.wireframe.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
            Shader.disable();
            Shaders.colored.enable();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("setProgramUniform3f");
            textured.enable();
            textured.setProgramUniform1i("tex0", 0);
            textured_to_srgb.enable();
            textured_to_srgb.setProgramUniform1i("tex0", 0);
            textured_to_lin.enable();
            textured_to_lin.setProgramUniform1i("tex0", 0);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("setProgramUniform1i");
            Shader.disable();
        } catch (ShaderCompileError e) {
            newshaders.release();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                if (Game.instance != null)
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
    }
}
