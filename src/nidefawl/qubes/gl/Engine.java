package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

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
    public final static int   MAX_DISPLAY_LISTS  = 512;
    public final static int   SHADOW_BUFFER_SIZE = 4096;
    public final static int   SHADOW_ORTHO_DIST  = 240;
    public final static float shadowZnear        = -128F;
    public final static float shadowZfar         = 512.0F;
    
    private static FloatBuffer    colorBuffer;
    private static IntBuffer      viewport;
    private static FloatBuffer    position;
    private static FloatBuffer    mat;

    private static BufferedMatrix projection;
    private static BufferedMatrix view;
    private static BufferedMatrix modelview;
    private static BufferedMatrix shadowProjection;
    private static BufferedMatrix sunModelView;
    private static BufferedMatrix shadowModelView;

    private static FloatBuffer       depthRead;
    private static FloatBuffer       fog;

    public static FrameBuffer        fbScene;
    public static FrameBuffer        fbDbg;
    public static FrameBuffer        fbShadow;
    
    public static float              znear;
    public static float              zfar;
    

    private static RegionDisplayList[]     lists;
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
    private static DisplayList fullscreenQuad;

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

    public static void init() {
        colorBuffer = BufferUtils.createFloatBuffer(16);
        viewport = BufferUtils.createIntBuffer(16);
        mat = BufferUtils.createFloatBuffer(16);
        position = BufferUtils.createFloatBuffer(3);
        projection = new BufferedMatrix();
        view = new BufferedMatrix();
        modelview = new BufferedMatrix();
        shadowProjection = new BufferedMatrix();
        sunModelView = new BufferedMatrix();
        shadowModelView = new BufferedMatrix();
        depthRead = BufferUtils.createFloatBuffer(16);
        fog = BufferUtils.createFloatBuffer(16);
        allocateDisplayLists(MAX_DISPLAY_LISTS);
        

        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        glActiveTexture(GL_TEXTURE1);
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();
        glScalef(1/256F, 1/256F, 1/256F);
        glTranslatef(8, 8, 8);
        glMatrixMode(GL_MODELVIEW);
        glActiveTexture(GL_TEXTURE0);

        Shaders.init();
        TextureManager.getInstance().init();
        AssetManager.getInstance().init();
        BlockTextureArray.getInstance().init();
        regionLoader.init();
        regionRenderThread.init();
        regionRenderer.init();
        BlockTextureArray.getInstance().reload();
        switchRenderer(true);
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

        

        if (fbDbg != null)
            fbDbg.cleanUp();
        fbDbg = new FrameBuffer(displayWidth, displayHeight);
        fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA);
        fbDbg.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbDbg.setup();
        if (fbShadow != null)
            fbShadow.cleanUp();
        fbShadow = new FrameBuffer(SHADOW_BUFFER_SIZE, SHADOW_BUFFER_SIZE);
//        fbShadow.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA);
//        fbShadow.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbShadow.setShadowBuffer();
        fbShadow.setup();
        if (fullscreenQuad == null) {
            fullscreenQuad = newDisplayList();
        }
        {
            Tess.instance.resetState();
            int tw = displayWidth;
            int th = displayHeight;
            float x = 0;
            float y = 0;
            Tess.instance.add(x + tw, y, 0, 1, 1);
            Tess.instance.add(x, y, 0, 0, 1);
            Tess.instance.add(x, y + th, 0, 0, 0);
            Tess.instance.add(x + tw, y + th, 0, 1, 0);
            glNewList(fullscreenQuad.list, GL_COMPILE);
            Tess.instance.draw(GL_QUADS);
            glEndList();
        }
        
        if (worldRenderer != null) {
            worldRenderer.resize(displayWidth, displayHeight);
        }
        if (outRenderer != null) {
            outRenderer.resize(displayWidth, displayHeight);
        }
    }
    public static void drawFullscreenQuad() {
        glCallList(fullscreenQuad.list);
    }

    public static float readDepth(int x, int y) {
        depthRead.clear();
        GL11.glReadPixels(x, y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, depthRead);
        return depthRead.get(0);
    }

    public static FloatBuffer getProjectionMatrix() {
        return projection.get();
    }

    public static FloatBuffer getProjectionMatrixInv() {
        return projection.getInv();
    }

    public static FloatBuffer getViewMatrix() {
        return view.get();
    }

    public static FloatBuffer getViewMatrixInv() {
        return view.getInv();
    }

    public static FloatBuffer getViewMatrixPrev() {
        return view.getPrev();
    }

    public static FloatBuffer getModelViewMatrixInv() {
        return modelview.getInv();
    }

    public static FloatBuffer getModelViewMatrix() {
        return modelview.get();
    }

    public static FloatBuffer getModelViewMatrixPrev() {
        return modelview.getPrev();
    }

    public static FloatBuffer getProjectionMatrixPrev() {
        return projection.getPrev();
    }

    public static FloatBuffer getShadowProjectionMatrixInv() {
        return shadowProjection.getInv();
    }

    public static FloatBuffer getShadowProjectionMatrix() {
        return shadowProjection.get();
    }

    public static FloatBuffer getShadowModelViewMatrixInv() {
        return shadowModelView.getInv();
    }

    public static FloatBuffer getShadowModelViewMatrix() {
        return shadowModelView.get();
    }

    public static void updateCamera() {
        up.set(0, 100, 0);
//        back.set(0, -10, 0);
        Matrix4f cam = camera.getViewMatrix();
        view.load(cam);
        view.update();
//        Vector4f zdepth = new Vector4f(0,0,-100, 0);
//        System.out.println(zdepth);
//        Matrix4f.transform(view, back, back);
//        System.out.println(back);
        
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
        modelview.update();
        
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


    public static RegionDisplayList nextFreeDisplayList() {
        for (int i = 0; i < lists.length; i++) {
            if (!lists[i].inUse) {
                RegionDisplayList alloc = lists[i];
                alloc.inUse = true;
                return alloc;
            }
        }
        return null;
    }

    public static void release(RegionDisplayList displayList) {
        displayList.inUse = false;
    }

    public static void allocateDisplayLists(int maxRegions) {
        lists = new RegionDisplayList[maxRegions];
        int a = glGenLists(maxRegions * MeshedRegion.NUM_LAYERS*WorldRenderer.NUM_PASSES);
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new RegionDisplayList();
            lists[i].list = a + i * MeshedRegion.NUM_LAYERS*WorldRenderer.NUM_PASSES;
        }
    }
    public static DisplayList newDisplayList() {
        DisplayList list = new DisplayList();
        list.inUse = true;
        list.list = glGenLists(1);
        return list;
    }


    public static boolean hasFree() {
        for (int i = 0; i < lists.length; i++) {
            if (!lists[i].inUse) {
                return true;
            }
        }
        return false;
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

    protected static final Quaternion   q1       = new Quaternion();
    protected static final Quaternion   q2     = new Quaternion();
    protected static final Quaternion   q3    = new Quaternion();
    protected static final Quaternion   q4    = new Quaternion();
    protected static final Vector4f     _tmp1       = new Vector4f();
    protected static final Vector3f     _tmp2       = new Vector3f();
    protected static final Matrix4f _mat = new Matrix4f();
    protected static final Matrix4f _mat2 = new Matrix4f();
    protected static final Matrix4f _mat3 = new Matrix4f();

    public static void updateSun(float fTime) {
        boolean mode = Main.matrixSetupMode;
        float sunPathRotation = -40.0F;
        float ca = Main.instance.getWorld().getSunAngle(fTime);
        {

            sunAngle = ca < 0.75F ? ca + 0.25F : ca - 0.75F;
            float angle = ca * -360.0F;
            if (mode) {
                glMatrixMode(GL_MODELVIEW);
                glLoadIdentity();

                glTranslatef(0.0F, 0.0F, -100.0F);
                glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                if ((double) sunAngle <= 0.5D) {
                    glRotatef(angle, 0.0F, 0.0F, 1.0F);
                    glRotatef(sunPathRotation, 1.0F, 0.0F, 0.0F);
                } else {
                    glRotatef(sunPathRotation, 0.0F, 0.0F, 1.0F);
                    glRotatef(angle + 180.0F, 1.0F, 0.0F, 0.0F);
                }
                readMat(GL_MODELVIEW_MATRIX, shadowModelView);
                shadowModelView.update();
            } else {
                _tmp2.set(0, 0, -100);
                _mat.setIdentity();
                _mat.translate(_tmp2);
                Vector3f vec = camera.getPosition();
                float trans = 12;
                float trans2 = trans / 2.0f;
                float x= (float)vec.x % trans - trans2;
                float y= (float)vec.y % trans - trans2;
                float z= (float)vec.z % trans - trans2;
                _tmp2.set(x,y,z);
                _mat3.setIdentity();
                _mat3.translate(_tmp2);
                _tmp1.set(1f, 0f, 0f, 90.0F * Camera.PI_OVER_180);
                q4.setFromAxisAngle(_tmp1);
                if ((double) sunAngle <= 0.5D) {
                    //            GL11.glRotatef(ca * -360.0F, 0.0F, 0.0F, 1.0F);
                    //            GL11.glRotatef(-40, 1.0F, 0.0F, 0.0F);
                    _tmp1.set(0f, 0f, 1f, angle * Camera.PI_OVER_180);
                    q1.setFromAxisAngle(_tmp1);
                    _tmp1.set(1f, 0f, 0f, sunPathRotation * Camera.PI_OVER_180);
                    q2.setFromAxisAngle(_tmp1);
                } else {
                    _tmp1.set(0f, 0f, 1f, sunPathRotation * Camera.PI_OVER_180);
                    q1.setFromAxisAngle(_tmp1);
                    _tmp1.set(1f, 0f, 0f, (angle + 180.0F) * Camera.PI_OVER_180);
                    q2.setFromAxisAngle(_tmp1);
                    //            GL11.glRotatef(ca * -360.0F + 180.0F, 0.0F, 0.0F, 1.0F);
                    //            GL11.glRotatef(-40, 1.0F, 0.0F, 0.0F);
                }
                Quaternion.mul(q4, q1, q1);
                Quaternion.mul(q1, q2, q3);
                GameMath.convertQuaternionToMatrix4f(q3, _mat2);
                Matrix4f.mul(_mat2, _mat3, _mat2);
                Matrix4f.mul(_mat, _mat2, shadowModelView);
//                float x = vec.x - 0;
//                float y = vec.y - 0;
//                float z = vec.z - 0;
//                SMCLog.info("shadow interval %.2f %.2f %.2f", fx, fy, fz);
//                glTranslatef(fx,fy,fz);
//                shadowModelView.m30 += shadowModelView.m00 * -x + shadowModelView.m10 * -y + shadowModelView.m20 * -z;
//                shadowModelView.m31 += shadowModelView.m01 * -x + shadowModelView.m11 * -y + shadowModelView.m21 * -z;
//                shadowModelView.m32 += shadowModelView.m02 * -x + shadowModelView.m12 * -y + shadowModelView.m22 * -z;
//                shadowModelView.m33 += shadowModelView.m03 * -x + shadowModelView.m13 * -y + shadowModelView.m23 * -z;
                shadowModelView.update();
            }
        }
        {
            float angle = ca * 360.0F;

            if (mode) {
                glMatrixMode(GL_MODELVIEW);
                glLoadIdentity();
                GL.glLoadMatrixf(getViewMatrix());
                glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                glRotatef(sunPathRotation, 0.0F, 0.0F, 1.0F);
                glRotatef(angle, 1.0F, 0.0F, 0.0F);
                readMat(GL_MODELVIEW_MATRIX, sunModelView);
                sunModelView.update();
            } else {
                _tmp1.set(0f, 1f, 0f, -90.0F * Camera.PI_OVER_180);
                q4.setFromAxisAngle(_tmp1);
                _tmp1.set(0f, 0f, 1f, sunPathRotation * Camera.PI_OVER_180);
                q1.setFromAxisAngle(_tmp1);
                _tmp1.set(1f, 0f, 0f, angle * Camera.PI_OVER_180);
                q2.setFromAxisAngle(_tmp1);
                Quaternion.mul(q4, q1, q1);
                Quaternion.mul(q1, q2, q3);
                GameMath.convertQuaternionToMatrix4f(q3, sunModelView);
                Matrix4f.mul(view, sunModelView, sunModelView);
                sunModelView.update();
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
        if (!Project.gluUnProject(winX, winY, 1F, getModelViewMatrix(), getProjectionMatrix(), viewport, position)) {
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
        if (!Project.gluUnProject(winX, winY, 0F, getModelViewMatrix(), getProjectionMatrix(), viewport, position)) {
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

}
