package nidefawl.qubes.entity;

import nidefawl.qubes.input.Movement;
import nidefawl.qubes.util.GameMath;

public class PlayerSelf extends Entity {

    private float   forward;
    private float   strafe;
    private float   maxSpeed = 0.82F;
    private boolean fly      = false;
    private float   jump;
    private boolean   jumped;
    private float   sneak;
    public float eyeHeight = 1.3F;

    public PlayerSelf(int id) {
        super(id);
    }

    public void updateInputDirect(Movement movement) {
        float fa = 0.14F;
        float mx = movement.mX * fa;
        float my = -movement.mY * fa;
        float newP = (float) Math.max(-90F, Math.min(90F, this.pitch + my));
        float newY = (float) this.yaw + mx;
        float diffP = newP - this.pitch;
        float diffY = newY - this.yaw;
        this.pitch = newP;
        this.yaw = newY;
        this.lastPitch += diffP;
        this.lastYaw += diffY;
        this.strafe = movement.strafe;
        this.forward = movement.forward;
        if (this.fly) {
            this.jump = movement.jump;
            jumped = false;
        } else {
            if (jumped) {
                if (movement.jump <= 0) {
                    jumped = false;
                }
                movement.jump = 0;
            } else {
                if (hitGround) {
                    jumped = movement.jump > 0;
                    this.jump = movement.jump;
                }
            }
        }
        this.sneak = movement.sneak;
        movement.mX = 0;
        movement.mY = 0;
    }

    @Override
    public void tickUpdate() {
        float vel = GameMath.sqrtf(this.forward * this.forward + this.strafe * this.strafe);
        float slowdown = 0.28F;
        float f = -getGravity();
        float fn = 0.11F;
        this.noclip = this.fly;
        if (this.fly) {
            maxSpeed = 0.9F;
            float var7 = 0.0F;
            this.mot.y -= 0.98D * this.sneak;
            this.mot.y += 0.98D * this.jump;

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
            this.jump *= 1;
        } else {
            slowdown = 0.28F;
            maxSpeed = 0.2F;
            this.mot.y += 0.49171D * this.jump;
            this.jump *= 0.5;
            if (vel > 0.01F) {
                if (vel < 1)
                    vel = 1;
                float fm = maxSpeed / vel;
                float forward = -this.forward * fm;
                float strafe = -this.strafe * fm;
                float sinY = GameMath.sin(GameMath.degreesToRadians(this.yaw));
                float cosY = GameMath.cos(GameMath.degreesToRadians(this.yaw));
                this.mot.x += -forward * sinY + strafe * cosY;
                this.mot.z += forward * cosY + strafe * sinY;
            }
        }
        super.tickUpdate();
        this.mot.x *= slowdown;
        this.mot.z *= slowdown;
        if (this.fly) {

            this.mot.y *= slowdown;
        } else {

            this.mot.y = this.mot.y*(1F-fn)+f*fn;
        }
//      float f = getGravity();
//      if (this.mot.y > 0) {
//          this.mot.y *= slowdown;
//      }
//      if (this.mot.y > f) {
//      }
    }

    public float getGravity() {
        return this.fly ? 0 : 0.98F;
    }

    public void toggleFly() {
        this.fly = !fly;
    }

}
