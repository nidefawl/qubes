package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import org.lwjgl.opengl.*;

import com.google.common.collect.Maps;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.item.ItemRenderer;
import nidefawl.qubes.meshing.MeshThread;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.models.render.QModelRender;
import nidefawl.qubes.render.*;
import nidefawl.qubes.render.gui.SingleBlockDraw;
import nidefawl.qubes.render.gui.SingleBlockRenderer;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.SunLightModel;

public class Engine {
    public final static int NUM_PROJECTIONS    = 3 + 1;   // 3 sun view shadow pass + player view camera
    public final static int MAX_LIGHTS       = 1024;
    public final static int SIZE_OF_STRUCT_LIGHT  = 16*4;

    public final static BlockPos GLOBAL_OFFSET = new BlockPos();
    private final static BlockPos LAST_REPOS = new BlockPos();
    private static Map<String, Integer> bufferBindingPoints = Maps.newHashMap();
    private static int NEXT_BUFFER_BINDING_POINT = 0;

    public static boolean initRenderers = true;

    private static IntBuffer   viewportBuf;
    private static FloatBuffer position;
    private static IntBuffer   allocBuffer;

    private static BufferedMatrix projection;
    private static BufferedMatrix view;
    private static BufferedMatrix viewInvYZ;
    private static BufferedMatrix viewprojection;
    private static BufferedMatrix modelviewprojection;
    private static Matrix4f       modelviewprojectionInv;
    private static BufferedMatrix modelview;
    private static BufferedMatrix normalMatrix;
    private static BufferedMatrix orthoP;
    private static BufferedMatrix orthoMV;
    private static BufferedMatrix orthoMVP;
    private static BufferedMatrix ortho3DP;
    private static BufferedMatrix ortho3DMV;
    private static BufferedMatrix tempMatrix;
    private static BufferedMatrix tempMatrix2;
    private static BufferedMatrix identity;
    public static Vector3f       pxOffset = new Vector3f();
    public final static TransformStack pxStack = new TransformStack();
    public final static Matrix4f invertYZ = new Matrix4f().scale(1, -1, -1);
    

    public static FrameBuffer fbScene;
    public static FrameBuffer fbDbg;
    public static float znear;
    public static float zfar;

    static TesselatorState fullscreenquad;
    static TesselatorState quad;

    public static Frustum        camFrustum;
    public static Vector3f       up;
    public static Vector4f       back;
    public static Vector3f       lightPosition;
    public static Vector3f       lightDirection;
    public static float          sunAngle     = 0F;
    public static Camera         camera;
    public static Camera         camera2;
    public static ShadowProjector shadowProj;
    public static WorldRenderer  worldRenderer;
    public static ShadowRenderer  shadowRenderer;
    public static BlurRenderer   blurRenderer;
    public static FinalRenderer  outRenderer;
    public static RegionRenderer regionRenderer;
    public static LightCompute  lightCompute;
    public static MeshThread     regionRenderThread;
    public static QModelBatchedRender renderBatched;
    private static float         aspectRatio;
    private static int           fieldOfView;

    public static boolean renderWireFrame = false;
    private static boolean isDepthMask = true;

    public static boolean isScissors = false;

    public static boolean updateRenderOffset;
    public final static SingleBlockRenderer blockRender = new SingleBlockRenderer();
    public final static SingleBlockDraw blockDraw = new SingleBlockDraw();
    public final static ItemRenderer itemRender = new ItemRenderer();
    static GLVAO active = null;
    final static int[] viewport = new int[] {0,0,0,0};
    public final static ShaderBuffer        debugOutput         = new ShaderBuffer("DebugOutputBuffer").setSize(4096*4);
    
    public final static ShaderBuffer        ssbo_lights = new ShaderBuffer("PointLightStorageBuffer").setSize(MAX_LIGHTS * SIZE_OF_STRUCT_LIGHT);
    
    
    public final static ShaderBuffer        ssbo_model_modelmat         = new ShaderBuffer("QModel_mat_model")
            .setSize(BatchedRiggedModelRenderer.SIZE_OF_MAT4*BatchedRiggedModelRenderer.MAX_INSTANCES);
    
    public final static ShaderBuffer        ssbo_model_normalmat         = new ShaderBuffer("QModel_mat_normal")
            .setSize(BatchedRiggedModelRenderer.SIZE_OF_MAT4*BatchedRiggedModelRenderer.MAX_INSTANCES);
    
    public final static ShaderBuffer        ssbo_model_bonemat         = new ShaderBuffer("QModel_mat_bone")
            .setSize(BatchedRiggedModelRenderer.SIZE_OF_MAT4*BatchedRiggedModelRenderer.MAX_INSTANCES*32);
    
    public final static SunLightModel sunlightmodel = new SunLightModel();
    private final static ReallocIntBuffer[] buffers = new ReallocIntBuffer[4];
    
    
    public static void bindVAO(GLVAO vao) {
        if (active != vao) {
            active = vao;
            int id = active == null ? 0 : active.vaoId;
            GL30.glBindVertexArray(id);
        }
    }

    public static void bindIndexBuffer(int i) {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, i);
    }

    public static void bindBuffer(int i) {
        GL43.glBindVertexBuffer(0, i, 0, active.stride);
    }
    

    public static void generateLightMapTexture() {

    }

    public static boolean checkGLError(String s) {
        int i = GL11.glGetError();
        if (i != 0) {
            String s1 = GameBase.getGlErrorString(i);
            throw new GameError("Error - " + s + ": " + s1);
        }
        return false;
    }

    public static void baseInit() {
        GLVAO.initVAOs();
        viewportBuf = Memory.createIntBufferHeap(16);
        position = Memory.createFloatBufferHeap(3);
        allocBuffer = Memory.createIntBuffer(8);
        projection = new BufferedMatrix();
        view = new BufferedMatrix();
        viewInvYZ = new BufferedMatrix();
        viewprojection = new BufferedMatrix();
        modelview = new BufferedMatrix();
        modelviewprojection = new BufferedMatrix();
        modelviewprojectionInv = new Matrix4f();
        normalMatrix = new BufferedMatrix();
        orthoP = new BufferedMatrix();
        orthoMV = new BufferedMatrix();
        orthoMVP = new BufferedMatrix();
        ortho3DP = new BufferedMatrix();
        ortho3DMV = new BufferedMatrix();
        tempMatrix = new BufferedMatrix();
        tempMatrix2 = new BufferedMatrix();
        identity = new BufferedMatrix();
        identity.setIdentity();
        identity.update();
        identity.update();
        camFrustum = new Frustum();
        up = new Vector3f();
        lightPosition = new Vector3f();
        lightDirection = new Vector3f();
        back = new Vector4f();
        camera = new Camera();
        camera2 = new Camera();
        if (initRenderers) {
            shadowProj = new ShadowProjector();
            worldRenderer = new WorldRenderer();
            shadowRenderer = new ShadowRenderer();
            outRenderer = new FinalRenderer();
            blurRenderer = new BlurRenderer();
            regionRenderer = new RegionRenderer();
            regionRenderThread = new MeshThread(3);
        }
        renderBatched = new QModelBatchedRender();
        sunlightmodel.setDayLen(10000);
        sunlightmodel.setTime(7500);
        System.out.println("Engine.baseinit: "+GameContext.getTimeSinceStart());
    }
    
    public static void init() {
        glActiveTexture(GL_TEXTURE0);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glActiveTexture(GL_TEXTURE0)");
        baseInit();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("baseInit");
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("GL30.glBindVertexArray");
        UniformBuffer.init();
        Shaders.init();
        ShaderBuffer.init();
        if (initRenderers) {
            regionRenderThread.init();
            regionRenderer.init();
            reloadRenderer(true);
        }
        renderBatched.init();
        blockDraw.init();
        itemRender.init();
        pxStack.setCallBack(new StackChangeCallBack() {
            @Override
            public void onChange(Vector3f vec) {
                pxOffset.set(vec);
                UniformBuffer.updatePxOffset();
            }
        });
        System.out.println("Engine.init: "+GameContext.getTimeSinceStart());
    }
    
    public static void resize(int displayWidth, int displayHeight) {
        fieldOfView = 70;
        aspectRatio = (float) displayWidth / (float) displayHeight;
        znear = 0.1F;
        zfar = 1024F;
        viewportBuf.position(0);
        viewportBuf.put(0);
        viewportBuf.put(0);
        viewportBuf.put(displayWidth);
        viewportBuf.put(displayHeight);
        viewportBuf.flip();
        

        Project.fovProjMat(fieldOfView, aspectRatio, znear, zfar, projection);
        camFrustum.setCamInternals(fieldOfView, aspectRatio, znear, zfar);
        projection.update();
        projection.update();
        if (initRenderers) {
            shadowProj.setSplits(new float[] {znear, 14, 64, 420}, fieldOfView, aspectRatio);
        }


        updateOrthoMatrix(displayWidth, displayHeight);
        

        if (fbDbg != null)
            fbDbg.release();
        fbDbg = new FrameBuffer(displayWidth, displayHeight);
        fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        fbDbg.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbDbg.setup(null);

        if (fullscreenquad == null) {
            fullscreenquad = new TesselatorState(GL15.GL_STATIC_DRAW);
        }
        if (quad == null) {
            quad = new TesselatorState(GL15.GL_STATIC_DRAW);
        }
        Tess.instance.resetState();
        int tw = Game.displayWidth;
        int th = Game.displayHeight;
        float x = 0;
        float y = 0;
        Tess.instance.setColor(0xFFFFFF, 0xff);
        Tess.instance.add(x + tw, y, 0, 1, 1);
        Tess.instance.add(x, y, 0, 0, 1);
        Tess.instance.add(x, y + th, 0, 0, 0);
        Tess.instance.add(x + tw, y + th, 0, 1, 0);
        Tess.instance.draw(GL_QUADS, fullscreenquad);
        Tess.instance.resetState();
        Tess.instance.setColor(0xFFFFFF, 0xff);
        Tess.instance.add(1, 0, 0, 1, 0);
        Tess.instance.add(0, 0, 0, 0, 0);
        Tess.instance.add(0, 1, 0, 0, 1);
        Tess.instance.add(1, 1, 0, 1, 1);
        Tess.instance.draw(GL_QUADS, quad);
        if (blurRenderer != null) {
            blurRenderer.resize(displayWidth, displayHeight);
        }
        if (worldRenderer != null) {
            worldRenderer.resize(displayWidth, displayHeight);
        }
        if (outRenderer != null) {
            outRenderer.resize(displayWidth, displayHeight);
        }
        if (shadowRenderer != null) {
            shadowRenderer.resize(displayWidth, displayHeight);
        }
        if (lightCompute != null) {
            lightCompute.resize(displayWidth, displayHeight);
        }
        UniformBuffer.rebindShaders(); // For some stupid reason we have to rebind
        ShaderBuffer.rebindShaders();
    }
    public static void updateOrthoMatrix(float displayWidth, float displayHeight) {
        orthoMV.setIdentity();
        orthoMV.update();
        orthoP.setZero();
        Project.orthoMat(0, displayWidth, 0, displayHeight, -4200, 200, orthoP);
        orthoP.update();
        Matrix4f.mul(orthoP, orthoMV, orthoMVP);
        orthoMVP.update();
        
        ortho3DMV.setIdentity();
        ortho3DMV.update();
        
        ortho3DP.setZero();
        Project.orthoMat(0, displayWidth, 0, displayHeight, -400, 400, ortho3DP);
        ortho3DP.update();
    }

    public static void drawFullscreenQuad() {
        fullscreenquad.drawQuads();
    }

    public static void drawQuad() {
        quad.drawQuads();
    }


    public static BufferedMatrix getMatSceneP() {
        return projection;
    }
    public static BufferedMatrix getMatSceneV() {
        return view;
    }
    public static BufferedMatrix getMatSceneV_YZ_Inv() {
        return viewInvYZ;
    }
    public static BufferedMatrix getMatSceneVP() {
        return viewprojection;
    }

    public static BufferedMatrix getMatSceneMV() {
        return modelview;
    }
    public static BufferedMatrix getMatSceneMVP() {
        return modelviewprojection;
    }

    public static BufferedMatrix getMatSceneNormal() {
        return normalMatrix;
    }
    
    public static BufferedMatrix getMatOrthoP() {
        return orthoP;
    }
    public static BufferedMatrix getMatOrthoMV() {
        return orthoMV;
    }
    public static BufferedMatrix getMatOrthoMVP() {
        return orthoMVP;
    }
    public static BufferedMatrix getMatOrtho3DMV() {
        return ortho3DMV;
    }

    public static BufferedMatrix getMatOrtho3DP() {
        return ortho3DP;
    }

    public static BufferedMatrix getTempMatrix() {
        return tempMatrix;
    }

    public static BufferedMatrix getTempMatrix2() {
        return tempMatrix2;
    }
    public static BufferedMatrix getIdentityMatrix() {
        return identity;
    }

    public static void updateCamera() {
        updateCamera(camera);
    }

    public static void updateCamera(Camera camera) {
        up.set(0, 100, 0);
//        back.set(0, -10, 0);
        Matrix4f cam = camera.getViewMatrix();
        view.load(cam);
        view.update();
        viewInvYZ.load(view);
        viewInvYZ.mulMat(invertYZ);
        viewInvYZ.update();
        
        Vector3f vec = camera.getPosition();
        updateRenderOffset = updateGlobalRenderOffset(vec.x, vec.y, vec.z);
        modelview.setIdentity();
        modelview.translate(-vec.x, -vec.y, -vec.z);
        modelview.translate(GLOBAL_OFFSET.x, 0, GLOBAL_OFFSET.z);
//        System.out.println(GLOBAL_OFFSET);
        
        Matrix4f.mul(view, modelview, modelview);
        Matrix4f.mul(projection, modelview, modelviewprojection);
        Matrix4f.mul(projection, view, viewprojection);
        viewprojection.update();
        modelview.update();
        modelviewprojection.update();
        normalMatrix.setIdentity();
        normalMatrix.invert().transpose();
        normalMatrix.update();
        camFrustum.setPos(vec, view);
        camFrustum.set(modelviewprojection);
        Matrix4f.invert(modelviewprojection, modelviewprojectionInv);
        updateOrthoMatrix(Game.displayWidth, Game.displayHeight);//TODO: this only needs to be updated when resolution has changed
    }

    
    private static boolean updateGlobalRenderOffset(float x, float y, float z) {
        final int OFFSET_BITS = 5;
        final int OFFSET_REPOS_DIST = 512;
        int ix = GameMath.floor(x);
        int iz = GameMath.floor(z);
//        int dbgX = -(3<<OFFSET_BITS);
//        if (1==1){
//            boolean update = !GLOBAL_OFFSET.isEqualTo(dbgX,0,0);
//            GLOBAL_OFFSET.set(dbgX, 0, 0);
//            return update;
//        }
        final int distSq3D = GameMath.distSq3Di(ix, 0, iz, LAST_REPOS.x, 0, LAST_REPOS.z);
        if (distSq3D < 0/*INTEGER OVERFLOW*/ || distSq3D > OFFSET_REPOS_DIST*OFFSET_REPOS_DIST) {
            int offX = (ix>>OFFSET_BITS<<OFFSET_BITS);
            int offZ = (iz>>OFFSET_BITS<<OFFSET_BITS);
            boolean update = !GLOBAL_OFFSET.isEqualTo(offX, 0, offZ);
            GLOBAL_OFFSET.set(offX, 0, offZ);
//            GLOBAL_OFFSET.set(0, 0, 0);
            LAST_REPOS.set(ix, 0, iz);
            if (update)
                System.out.println("REPOS "+GLOBAL_OFFSET);
            return update;
        }
        return false;
    }

    public static FrameBuffer getSceneFB() {
        return fbScene;
    }



    public static void flushRenderTasks() {
        if (regionRenderThread != null) {
            regionRenderThread.flush();
            while (regionRenderThread.hasTasks() && regionRenderThread.isRunning()) {
                regionRenderThread.finishTask();
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static void restartRenderThreads() {
        if (regionRenderThread != null) {
            while (regionRenderThread.hasTasks()) {
                regionRenderThread.finishTask();
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            regionRenderThread.stopThread();
            regionRenderThread = null;
        }
        regionRenderThread = new MeshThread(3);
        regionRenderThread.init();
    }


    public static void setLightPosition(Vector3f v) {
        lightPosition.set(v);
        lightDirection.set(v);
        lightDirection.normalise();
    }
    public static void updateShadowProjections(float fTime) {
//        Engine.worldRenderer.debugBBs.clear();
        shadowProj.calcSplits(modelview, lightDirection, shadowRenderer.getTextureSize() / 2.0f); //divide tex size by 2 as we use only a quarter per cascade

        
    }

    public static void stop() {
        if (regionRenderThread != null) {
            regionRenderThread.stopThread();
            regionRenderThread.cleanup();
        }
        UniformBuffer.destroy();
    }


    public static final Vector3f vOrigin = new Vector3f();
    public static Vector3f vDir = null;

    public static final Vector3f vDirTmp = new Vector3f();
    public static final Vector3f vTarget = new Vector3f();
    public static final Vector3f t = new Vector3f();

    public static void updateMouseOverView(float winX, float winY, boolean cameraOffset) {
        viewportBuf.position(0);
        position.position(0);
        if (!Project.gluUnProject(winX, winY, 1F, getMatSceneMV().get(), getMatSceneP().get(), viewportBuf, position)) {
            System.err.println("unproject fail 1");
        }
        vTarget.x = position.get(0)+Engine.GLOBAL_OFFSET.x;
        vTarget.y = position.get(1)+Engine.GLOBAL_OFFSET.y;
        vTarget.z = position.get(2)+Engine.GLOBAL_OFFSET.z;
//        Vector4f zdepth = new Vector4f(0,0,-100, 0);
//        Matrix4f.transform(view, zdepth, zdepth);
//        System.out.println(vTarget); 
        viewportBuf.position(0);
        position.position(0);
        //TODO: optimize
        if (!Project.gluUnProject(winX, winY, 0F, getMatSceneMV().get(), getMatSceneP().get(), viewportBuf, position)) {
            System.err.println("unproject fail 2");
        }
        vOrigin.x = position.get(0)+Engine.GLOBAL_OFFSET.x;
        vOrigin.y = position.get(1)+Engine.GLOBAL_OFFSET.y;
        vOrigin.z = position.get(2)+Engine.GLOBAL_OFFSET.z;
        Vector3f.sub(vTarget, vOrigin, vDirTmp);
        vDir = vDirTmp.normaliseNull();
        if (vDir != null) {
            if (cameraOffset) {

                vOrigin.subtract(camera.getCameraOffset());
                vDir.subtract(camera.getCameraOffset());
            }
            t.set(vDir);
            t.scale(-0.1F);
            Vector3f.add(vOrigin, t, vOrigin);
        }
//      System.out.println(vDir); 
    }

    public static void reloadRenderer(boolean useBasicShaders) {
        flushRenderTasks();
        if (worldRenderer != null) worldRenderer.release();
        if (outRenderer != null) outRenderer.release();
        if (shadowRenderer != null) shadowRenderer.release();
        if (blurRenderer != null) blurRenderer.release();
        worldRenderer = new WorldRenderer();
        outRenderer = new FinalRenderer();
        shadowRenderer = new ShadowRenderer();
        lightCompute = new LightCompute();
        blurRenderer = new BlurRenderer();
        blurRenderer.init();
        worldRenderer.init();
        outRenderer.init();
        shadowRenderer.init();
        lightCompute.init();
        blurRenderer.resize(Game.displayWidth, Game.displayHeight);
        worldRenderer.resize(Game.displayWidth, Game.displayHeight);
        outRenderer.resize(Game.displayWidth, Game.displayHeight);
        shadowRenderer.resize(Game.displayWidth, Game.displayHeight);
        lightCompute.resize(Game.displayWidth, Game.displayHeight);
        regionRenderer.reRender();
    }

    public static void setSceneFB(FrameBuffer fb) {
        fbScene = fb;
    }

    public static IntBuffer glGenBuffers(int i) {
        if (i < 1) {
            throw new IllegalArgumentException("i < 1");
        }
        allocBuffer.clear();
        allocBuffer.position(0);
        allocBuffer.limit(i);
        GL15.glGenBuffers(allocBuffer);
        allocBuffer.position(0);
        allocBuffer.limit(i);
        return allocBuffer;
    }

    public static void deleteBuffers(int ...buffers) {
        allocBuffer.clear();
        allocBuffer.put(buffers);
        allocBuffer.flip();
        GL15.glDeleteBuffers(allocBuffer);
    }


    public static void toggleWireFrame() {
        renderWireFrame = !renderWireFrame;
        worldRenderer.initShaders();
    }



    public static void toggleDrawMode() {
        worldRenderer.initShaders();
    }

    /**
     * @param mat2
     */
    public static void setOrthoMV(BufferedMatrix mv) {
        Matrix4f.mul(orthoP, mv, orthoMVP);
        orthoMVP.update();
        UniformBuffer.updateOrtho();
    }

    /**
     * 
     */
    public static void restoreOrtho() {
        Matrix4f.mul(orthoP, orthoMV, orthoMVP);
        orthoMVP.update();
        UniformBuffer.updateOrtho();
    }

    public static Vector3f getPxOffset() {
        return pxOffset;
    }

    public static int getBindingPoint(String name) {
        Integer i = bufferBindingPoints.get(name);;
        if (i == null) {
            i = NEXT_BUFFER_BINDING_POINT;
            bufferBindingPoints.put(name, NEXT_BUFFER_BINDING_POINT++);
        }
        return i;
    }

    public static void enableDepthMask(boolean flag) {
        isDepthMask = flag;
        GL11.glDepthMask(flag);
    }

    public static void enableScissors() {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        isScissors = true;
    }
    public static void disableScissors() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        isScissors = false;
    }
    public static void setOverrideScissorTest(boolean b) {
        if (!b) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);    
        } else {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
    public static void restoreScissorTest() {
        if (!isScissors) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);    
        } else {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
    public static void restoreDepthMask() {
        GL11.glDepthMask(isDepthMask);
    }
    //May need stack sometime
    public static void setOverrideDepthMask(boolean b) {
        GL11.glDepthMask(b);
    }

    public static void setViewport(int x, int y, int w, int h) {
        if (viewport[0] != x || viewport[1] != y || viewport[2] != w || viewport[3] != h) {
            viewport[0] = x;
            viewport[1] = y;
            viewport[2] = w;
            viewport[3] = h;
            GL11.glViewport(x, y, w, h);
        }
    }

    public static void setDefaultViewport() {
        setViewport(0, 0, Game.displayWidth, Game.displayHeight);
    }

    public static SunLightModel getSunLightModel() {
        return sunlightmodel;
    }

    public static ReallocIntBuffer getIntBuffer() {
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] == null) {
                buffers[i] = new ReallocIntBuffer();
                buffers[i].setInUse(true);
                return buffers[i];
            }
            if (!buffers[i].isInUse()) {
                buffers[i].setInUse(true);
                return buffers[i];
            }
        }
        return null;
    }

}
