package nidefawl.qubes.shader;

import java.util.Arrays;

import nidefawl.game.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.render.WorldRenderer;

public class Shaders {

    public static Shader[] allShaders = new Shader[16];
    public static int numShaders = 0;

    public static void register(Shader shader) {
        allShaders[numShaders++] = shader;
    }
    boolean startup = true;
    public static Shader       waterShader;
    public static Shader       composite1;
    public static Shader       composite2;
    public static Shader       composite3;
    public static Shader       compositeFinal;
    public static Shader       depthBufShader;
    public static Shader       sky;
    public static Shader       sky2;
    public static Shader       terrain;
    public static Shader       testShader;
    public static Shader       shadow;
    public static Shader       normals;

    public void initShaders() {
        try {
            numShaders = 0;
            Arrays.fill(allShaders, null);
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_waterShader = assetMgr.loadShader("shaders/water", false);
            if (Shaders.waterShader != null)
                Shaders.waterShader.release();
            new_waterShader.bindAttribute(Tess.ATTR_BLOCK, "blockinfo");
            new_waterShader.linkProgram();
            Shaders.waterShader = new_waterShader;
            Shader new_depthBufShader = assetMgr.loadShader("shaders/renderdepth");
            if (Shaders.depthBufShader != null)
                Shaders.depthBufShader.release();
            Shaders.depthBufShader = new_depthBufShader;
            Shader new_composite1 = assetMgr.loadShader(Main.useEmptyShaders ? "shaders/empty" : "shaders/composite");
            if (Shaders.composite1 != null)
                Shaders.composite1.release();
            Shaders.composite1 = new_composite1;
            Shader new_composite2 = assetMgr.loadShader(Main.useEmptyShaders ? "shaders/empty" : "shaders/composite1");
            if (Shaders.composite2 != null)
                Shaders.composite2.release();
            Shaders.composite2 = new_composite2;
            Shader new_composite3 = assetMgr.loadShader(Main.useEmptyShaders ? "shaders/empty" : "shaders/composite2");
            if (Shaders.composite3 != null)
                Shaders.composite3.release();
            Shaders.composite3 = new_composite3;
            Shader new_compositeFinal = assetMgr.loadShader(Main.useEmptyShaders ? "shaders/empty" : "shaders/final");
            if (Shaders.compositeFinal != null)
                Shaders.compositeFinal.release();
            Shaders.compositeFinal = new_compositeFinal;
            Shader new_testShader = assetMgr.loadShader("shaders/test", false);
            if (Shaders.testShader != null)
                Shaders.testShader.release();
            new_testShader.bindAttribute(Tess.ATTR_BLOCK, "blockinfo");
            new_testShader.linkProgram();
            Shaders.testShader = new_testShader;
            Shader new_sky = assetMgr.loadShader("shaders/sky");
            if (Shaders.sky != null)
                Shaders.sky.release();
            Shaders.sky = new_sky;
            Shader new_sky2 = assetMgr.loadShader("shaders/sky");
            if (Shaders.sky2 != null)
                Shaders.sky2.release();
            Shaders.sky2 = new_sky2;
            Shader new_terrain = assetMgr.loadShader("shaders/terrain", false);
            if (Shaders.terrain != null)
                Shaders.terrain.release();
            new_terrain.bindAttribute(Tess.ATTR_BLOCK, "blockinfo");
            new_terrain.linkProgram();
            Shaders.terrain = new_terrain;
            Shader new_shadow = assetMgr.loadShader("shaders/shadow", false);
            if (Shaders.shadow != null)
                Shaders.shadow.release();
            new_shadow.bindAttribute(Tess.ATTR_BLOCK, "blockinfo");
            new_shadow.linkProgram();
            Shaders.shadow = new_shadow;
            Shader new_normals = assetMgr.loadShader("shaders/visnormals", false);
            if (Shaders.normals != null)
                Shaders.normals.release();
            new_normals.bindAttribute(Tess.ATTR_BLOCK, "blockinfo");
            new_normals.linkProgram();
            Shaders.normals = new_normals;
            startup = false;
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            if (startup) {
                System.out.println(e.getLog());
                Main.instance.setException(e);
            } else {
                Main.instance.addDebugOnScreen("\0uff3333shader "+e.getName()+" failed to compile");
                System.out.println("shader "+e.getName()+" failed to compile");
                System.out.println(e.getLog());
            }
        }
    }

    public void init() {
        initShaders();
    }

    public void reload() {
        initShaders();
    }
    


    public static void setUniforms(Shader sh, float fTime) {
        WorldRenderer wr = Engine.worldRenderer;
        sh.setProgramUniform1i("renderWireFrame", Main.renderWireFrame? 1 : 0);
        sh.setProgramUniform1f("near", Engine.znear);
        sh.setProgramUniform1f("far", Engine.zfar);
        sh.setProgramUniform1f("viewWidth", Main.displayWidth);
        sh.setProgramUniform1f("viewHeight", Main.displayHeight);
        sh.setProgramUniform1f("rainStrength", 0F);
        sh.setProgramUniform1f("wetness", 0);
        sh.setProgramUniform1f("aspectRatio", Main.displayWidth / (float) Main.displayHeight);
        sh.setProgramUniform1f("sunAngle", Engine.sunAngle);
        sh.setProgramUniform1f("shadowAngle", Engine.sunAngle);
        sh.setProgramUniform1f("frameTimeCounter", (Main.ticksran + fTime)/20F);
        sh.setProgramUniform3f("cameraPosition", Engine.camera.getPosition());
        sh.setProgramUniform3f("previousCameraPosition", Engine.camera.getPrevPosition());
        sh.setProgramUniform3f("upPosition", Engine.up);
        sh.setProgramUniform3f("sunPosition", Engine.sunPosition);
        sh.setProgramUniform3f("shadowLightPosition", Engine.sunPosition);
        sh.setProgramUniform3f("moonPosition", Engine.moonPosition);
        sh.setProgramUniform3f("skyColor", wr.skyColor);
        sh.setProgramUniform1i("isEyeInWater", 0);
        sh.setProgramUniform1i("heldBlockLightValue", 0);
        sh.setProgramUniform1i("worldTime", Main.ticksran%24000);
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
        sh.setProgramUniform2i("eyeBrightness", 0, 0);
        sh.setProgramUniform2i("eyeBrightnessSmooth", 0, 240);
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

    public static int getAndResetNumCalls() {
        int total = 0;
        for (int i = 0; i < numShaders; i++) {
            total += allShaders[i].getAndResetNumCalls();
        }
        return total;
    }
}
