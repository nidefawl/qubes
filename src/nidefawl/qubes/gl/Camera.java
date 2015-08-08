package nidefawl.qubes.gl;

import java.nio.FloatBuffer;

import nidefawl.game.Main;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.util.GameMath;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class Camera {

    private final static float PI_OVER_180      = 0.0174532925f;

    private Vector3f           position         = new Vector3f();

    private float              pitchAngle;
    private float              bearingAngle;
    private Quaternion         pitch;
    private Quaternion         bearing;
    private Quaternion         rotation;
    Matrix4f                   viewMatrix           = new Matrix4f();

    public boolean             changed          = true;

    public Camera() {
        pitch = new Quaternion();
        bearing = new Quaternion();
        rotation = new Quaternion();
        bearingAngle = 0;
        pitchAngle = 0;
    }

    public float getYaw() {
        return bearingAngle;
    }

    public float getPitch() {
        return pitchAngle;
    }

    public void reorient() {
        pitch.setFromAxisAngle(new Vector4f(1f, 0f, 0f, pitchAngle * PI_OVER_180));
        bearing.setFromAxisAngle(new Vector4f(0f, 1f, 0f, bearingAngle * PI_OVER_180));
        Quaternion.mul(pitch, bearing, rotation);
        this.viewMatrix.setIdentity();
        convertQuaternionToMatrix4f();
    }

    public void set(PlayerSelf entSelf, float f) {
        float px = entSelf.lastPos.x + (entSelf.pos.x - entSelf.lastPos.x) * f;
        float py = entSelf.lastPos.y + (entSelf.pos.y - entSelf.lastPos.y) * f;
        float pz = entSelf.lastPos.z + (entSelf.pos.z - entSelf.lastPos.z) * f;
        float yaw = entSelf.yaw;
        float pitch = entSelf.pitch;
        set(px, py, pz, yaw, pitch);
    }

    public void set(float x, float y, float z, float yaw, float pitch) {
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
        this.pitchAngle = pitch;
        this.bearingAngle = yaw;
        reorient();
    }

    /**
     * @return the camera's position in the world
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * Call GL11.glMultMatrix() on this matrix in your render loop to set the camera's view.
     * 
     * @return buffer of a 4x4 matrix for the view transformation
     */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    private Matrix4f convertQuaternionToMatrix4f() {
        Quaternion q = this.rotation;
        Matrix4f matrix = this.viewMatrix;
        matrix.m00 = 1.0f - 2.0f * (q.getY() * q.getY() + q.getZ() * q.getZ());
        matrix.m01 = 2.0f * (q.getX() * q.getY() + q.getZ() * q.getW());
        matrix.m02 = 2.0f * (q.getX() * q.getZ() - q.getY() * q.getW());
        matrix.m03 = 0.0f;

        // Second row
        matrix.m10 = 2.0f * (q.getX() * q.getY() - q.getZ() * q.getW());
        matrix.m11 = 1.0f - 2.0f * (q.getX() * q.getX() + q.getZ() * q.getZ());
        matrix.m12 = 2.0f * (q.getZ() * q.getY() + q.getX() * q.getW());
        matrix.m13 = 0.0f;

        // Third row
        matrix.m20 = 2.0f * (q.getX() * q.getZ() + q.getY() * q.getW());
        matrix.m21 = 2.0f * (q.getY() * q.getZ() - q.getX() * q.getW());
        matrix.m22 = 1.0f - 2.0f * (q.getX() * q.getX() + q.getY() * q.getY());
        matrix.m23 = 0.0f;

        // Fourth row
        matrix.m30 = 0;
        matrix.m31 = 0;
        matrix.m32 = 0;
        matrix.m33 = 1.0f;

        return matrix;
    }

}
