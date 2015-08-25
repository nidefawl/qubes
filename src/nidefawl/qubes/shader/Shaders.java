package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBUniformBufferObject.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.vec.Matrix4f;

public class Shaders {
    final static int NUM_MATRIXES = 13;
    final static String headerMatrix = "\r\n" + 
            "layout(std140) uniform scenedata\r\n" + 
            "{\r\n" + 
            "    mat4 mvp;\r\n" + 
            "    mat4 mv;\r\n" + 
            "    mat4 view;\r\n" + 
            "    mat4 proj;\r\n" + 
            "    mat4 mv_inv;\r\n" + 
            "    mat4 view_inv;\r\n" + 
            "    mat4 proj_inv;\r\n" + 
            "    mat4 shadow_mvp;\r\n" + 
            "    mat4 shadow_mv;\r\n" + 
            "    mat4 shadow_proj;\r\n" + 
            "    mat4 shadow_mvp_inv;\r\n" + 
            "    mat4 shadow_mv_inv;\r\n" + 
            "    mat4 shadow_proj_inv;\r\n" + 
            "} in_matrix;\r\n" + 
            "\r\n";
    final static String headerUniforms = "\r\n" + 
            "in vec4 in_position;\r\n" + 
            "in vec4 in_normal;\r\n" + 
            "in vec4 in_texcoord;\r\n" + 
            "in vec4 in_color;\r\n" + 
            "in vec4 in_brightness;\r\n" + 
            "in vec4 in_blockinfo;\r\n" + 
            "";

    public static void reinit() {
        Shaders.depthBufShader = null;
        Shaders.normals = null;
        Shaders.textured = null;
        Shaders.uboMatrix = 0;
        Shaders.uboLight = 0;
        Shaders.uboMaterial = 0;
        Shaders.uboBuffer = null;
        initShaders();
    }

    public static void init() {
        initShaders();
        Shaders.colored.enable();
        colored.setProgramUniform3f("in_offset", 0, 0, 0);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("setProgramUniform3f");
        Shaders.textured.enable();
        textured.setProgramUniform1i("tex0", 0);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("setProgramUniform1i");
        Shader.disable();
    }

    public static Shader depthBufShader;
    public static Shader normals;
    public static Shader textured;
    public static Shader colored;
    private static FloatBuffer uboBuffer;
    public static int uboMatrix;
    public static int uboMaterial;
    public static int uboLight;


    public static void initShaders() {
        try {
            uboBuffer = BufferUtils.createFloatBuffer(NUM_MATRIXES*64);
            if (uboMatrix == 0) {
                int blockSize = NUM_MATRIXES*64;
                uboMatrix = Engine.glGenBuffers(1).get();
                GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboMatrix);
                GL15.glBufferData(GL_UNIFORM_BUFFER, blockSize, GL15.GL_DYNAMIC_DRAW);
                if (Main.GL_ERROR_CHECKS)
                    Engine.checkGLError("UBO Matrix");
            }
            if (uboMaterial == 0) {
                int blockSize = 128;
                uboMaterial = Engine.glGenBuffers(1).get();
                GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboMaterial);
                GL15.glBufferData(GL_UNIFORM_BUFFER, blockSize, GL15.GL_DYNAMIC_DRAW);
                if (Main.GL_ERROR_CHECKS)
                    Engine.checkGLError("UBO Matrix");
            }
            if (uboLight == 0) {
                int blockSize = 128;
                uboLight = Engine.glGenBuffers(1).get();
                GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboLight);
                GL15.glBufferData(GL_UNIFORM_BUFFER, blockSize, GL15.GL_DYNAMIC_DRAW);
                if (Main.GL_ERROR_CHECKS)
                    Engine.checkGLError("UBO Matrix");
            }
            AssetManager assetMgr = AssetManager.getInstance();

            Shader new_depthBufShader = assetMgr.loadShader("shaders/renderdepth");
            Shader new_normals = assetMgr.loadShader("shaders/visnormals");
            Shader new_textured = assetMgr.loadShader("shaders/textured");
            Shader new_colored = assetMgr.loadShader("shaders/colored");
            if (Shaders.depthBufShader != null)
                Shaders.depthBufShader.release();
            if (Shaders.normals != null)
                Shaders.normals.release();
            if (Shaders.textured != null)
                Shaders.textured.release();
            if (Shaders.colored != null)
                Shaders.colored.release();
            Shaders.depthBufShader = new_depthBufShader;
            Shaders.normals = new_normals;
            Shaders.textured = new_textured;
            Shaders.colored = new_colored;
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            Main.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
    }

    public static void pushMat(BufferedMatrix mat) {
        pushdmv=mat;
        Matrix4f.mul(Engine.getMatOrthoP(), mat, pushdmvp);
        pushdmvp.update();
        uboBuffer.position(0).limit(128);
        uboBuffer.put(pushdmvp.get());
        uboBuffer.put(pushdmv.get());
        uboBuffer.flip();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboMatrix);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_UNIFORM_BUFFER");
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, 0, uboBuffer);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData GL_UNIFORM_BUFFER");
    }
    public static void popMat() {
        bindOrthoUBO();
    }
    static BufferedMatrix pushdmv;
    static BufferedMatrix pushdmvp = new BufferedMatrix();
    public static void bindOrthoUBO() {
        uboBuffer.position(0).limit(128);
        uboBuffer.put(Engine.getMatOrthoP().get());
        uboBuffer.put(Engine.getMatOrthoMV().get());
        uboBuffer.flip();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboMatrix);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_UNIFORM_BUFFER");
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, 0, uboBuffer);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData GL_UNIFORM_BUFFER");
    }
    public static void updateUBO() {
        uboBuffer.position(0).limit(NUM_MATRIXES*64);
        uboBuffer.put(Engine.getMatSceneMVP().get());
        uboBuffer.put(Engine.getMatSceneMV().get());
        uboBuffer.put(Engine.getMatSceneV().get());
        uboBuffer.put(Engine.getMatSceneP().get());
        uboBuffer.put(Engine.getMatSceneMV().getInv());
        uboBuffer.put(Engine.getMatSceneV().getInv());
        uboBuffer.put(Engine.getMatSceneP().getInv());
        uboBuffer.put(Engine.getMatShadowMVP().get());
        uboBuffer.put(Engine.getMatShadowMV().get());
        uboBuffer.put(Engine.getMatShadowP().get());
        uboBuffer.put(Engine.getMatShadowMVP().getInv());
        uboBuffer.put(Engine.getMatShadowMV().getInv());
        uboBuffer.put(Engine.getMatShadowP().getInv());
        uboBuffer.flip();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboMatrix);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_UNIFORM_BUFFER");
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, 0, uboBuffer);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData GL_UNIFORM_BUFFER");
        //    System.out.println(name+"/"+blockIndex);
        // Uniform A
        uboBuffer.position(0).limit(128);
//        System.out.println(Engine.sunPosition);
        uboBuffer.put(Engine.sunPosition.x);
        uboBuffer.put(Engine.sunPosition.y);
        uboBuffer.put(Engine.sunPosition.z);
//        uboBuffer.put(0);
//        uboBuffer.put(0.8f);
//        uboBuffer.put(0.3f);
        uboBuffer.put(1F);
        float ambIntens = 0.8F;
        float diffIntens = 0.9F;
        float specIntens = 0.4F;
        for (int a = 0; a < 4; a++) {
            uboBuffer.put(ambIntens);
        }
        for (int a = 0; a < 4; a++) {
            uboBuffer.put(diffIntens);
        }
        for (int a = 0; a < 3; a++) {
            uboBuffer.put(specIntens);
        }
        uboBuffer.put(0);
        uboBuffer.flip();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboLight);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_UNIFORM_BUFFER");
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, 0, uboBuffer);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData GL_UNIFORM_BUFFER");

        uboBuffer.position(0).limit(128);
        float matAmbRef = 0.4F;
        float matDiffRef = 0.4F;
        float matSpecRef = 0.0F;
        float matShininess = 0.0F;
        for (int a = 0; a < 4; a++) {
            uboBuffer.put(a, matAmbRef);
            uboBuffer.put(4+a, matDiffRef);
            uboBuffer.put(8+a, matSpecRef);
            uboBuffer.put(12+a, matShininess);
        }
        uboBuffer.flip();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboMaterial);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_UNIFORM_BUFFER");
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, 0, uboBuffer);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData GL_UNIFORM_BUFFER");
        
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

        sh.setProgramUniformMatrix4ARB("gbufferView", false, Engine.getMatSceneV().get(), false);
        sh.setProgramUniformMatrix4ARB("gbufferModelView", false, Engine.getMatSceneMV().get(), false);
        sh.setProgramUniformMatrix4ARB("gbufferModelViewInverse", false, Engine.getMatSceneMV().getInv(), false);
        sh.setProgramUniformMatrix4ARB("gbufferPreviousModelView", false, Engine.getMatSceneMV().getPrev(), false);

        sh.setProgramUniformMatrix4ARB("gbufferProjection", false, Engine.getMatSceneP().get(), false);
        sh.setProgramUniformMatrix4ARB("gbufferProjectionInverse", false, Engine.getMatSceneP().getInv(), false);
        sh.setProgramUniformMatrix4ARB("gbufferPreviousProjection", false, Engine.getMatSceneP().getPrev(), false);

        sh.setProgramUniformMatrix4ARB("shadowModelView", false, Engine.getMatShadowMV().get(), false);
        sh.setProgramUniformMatrix4ARB("shadowModelViewInverse", false, Engine.getMatShadowMV().getInv(), false);
        sh.setProgramUniformMatrix4ARB("shadowProjection", false, Engine.getMatShadowP().get(), false);
        sh.setProgramUniformMatrix4ARB("shadowProjectionInverse", false, Engine.getMatShadowP().getInv(), false);

    }
}
