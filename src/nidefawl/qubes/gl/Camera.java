package nidefawl.qubes.gl;

import nidefawl.qubes.util.GameMath;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class Camera {

    protected final static float PI_OVER_180      = 0.0174532925f;


    protected float              pitchAngle = 0;
    protected float              bearingAngle = 0;
    protected final Vector3f     position    = new Vector3f();
    protected final Vector3f     prevposition    = new Vector3f();
    protected final Quaternion   pitch       = new Quaternion();
    protected final Quaternion   bearing     = new Quaternion();
    protected final Quaternion   rotation    = new Quaternion();
    protected final Matrix4f      viewMatrix  = new Matrix4f();
    protected final Vector4f     _tmp1       = new Vector4f();

    public boolean             changed          = true;

    public float getYaw() {
        return bearingAngle;
    }

    public float getPitch() {
        return pitchAngle;
    }
    public void reorient() {
        _tmp1.set(1f, 0f, 0f, pitchAngle * PI_OVER_180);
        pitch.setFromAxisAngle(_tmp1);
        _tmp1.set(0f, 1f, 0f, bearingAngle * PI_OVER_180);
        bearing.setFromAxisAngle(_tmp1);
        Quaternion.mul(pitch, bearing, rotation);
        GameMath.convertQuaternionToMatrix4f(rotation, this.viewMatrix);
    }



    public void setPosition(float x, float y, float z) {
        this.prevposition.set(this.position);
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
    }

    public void setOrientation(float yaw, float pitch) {
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
     * @return the camera's position in the world
     */
    public Vector3f getPrevPosition() {
        return prevposition;
    }

    /**
     * Call GL11.glMultMatrix() on this matrix in your render loop to set the camera's view.
     * 
     * @return buffer of a 4x4 matrix for the view transformation
     */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

}
