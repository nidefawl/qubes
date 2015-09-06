package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.input.Selection;
import nidefawl.qubes.meshing.MeshThread;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.render.*;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;

public class Engine {
    public final static int NUM_PROJECTIONS    = 3 + 1;   // 3 sun view shadow pass + player view camera

    private static IntBuffer   viewport;
    private static FloatBuffer position;
    private static FloatBuffer mat;
    private static ByteBuffer  buffer;
    private static IntBuffer   intbuffer;

    private static BufferedMatrix projection;
    private static BufferedMatrix view;
    private static BufferedMatrix viewprojection;
    private static BufferedMatrix modelviewprojection;
    private static Matrix4f       modelviewprojectionInv;
    private static BufferedMatrix modelview;
    private static BufferedMatrix normalMatrix;
    private static BufferedMatrix orthoP;
    private static BufferedMatrix orthoMV;

    private static FloatBuffer depthRead;
    private static FloatBuffer fog;

    public static FrameBuffer fbScene;
    public static FrameBuffer fbDbg;
    public static float znear;
    public static float zfar;

    static TesselatorState fullscreenquad;

    public static Frustum        camFrustum;
    public static Vector3f       up;
    public static Vector4f       back;
    public static Vector3f       lightPosition;
    public static Vector3f       lightDirection;
    public static float          sunAngle     = 0F;
    public static Camera         camera;
    public static ShadowProjector shadowProj;
    public static WorldRenderer  worldRenderer;
    public static ShadowRenderer  shadowRenderer;
    public static FinalRenderer  outRenderer;
    public static RegionRenderer regionRenderer;
    public static MeshThread     regionRenderThread;
    public static Selection      selection;
    public static int            vaoId        = 0;
    public static int            vaoTerrainId = 0;
    private static float         aspectRatio;
    private static int           fieldOfView;

    public static boolean renderWireFrame = false;
    
    

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
        vaoId = GL30.glGenVertexArrays();
        vaoTerrainId = GL30.glGenVertexArrays();
        viewport = BufferUtils.createIntBuffer(16);
        mat = BufferUtils.createFloatBuffer(16);
        buffer = BufferUtils.createByteBuffer(1024*1024*32);
        intbuffer = buffer.asIntBuffer();
        position = BufferUtils.createFloatBuffer(3);
        projection = new BufferedMatrix();
        view = new BufferedMatrix();
        viewprojection = new BufferedMatrix();
        modelview = new BufferedMatrix();
        modelviewprojection = new BufferedMatrix();
        modelviewprojectionInv = new Matrix4f();
        normalMatrix = new BufferedMatrix();
        orthoP = new BufferedMatrix();
        orthoMV = new BufferedMatrix();
        depthRead = BufferUtils.createFloatBuffer(16);
        fog = BufferUtils.createFloatBuffer(16);
        camFrustum = new Frustum();
        up = new Vector3f();
        lightPosition = new Vector3f();
        lightDirection = new Vector3f();
        back = new Vector4f();
        camera = new Camera();
        shadowProj = new ShadowProjector();
        worldRenderer = new WorldRenderer();
        shadowRenderer = new ShadowRenderer();
        outRenderer = new FinalRenderer();
        regionRenderer = new RegionRenderer();
        regionRenderThread = new MeshThread(3);
        selection = new Selection();
    }
    
    public static void init() {
        glActiveTexture(GL_TEXTURE0);

        baseInit();
        UniformBuffer.reinit();
        Shaders.reinit();
        
        Shaders.init();
        regionRenderThread.init();
        regionRenderer.init();
        selection.init();
        reloadRenderer(true);
        GL30.glBindVertexArray(vaoId);
    }
    
    public static void resize(int displayWidth, int displayHeight) {
        fieldOfView = 70;
        aspectRatio = (float) displayWidth / (float) displayHeight;
        znear = 0.1F;
        zfar = 1024F;
        viewport.position(0);
        viewport.put(0);
        viewport.put(0);
        viewport.put(displayWidth);
        viewport.put(displayHeight);
        viewport.flip();
        

        Project.fovProjMat(fieldOfView, aspectRatio, znear, zfar, projection);
        projection.update();
        projection.update();
        shadowProj.setSplits(new float[] {znear, 20, 80, 160}, fieldOfView, aspectRatio);


        updateOrthoMatrix(displayWidth, displayHeight);
        

        if (fbDbg != null)
            fbDbg.cleanUp();
        fbDbg = new FrameBuffer(displayWidth, displayHeight);
        fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        fbDbg.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbDbg.setup();

        if (fullscreenquad == null) {
            fullscreenquad = new TesselatorState();
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
        if (worldRenderer != null) {
            worldRenderer.resize(displayWidth, displayHeight);
        }
        if (outRenderer != null) {
            outRenderer.resize(displayWidth, displayHeight);
        }
        if (shadowRenderer != null) {
            shadowRenderer.resize(displayWidth, displayHeight);
        }
    }
    public static void updateOrthoMatrix(float displayWidth, float displayHeight) {
        orthoMV.setIdentity();
        orthoMV.update();
        orthoMV.update();
        orthoP.setZero();
        Project.orthoMat(-0, displayWidth, 0, displayHeight, -100, 100, orthoP);
        Matrix4f.mul(orthoP, orthoMV, orthoP);
        orthoP.update();
        orthoP.update();
    }
    
    public static void drawFullscreenQuad() {
        fullscreenquad.drawQuads();
    }

    public static float readDepth(int x, int y) {
        depthRead.clear();
        GL11.glReadPixels(x, y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, depthRead);
        return depthRead.get(0);
    }


    public static BufferedMatrix getMatSceneP() {
        return projection;
    }
    public static BufferedMatrix getMatSceneV() {
        return view;
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


    public static void updateCamera() {
        up.set(0, 100, 0);
//        back.set(0, -10, 0);
        Matrix4f cam = camera.getViewMatrix();
        view.load(cam);
        view.update();
        
        Vector3f vec = camera.getPosition();
        modelview.setIdentity();
        modelview.translate(-vec.x, -vec.y, -vec.z);
        Matrix4f.mul(view, modelview, modelview);
        Matrix4f.mul(projection, modelview, modelviewprojection);
        Matrix4f.mul(projection, view, viewprojection);
        viewprojection.update();
        modelview.update();
        modelviewprojection.update();
        normalMatrix.setIdentity();
        normalMatrix.invert().transpose();
        normalMatrix.update();
        camFrustum.set(modelviewprojection);
        Matrix4f.invert(modelviewprojection, modelviewprojectionInv);
    }

    public static void setFog(Vector3f fogColor, float alpha) {
        fog.position(0);
        fog.put(fogColor.x);
        fog.put(fogColor.y);
        fog.put(fogColor.z);
        fog.put(1);
        fog.flip();
        GL.glFogv(GL_FOG_COLOR, fog);

    }

    public static FrameBuffer getSceneFB() {
        return fbScene;
    }



    public static void flushRenderTasks() {
        if (regionRenderThread != null) {
            regionRenderThread.flush();
            while (regionRenderThread.hasTasks()) {
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

    public static void readMat(int type, BufferedMatrix out) {
        mat.position(0).limit(16);
        GL.glGetFloatv(type, mat);
        mat.position(0).limit(16);
        out.load(mat);
        mat.position(0).limit(16);
    }

    public static void setLightPosition(Vector3f v) {
        lightPosition.set(v);
        lightDirection.set(v);
        lightDirection.normalise();
    }
    public static void updateShadowProjections(float fTime) {
//        Engine.worldRenderer.debugBBs.clear();
        if (Game.DO_TIMING) TimingHelper.startSec("calcShadow");
        shadowProj.calcSplits(modelview, lightDirection, shadowRenderer.getTextureSize() / 2.0f); //divide tex size by 2 as we use only a quarter per cascade
        
        if (Game.DO_TIMING) TimingHelper.endSec();
        
    }

    public static void stop() {
        regionRenderThread.stopThread();
    }


    public static final Vector3f vOrigin = new Vector3f();
    public static Vector3f vDir = null;
    public static final Vector3f vDirTmp = new Vector3f();
    public static final Vector3f vTarget = new Vector3f();
    public static final Vector3f t = new Vector3f();

    public static void updateMouseOverView(float winX, float winY) {
        viewport.position(0);
        position.position(0);
        if (!Project.gluUnProject(winX, winY, 1F, getMatSceneMV().get(), getMatSceneP().get(), viewport, position)) {
            System.err.println("unproject fail 1");
        }
        vTarget.x = position.get(0);
        vTarget.y = position.get(1);
        vTarget.z = position.get(2);
//        Vector4f zdepth = new Vector4f(0,0,-100, 0);
//        Matrix4f.transform(view, zdepth, zdepth);
//        System.out.println(vTarget); 
        viewport.position(0);
        position.position(0);
        //TODO: optimize
        if (!Project.gluUnProject(winX, winY, 0F, getMatSceneMV().get(), getMatSceneP().get(), viewport, position)) {
            System.err.println("unproject fail 2");
        }
        vOrigin.x = position.get(0);
        vOrigin.y = position.get(1);
        vOrigin.z = position.get(2);
        Vector3f.sub(vTarget, vOrigin, vDirTmp);
        vDir = vDirTmp.normaliseNull();
        if (vDir != null) {
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
        worldRenderer = new WorldRenderer();
        outRenderer = new FinalRenderer();
        shadowRenderer = new ShadowRenderer();
        worldRenderer.init();
        outRenderer.init();
        shadowRenderer.init();
        worldRenderer.resize(Game.displayWidth, Game.displayHeight);
        outRenderer.resize(Game.displayWidth, Game.displayHeight);
        shadowRenderer.resize(Game.displayWidth, Game.displayHeight);
        regionRenderer.reRender();
    }

    public static void setSceneFB(FrameBuffer fb) {
        fbScene = fb;
    }

    public static ByteBuffer getBuffer() {
        return buffer;
    }

    public static IntBuffer getIntBuffer() {
        return intbuffer;
    }

    public static FloatBuffer getFloatBuffer() {
        return fog;
    }

    public static IntBuffer glGenBuffers(int i) {
        ByteBuffer buf = Engine.getBuffer();
        buf.clear();
        IntBuffer intbuf = Engine.getIntBuffer();
        intbuf.clear();
        GL15.glGenBuffers(i, buf);
        return intbuf;
    }

    public static void deleteBuffers(int ...buffers) {
        ByteBuffer buf = Engine.getBuffer();
        IntBuffer intbuf = Engine.getIntBuffer();
        intbuf.clear();
        intbuf.put(buffers);
        buf.position(0).limit(buffers.length*4);
        GL15.glDeleteBuffers(buffers.length, buf);
    }

    public static String getDefinition(String define) {
//        if ("RENDER_WIREFRAME".equals(define)) {
//            return renderWireFrame ? "#define RENDER_WIREFRAME" : "";
//        }
        return "";
    }


    public static void toggleWireFrame() {
        renderWireFrame = !renderWireFrame;
        worldRenderer.initShaders();
    }


}
