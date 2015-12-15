package nidefawl.qubes.gl;

import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;

public class Camera {

    protected float          pitchAngle   = 0;
    protected float          bearingAngle = 0;
    protected final Vector3f position     = new Vector3f();
    protected final Vector3f prevposition = new Vector3f();
    protected final Matrix4f viewMatrix   = new Matrix4f();
    protected final Matrix4f thirdPersonMat = new Matrix4f();
    protected final Vector3f thirdPersonOffset = new Vector3f();
    public boolean changed = true;

    public float getYaw() {
        return bearingAngle;
    }

    public float getPitch() {
        return pitchAngle;
    }


    /**
     * @param vCam
     */
    public void setPosition(Vector3f vCam) {
        setPosition(vCam.x, vCam.y, vCam.z);
    }


    public void setPosition(float x, float y, float z) {
        this.prevposition.set(this.position);
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
    }



    public void setPosition(Vec3D v) {
        this.prevposition.set(this.position);
        this.position.set(v);
    }

    public void setOrientation(float yaw, float pitch, boolean thirdPerson, float thirdPersonDistance) {
        this.pitchAngle = pitch;
        this.bearingAngle = yaw;

        this.viewMatrix.setIdentity();
        this.viewMatrix.rotate(this.pitchAngle * GameMath.PI_OVER_180, 1f, 0f, 0f);
        this.viewMatrix.rotate(this.bearingAngle * GameMath.PI_OVER_180, 0f, 1f, 0f);

        if (thirdPerson) {
            this.thirdPersonMat.setIdentity();
            this.thirdPersonMat.rotate(-this.bearingAngle * GameMath.PI_OVER_180, 0.0F, 1.0F, 0.0F);
            this.thirdPersonMat.rotate(-this.pitchAngle * GameMath.PI_OVER_180, 1.0F, 0.0F, 0.0F);
            this.thirdPersonMat.translate(0.0f, 0.0f, thirdPersonDistance);
            Matrix4f.transform(this.thirdPersonMat, Vector3f.ZERO, this.thirdPersonOffset);
        } else {
            this.thirdPersonOffset.set(0, 0, 0);
        }
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
    
    /**
     * @return the thirdPersonOffset
     */
    public Vector3f getCameraOffset() {
        return this.thirdPersonOffset;
    }

}
