package nidefawl.qubes.shader;

import java.util.Arrays;

import nidefawl.game.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.WorldRenderer;

public class Shaders {

    public static Shader[] allShaders = new Shader[16];
    public static int numShaders = 0;

    public static void register(Shader shader) {
        allShaders[numShaders++] = shader;
    }

    public static Shader       waterShader;
    public static Shader       waterShader2;
    public static Shader       composite1;
    public static Shader       composite2;
    public static Shader       composite3;
    public static Shader       compositeFinal;
    public static Shader       depthBufShader;
    public static Shader       sky;
    public static Shader       sky2;
    public static Shader       testShader;

    public void initShaders() {
        try {
            numShaders = 0;
            Arrays.fill(allShaders, null);
            Shader new_waterShader = AssetManager.getInstance().loadShader("shaders/water");
            if (Shaders.waterShader != null)
                Shaders.waterShader.release();
            Shaders.waterShader = new_waterShader;
            Shader new_waterShader2 = AssetManager.getInstance().loadShader("shaders/water2");
            if (Shaders.waterShader2 != null)
                Shaders.waterShader2.release();
            Shaders.waterShader2 = new_waterShader2;
            Shader new_depthBufShader = AssetManager.getInstance().loadShader("shaders/renderdepth");
            if (Shaders.depthBufShader != null)
                Shaders.depthBufShader.release();
            Shaders.depthBufShader = new_depthBufShader;
            Shader new_composite1 = AssetManager.getInstance().loadShader("shaders/composite");
            if (Shaders.composite1 != null)
                Shaders.composite1.release();
            Shaders.composite1 = new_composite1;
            Shader new_composite2 = AssetManager.getInstance().loadShader("shaders/composite1");
            if (Shaders.composite2 != null)
                Shaders.composite2.release();
            Shaders.composite2 = new_composite2;
            Shader new_composite3 = AssetManager.getInstance().loadShader("shaders/composite2");
            if (Shaders.composite3 != null)
                Shaders.composite3.release();
            Shaders.composite3 = new_composite3;
            Shader new_compositeFinal = AssetManager.getInstance().loadShader("shaders/final");
            if (Shaders.compositeFinal != null)
                Shaders.compositeFinal.release();
            Shaders.compositeFinal = new_compositeFinal;
            Shader new_testShader = AssetManager.getInstance().loadShader("shaders/test");
            if (Shaders.testShader != null)
                Shaders.testShader.release();
            Shaders.testShader = new_testShader;
            Shader new_sky = AssetManager.getInstance().loadShader("shaders/sky");
            if (Shaders.sky != null)
                Shaders.sky.release();
            Shaders.sky = new_sky;
            Shader new_sky2 = AssetManager.getInstance().loadShader("shaders/sky");
            if (Shaders.sky2 != null)
                Shaders.sky2.release();
            Shaders.sky2 = new_sky2;
        } catch (ShaderCompileError e) {
            Main.instance.addDebugOnScreen("\0uff3333shader "+e.getName()+" failed to compile");
            System.out.println("shader "+e.getName()+" failed to compile");
            System.out.println(e.getLog());
        }
    }

    public void init() {
        initShaders();
    }

    public void reload() {
        initShaders();
    }
    


    public static void setUniforms(Shader compositeShader, float fTime) {
        WorldRenderer wr = Engine.worldRenderer;
        compositeShader.setProgramUniform1f("near", Engine.znear);
        compositeShader.setProgramUniform1f("far", Engine.zfar);
        compositeShader.setProgramUniform1f("viewWidth", Main.displayWidth);
        compositeShader.setProgramUniform1f("viewHeight", Main.displayHeight);
        compositeShader.setProgramUniform1f("rainStrength", 0F);
        compositeShader.setProgramUniform1f("wetness", 0);
        compositeShader.setProgramUniform1f("aspectRatio", Main.displayWidth / (float) Main.displayHeight);
        compositeShader.setProgramUniform1f("sunAngle", wr.sunAngle);
        compositeShader.setProgramUniform1f("frameTimeCounter", (Main.ticksran + fTime)/20F);
        compositeShader.setProgramUniform3f("cameraPosition", Engine.camera.getPosition());
        compositeShader.setProgramUniform3f("upPosition", wr.up);
        compositeShader.setProgramUniform3f("sunPosition", wr.sun);
        compositeShader.setProgramUniform3f("moonPosition", wr.moonPosition);
        compositeShader.setProgramUniform3f("skyColor", wr.skyColor);
        compositeShader.setProgramUniform1i("isEyeInWater", 0);
        compositeShader.setProgramUniform1i("heldBlockLightValue", 0);
        compositeShader.setProgramUniform1i("worldTime", Main.ticksran%24000);
        compositeShader.setProgramUniform1i("gcolor", 0);
        compositeShader.setProgramUniform1i("gdepth", 1);
        compositeShader.setProgramUniform1i("gnormal", 2);
        compositeShader.setProgramUniform1i("shadow", 1);
        compositeShader.setProgramUniform1i("composite", 3);
        compositeShader.setProgramUniform1i("gdepthtex", 5);
        compositeShader.setProgramUniform1i("noisetex", 4);
        compositeShader.setProgramUniform1i("eyeAltitude", 4);
        compositeShader.setProgramUniform1i("fogMode", 1);
        compositeShader.setProgramUniform2i("eyeBrightness", 0, 0);
        compositeShader.setProgramUniform2i("eyeBrightnessSmooth", 0, 0);
        compositeShader.setProgramUniformMatrix4ARB("gbufferModelView", false, Engine.getModelViewMatrix(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferModelViewInverse", false, Engine.getModelViewMatrixInv(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferPreviousModelView", false, Engine.getModelViewMatrixPrev(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferProjection", false, Engine.getProjectionMatrix(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferProjectionInverse", false, Engine.getProjectionMatrixInv(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferPreviousProjection", false, Engine.getProjectionMatrixPrev(), false);
        
        compositeShader.setProgramUniformMatrix4ARB("shadowModelView", false, Engine.getShadowModelViewMatrix(), false);
        compositeShader.setProgramUniformMatrix4ARB("shadowModelViewInverse", false, Engine.getShadowModelViewMatrixInv(), false);
        compositeShader.setProgramUniformMatrix4ARB("shadowProjection", false, Engine.getShadowProjectionMatrix(), false);
        compositeShader.setProgramUniformMatrix4ARB("shadowProjectionInverse", false, Engine.getShadowProjectionMatrixInv(), false);
    }

    public static int getAndResetNumCalls() {
        int total = 0;
        for (int i = 0; i < numShaders; i++) {
            total += allShaders[i].getAndResetNumCalls();
        }
        return total;
    }
}
