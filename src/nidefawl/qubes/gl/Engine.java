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

import nidefawl.game.GL;
import nidefawl.game.GLGame;
import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.input.Selection;
import nidefawl.qubes.meshing.MeshThread;
import nidefawl.qubes.render.*;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;

public class Engine {
    public final static int   SHADOW_BUFFER_SIZE = 1024*4;
    public final static int   SHADOW_ORTHO_DIST  = 16*16;
    public final static int   NUM_PROJECTIONS  = 3 + 1; // 3 sun view shadow pass + player view camera
    
    private static IntBuffer      viewport;
    private static FloatBuffer    position;
    private static FloatBuffer    mat;
    private static ByteBuffer    buffer;
    private static IntBuffer    intbuffer;

    private static BufferedMatrix projection;
    private static BufferedMatrix view;
    private static BufferedMatrix modelviewprojection;
    private static BufferedMatrix modelview;
    private static BufferedMatrix shadowProjection;
    private static BufferedMatrix orthoP;
    private static BufferedMatrix orthoMV;
    private static BufferedMatrix sunModelView;
    private static BufferedMatrix shadowModelView;
    private static BufferedMatrix shadowModelViewProjection;

    private static Matrix4f[] shadowSplitProj;
    private static BufferedMatrix[] shadowSplitMVP;
    public static float[] shadowSplitDepth;
    public static Frustum[] shadowCamFrustum;

    private static FloatBuffer       depthRead;
    private static FloatBuffer       fog;

    public static FrameBuffer        fbScene;
    public static FrameBuffer        fbDbg;
    public static FrameBuffer        fbShadow;
    
    public static float              znear;
    public static float              zfar;
    
    static TesselatorState fullscreenquad;

    public static Frustum            camFrustum         = new Frustum();
    public static Vector4f           sunPosition        = new Vector4f();
    public static Vector4f           moonPosition       = new Vector4f();
    public static Vector3f           up                 = new Vector3f();
    public static Vector4f           back               = new Vector4f();
    public static float              sunAngle           = 0F;
    public static Camera             camera             = new Camera();
    public static WorldRenderer      worldRenderer      = new WorldRenderer();
    public static FinalRenderer      outRenderer        = new FinalRenderer();
    public static RegionRenderer     regionRenderer     = new RegionRenderer();
    public static MeshThread regionRenderThread = new MeshThread(3);
    public static RegionLoader       regionLoader       = new RegionLoader();
    public static Selection          selection          = new Selection();
    public static int                vaoId              = 0;
    public static int                vaoTerrainId       = 0;

    public static void generateLightMapTexture() {

    }

    public static boolean checkGLError(String s) {
        int i = GL11.glGetError();
        if (i != 0) {
            String s1 = GLGame.getGlErrorString(i);
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
        modelview = new BufferedMatrix();
        modelviewprojection = new BufferedMatrix();
        shadowProjection = new BufferedMatrix();
        shadowModelViewProjection = new BufferedMatrix();
        orthoP = new BufferedMatrix();
        orthoMV = new BufferedMatrix();
        sunModelView = new BufferedMatrix();
        shadowModelView = new BufferedMatrix();
        depthRead = BufferUtils.createFloatBuffer(16);
        fog = BufferUtils.createFloatBuffer(16);
    }
    public static void init() {
        glActiveTexture(GL_TEXTURE0);

        baseInit();
        UniformBuffer.reinit();
        Shaders.reinit();
        
        TextureManager.getInstance().init();
        AssetManager.getInstance().init();
        BlockTextureArray.getInstance().init();
        regionLoader.init();
        regionRenderThread.init();
        regionRenderer.init();
        selection.init();
        BlockTextureArray.getInstance().reload();
        reloadRenderer(true);
        GL30.glBindVertexArray(vaoId);
    }
    public static void resize(int displayWidth, int displayHeight) {
        float fieldOfView = 70;
        float aspectRatio = (float) displayWidth / (float) displayHeight;
        znear = 0.05F;
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
        float splits[] = new float[] {
                10, 50
        };
        shadowSplitProj = new Matrix4f[splits.length+1];
        shadowSplitMVP = new BufferedMatrix[splits.length+1];
        shadowSplitDepth = new float[splits.length+1];
        shadowCamFrustum = new Frustum[splits.length+1];
        int i;
        for (i = 0; i < shadowCamFrustum.length; i++) {
            shadowCamFrustum[i] = new Frustum();
        }
        for (i = 0; i < shadowSplitProj.length; i++) {
            shadowSplitProj[i] = new Matrix4f();
        }
        for (i = 0; i < shadowSplitMVP.length; i++) {
            shadowSplitMVP[i] = new BufferedMatrix();
        }
        float last = znear;
        for (i = 0; i < splits.length; i++) {
            Project.fovProjMat(fieldOfView, aspectRatio, last, splits[i], shadowSplitProj[i]);
            last = splits[i];
        }
        Project.fovProjMat(fieldOfView, aspectRatio, last, 200, shadowSplitProj[i]);
        
        Project.orthoMat(-SHADOW_ORTHO_DIST, SHADOW_ORTHO_DIST, SHADOW_ORTHO_DIST, -SHADOW_ORTHO_DIST, -512F, 512F*8, shadowProjection);
        shadowProjection.update();
        shadowProjection.update();

        updateOrthoMatrix(displayWidth, displayHeight);
        

        if (fbDbg != null)
            fbDbg.cleanUp();
        fbDbg = new FrameBuffer(displayWidth, displayHeight);
        fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        fbDbg.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbDbg.setup();
        if (fbShadow != null)
            fbShadow.cleanUp();
        fbShadow = new FrameBuffer(SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
        fbShadow.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA);
        fbShadow.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbShadow.setShadowBuffer();
        fbShadow.setup();

        if (fullscreenquad == null) {
            fullscreenquad = new TesselatorState();
        }
        Tess.instance.resetState();
        int tw = Main.displayWidth;
        int th = Main.displayHeight;
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
    public static BufferedMatrix getMatSceneMV() {
        return modelview;
    }
    public static BufferedMatrix getMatSceneMVP() {
        return modelviewprojection;
    }
    public static BufferedMatrix getMatOrthoP() {
        return orthoP;
    }
    public static BufferedMatrix getMatOrthoMV() {
        return orthoMV;
    }
    public static BufferedMatrix getMatShadowP() {
        return shadowProjection;
    }

    public static BufferedMatrix getMatShadowMV() {
        return shadowModelView;
    }

    public static BufferedMatrix getMatShadowMVP() {
        return shadowModelViewProjection;
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
        modelview.update();
        modelviewprojection.update();
        camFrustum.set(modelviewprojection);
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
                regionRenderThread.finishTasks();
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
                regionRenderThread.finishTasks();
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

    public static void updateSun(float fTime) {
        float sunPathRotation = -15.0F;
        float moonPathRotation = -50.0F;
        float ca = Main.instance.getWorld().getSunAngle(fTime);
        {
            float angle = ca * 360.0F;
            sunModelView.setIdentity();
            sunModelView.rotate(-90.0F * Camera.PI_OVER_180, 0f, 1f, 0f);
            sunModelView.rotate(sunPathRotation * Camera.PI_OVER_180, 0.0F, 0.0F, 1.0F);
            sunModelView.rotate(angle * Camera.PI_OVER_180, 1.0F, 0.0F, 0.0F);
            sunModelView.update();
            sunPosition.set(0, 100, 0);
            Matrix4f.transform(sunModelView, sunPosition, sunPosition);
            sunModelView.setIdentity();
            sunModelView.rotate(-90.0F * Camera.PI_OVER_180, 0f, 1f, 0f);
            sunModelView.rotate(angle * Camera.PI_OVER_180, 0f, 0f, 1f);
            sunModelView.rotate(moonPathRotation * Camera.PI_OVER_180, 1f, 0f, 0f);
            sunModelView.update();
            moonPosition.set(0, -100, 0);
            Matrix4f.transform(sunModelView, moonPosition, moonPosition);
        }
        {
            Vector4f lightPos;
            if (sunPosition.y <= 0) {
                lightPos = moonPosition;
            } else {
                lightPos = sunPosition;
            }
            shadowModelView.setIdentity();
            shadowModelView.translate(0, 0, 100);
            Project.lookAt(lightPos.x, lightPos.y, lightPos.z, 0,0,0, 0, 1, 0, shadowModelView);  

            float x = regionRenderer.renderChunkX<<(Chunk.SIZE_BITS+Region.REGION_SIZE_BITS);
            float z = regionRenderer.renderChunkZ<<(Chunk.SIZE_BITS+Region.REGION_SIZE_BITS);
            shadowModelView.translate(-x, -180, -z);
            
            Matrix4f.mul(shadowProjection, shadowModelView, shadowModelViewProjection);
            shadowModelView.update();
            shadowModelViewProjection.update();
        }
//        Engine.worldRenderer.debugBBs.clear();
        if (Main.DO_TIMING) TimingHelper.startSec("calcShadow");
        int i;
        for (i = 0; i < shadowSplitProj.length; i++) {
            calcShadow(i);
        }
        if (Main.DO_TIMING) TimingHelper.endSec();
        
    }
    public static void calcShadow(int split) {
        Vector3f[] frustumCorners = new Vector3f[] {
                new Vector3f(-1,  1, 0),
                new Vector3f( 1,  1, 0),
                new Vector3f( 1, -1, 0),
                new Vector3f(-1, -1, 0),
                new Vector3f(-1,  1, 1),
                new Vector3f( 1,  1, 1),
                new Vector3f( 1, -1, 1),
                new Vector3f(-1, -1, 1), 
        };
        Matrix4f newMat = new Matrix4f();
        Matrix4f newMatInv = new Matrix4f();
        Matrix4f.mul(projection, view, newMat);
        Matrix4f.mul(shadowSplitProj[split], modelview, newMat);
        Matrix4f.transpose(newMat, newMat);
        Matrix4f.invert(newMat, newMatInv);
        AABB bb = new AABB(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        for (int i = 0; i < 8; i++) {
            
            Matrix4f.transformTransposed(newMatInv, frustumCorners[i], frustumCorners[i]);
            if (bb.minX > frustumCorners[i].x)
                bb.minX = frustumCorners[i].x;
            if (bb.maxX < frustumCorners[i].x)
                bb.maxX = frustumCorners[i].x;
            if (bb.minY > frustumCorners[i].y)
                bb.minY = frustumCorners[i].y;
            if (bb.maxY < frustumCorners[i].y)
                bb.maxY = frustumCorners[i].y;
            if (bb.minZ > frustumCorners[i].z)
                bb.minZ = frustumCorners[i].z;
            if (bb.maxZ < frustumCorners[i].z)
                bb.maxZ = frustumCorners[i].z;
        }
        Vector3f frustumCenter = new Vector3f(0,0,0);
        for (int i = 0; i < 8; i++) {
            Vector3f.add(frustumCenter, frustumCorners[i], frustumCenter);
        }
        frustumCenter.scale(1.0f/8.0f);
//        Engine.worldRenderer.debugBBs.put(split, bb);
        float radius = Vector3f.sub(frustumCorners[0], frustumCorners[6], null).length()/2.0f;
        Vector3f lightPos = new Vector3f();
        if (sunPosition.y <= 0) {
            lightPos.set(moonPosition.x, moonPosition.y, moonPosition.z);
        } else {
            lightPos.set(sunPosition.x, sunPosition.y, sunPosition.z);
        }
        lightPos.scale(-1);
        lightPos.normalise();
        
        /** SNAP TO TEXTURE INCREMENTS, (Seems not to work with deferred) */
        /*
        */
        Matrix4f matLookAt = new Matrix4f();
        Matrix4f matLookAtInv = new Matrix4f();
        Matrix4f scale = new Matrix4f();
        float texelsPerUnit = SHADOW_BUFFER_SIZE / (radius*2.0f);
        scale.scale(new Vector3f(texelsPerUnit, texelsPerUnit, texelsPerUnit));
        Project.lookAt(0, 0, 0, -lightPos.x, -lightPos.y, -lightPos.z, 0, 1, 0, matLookAt);
        Matrix4f.mul(matLookAt, scale, matLookAt);
        Matrix4f.invert(matLookAt, matLookAtInv);
        Matrix4f.transform(matLookAt, frustumCenter, frustumCenter);
        frustumCenter.x = GameMath.floor(frustumCenter.x);
        frustumCenter.y = GameMath.floor(frustumCenter.y);
        Matrix4f.transform(matLookAtInv, frustumCenter, frustumCenter);
        
        
        
        Vector3f eye = new Vector3f();
        Vector3f.sub(frustumCenter, lightPos.scale(radius*2.0f), eye);
        
        shadowSplitMVP[split].setIdentity();
        Project.lookAt(eye.x, eye.y, eye.z, frustumCenter.x, frustumCenter.y, frustumCenter.z, 0, 1, 0, shadowSplitMVP[split]);
        Matrix4f matOrtho = new Matrix4f();
        Project.orthoMat(-radius, radius, radius, -radius, -512, 512*8, matOrtho);
        Matrix4f.mul(matOrtho, shadowSplitMVP[split], shadowSplitMVP[split]);
        shadowSplitMVP[split].update();
        shadowSplitMVP[split].update();
        shadowSplitDepth[split] = radius;
        shadowCamFrustum[split].set(shadowSplitMVP[split]);
//        System.out.println(radius);
        
    }

    public static void stop() {
        regionRenderThread.stopThread();
        regionLoader.stop();
    }


    public static final Vec3 vOrigin = new Vec3();
    public static Vec3 vDir = null;
    public static final Vec3 vDirTmp = new Vec3();
    public static final Vec3 vTarget = new Vec3();
    public static final Vec3 t = new Vec3();

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
        Vec3.sub(vTarget, vOrigin, vDirTmp);
        vDir = vDirTmp.normaliseNull();
        if (vDir != null) {
            t.set(vDir);
            t.scale(-0.1F);
            Vec3.add(vOrigin, t, vOrigin);
        }
//      System.out.println(vDir); 
    }

    public static void reloadRenderer(boolean useBasicShaders) {
        flushRenderTasks();
        if (worldRenderer != null) worldRenderer.release();
        if (outRenderer != null) outRenderer.release();
        worldRenderer = new WorldRenderer();
        outRenderer = new FinalRenderer();
        worldRenderer.init();
        outRenderer.init();
        worldRenderer.resize(Main.displayWidth, Main.displayHeight);
        outRenderer.resize(Main.displayWidth, Main.displayHeight);
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

    public static BufferedMatrix getMatShadowSplitMVP(int i) {
        return shadowSplitMVP[i];
    }

}
