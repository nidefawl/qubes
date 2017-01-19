package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBUniformBufferObject.*;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.opengl.GL15;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Memory;
import nidefawl.qubes.models.render.ModelConstants;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.SunLightModel;
import nidefawl.qubes.world.WorldClient;

public class UniformBuffer {
    static int nextIdx = 0;
    static UniformBuffer[] buffers = new UniformBuffer[8];
    String name;
    private int buffer;
    protected int len;
    private FloatBuffer floatBuffer;
    private int bindingPoint;
    private boolean autoBind;
    UniformBuffer(String name) {
        this(name, 0, true);
    }
    UniformBuffer(String name, int len, boolean autoBind) {
        buffers[nextIdx++] = this;
        this.autoBind = autoBind;
        this.bindingPoint = Engine.getBindingPoint(name);
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
    public void reset() {
        this.floatBuffer.position(0).limit(this.len);
    }
    public void put(FloatBuffer floatBuffer) {
        this.floatBuffer.put(floatBuffer);
    }
    private void put(float[] mat4x4) {
        this.floatBuffer.put(mat4x4);
    }
    public FloatBuffer getFloatBuffer() {
        return this.floatBuffer;
    }
    void put(float f) {
        this.floatBuffer.put(f);
    }
    void skip() {
        this.floatBuffer.position(this.floatBuffer.position()+1);
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
    public void update() {
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer "+this.buffer);
        this.floatBuffer.position(0).limit(this.len);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer GL_UNIFORM_BUFFER");
        GL15.glBufferSubData(GL_UNIFORM_BUFFER, 0, this.floatBuffer);
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferSubData GL_UNIFORM_BUFFER "+this.name+"/"+this.buffer+"/"+this.floatBuffer+"/"+this.len);
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    public void setup() {
        this.floatBuffer = Memory.createFloatBufferAligned(64, this.len);
        this.buffer = Engine.glGenBuffers(1).get();
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, this.buffer);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("UBO Engine.glGenBuffers");
        GL15.glBufferData(GL_UNIFORM_BUFFER, this.len*4, GL15.GL_DYNAMIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("UBO Matrix");
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    private void release() {
        if (this.floatBuffer != null) {
            Memory.free(this.floatBuffer);
            this.floatBuffer=null;   
        }
        if (this.buffer > 0) {
            Engine.deleteBuffers(this.buffer);
            this.buffer = 0;
        }
        
    }
    public static UniformBuffer uboMatrix3D = new UniformBuffer("uboMatrix3D")
            .addMat4() //mvp
            .addMat4() //mv
            .addMat4() //view
            .addMat4() //vp
            .addMat4() //p
            .addMat4() //normal
            .addMat4() //mv_inv
            .addMat4(); // proj_inv
    public static UniformBuffer uboMatrix3D_Temp = new UniformBuffer("uboMatrix3D", uboMatrix3D.len, false);
    static UniformBuffer uboMatrix2D = new UniformBuffer("uboMatrix2D")
            .addMat4() //mvp
            .addMat4() //3DOrthoP
            .addMat4(); //3DOrthoMV
    static UniformBuffer uboMatrixShadow = new UniformBuffer("uboMatrixShadow")
            .addMat4() //shadow_split_mvp
            .addMat4() //shadow_split_mvp
            .addMat4() //shadow_split_mvp
            .addMat4() //shadow_split_mvp
            .addVec4(); //shadow_split_depth
    static UniformBuffer uboSceneData = new UniformBuffer("uboSceneData")
            .addVec4() //camera (xyzw)
            .addVec4() //globaloffset (xyz) time (w)
            .addVec4() //viewport (xyzw)
            .addVec4() //pxoffset (xyz) 1.0f (w)
            .addVec4(); //prev camera (xyz) 1.0f (w)
    static UniformBuffer LightInfo = new UniformBuffer("LightInfo")
            .addVec4() //dayLightTime
            .addVec4() //posSun
            .addVec4() //lightDir
            .addVec4() // Ambient light intensity
            .addVec4() // Diffuse light intensity
            .addVec4(); // Specular light intensity
    static UniformBuffer VertexDirections = new UniformBuffer("VertexDirections", 64*4, true);
    static UniformBuffer TBNMat = new UniformBuffer("TBNMatrix", 16*6, true);
//    public static UniformBuffer BoneMatUBO = new UniformBuffer("BoneMatUBO", BatchedRiggedModelRenderer.STRUCT_SIZE*7);
    
    
    public static void init() {
//        int size = GL11.glGetInteger(GL_MAX_UNIFORM_BLOCK_SIZE);
//        System.out.println("GL_MAX_UNIFORM_BLOCK_SIZE: "+size);
        for (int i = 0; i < buffers.length; i++) {
            buffers[i].setup();
        }
        updateVertDir();
        updateTBNMatrices();
    }
    public static void destroy() {
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] != null)
                buffers[i].release();
        }
    }

    public static void rebindShaders() {
        for (int i = 0; i < buffers.length; i++) {
            for (Shader shader : buffers[i].shaders) {
                if (shader.valid) {
                    bindBuffers(shader);
                }
            }
        }
    }

    public static void bindBuffers(Shader shader) {
        for (int i = 0; i < buffers.length; i++) {
            final int blockIndex = glGetUniformBlockIndex(shader.shader, buffers[i].name);
            if (blockIndex != -1) {
                buffers[i].addShader(shader);
                glUniformBlockBinding(shader.shader, blockIndex, buffers[i].bindingPoint);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glUniformBlockBinding blockIndex " + blockIndex);
                if (buffers[i].autoBind) {
                    buffers[i].bind();
                }
            }
        }
    }

    public void bind() {
        //      System.out.println("bind blockidx "+blockIndex+" of buffer "+buffers[i].name+"/"+i+"/"+buffers[i].buffer+" to shader "+shader.name);
        glBindBufferBase(GL_UNIFORM_BUFFER, this.bindingPoint, this.buffer); 
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBufferBase GL_UNIFORM_BUFFER");
    }
    List<Shader> shaders = Lists.newArrayList();
    /**
     * @param shader
     */
    private void addShader(Shader shader) {
        if (this.shaders.contains(shader)) return;
        for (int i = 0; i < this.shaders.size(); i++) {
            Shader s = this.shaders.get(i);
            if (!s.valid) {
                this.shaders.remove(i--);
            }
        }
        this.shaders.add(shader);
    }
    static boolean once = false;
    public static void updateUBO(WorldClient world, float f) {
//        Shaders.colored.enable();
//        Shaders.colored.setProgramUniformMatrix4ARB("matortho", false, Engine.getMatOrthoMVP().get(), false);
//        Shaders.textured.enable();
//        Shaders.textured.setProgramUniformMatrix4ARB("matortho", false, Engine.getMatOrthoMVP().get(), false);
//        Shader.disable();

        updateOrtho();
//        updateVertDir();
        updateTBNMatrices();
        
        updateSceneMatrices();

        uboMatrixShadow.reset();
        
        if (Engine.shadowProj != null) {
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
        Vector3f vCam = Engine.camera.getPosition();
        
        uboSceneData.put(vCam.x-Engine.GLOBAL_OFFSET.x);
        uboSceneData.put(vCam.y-Engine.GLOBAL_OFFSET.y);
        uboSceneData.put(vCam.z-Engine.GLOBAL_OFFSET.z);
        uboSceneData.put(1F); // camera w component

        uboSceneData.put(Engine.GLOBAL_OFFSET.x);
        uboSceneData.put(Engine.GLOBAL_OFFSET.y);
        uboSceneData.put(Engine.GLOBAL_OFFSET.z);
        uboSceneData.put(Game.ticksran+f);

        uboSceneData.put(Game.displayWidth);
        uboSceneData.put(Game.displayHeight);
        uboSceneData.put(Engine.znear);
        uboSceneData.put(Engine.zfar);
        uboSceneData.put(Engine.pxOffset.x);
        uboSceneData.put(Engine.pxOffset.y);
        uboSceneData.put(Engine.pxOffset.z);
        uboSceneData.put(1F);
        vCam = Engine.camera.getPrevPosition();
        uboSceneData.put(vCam.x-Engine.GLOBAL_OFFSET.x);
        uboSceneData.put(vCam.y-Engine.GLOBAL_OFFSET.y);
        uboSceneData.put(vCam.z-Engine.GLOBAL_OFFSET.z);
        uboSceneData.put(1F); // camera w component
        uboSceneData.update();
        
        LightInfo.reset();
        float nightNoon = 0;
        if (world != null) {
            nightNoon = world.getNightNoonFloat();
            LightInfo.put(world.getDayNoonFloat()); // dayTime
            LightInfo.put(nightNoon); // nightlight
            LightInfo.put(world.getDayLightIntensity()); // dayLightIntens
            LightInfo.put(world.getLightAngleUp()); // lightAngleUp      
        } else {
            SunLightModel model = Engine.getSunLightModel();
            nightNoon = model.getNightNoonFloat();
            LightInfo.put(model.getDayNoonFloat()); // dayTime
            LightInfo.put(nightNoon); // nightlight
            LightInfo.put(model.getDayLightIntensity()); // dayLightIntens
            LightInfo.put(model.getLightAngleUp()); // lightAngleUp
        }
        LightInfo.put(Engine.lightPosition.x);
        LightInfo.put(Engine.lightPosition.y);
        LightInfo.put(Engine.lightPosition.z);
        LightInfo.put(1F);
//        System.out.println(Engine.lightDirection);
        LightInfo.put(Engine.lightDirection.x);
        LightInfo.put(Engine.lightDirection.y);
        LightInfo.put(Engine.lightDirection.z);
        LightInfo.put(1F);

        float ambIntens = 0.1f;
        float diffIntens = 0.1f;
        float specIntens = 0.7F;
//        float ambIntens = 0.12f;
//        float diffIntens = 0.17F;
//        float specIntens = 0.16F;
        float fNight = GameMath.easeInOutCubic(nightNoon);
        ambIntens*=Math.max(0, 1.0f-fNight*0.98f);
        diffIntens*=1.0f-fNight*0.97f;
        specIntens*=1.0f-fNight*0.91f;
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
        GL15.glBindBuffer(GL_UNIFORM_BUFFER, 0);

    }

    private static void updateTBNMatrices() {
        // our terrain normal mat is constant!
        // thats good, so we only calculate this matrices once
        // BufferedMatrix normalMat = Engine.getMatSceneNormal(); <-- not required (is identity)
        Vector3f normal = new Vector3f();
        Vector3f tangent = new Vector3f();
        Vector3f bitangent = new Vector3f();
        UniformBuffer buf = TBNMat;
        buf.reset();
        for (int i = 0; i < 6; i++) {
            int x = Dir.getDirX(i);
            int y = Dir.getDirY(i);
            int z = Dir.getDirZ(i);
            normal.set(x, y, z);
            
            /*
            +y
            |
            |     +z
            |    /
            |   /
            |  /
            | /
            |/_____________ +x
            */

            //there might be a smarter way to do this, but we only have 6 cases
            if (x > 0) { //POS X face
                //  1.0,  0.0,  0.0
                tangent.set(0, 0, -1);
                bitangent.set(0, -1, 0);
            } else if (x < -0.5) { //NEG X face
                //  -1.0,  0.0,  0.0
                tangent.set(0, 0, 1);
                bitangent.set(0, -1, 0);
            } else if (y > 0.5) { //POS Y face
                //  0.0,  0.0,  1.0
                tangent.set(0, 0, 1);
                bitangent.set(1, 0, 0);
            } else if (y < -0.5) { //NEG Y face
                //  0.0, -1.0,  0.0
                tangent.set(0, 0, -1);
                bitangent.set(-1, 0, 0);
            } else if (z > 0.5) {  //POS Z face
                //  0.0, -1.0,  0.0
                tangent.set(-1, 0, 0);
                bitangent.set(0, -1, 0);
            } else if (z < -0.5) { // NEG Z face
                //  0.0,  0.0, -1.0
                tangent.set(1, 0, 0);
                bitangent.set(0, -1, 0);
            }
            
            float[] mat4x4 = new float[] {
                bitangent.x, bitangent.y, bitangent.z, 0,
                normal.x, normal.y, normal.z, 0,
                tangent.x, tangent.y, tangent.z, 0,
                0, 0, 0, 1
            };
            
            buf.setPosition(16*i);
            buf.put(mat4x4);
        }
        TBNMat.update();
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
        uboMatrix2D.put(Engine.getMatOrtho3DP().get());
        uboMatrix2D.put(Engine.getMatOrtho3DMV().get());
        uboMatrix2D.update();
        // unbind Uniform buffer?!
    }
    
    public static void setNormalMat(FloatBuffer mat) {
        uboMatrix3D.setPosition(5*16);
        uboMatrix3D.put(mat);//5
        uboMatrix3D.update();
    }
    
    public static void setMVP(FloatBuffer mat) {
        uboMatrix3D.setPosition(0*16);
        uboMatrix3D.put(mat);//0
        uboMatrix3D.update();
    }
    public static void updateSceneMatrices() {
        uboMatrix3D.reset();
        uboMatrix3D.put(Engine.getMatSceneMVP().get());//0
        uboMatrix3D.put(Engine.getMatSceneMV().get());//1
        uboMatrix3D.put(Engine.getMatSceneV().get());//2
        uboMatrix3D.put(Engine.getMatSceneVP().get());//3
        uboMatrix3D.put(Engine.getMatSceneP().get());//4
        uboMatrix3D.put(Engine.getMatSceneNormal().get());//5
        uboMatrix3D.put(Engine.getMatSceneMV().getInv());//6
        uboMatrix3D.put(Engine.getMatSceneP().getInv());//7
        uboMatrix3D.update();
    }
    public static void updatePxOffset() {
        uboSceneData.setPosition(12);
        uboSceneData.put(Engine.pxOffset.x);
        uboSceneData.put(Engine.pxOffset.y);
        uboSceneData.put(Engine.pxOffset.z);
        uboSceneData.put(1F);
        uboSceneData.update();
    }
    public static int getMaxBindingPoint() {
        return buffers.length-1;
    }

}
