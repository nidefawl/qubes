/**
 * 
 */
package nidefawl.qubes.input;

import nidefawl.qubes.gl.Camera;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vr.VR;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class CameraController {
	final static float SPEED_MODIFIER=0.02f;
	public Vec3D pos = new Vec3D();
	public Vec3D lastPos = new Vec3D();
	public Vec3D mot = new Vec3D();
	public Vec3D lastMot = new Vec3D();
	public float yaw, lastYaw;
	public float pitch, lastPitch;
	protected float   forward;
	protected float   strafe;
	protected float   maxSpeed = 0.82F;
	protected float   jump;
	protected boolean   sneak;

	/**
	 * @param movement 
	 * 
	 */
	public void update(KeybindManager movement) {
        float fa = 0.14F;
        float mx = movement.mX * fa;
        float my = -movement.mY * fa;
        float newP = (float) Math.max(-90F, Math.min(90F, this.pitch + my));
        float newY = (float) this.yaw + mx;
        update(newP, newY, movement.forward*SPEED_MODIFIER, movement.strafe*SPEED_MODIFIER, movement.jump?1:0, movement.sneak);
        movement.mX = 0;
        movement.mY = 0;
	}
    public void updateVR() {
        Vector3f tmp = Vector3f.pool();
        VR.pose.toEuler(tmp);
        float yaw = 180-(tmp.y*GameMath.P_180_OVER_PI);
        float pitch = (tmp.x*GameMath.P_180_OVER_PI);
        float forward = VR.getAxis(0, 0, 1)*-0.1f;
        float strafe = VR.getAxis(0, 0, 0)*0.1f;
        this.update(pitch, yaw, forward, strafe, 0, false);
    }
	
	void update(float newP, float newY, float forward, float strafe, float jump, boolean sneak) {
        float diffP = newP - this.pitch;
        float diffY = newY - this.yaw;
        this.pitch = newP;
        this.yaw = newY;
        this.lastPitch += diffP;
        this.lastYaw += diffY;
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
	}
	
	public void tickUpdate() {
        float vel = GameMath.sqrtf(this.forward * this.forward + this.strafe * this.strafe);

        maxSpeed = 0.9F;
        float var7 = 0.0F;
        this.mot.y -= 0.98D * (this.sneak?1:0)* SPEED_MODIFIER*5f;
        this.mot.y += 0.98D * this.jump * SPEED_MODIFIER*5f;

        float f4 = 0.0F;
        float f5 = 0.0F;
        float f6 = 0.0F;
        float f7 = 0.0F;
        if (vel >= 0.01F) {
            if (vel < 1.0F) {
                vel = 1.0F;
            }

            float strafe = -this.strafe / vel;
            float forward = -this.forward / vel;
            float sinY = GameMath.sin(GameMath.degreesToRadians(this.yaw));
            float cosY = GameMath.cos(GameMath.degreesToRadians(this.yaw));
            f4 = strafe * cosY;
            f5 = -forward * sinY;
            f6 = strafe * sinY;
            f7 = forward * cosY;
        }
	    if (this.yaw > 360)
	        this.yaw -= 360;
	    if (this.yaw < 0)
	        this.yaw += 360;
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;

        float f8 = GameMath.degreesToRadians(-this.pitch);
        float fm = GameMath.cos(f8);
        float f1 = -GameMath.sin(f8) * Math.signum(-this.forward);
        float f2 = f5 * fm + f4;
        float f3 = GameMath.sqrtf(f5 * f5 + f7 * f7) * f1 + var7;
        float f9 = f7 * fm + f6;
        float f10 = GameMath.sqrtf(GameMath.sqrtf(f2 * f2 + f9 * f9) + f3 * f3);
        if (f10 > 0.01F) {
            float f11 = maxSpeed / f10;
            this.mot.x += (double) (f2 * f11);
            this.mot.y += (double) (f3 * f11);
            this.mot.z += (double) (f9 * f11);
        }
        move();
	}
	
	public void move() {

        this.jump *= 1;

        this.lastMot.set(this.mot);
        this.lastPos.set(this.pos);
        Vec3D.add(this.pos, this.mot, this.pos);
        float slowdown = 0.28F;
        this.mot.x *= slowdown;
        this.mot.z *= slowdown;
        this.mot.y *= slowdown;
	}
	
	public void set(float x, float y, float z, float pitch, float yaw) {
		this.pos.set(x, y, z);
		this.lastPos.set(x, y, z);
		this.pitch = this.lastPitch = pitch;
		this.yaw = this.lastYaw = yaw;
	}
    public Vector3f getRenderPos(float f) {
        Vector3f tmp = Vector3f.pool();
        Vector3f.interp(this.lastPos, this.pos, f, tmp);
        return tmp;
    }
    public Vector3f orientCamera(Camera camera, KeybindManager movement, boolean vr, float f) {
        if (vr) {
            this.updateVR();
        } else {
            this.update(movement);
        }
        Vector3f cameraPos = getRenderPos(f);
        Engine.camera.setPosition(cameraPos);
        if (!vr) {
            Engine.camera.setOrientation(this.yaw, this.pitch, false, 4.0f);   
        }
        return cameraPos;
    }
}
