package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
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
import nidefawl.qubes.chunk.RegionLoader;
import nidefawl.qubes.render.*;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Project;
import nidefawl.qubes.vec.*;

public class Engine {
    public final static int   SHADOW_BUFFER_SIZE = 1024*4;
    public final static int   SHADOW_ORTHO_DIST  = 16*16;
    public final static float shadowZnear        = 8F;
    public final static float shadowZfar         = 512.0F;
    
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

    private static FloatBuffer       depthRead;
    private static FloatBuffer       fog;

    public static FrameBuffer        fbScene;
    public static FrameBuffer        fbDbg;
    public static FrameBuffer        fbShadow;
    
    public static float              znear;
    public static float              zfar;
    

    public static Vector4f           sunPosition        = new Vector4f();
    public static Vector4f           moonPosition       = new Vector4f();
    public static Vector3f           up                 = new Vector3f();
    public static Vector4f           back                 = new Vector4f();
    public static float              sunAngle        = 0F;
    public static Camera             camera             = new Camera();
    public static WorldRenderer      worldRenderer      = new WorldRenderer();
    public static FinalRendererBase  outRenderer        = new FinalRenderer();
    public static RegionRenderer     regionRenderer     = new RegionRenderer();
    public static RegionRenderThread regionRenderThread = new RegionRenderThread(3);
    public static RegionLoader       regionLoader       = new RegionLoader();
    public static int vaoId         = 0;
    public static int vaoTerrainId  = 0;

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
//        allocateDisplayLists(MAX_DISPLAY_LISTS);
//        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        glActiveTexture(GL_TEXTURE0);
//        glActiveTexture(GL_TEXTURE1);
//        glMatrixMode(GL_TEXTURE);
//        glLoadIdentity();
//        glScalef(1/256F, 1/256F, 1/256F);
//        glTranslatef(8, 8, 8);
//        glMatrixMode(GL_MODELVIEW);

        baseInit();
        Shaders.reinit();
        
        TextureManager.getInstance().init();
        AssetManager.getInstance().init();
        BlockTextureArray.getInstance().init();
        regionLoader.init();
        regionRenderThread.init();
        regionRenderer.init();
        BlockTextureArray.getInstance().reload();
        switchRenderer(true);
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

        float y_scale = GameMath.coTangent(GameMath.degreesToRadians(fieldOfView / 2f));
        float x_scale = y_scale / aspectRatio;
        float frustum_length = zfar - znear;

        projection.setIdentity();
        projection.m00 = x_scale;
        projection.m11 = y_scale;
        projection.m22 = -((zfar + znear) / frustum_length);
        projection.m23 = -1;
        projection.m32 = -((2 * znear * zfar) / frustum_length);
        projection.m33 = 0;
        projection.update();
        projection.update();
        
        shadowProjection.setZero();
        
        {
            float shadowHalfPlane = SHADOW_ORTHO_DIST;
            float left = -shadowHalfPlane;
            float right = shadowHalfPlane;
            float bottom = -shadowHalfPlane;
            float top = shadowHalfPlane;
            // First the scale part
            shadowProjection.m00 = 2.0f / (right - left);
            shadowProjection.m11 = 2.0f / (top - bottom);
            shadowProjection.m22 = (-2.0f) / (shadowZfar - shadowZnear);
            shadowProjection.m33 = 1.0f;
            
            // Then the translation part
            shadowProjection.m30 = -( (right+left) / (right-left) );
            shadowProjection.m31 = -( (top+bottom) / (top-bottom) );
            shadowProjection.m32 = -( (shadowZfar+shadowZnear) / (shadowZfar-shadowZnear) );
        }
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
//        fbShadow.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA);
//        fbShadow.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
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
        
        {
            float left = 0;
            float right = displayWidth;
            float bottom = displayHeight;
            float top = 0;
            float zNear2D = -100;
            float zFar2D = 100;
            // First the scale part
            orthoP.m00 = 2.0f / (right - left);
            orthoP.m11 = 2.0f / (top - bottom);
            orthoP.m22 = (-2.0f) / (zFar2D - zNear2D);
            orthoP.m33 = 1.0f;
            
            // Then the translation part
            orthoP.m30 = -( (right+left) / (right-left) );
            orthoP.m31 = -( (top+bottom) / (top-bottom) );
            orthoP.m32 = -( (zFar2D+zNear2D) / (zFar2D-zNear2D) );
        }

        Matrix4f.mul(orthoP, orthoMV, orthoP);
        
        orthoP.update();
        orthoP.update();
    }
    static TesselatorState fullscreenquad;
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
        float x = vec.x - 0;
        float y = vec.y - 0;
        float z = vec.z - 0;
        modelview.m30 += modelview.m00 * -x + modelview.m10 * -y + modelview.m20 * -z;
        modelview.m31 += modelview.m01 * -x + modelview.m11 * -y + modelview.m21 * -z;
        modelview.m32 += modelview.m02 * -x + modelview.m12 * -y + modelview.m22 * -z;
        modelview.m33 += modelview.m03 * -x + modelview.m13 * -y + modelview.m23 * -z;
        
        Matrix4f.mul(view, modelview, modelview);
        Matrix4f.mul(projection, modelview, modelviewprojection);
        modelview.update();
        modelviewprojection.update();
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

    public static IFrameBuffer getSceneFB() {
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
        regionRenderThread = new RegionRenderThread(3);
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
        boolean mode = Main.matrixSetupMode;
        float sunPathRotation = -40.0F;
        float ca = Main.instance.getWorld().getSunAngle(fTime);
        {

            sunAngle = ca < 0.75F ? ca + 0.25F : ca - 0.75F;
            float angle = ca * -360.0F;

            shadowModelView.setIdentity();
            Vector3f vec = camera.getPosition();
            float trans = 12;
            float trans2 = trans / 2.0f;
            float x= (float)vec.x;// % trans - trans2;
            float y= (float)vec.y;// % trans - trans2;
            float z= (float)vec.z;// % trans - trans2;
            shadowModelView.translate(0.0F, 0.0F, -100.0F);
            shadowModelView.rotate(90.0F * Camera.PI_OVER_180, 1f, 0f, 0f);
            if ((double) sunAngle <= 0.5D) {
                shadowModelView.rotate(angle * Camera.PI_OVER_180, 0f, 0f, 1f);
                shadowModelView.rotate(sunPathRotation * Camera.PI_OVER_180, 1f, 0f, 0f);
            } else {
                shadowModelView.rotate(sunPathRotation * Camera.PI_OVER_180, 0f, 0f, 1f);
                shadowModelView.rotate((angle + 180.0F) * Camera.PI_OVER_180, 1f, 0f, 0f);
            }
            shadowModelView.translate(-x, -260, -z);
            
            Matrix4f.mul(shadowProjection, shadowModelView, shadowModelViewProjection);
            shadowModelView.update();
            shadowModelViewProjection.update();
        }
        {
            float angle = ca * 360.0F;

            if (mode) {
//                glMatrixMode(GL_MODELVIEW);
//                glLoadIdentity();
//                GL.glLoadMatrixf(getMatSceneV().get());
//                glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
//                glRotatef(sunPathRotation, 0.0F, 0.0F, 1.0F);
//                glRotatef(angle, 1.0F, 0.0F, 0.0F);
//                readMat(GL_MODELVIEW_MATRIX, sunModelView);
//                sunModelView.update();
            } else {
                sunModelView.setIdentity();
                sunModelView.rotate(-90.0F * Camera.PI_OVER_180, 0f, 1f, 0f);
                sunModelView.rotate(sunPathRotation * Camera.PI_OVER_180, 0.0F, 0.0F, 1.0F);
                sunModelView.rotate(angle * Camera.PI_OVER_180, 1.0F, 0.0F, 0.0F);
                sunModelView.update();
                Matrix4f.mul(view, sunModelView, sunModelView);
            }

            sunPosition.set(0, 100, 0);
            moonPosition.set(0, -100, 0);
            Matrix4f.transform(sunModelView, sunPosition, sunPosition);
            Matrix4f.transform(sunModelView, moonPosition, moonPosition);
        }
        
    }

    public static void stop() {
        regionRenderThread.stopThread();
        regionLoader.stop();
    }


    public static final Vec3 vOrigin = new Vec3();
    public static final Vec3 vDir = new Vec3();
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
        Vec3.sub(vTarget, vOrigin, vDir);
        vDir.normalise();
        t.set(vDir);
        t.scale(-0.1F);
        Vec3.add(vOrigin, t, vOrigin);
//      System.out.println(vDir); 
    }

    public static void switchRenderer(boolean useBasicShaders) {
        flushRenderTasks();
        if (worldRenderer != null) worldRenderer.release();
        if (outRenderer != null) outRenderer.release();
        if (useBasicShaders) {
            worldRenderer = new WorldRenderer();
            outRenderer = new FinalRenderer();
        } else {
            worldRenderer = new WorldRendererAdv();
            outRenderer = new FinalRendererAdv();
        }
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

}
