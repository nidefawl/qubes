package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.Textures;
import nidefawl.qubes.render.OutputRenderer;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vec3;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class Engine {
    public final static int MAX_DISPLAY_LISTS = 256;
    private static FloatBuffer    colorBuffer;
    private static IntBuffer      viewport;
    private static FloatBuffer    winZ;
    private static FloatBuffer    position;

    private static BufferedMatrix projection;
    private static BufferedMatrix view;
    private static BufferedMatrix modelview;
    private static BufferedMatrix shadowProjection;
    private static BufferedMatrix shadowModelView;

    private static FloatBuffer    depthRead;
    private static FloatBuffer    fog;
    
    public static FrameBuffer     fb2;
    public static FrameBuffer     fbComposite0;
    public static FrameBuffer     fbComposite1;
    public static FrameBuffer     fbComposite2;
    public static FrameBuffer     fbDbg;
    public static float           znear;
    public static float           zfar;
    private static DisplayList[]      lists;
    public static Camera       camera = new Camera();
    public static WorldRenderer worldRenderer = new WorldRenderer();
    public static OutputRenderer outRenderer = new OutputRenderer();
    public static Shaders shaders = new Shaders();
    public static Textures textures = new Textures();

    public static void generateLightMapTexture() {

    }

    public static boolean checkGLError(String s) {
        int i = GL11.glGetError();
        if (i != 0) {
            String s1 = GLU.gluErrorString(i);
            throw new GameError("Error - " + s + ": " + s1);
        }
        return false;
    }

    public static void init() {
        colorBuffer = BufferUtils.createFloatBuffer(16);
        viewport = BufferUtils.createIntBuffer(16);
        winZ = BufferUtils.createFloatBuffer(1);
        position = BufferUtils.createFloatBuffer(3);
        projection = new BufferedMatrix();
        view = new BufferedMatrix();
        modelview = new BufferedMatrix();
        shadowProjection = new BufferedMatrix();
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

        TextureManager.getInstance().init();
        AssetManager.getInstance().init();
        textures.init();
        shaders.init();
        worldRenderer.init();
        outRenderer.init();
    }


    /**
     * Render the current frame
     * 
     * @param fTime
     */
    public static void enableLighting() {

        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        glEnable(GL_LIGHT1);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
        glLight(GL_LIGHT0, GL_POSITION, setColorBuffer(0.16169041989141428, 0.8084520874101966, -0.5659164515496377, 0.0D));
        float diffuse = 0.6F;
        float ambient = 0.0F;
        float specular = 0.0F;
        glLight(GL_LIGHT0, GL_DIFFUSE, setColorBuffer(diffuse, diffuse, diffuse, 1.0F));
        glLight(GL_LIGHT0, GL_AMBIENT, setColorBuffer(ambient, ambient, ambient, 1.0F));
        specular = 0.1F;
        glLight(GL_LIGHT0, GL_SPECULAR, setColorBuffer(specular, specular, specular, 1.0F));
        glLight(GL_LIGHT1, GL_POSITION, setColorBuffer(-0.16169041989141428, 0.8084520874101966, 0.5659164515496377, 0.0D));
        glLight(GL_LIGHT1, GL_DIFFUSE, setColorBuffer(diffuse, diffuse, diffuse, 1.0F));
        glLight(GL_LIGHT1, GL_AMBIENT, setColorBuffer(ambient, ambient, ambient, 1.0F));
        specular = 0.1F;
        glLight(GL_LIGHT1, GL_SPECULAR, setColorBuffer(specular, specular, specular, 1.0F));
        specular = 0.2F;
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, setColorBuffer(specular, specular, specular, 1.0F));

        glShadeModel(GL_FLAT);
        glLightModel(GL_LIGHT_MODEL_AMBIENT, setColorBuffer(0.4F, 0.4F, 0.4F, 1.0F));
    }

    private static FloatBuffer setColorBuffer(double d, double d1, double d2, double d3) {
        return setColorBuffer((float) d, (float) d1, (float) d2, (float) d3);
    }

    private static FloatBuffer setColorBuffer(float f, float f1, float f2, float f3) {
        colorBuffer.clear();
        colorBuffer.put(f).put(f1).put(f2).put(f3);
        colorBuffer.flip();
        return colorBuffer;
    }

    static final Vec3 v = new Vec3();

    public static Vec3 unproject(float winX, float winY) {
        glReadPixels((int) winX, (int) winY, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, winZ);
        float depth = winZ.get(0);
        GLU.gluUnProject(winX, winY, depth, getModelViewMatrix(), getProjectionMatrix(), viewport, position);
        v.x = position.get(0);
        v.y = position.get(1);
        v.z = position.get(2);
        return v;
    }

    public static void resize(int displayWidth, int displayHeight) {
        projection.setIdentity();
        float fieldOfView = 60;
        float aspectRatio = (float) displayWidth / (float) displayHeight;
        znear = 0.04f;
        zfar = 5000f;
        viewport.position(0);
        viewport.put(0);
        viewport.put(0);
        viewport.put(displayWidth);
        viewport.put(displayHeight);
        viewport.flip();

        float y_scale = GameMath.coTangent(GameMath.degreesToRadians(fieldOfView / 2f));
        float x_scale = y_scale / aspectRatio;
        float frustum_length = zfar - znear;

        projection.m00 = x_scale;
        projection.m11 = y_scale;
        projection.m22 = -((zfar + znear) / frustum_length);
        projection.m23 = -1;
        projection.m32 = -((2 * znear * zfar) / frustum_length);
        projection.m33 = 0;
        projection.update();
        projection.update();
        if (fb2 != null)
            fb2.cleanUp();
        fb2 = new FrameBuffer(displayWidth, displayHeight);
        fb2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16);
        fb2.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB8);
        fb2.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGB16);
        fb2.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB8);
        fb2.setHasDepthAttachment();
        fb2.setup();
        if (fbComposite0 != null)
            fbComposite0.cleanUp();
        fbComposite0 = new FrameBuffer(displayWidth, displayHeight);
        fbComposite0.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16);
        fbComposite0.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB8);
        fbComposite0.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGB16);
        fbComposite0.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB8);
        fbComposite0.setup();
//        fbComposite0 = new FrameBuffer(false, new int[] { GL_RGB16, GL_RGB8, GL_RGB16, GL_RGB8 });
        if (fbComposite1 != null)
            fbComposite1.cleanUp();
        fbComposite1 = new FrameBuffer(displayWidth, displayHeight);
        fbComposite1.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGB16);
        fbComposite1.setup();
        if (fbComposite2 != null)
            fbComposite2.cleanUp();
        fbComposite2 = new FrameBuffer(displayWidth, displayHeight);
        fbComposite2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16);
        fbComposite2.setup();
        if (fbDbg != null)
            fbDbg.cleanUp();
        fbDbg = new FrameBuffer(displayWidth, displayHeight);
        fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA);
        fbDbg.setup();
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

    public static void update() {
        Matrix4f cam = camera.getViewMatrix();
        view.load(cam);
        view.update();
        Vector3f vec = camera.getPosition();
        modelview.setIdentity();
        modelview.m30 += modelview.m00 * -vec.x + modelview.m10 * -vec.y + modelview.m20 * -vec.z;
        modelview.m31 += modelview.m01 * -vec.x + modelview.m11 * -vec.y + modelview.m21 * -vec.z;
        modelview.m32 += modelview.m02 * -vec.x + modelview.m12 * -vec.y + modelview.m22 * -vec.z;
        modelview.m33 += modelview.m03 * -vec.x + modelview.m13 * -vec.y + modelview.m23 * -vec.z;
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
        glFog(GL_FOG_COLOR, fog);

    }

    public static void setShadow() {
        shadowProjection.load(projection);
        shadowModelView.load(modelview);
        shadowModelView.update();
        shadowModelView.update();
        shadowProjection.update();
        shadowProjection.update();
    }

    public static DisplayList nextFreeDisplayList() {
        for (int i = 0; i < lists.length; i++) {
            if (!lists[i].inUse) {
                DisplayList alloc = lists[i];
                alloc.inUse = true;
                return alloc;
            }
        }
        return null;
    }

    public static void release(DisplayList displayList) {
        displayList.inUse = false;
    }

    public static void allocateDisplayLists(int maxRegions) {
        lists = new DisplayList[maxRegions];
        int a = glGenLists(maxRegions * 2);
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new DisplayList();
            lists[i].list = a + i * 2;
        }
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
        return fb2;
    }


}
