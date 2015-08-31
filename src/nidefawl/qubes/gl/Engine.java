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
    private static Matrix4f modelviewprojectionInv;
    private static BufferedMatrix modelview;
    private static BufferedMatrix normalMatrix;
    private static BufferedMatrix shadowProjection;
    private static BufferedMatrix orthoP;
    private static BufferedMatrix orthoMV;
    private static Matrix4f sunModelView;
    private static Matrix4f moonModelView;
    private static Matrix4f lightViewMat;
    
    static float[] splits = null;
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

    public static Frustum        camFrustum;
    public static Vector4f       sunPosition;
    public static Vector4f       moonPosition;
    public static Vector3f       up;
    public static Vector4f       back;
    public static float          sunAngle     = 0F;
    public static Camera         camera;
    public static WorldRenderer  worldRenderer;
    public static FinalRenderer  outRenderer;
    public static RegionRenderer regionRenderer;
    public static MeshThread     regionRenderThread;
    public static RegionLoader   regionLoader;
    public static Selection      selection;
    public static int            vaoId        = 0;
    public static int            vaoTerrainId = 0;
    private static float         aspectRatio;
    private static int           fieldOfView;

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
        modelviewprojectionInv = new Matrix4f();
        normalMatrix = new BufferedMatrix();
        shadowProjection = new BufferedMatrix();
        orthoP = new BufferedMatrix();
        orthoMV = new BufferedMatrix();
        sunModelView = new Matrix4f();
        moonModelView = new Matrix4f();
        lightViewMat = new BufferedMatrix();
        depthRead = BufferUtils.createFloatBuffer(16);
        fog = BufferUtils.createFloatBuffer(16);
        camFrustum = new Frustum();
        sunPosition = new Vector4f();
        moonPosition = new Vector4f();
        up = new Vector3f();
        back = new Vector4f();
        camera = new Camera();
        worldRenderer = new WorldRenderer();
        outRenderer = new FinalRenderer();
        regionRenderer = new RegionRenderer();
        regionRenderThread = new MeshThread(3);
        regionLoader = new RegionLoader();
        selection = new Selection();
    }
    
    public static void init() {
        glActiveTexture(GL_TEXTURE0);

        baseInit();
        UniformBuffer.reinit();
        Shaders.reinit();
        
        Shaders.init();
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
        fieldOfView = 70;
        aspectRatio = (float) displayWidth / (float) displayHeight;
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
        splits = new float[] {
                10, 50
        };
        shadowSplitMVP = new BufferedMatrix[splits.length+1];
        shadowSplitDepth = new float[splits.length+1];
        shadowCamFrustum = new Frustum[splits.length+1];
        int i;
        for (i = 0; i < shadowCamFrustum.length; i++) {
            shadowCamFrustum[i] = new Frustum();
        }
        for (i = 0; i < shadowSplitMVP.length; i++) {
            shadowSplitMVP[i] = new BufferedMatrix();
        }
        
        Project.orthoMat(-SHADOW_ORTHO_DIST, SHADOW_ORTHO_DIST, SHADOW_ORTHO_DIST, -SHADOW_ORTHO_DIST, -512F, 512F*8, shadowProjection);
        shadowProjection.update();
        shadowProjection.update();

        updateOrthoMatrix(displayWidth, displayHeight);
        updateFrustumSegmentFarPlanes();
        

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

    public static BufferedMatrix getMatSceneNormal() {
        return normalMatrix;
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
            sunPosition.set(0, 100, 0);
            Matrix4f.transform(sunModelView, sunPosition, sunPosition);
            moonModelView.setIdentity();
            moonModelView.rotate(-90.0F * Camera.PI_OVER_180, 0f, 1f, 0f);
            moonModelView.rotate(angle * Camera.PI_OVER_180, 0f, 0f, 1f);
            moonModelView.rotate(moonPathRotation * Camera.PI_OVER_180, 1f, 0f, 0f);
            moonPosition.set(0, -100, 0);
            Matrix4f.transform(moonModelView, moonPosition, moonPosition);
        }
        {
            Vector4f lightPos;
            if (sunPosition.y <= 0) {
                lightPos = moonPosition;
            } else {
                lightPos = sunPosition;
            }
            lightViewMat.setIdentity();
            Project.lookAt(lightPos.x, lightPos.y, lightPos.z, 0,0,0, 0, 1, 0, lightViewMat);  

        }
//        Engine.worldRenderer.debugBBs.clear();
        if (Main.DO_TIMING) TimingHelper.startSec("calcShadow");
        Vector3f lightPos = new Vector3f();
        Matrix4f lightRotationInv = new Matrix4f();
        float scaleZ = zfar;
        for (int i = 0; i < splits.length+1; i++) {
            float shadowZNear = i==0?znear:splits[i-1];
            float shadowZFar = i >= splits.length ? Math.min(zfar, 200) : splits[i];
            if (sunPosition.y <= 0) {
                lightPos.set(moonPosition.x, moonPosition.y, moonPosition.z);
                Matrix4f.invert(moonModelView, lightRotationInv);
            } else {
                lightPos.set(sunPosition.x, sunPosition.y, sunPosition.z);
                Matrix4f.invert(sunModelView, lightRotationInv);
            }
            lightPos.scale(-1);
            lightPos.normalise();
            calcShadow(i, lightPos, lightRotationInv, shadowZNear/scaleZ, shadowZFar/scaleZ);
        }
        updateLightProjAndViewports();
        if (Main.DO_TIMING) TimingHelper.endSec();
        
    }
    /*
     * 
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
        shadowSplitDepth[split] = radius/1.41F; // I'm 90% sure this is wrong, the shader requires 1 x radius and should reduce the input depth for each cascade as they are not centered on the same point
        shadowCamFrustum[split].set(shadowSplitMVP[split]);
//        System.out.println(radius);
        
    }
     */
    static float m_farPlanes[] = new float[4];
    static float m_normalizedFarPlanes[] = new float[4];
    static int m_frustumSegmentCount = 3;
    static void updateFrustumSegmentFarPlanes()
    {

        float m_nearPlane = znear;
        float m_farPlane = zfar;
        float m_frustumSplitCorrection = 0.8f;
        for (int i = 1; i <= m_frustumSegmentCount; ++i)
        {
            float distFactor = i / (float) m_frustumSegmentCount;
            float stdTerm = (float) (m_nearPlane * Math.pow(m_farPlane / m_nearPlane, distFactor));
            float corrTerm = m_nearPlane + distFactor * (m_farPlane - m_nearPlane);
            float viewDepth = m_frustumSplitCorrection * stdTerm + (1.0f - m_frustumSplitCorrection) * corrTerm;
            m_farPlanes[i - 1] = viewDepth;
            Vector4f projectedDepth = new Vector4f(0.0f, 0.0f, - viewDepth, 1.0f);
            Matrix4f.transform(projection, projectedDepth, projectedDepth);
//             nv::vec4f projectedDepth = m_projMatrix * nv::vec4f(0.0f, 0.0f, - viewDepth, 1.0f);
            // Normalized to [0, 1] depth range.
            m_normalizedFarPlanes[i - 1] = (projectedDepth.z / projectedDepth.w) * 0.5f + 0.5f;
        }
    }
    
    static AABB frustumBoundingBoxLightViewSpace(float nearPlane, float farPlane)
    {
//        nv::vec4f frustumMin(std::numeric_limits<float>::max());
//        nv::vec4f frustumMax(std::numeric_limits<float>::lowest());
    
         float nearHeight = 2.0f * (float)Math.tan(fieldOfView * 0.5f) * nearPlane;
         float nearWidth = nearHeight * aspectRatio;
         float farHeight = 2.0f * (float)Math.tan(fieldOfView * 0.5f) * farPlane;
         float farWidth = farHeight * aspectRatio;
         Matrix4f camTrans = new Matrix4f();
         Vector3f vec = camera.getPosition();
         camTrans.translate(vec.x, vec.y, vec.z);
         Matrix4f camRotInv = new Matrix4f();
         Matrix4f.invert(view, camRotInv);
//         nv::vec4f cameraPos = nv::inverse(m_camera.getTranslationMat()) * nv::vec4f(0.0f, 0.0f, 0.0f, 1.0f);
//         nv::matrix4f invRot = nv::inverse(m_camera.getRotationMat());
//         nv::vec4f viewDir = invRot * nv::vec4f(0.0f, 0.0f, -1.0f, 0.0f);
//         nv::vec4f upDir = invRot * nv::vec4f(0.0f, 1.0f, 0.0f, 0.0f);
//         nv::vec4f rightDir = invRot * nv::vec4f(1.0f, 0.0f, 0.0f, 0.0f);
//         nv::vec4f nc = cameraPos + viewDir * nearPlane; // near center
//         nv::vec4f fc = cameraPos + viewDir * farPlane; // far center
         Vector4f cameraPos = new Vector4f(0, 0, 0, 1);
         Vector4f viewDir = new Vector4f(0, 0, -1, 0);
         Vector4f upDir = new Vector4f(0, 1, 0, 0);
         Vector4f rightDir = new Vector4f(1, 0, 0, 0);
         Matrix4f.transform(camTrans, cameraPos, cameraPos);
         Matrix4f.transform(camRotInv, viewDir, viewDir);
         Matrix4f.transform(camRotInv, upDir, upDir);
         Matrix4f.transform(camRotInv, rightDir, rightDir);
         Vector4f nc =  Vector4f.add(cameraPos, new Vector4f(viewDir).scale(nearPlane), null);
         Vector4f fc =  Vector4f.add(cameraPos, new Vector4f(viewDir).scale(farPlane), null);
         

        // Vertices in a world space.
//        Vector4f vertices[] = {
//            nc - upDir * nearHeight * 0.5f - rightDir * nearWidth * 0.5f, // nbl (near, bottom, left)
//            nc - upDir * nearHeight * 0.5f + rightDir * nearWidth * 0.5f, // nbr
//            nc + upDir * nearHeight * 0.5f + rightDir * nearWidth * 0.5f, // ntr
//            nc + upDir * nearHeight * 0.5f - rightDir * nearWidth * 0.5f, // ntl
//            fc - upDir * farHeight  * 0.5f - rightDir * farWidth * 0.5f, // fbl (far, bottom, left)
//            fc - upDir * farHeight  * 0.5f + rightDir * farWidth * 0.5f, // fbr
//            fc + upDir * farHeight  * 0.5f + rightDir * farWidth * 0.5f, // ftr
//            fc + upDir * farHeight  * 0.5f - rightDir * farWidth * 0.5f, // ftl
//        };.
       Vector4f vertices[] = new Vector4f[8];
       Vector4f nearUp = new Vector4f(upDir).scale(nearHeight*0.5f);
       Vector4f nearRight = new Vector4f(rightDir).scale(nearWidth*0.5f);
       Vector4f farUp = new Vector4f(upDir).scale(farHeight*0.5f);
       Vector4f farRight = new Vector4f(rightDir).scale(farWidth*0.5f);
       vertices[0] = new Vector4f(nc);
       vertices[1] = new Vector4f(nc);
       vertices[2] = new Vector4f(nc);
       vertices[3] = new Vector4f(nc);
       vertices[4] = new Vector4f(fc);
       vertices[5] = new Vector4f(fc);
       vertices[6] = new Vector4f(fc);
       vertices[7] = new Vector4f(fc);
       Vector4f.sub(vertices[0], nearUp, vertices[0]); Vector4f.sub(vertices[0], nearRight, vertices[0]);
       Vector4f.sub(vertices[1], nearUp, vertices[1]); Vector4f.add(vertices[1], nearRight, vertices[1]);
       Vector4f.add(vertices[2], nearUp, vertices[2]); Vector4f.add(vertices[2], nearRight, vertices[2]);
       Vector4f.add(vertices[3], nearUp, vertices[3]); Vector4f.sub(vertices[3], nearRight, vertices[3]);
       Vector4f.sub(vertices[4], farUp, vertices[4]); Vector4f.sub(vertices[4], farRight, vertices[4]);
       Vector4f.sub(vertices[5], farUp, vertices[5]); Vector4f.add(vertices[5], farRight, vertices[5]);
       Vector4f.add(vertices[6], farUp, vertices[6]); Vector4f.add(vertices[6], farRight, vertices[6]);
       Vector4f.add(vertices[7], farUp, vertices[7]); Vector4f.sub(vertices[7], farRight, vertices[7]);
//    
       AABB bb = new AABB(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

        for (int i = 0; i < 8; ++i) {
//            if (nearPlane < 0.001)
//            System.out.println(""+i+" "+vertices[i]);
            Matrix4f.transform(lightViewMat, vertices[i], vertices[i]);
//            if (nearPlane < 0.001)
//            System.out.println(""+i+" "+vertices[i]);
            // Update bounding box.
            if (bb.minX > vertices[i].x)
                bb.minX = vertices[i].x;
             if (bb.maxX < vertices[i].x)
                bb.maxX = vertices[i].x;
             if (bb.minY > vertices[i].y)
                bb.minY = vertices[i].y;
             if (bb.maxY < vertices[i].y)
                bb.maxY = vertices[i].y;
             if (bb.minZ > vertices[i].z)
                bb.minZ = vertices[i].z;
             if (bb.maxZ < vertices[i].z)
                bb.maxZ = vertices[i].z;
//            frustumMin = nv::min(frustumMin, vertices[vertId]);
//            frustumMax = nv::max(frustumMax, vertices[vertId]);
        }
        return bb;
    }
    static void updateLightProjAndViewports()
    {

        AABB bbT = frustumBoundingBoxLightViewSpace(znear, zfar);
        // Find a bounding box of segment in light view space.
        float nearSegmentPlane = 0.0f;
        worldRenderer.debugBBs.put(0, bbT);
        for (int i = 0; i < m_frustumSegmentCount; ++i) {
//            nv::vec4f segmentMin(std::numeric_limits<float>::max());
//            nv::vec4f segmentMax(std::numeric_limits<float>::lowest());
            AABB bb = frustumBoundingBoxLightViewSpace(nearSegmentPlane, m_farPlanes[i]);
            worldRenderer.debugBBs.put(i+1, bb);

//            // Update viewports.
//            nv::vec2f frustumSize(frustumMax.x - frustumMin.x, frustumMax.y - frustumMin.y);
            float segmentSizeX = (float)bb.getWidth();
            float segmentSizeY = (float)bb.getHeight();
             float segmentSize = segmentSizeX < segmentSizeY ? segmentSizeY : segmentSizeX;
//             nv::vec2f offsetBottomLeft(segmentMin.x - frustumMin.x, segmentMin.y - frustumMin.y);
//             nv::vec2f offsetSegmentSizeRatio(offsetBottomLeft.x / segmentSize, offsetBottomLeft.y / segmentSize);
//             nv::vec2f frustumSegmentSizeRatio(frustumSize.x / segmentSize, frustumSize.y / segmentSize);
//
//            nv::vec2f pixelOffsetTopLeft(offsetSegmentSizeRatio * LIGHT_TEXTURE_SIZE);
//            nv::vec2f pixelFrustumSize(frustumSegmentSizeRatio * LIGHT_TEXTURE_SIZE);
//
//            // Scale factor that helps if frustum size is supposed to be bigger
//            // than maximum viewport size.
//            nv::vec2f scaleFactor(
//                m_viewportDims[0] < pixelFrustumSize.x ? m_viewportDims[0] / pixelFrustumSize.x : 1.0f,
//                m_viewportDims[1] < pixelFrustumSize.y ? m_viewportDims[1] / pixelFrustumSize.y : 1.0f);
//
//            pixelOffsetTopLeft *= scaleFactor;
//            pixelFrustumSize *= scaleFactor;
//
//            m_lightViewports[i] = nv::vec4f(-pixelOffsetTopLeft.x, -pixelOffsetTopLeft.y, pixelFrustumSize.x, pixelFrustumSize.y);
//            glViewportIndexedfv(i, m_lightViewports[i]._array);
//
//            // Update light view-projection matrices per segment.
//            nv::matrix4f lightProj;
//            nv::ortho3D(lightProj, segmentMin.x, segmentMin.x + segmentSize, segmentMin.y, segmentMin.y + segmentSize, 0.0f, frustumMin.z);
//            nv::matrix4f lightScale;
//            lightScale.set_scale(nv::vec3f(0.5f * scaleFactor.x, 0.5f * scaleFactor.y, 0.5f));
//            nv::matrix4f lightBias;
//            lightBias.set_translate(nv::vec3f(0.5f * scaleFactor.x, 0.5f * scaleFactor.y, 0.5f));
//            m_lightSegmentVPSBMatrices[i] = lightBias * lightScale * lightProj * m_lightViewMatrix;

            shadowSplitMVP[i].setIdentity();
            Matrix4f matOrtho = new Matrix4f();
            Project.orthoMat((float)bb.minX, (float)bb.minX + segmentSize, (float)bb.minY + segmentSize, (float)bb.minY, 0, (float)bbT.minZ, matOrtho);
            System.out.println(i+" - "+bb.minX+"/"+(bb.minX + segmentSize));
            Matrix4f.mul(matOrtho, modelview, shadowSplitMVP[i]);
//            shadowSplitMVP[i].load(modelviewprojection);
            shadowSplitMVP[i].update();
            shadowSplitMVP[i].update();
            shadowSplitDepth[i] = segmentSize/1.41F; // I'm 90% sure this is wrong, the shader requires 1 x radius and should reduce the input depth for each cascade as they are not centered on the same point
            shadowCamFrustum[i].set(shadowSplitMVP[i]);
            nearSegmentPlane = m_normalizedFarPlanes[i];
        }
        
    }
    public static void calcShadow(int split, Vector3f lightPos, Matrix4f lightRotationInv, float shadowZNear, float shadowZFar) {
        Vector3f eye = new Vector3f();
        Vector3f nearCenter = new Vector3f();
        Vector3f farCorner1 = new Vector3f();
        Vector3f farCorner2 = new Vector3f();
        Vector3f farcenter = new Vector3f();
        Vector3f center  = new Vector3f();
        Matrix4f.transform(modelviewprojectionInv, new Vector3f(0.0f, 0.0f, shadowZNear), nearCenter);
        Matrix4f.transform(modelviewprojectionInv, new Vector3f(1.0f, 1.0f, shadowZFar), farCorner1);
        Matrix4f.transform(modelviewprojectionInv, new Vector3f(-1.0f, -1.0f, shadowZFar), farCorner2);
        Vector3f.add(farCorner1, farCorner2, farcenter);
        farcenter.scale(0.5f);
        Vector3f.add(nearCenter, farcenter, center);
        center.scale(0.5f);
        float radius = Vector3f.sub(farCorner1, center, null).length();
        float pixelsize = (radius*2.0F)/(float)SHADOW_BUFFER_SIZE;
        Vector3f.sub(center, lightPos.scale(512), eye);
        AABB bb = new AABB();
        float bbsize = radius;
        bb.minX = center.x-bbsize;
        bb.maxX = center.x+bbsize;
        bb.minY = center.y-bbsize;
        bb.maxY = center.y+bbsize;
        bb.minZ = center.z-bbsize;
        bb.maxZ = center.z+bbsize;
//        worldRenderer.debugBBs.put(split, bb);
        
        if (split == 0) {

//            System.out.println(shadowZNear/(zfar-znear));
//            System.out.println(center);   
        }
        Matrix4f.transform(lightRotationInv, eye, eye);
//        eye.x /= pixelsize;
//        eye.y /= pixelsize;
//        eye.x = GameMath.floor(eye.x);
//        eye.y = GameMath.floor(eye.y);
//        eye.z = GameMath.floor(eye.z);
//        eye.x *= pixelsize;
//        eye.y *= pixelsize;
//        Matrix4f.transform(lightRotationInv, eye, eye);

        Matrix4f modelviewInv = new Matrix4f();
        Matrix4f projection = new Matrix4f();
        projection.setIdentity();
//        System.out.println(split+"/"+radius);
        Project.orthoMat(-radius, radius, radius, -radius, 0, 512+radius*2, projection);
        shadowSplitMVP[split].setIdentity();
        Project.lookAt(eye.x, eye.y, eye.z, eye.x-lightPos.x, eye.y-lightPos.y, eye.z-lightPos.z, 0, 1, 0, shadowSplitMVP[split]);
        Matrix4f.invert(modelview, modelviewInv);
        Matrix4f.mul(projection, shadowSplitMVP[split], shadowSplitMVP[split]);
        shadowSplitMVP[split].update();
        shadowSplitMVP[split].update();
        shadowSplitDepth[split] = radius/1.21F; // I'm 90% sure this is wrong, the shader requires 1 x radius and should reduce the input depth for each cascade as they are not centered on the same point
        shadowCamFrustum[split].set(shadowSplitMVP[split]);
//        System.out.println(radius);

    }

    public static void stop() {
        regionRenderThread.stopThread();
        regionLoader.stop();
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

    public static BufferedMatrix getMatShadowSplitMVP(int i) {
        return shadowSplitMVP[i];
    }

}
