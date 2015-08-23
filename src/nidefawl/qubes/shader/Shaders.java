package nidefawl.qubes.shader;

import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.WorldRenderer;

public class Shaders {

    public static void reinit() {
        Shaders.depthBufShader = null;
        Shaders.normals = null;
        Shaders.font = null;
        initShaders();
    }

    public static void init() {
        initShaders();
    }

    public static Shader depthBufShader;
    public static Shader normals;
    public static Shader font;

    public static void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();

            Shader new_depthBufShader = assetMgr.loadShader("shaders/renderdepth");
            Shader new_normals = assetMgr.loadShader("shaders/visnormals");
            Shader new_font = assetMgr.loadShader("shaders/font");
            if (Shaders.depthBufShader != null)
                Shaders.depthBufShader.release();
            if (Shaders.normals != null)
                Shaders.normals.release();
            if (Shaders.font != null)
                Shaders.font.release();
            Shaders.depthBufShader = new_depthBufShader;
            Shaders.normals = new_normals;
            Shaders.font = new_font;
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            Main.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
    }

    public static void setUniforms(Shader sh, float fTime) {
        WorldRenderer wr = Engine.worldRenderer;
        sh.setProgramUniform1i("renderWireFrame", Main.renderWireFrame ? 1 : 0);
        sh.setProgramUniform1f("near", Engine.znear);
        sh.setProgramUniform1f("far", Engine.zfar);
        sh.setProgramUniform1f("viewWidth", Main.displayWidth);
        sh.setProgramUniform1f("viewHeight", Main.displayHeight);
        sh.setProgramUniform1f("rainStrength", 0);
        sh.setProgramUniform1f("wetness", 0);
        sh.setProgramUniform1f("aspectRatio", Main.displayWidth / (float) Main.displayHeight);
        sh.setProgramUniform1f("sunAngle", Engine.sunAngle);
        sh.setProgramUniform1f("shadowAngle", Engine.sunAngle);
        sh.setProgramUniform1f("frameTimeCounter", (Main.ticksran + fTime) / 20F);
        sh.setProgramUniform3f("cameraPosition", Engine.camera.getPosition());
        sh.setProgramUniform3f("previousCameraPosition", Engine.camera.getPrevPosition());
        sh.setProgramUniform3f("upPosition", Engine.up);
        sh.setProgramUniform3f("sunPosition", Engine.sunPosition);
        sh.setProgramUniform3f("shadowLightPosition", Engine.sunPosition);
        sh.setProgramUniform3f("moonPosition", Engine.moonPosition);
        sh.setProgramUniform3f("skyColor", wr.skyColor);
        sh.setProgramUniform1i("isEyeInWater", 0);
        sh.setProgramUniform1i("heldBlockLightValue", 15);
        sh.setProgramUniform1i("worldTime", Main.ticksran % 24000);
        sh.setProgramUniform1i("gcolor", 0);
        sh.setProgramUniform1i("gdepth", 1);
        sh.setProgramUniform1i("gnormal", 2);
        sh.setProgramUniform1i("shadow", 6);
        sh.setProgramUniform1i("composite", 3);
        sh.setProgramUniform1i("gdepthtex", 5);
        sh.setProgramUniform1i("noisetex", 4);
        sh.setProgramUniform1i("eyeAltitude", 4);
        sh.setProgramUniform1i("fogMode", 1);
        sh.setProgramUniform1i("shadowMapResolution", Engine.SHADOW_BUFFER_SIZE);
        sh.setProgramUniform1i("shadowDistance", Engine.SHADOW_ORTHO_DIST);
        sh.setProgramUniform2i("eyeBrightness", 0, 0);
        sh.setProgramUniform2i("eyeBrightnessSmooth", 0, 200);

        sh.setProgramUniformMatrix4ARB("gbufferView", false, Engine.getViewMatrix(), false);
        sh.setProgramUniformMatrix4ARB("gbufferModelView", false, Engine.getViewMatrix(), false);
        sh.setProgramUniformMatrix4ARB("gbufferModelViewInverse", false, Engine.getViewMatrixInv(), false);
        sh.setProgramUniformMatrix4ARB("gbufferPreviousModelView", false, Engine.getViewMatrixPrev(), false);

        sh.setProgramUniformMatrix4ARB("gbufferProjection", false, Engine.getProjectionMatrix(), false);
        sh.setProgramUniformMatrix4ARB("gbufferProjectionInverse", false, Engine.getProjectionMatrixInv(), false);
        sh.setProgramUniformMatrix4ARB("gbufferPreviousProjection", false, Engine.getProjectionMatrixPrev(), false);

        sh.setProgramUniformMatrix4ARB("shadowModelView", false, Engine.getShadowModelViewMatrix(), false);
        sh.setProgramUniformMatrix4ARB("shadowModelViewInverse", false, Engine.getShadowModelViewMatrixInv(), false);
        sh.setProgramUniformMatrix4ARB("shadowProjection", false, Engine.getShadowProjectionMatrix(), false);
        sh.setProgramUniformMatrix4ARB("shadowProjectionInverse", false, Engine.getShadowProjectionMatrixInv(), false);

    }
}
