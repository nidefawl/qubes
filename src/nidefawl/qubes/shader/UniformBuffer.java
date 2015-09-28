package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBUniformBufferObject.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.world.WorldClient;

public class UniformBuffer {
    static int nextIdx = 0;
    static UniformBuffer[] buffers = new UniformBuffer[5];
    String name;
    private int buffer;
    private int len;
    private FloatBuffer floatBuffer;
    UniformBuffer(String name) {
        buffers[nextIdx++] = this;
        this.name = name;
    }
    UniformBuffer addMat4() {
        this.len+=16;
        return this;
    }
    UniformBuffer addVec4() {
        this.len+=4;
        return this;
    }
    UniformBuffer addFloat() {
        this.len+=4;
        return this;
    }
    private void reset() {
        this.floatBuffer.position(0).limit(this.len);
    }
    private void put(FloatBuffer floatBuffer) {
        this.floatBuffer.put(floatBuffer);
    }
    private void put(float f) {
        this.floatBuffer.put(f);
    }
    void update() {
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, this.buffer);
        this.floatBuffer.position(0).limit(this.len);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_UNIFORM_BUFFER");
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, 0, this.floatBuffer);
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferSubData GL_UNIFORM_BUFFER "+this.name+"/"+this.buffer+"/"+this.floatBuffer+"/"+this.len);
    }
    public void setup() {
        this.floatBuffer = BufferUtils.createByteBuffer(this.len << 2).asFloatBuffer();
        this.buffer = Engine.glGenBuffers(1).get();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("UBO Engine.glGenBuffers");
        GL15.glBufferData(GL_UNIFORM_BUFFER, this.len*4, GL15.GL_DYNAMIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("UBO Matrix");
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    static UniformBuffer uboMatrix3D = new UniformBuffer("uboMatrix3D")
            .addMat4() //mvp
            .addMat4() //mv
            .addMat4() //view
            .addMat4() //vp
            .addMat4() //normal
            .addMat4() //mv_inv
            .addMat4(); // proj_inv
    static UniformBuffer uboMatrix2D = new UniformBuffer("uboMatrix2D")
            .addMat4(); //mvp
//            .addMat4() //proj
//            .addMat4(); //mv
    static UniformBuffer uboMatrixShadow = new UniformBuffer("uboMatrixShadow")
            .addMat4() //shadow_split_mvp
            .addMat4() //shadow_split_mvp
            .addMat4() //shadow_split_mvp
            .addMat4() //shadow_split_mvp
            .addVec4(); //shadow_split_depth
    static UniformBuffer uboSceneData = new UniformBuffer("uboSceneData")
            .addMat4() //vp
            .addVec4() //cameraPosition
            .addFloat() //frameTime
            .addVec4(); //viewport
    static UniformBuffer LightInfo = new UniformBuffer("LightInfo")
            .addVec4() //dayLightTime
            .addVec4() //posSun
            .addVec4() //lightDir
            .addVec4() // Ambient light intensity
            .addVec4() // Diffuse light intensity
            .addVec4(); // Specular light intensity
    
    public static void init() {
        for (int i = 0; i < buffers.length; i++) {
            buffers[i].setup();
        }
    }
    public static void reinit() {
        init();
    }


    public static void bindBuffers(Shader shader) {
        for (int i = 0; i < buffers.length; i++) {
            final int blockIndex = glGetUniformBlockIndex(shader.shader, buffers[i].name);
            if (blockIndex != -1) {
                glBindBufferBase(GL_UNIFORM_BUFFER, shader.bufBindIdx, buffers[i].buffer);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glBindBufferBase GL_UNIFORM_BUFFER");
                glUniformBlockBinding(shader.shader, blockIndex, shader.bufBindIdx);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glUniformBlockBinding blockIndex "+blockIndex);
                shader.bufBindIdx++;
            }
        }
    }

    public static void updateUBO(WorldClient world, float f) {
        if (Game.DO_TIMING) TimingHelper.startSec("updateUBO");
        Shaders.colored.enable();
        Shaders.colored.setProgramUniformMatrix4ARB("matortho", false, Engine.getMatOrthoMVP().get(), false);
        Shaders.textured.enable();
        Shaders.textured.setProgramUniformMatrix4ARB("matortho", false, Engine.getMatOrthoMVP().get(), false);
        Shader.disable();
        
        updateOrtho();
        
        uboMatrix3D.reset();
        uboMatrix3D.put(Engine.getMatSceneMVP().get());
        uboMatrix3D.put(Engine.getMatSceneMV().get());
        uboMatrix3D.put(Engine.getMatSceneV().get());
        uboMatrix3D.put(Engine.getMatSceneVP().get());
        uboMatrix3D.put(Engine.getMatSceneNormal().get());
        uboMatrix3D.put(Engine.getMatSceneMV().getInv());
        uboMatrix3D.put(Engine.getMatSceneP().getInv());
        uboMatrix3D.update();

        uboMatrixShadow.reset();
        
        if (Engine.initRenderers) {
            uboMatrixShadow.put(Engine.shadowProj.getSMVP(0));
            uboMatrixShadow.put(Engine.shadowProj.getSMVP(1));
            uboMatrixShadow.put(Engine.shadowProj.getSMVP(2));
            uboMatrixShadow.put(Engine.shadowProj.getSMVP(2));
            
            uboMatrixShadow.put(Engine.shadowProj.shadowSplitDepth[0]);
            uboMatrixShadow.put(Engine.shadowProj.shadowSplitDepth[1]);
            uboMatrixShadow.put(Engine.shadowProj.shadowSplitDepth[2]);
            uboMatrixShadow.put(1F);
        } else {
            uboMatrixShadow.put(Engine.getMatSceneMVP().get());
            uboMatrixShadow.put(Engine.getMatSceneMVP().get());
            uboMatrixShadow.put(Engine.getMatSceneMVP().get());
            uboMatrixShadow.put(Engine.getMatSceneMVP().get());

            uboMatrixShadow.put(1F);
            uboMatrixShadow.put(1F);
            uboMatrixShadow.put(1F);
            uboMatrixShadow.put(1F);
        }
        uboMatrixShadow.update();
        

        uboSceneData.reset();
        Engine.camera.getPosition().store(uboSceneData.floatBuffer);
        uboSceneData.put(1F); // camera w component
        
        uboSceneData.put(Game.ticksran+f);
        uboSceneData.put(1F);
        uboSceneData.put(1F);
        uboSceneData.put(1F);
        
        uboSceneData.put(Game.displayWidth);
        uboSceneData.put(Game.displayHeight);
        uboSceneData.put(Engine.znear);
        uboSceneData.put(Engine.zfar);
        uboSceneData.update();
        
        LightInfo.reset();
        if (world != null) {
            LightInfo.put(world.getDayNoonFloat()); // dayTime
            LightInfo.put(world.getNightNoonFloat()); // nightlight
            LightInfo.put(world.getDayLightIntensity()); // dayLightIntens
            LightInfo.put(world.getLightAngleUp()); // lightAngleUp
        } else {
            LightInfo.put(0);
            LightInfo.put(0);
            LightInfo.put(0);
            LightInfo.put(0);
        }
        
        LightInfo.put(Engine.lightPosition.x);
        LightInfo.put(Engine.lightPosition.y);
        LightInfo.put(Engine.lightPosition.z);
        LightInfo.put(1F);
        LightInfo.put(Engine.lightDirection.x);
        LightInfo.put(Engine.lightDirection.y);
        LightInfo.put(Engine.lightDirection.z);
        LightInfo.put(1F);
        float ambIntens = 0.08F;
        float diffIntens = 0.52F;
        float specIntens = 0.12F;
        LightInfo.put(ambIntens);
        LightInfo.put(ambIntens);
        LightInfo.put(ambIntens);
        LightInfo.put(1);
        for (int a = 0; a < 3; a++) {
            LightInfo.put(diffIntens);
        }
        LightInfo.put(1);
        for (int a = 0; a < 3; a++) {
            LightInfo.put(specIntens);
        }
        LightInfo.put(0);
        LightInfo.update();
        if (Game.DO_TIMING) TimingHelper.endSec();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, 0);

    }
    
    /**
     * @param mat
     */
    public static void updateOrtho() {
        uboMatrix2D.reset();
        uboMatrix2D.put(Engine.getMatOrthoMVP().get());
        uboMatrix2D.update();
        // unbind Uniform buffer?!
    }
}
