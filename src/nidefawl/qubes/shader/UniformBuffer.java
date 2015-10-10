package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBUniformBufferObject.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Memory;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.WorldClient;

public class UniformBuffer {
    static int nextIdx = 0;
    static UniformBuffer[] buffers = new UniformBuffer[6];
    String name;
    private int buffer;
    protected int len;
    private FloatBuffer floatBuffer;
    UniformBuffer(String name) {
        this(name, 0);
    }
    UniformBuffer(String name, int len) {
        buffers[nextIdx++] = this;
        this.name = name;
        this.len = len;
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
    void setPosition(int n) {
        this.floatBuffer.position(n);
    }
    void reset() {
        this.floatBuffer.position(0).limit(this.len);
    }
    void put(FloatBuffer floatBuffer) {
        this.floatBuffer.put(floatBuffer);
    }
    void put(float f) {
        this.floatBuffer.put(f);
    }

    void put(float x, float y, float z) {
        this.floatBuffer.put(x);
        this.floatBuffer.put(y);
        this.floatBuffer.put(z);
        this.floatBuffer.put(1);
    }
    void putNeg(float x, float y, float z) {
        put(-x, -y, -z);
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
        this.floatBuffer = Memory.createFloatBufferAligned(64, this.len);
        System.err.println(this.floatBuffer+"/"+this.len);
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
    static UniformBuffer VertexDirections = new UniformBuffer("VertexDirections", 64*4);
    
    public static void init() {
        int size = GL11.glGetInteger(GL_MAX_UNIFORM_BLOCK_SIZE);
        System.out.println("GL_MAX_UNIFORM_BLOCK_SIZE: "+size);
        for (int i = 0; i < buffers.length; i++) {
            buffers[i].setup();
        }
        updateVertDir();
    }
    public static void reinit() {
        init();
    }


    public static void bindBuffers(Shader shader) {
        for (int i = 0; i < buffers.length; i++) {
            final int blockIndex = glGetUniformBlockIndex(shader.shader, buffers[i].name);
            if (blockIndex != -1) {
                glBindBufferBase(GL_UNIFORM_BUFFER, i, buffers[i].buffer);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glBindBufferBase GL_UNIFORM_BUFFER");
                glUniformBlockBinding(shader.shader, blockIndex, i);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glUniformBlockBinding blockIndex "+blockIndex);
            }
        }
    }

    public static void updateUBO(WorldClient world, float f) {
        if (Game.DO_TIMING) TimingHelper.startSec("updateUBO");
//        Shaders.colored.enable();
//        Shaders.colored.setProgramUniformMatrix4ARB("matortho", false, Engine.getMatOrthoMVP().get(), false);
//        Shaders.textured.enable();
//        Shaders.textured.setProgramUniformMatrix4ARB("matortho", false, Engine.getMatOrthoMVP().get(), false);
//        Shader.disable();

        updateOrtho();
//        updateVertDir();
        
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
        float ambIntens = 0.07F;
        float diffIntens = 0.55F;
        float specIntens = 0.45F;
        for (int a = 0; a < 3; a++) {
            LightInfo.put(ambIntens);
        }
        LightInfo.put(1);
        for (int a = 0; a < 3; a++) {
            LightInfo.put(diffIntens);
        }
        LightInfo.put(1);
        for (int a = 0; a < 3; a++) {
            LightInfo.put(specIntens);
        }
        LightInfo.put(1);
        LightInfo.update();
        if (Game.DO_TIMING) TimingHelper.endSec();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, 0);

    }
    
    /**
     * 
     */
    private static void updateVertDir() {
        UniformBuffer buf = VertexDirections;
        buf.reset();
//
        buf.put(0, 0, 0);
        
        //Dir.DIR_POS_X
        buf.put(0, -1, -1);
        buf.put(0,  1, -1); 
        buf.put(0,  1,  1); 
        buf.put(0, -1,  1); 
        //Dir.DIR_NEG_X
        buf.put(0, -1, -1);
        buf.put(0,  1, -1); 
        buf.put(0,  1,  1); 
        buf.put(0, -1,  1); 
        //Dir.DIR_POS_Y
        buf.put(-1,  0, -1);
        buf.put(-1,  0,  1); 
        buf.put( 1,  0,  1); 
        buf.put( 1,  0, -1); 
        //Dir.DIR_NEG_Y
        buf.put(-1,  0, -1);
        buf.put(-1,  0,  1); 
        buf.put( 1,  0,  1); 
        buf.put( 1,  0, -1); 
        //Dir.DIR_POS_Z
        buf.put(-1, -1, 0);
        buf.put( 1, -1, 0); 
        buf.put( 1,  1, 0); 
        buf.put(-1,  1, 0); 
        //Dir.DIR_NEG_Z
        buf.put(-1, -1, 0);
        buf.put( 1, -1, 0);  
        buf.put( 1,  1, 0); 
        buf.put(-1,  1, 0);
        
        
        buf.setPosition(32*4); //POS IN FLOATS
        buf.put(0, 0, 0);
        
        //Dir.DIR_POS_X
        buf.putNeg(0, -1, -1);
        buf.putNeg(0,  1, -1); 
        buf.putNeg(0,  1,  1); 
        buf.putNeg(0, -1,  1); 
        //Dir.DIR_NEG_X
        buf.putNeg(0, -1, -1);
        buf.putNeg(0,  1, -1); 
        buf.putNeg(0,  1,  1); 
        buf.putNeg(0, -1,  1); 
        //Dir.DIR_POS_Y
        buf.putNeg(-1,  0, -1);
        buf.putNeg(-1,  0,  1); 
        buf.putNeg( 1,  0,  1); 
        buf.putNeg( 1,  0, -1); 
        //Dir.DIR_NEG_Y
        buf.putNeg(-1,  0, -1);
        buf.putNeg(-1,  0,  1); 
        buf.putNeg( 1,  0,  1); 
        buf.putNeg( 1,  0, -1); 
        //Dir.DIR_POS_Z
        buf.putNeg(-1, -1, 0);
        buf.putNeg( 1, -1, 0); 
        buf.putNeg( 1,  1, 0); 
        buf.putNeg(-1,  1, 0); 
        //Dir.DIR_NEG_Z
        buf.putNeg(-1, -1, 0);
        buf.putNeg( 1, -1, 0);  
        buf.putNeg( 1,  1, 0); 
        buf.putNeg(-1,  1, 0);
        buf.update();
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
