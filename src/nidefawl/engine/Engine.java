package nidefawl.engine;

import static org.lwjgl.opengl.GL11.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import nidefawl.engine.util.GameError;
import nidefawl.engine.util.GameMath;
import nidefawl.engine.vec.Vec3;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class Engine {
    private static FloatBuffer colorBuffer;
    private static IntBuffer   viewport;
    private static FloatBuffer winZ;
    private static FloatBuffer position;
//    private static FloatBuffer projectionMatrix;
//    private static FloatBuffer projectionMatrixInv;
//    private static FloatBuffer modelViewMatrix;
//    private static FloatBuffer modelViewMatrixInv;
//
//    private static FloatBuffer shadowProjectionMatrix;
//    private static FloatBuffer shadowProjectionMatrixInv;
//    private static FloatBuffer shadowModelViewMatrix;
//    private static FloatBuffer shadowModelViewMatrixInv;

    private static BufferedMatrix projection;
    private static BufferedMatrix view;
    private static BufferedMatrix modelview;
    private static BufferedMatrix shadowProjection;
    private static BufferedMatrix shadowModelView;
    
//    private static Matrix4f    projectionMat4f;
    private static FloatBuffer   depthRead;
    private static FloatBuffer   fog;
//    private static Matrix4f    def    = new Matrix4f();
//    private static Matrix4f    tmp    = new Matrix4f();
    public static NewCamera    camera = new NewCamera();
    public static FrameBuffer fb;
    public static FrameBuffer fbComposite0;
    public static FrameBuffer fbComposite1;
    public static FrameBuffer fbComposite2;
    public static FrameBuffer fbDbg;
    public static float znear;
    public static float zfar;
    
    public static void generateLightMapTexture() {
        
    }

    public static boolean checkGLError(String s) {
        int i = GL11.glGetError();
        if (i != 0) {
            String s1 = GLU.gluErrorString(i);
            throw new GameError("Error - "+s+": "+s1);
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
        fog = BufferUtils.createFloatBuffer(4);
	}

    public static void set2DMode(float x, float width, float y, float height) {
        //        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glMatrixMode(GL11.GL_PROJECTION); // Select The Projection Matrix
        GL11.glPushMatrix(); // Store The Projection Matrix
        GL11.glLoadIdentity(); // Reset The Projection Matrix
        GL11.glOrtho(x, width, height, y, -100, 100); // Set Up An Ortho Screen
        GL11.glMatrixMode(GL11.GL_MODELVIEW); // Select The Modelview Matrix
        GL11.glPushMatrix(); // Store The Modelview Matrix
        GL11.glLoadIdentity(); // Reset The Modelview Matrix
    }

    public static void set3DMode() {
        GL11.glMatrixMode(GL11.GL_PROJECTION); // Select The Projection Matrix
        GL11.glPopMatrix(); // Restore The Old Projection Matrix
        GL11.glMatrixMode(GL11.GL_MODELVIEW); // Select The Modelview Matrix
        GL11.glPopMatrix(); // Restore The Old Projection Matrix
                GL11.glEnable(GL11.GL_DEPTH_TEST);
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
        float aspectRatio = (float)displayWidth / (float)displayHeight;
        znear = 0.1f;
        zfar = 1000f;
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
        if (fb != null)
            fb.cleanUp();
        fb = new FrameBuffer(true, new int[] { GL_RGB16, GL_RGB8, GL_RGB16, GL_RGB8 });
        if (fbComposite0 != null)
            fbComposite0.cleanUp();
        fbComposite0 = new FrameBuffer(false, new int[] { GL_RGB16, GL_RGB8, GL_RGB16, GL_RGB8 });
        if (fbComposite1 != null)
            fbComposite1.cleanUp();
        fbComposite1 = new FrameBuffer(false, new int[] { GL_RGB16, GL_RGB8, GL_RGB16, GL_RGB8 });
        if (fbComposite2 != null)
            fbComposite2.cleanUp();
        fbComposite2 = new FrameBuffer(false, new int[] { GL_RGB16, GL_RGB8, GL_RGB16, GL_RGB8 });
        if (fbDbg != null)
            fbDbg.cleanUp();
        fbDbg = new FrameBuffer(false, new int[] { GL_RGBA8 });
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
        fog.put(0);
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
}
