package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBUniformBufferObject.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Main;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.TimingHelper;
import nidefawl.qubes.vec.Matrix4f;

public class UniformBuffer {
    final static int NUM_MATRIXES = 11;
    final static int SIZE_STRUCT = NUM_MATRIXES*64+16+16+16+16;

    private static FloatBuffer uboBuffer;
    public static int uboMatrix;
    public static int uboMaterial;
    public static int uboLight;
    
    public static void init() {
        uboBuffer = BufferUtils.createFloatBuffer(SIZE_STRUCT);
        if (uboMatrix == 0) {
            int blockSize = SIZE_STRUCT;
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
    }
    public static void reinit() {
        uboMatrix = 0;
        uboLight = 0;
        uboMaterial = 0;
        uboBuffer = null;
        init();
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
        if (Main.DO_TIMING) TimingHelper.startSec("bindOrthoUBO");
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
        if (Main.DO_TIMING) TimingHelper.endSec();
    }
    public static void updateUBO(float f) {
        if (Main.DO_TIMING) TimingHelper.startSec("updateUBO");
        uboBuffer.position(0).limit(SIZE_STRUCT);
        uboBuffer.put(Engine.getMatSceneMVP().get());
        uboBuffer.put(Engine.getMatSceneMV().get());
        uboBuffer.put(Engine.getMatSceneV().get());
        uboBuffer.put(Engine.getMatSceneP().get());
        uboBuffer.put(Engine.getMatSceneMV().getInv());
        uboBuffer.put(Engine.getMatSceneV().getInv());
        uboBuffer.put(Engine.getMatSceneP().getInv());
        uboBuffer.put(Engine.getMatShadowMVP().get());
        uboBuffer.put(Engine.getMatShadowSplitMVP(0).get());
        uboBuffer.put(Engine.getMatShadowSplitMVP(1).get());
        uboBuffer.put(Engine.getMatShadowSplitMVP(2).get());
        uboBuffer.put(Engine.shadowSplitDepth[0]);
        uboBuffer.put(Engine.shadowSplitDepth[1]);
        uboBuffer.put(Engine.shadowSplitDepth[2]);
        uboBuffer.put(1F);
        uboBuffer.put(Engine.camera.getPosition().x);
        uboBuffer.put(Engine.camera.getPosition().y);
        uboBuffer.put(Engine.camera.getPosition().z);
        uboBuffer.put(1F);
        uboBuffer.put(Main.ticksran+f);
        uboBuffer.put(1F);
        uboBuffer.put(1F);
        uboBuffer.put(1F);
        uboBuffer.put(Main.displayWidth);
        uboBuffer.put(Main.displayHeight);
        uboBuffer.put(Engine.znear);
        uboBuffer.put(Engine.zfar);
        uboBuffer.flip();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, uboMatrix);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_UNIFORM_BUFFER");
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, 0, uboBuffer);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferSubData GL_UNIFORM_BUFFER "+uboMatrix+"/"+uboBuffer);
        //    System.out.println(name+"/"+blockIndex);
        // Uniform A
        uboBuffer.position(0).limit(128);
//        System.out.println(Engine.sunPosition);
        uboBuffer.put(Engine.sunPosition.x);
        uboBuffer.put(Engine.sunPosition.y);
        uboBuffer.put(Engine.sunPosition.z);
        uboBuffer.put(1F);
        uboBuffer.put(Engine.moonPosition.x);
        uboBuffer.put(Engine.moonPosition.y);
        uboBuffer.put(Engine.moonPosition.z);
        uboBuffer.put(1F);
        float ambIntens = 0.34F;
        float diffIntens = 0.7F;
        float specIntens = 0.3F;
        uboBuffer.put(ambIntens*1.1F);
        uboBuffer.put(ambIntens*1.1F);
        uboBuffer.put(ambIntens*1.1F);
        uboBuffer.put(1);
        for (int a = 0; a < 3; a++) {
            uboBuffer.put(diffIntens);
        }
        uboBuffer.put(1);
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
        if (Main.DO_TIMING) TimingHelper.endSec();
        
    }
    public static void bindBuffers(Shader shader) {
        // Get uniform block index and data size
        final int blockIndex = glGetUniformBlockIndex(shader.shader, "scenedata");
        if (blockIndex != -1) {
//            System.out.println(name+"/"+blockIndex);
            glBindBufferBase(GL_UNIFORM_BUFFER, 0, UniformBuffer.uboMatrix);
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBufferBase GL_UNIFORM_BUFFER");
            glUniformBlockBinding(shader.shader, blockIndex, 0);
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("glUniformBlockBinding blockIndex "+blockIndex);

        }
        // Get uniform block index and data size
        final int light = glGetUniformBlockIndex(shader.shader, "LightInfo");
        if (light != -1) {
//            final int blockSize = glGetActiveUniformBlocki(shader.shader, blockIndex, GL_UNIFORM_BLOCK_DATA_SIZE);
//            System.out.println(name+"/"+light+" light, size "+blockSize);
            glBindBufferBase(GL_UNIFORM_BUFFER, 1, UniformBuffer.uboLight);
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBufferBase GL_UNIFORM_BUFFER");
            glUniformBlockBinding(shader.shader, light, 1);
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("glUniformBlockBinding blockIndex "+light);

        }
        // Get uniform block index and data size
        final int mat = glGetUniformBlockIndex(shader.shader, "MaterialInfo");
        if (mat != -1) {
//            final int blockSize = glGetActiveUniformBlocki(shader.shader, blockIndex, GL_UNIFORM_BLOCK_DATA_SIZE);
//            System.out.println(name+"/"+light+" MaterialInfo, size "+blockSize);
            glBindBufferBase(GL_UNIFORM_BUFFER, 2, UniformBuffer.uboMaterial);
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBufferBase GL_UNIFORM_BUFFER");
            glUniformBlockBinding(shader.shader, mat, 2);
            if (Main.GL_ERROR_CHECKS)
                Engine.checkGLError("glUniformBlockBinding blockIndex "+mat);

        }
    }

}
