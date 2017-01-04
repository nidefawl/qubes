package nidefawl.qubes.gl;

import nidefawl.qubes.Game;
import nidefawl.qubes.entity.PlayerSelf;
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
    protected float xshake;
    protected float yshake;
    private float xshakeRot;
    private float zshakeRot;
    private float yshakeRot;

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
//        this.position.x+=xshake;
//        this.position.y+=yshake;
    }

    public void setOrientation(float yaw, float pitch, boolean thirdPerson, float thirdPersonDistance) {
        this.pitchAngle = pitch;
        this.bearingAngle = yaw;

        this.viewMatrix.setIdentity();
        addCameraShake(this.viewMatrix);
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

    public void addCameraShake(Matrix4f vm) {

        vm.translate(xshake, yshake, 0);
        vm.rotate(zshakeRot * GameMath.PI_OVER_180, 0f, 0f, 1f);
        vm.rotate(yshakeRot * GameMath.PI_OVER_180, 1f, 0f, 0f);
        vm.rotate(xshakeRot * GameMath.PI_OVER_180, 1f, 0f, 0f);
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

    public void calcViewShake(float distF, float f2, float f3, float f) {
        xshake = GameMath.sin(distF * GameMath.PI) * f2 * 0.5F;
        yshake = -1*Math.abs(GameMath.cos(distF * GameMath.PI) * f2);
        float af = 3F;
        zshakeRot=GameMath.sin(distF * (float)Math.PI) * f2 * af * 0;
        yshakeRot=Math.abs(GameMath.cos(distF * (float)Math.PI - 0.2F) * f2) * 5F;
        xshakeRot=f3;
    
    }

}
