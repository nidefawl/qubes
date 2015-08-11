package nidefawl.qubes.entity;

import nidefawl.qubes.input.Movement;
import nidefawl.qubes.util.GameMath;

public class PlayerSelf extends Entity {

    private float forward;
    private float strafe;
    private float maxSpeed = 0.82F;
    private boolean fly = true;
    private boolean jump;
    private boolean sneak;


    public PlayerSelf(int id) {
        super(id);
    }


    public void updateInputDirect(Movement movement) {
        float fa = 0.14F;
        float mx = movement.mX*fa;
        float my = -movement.mY*fa;
        float newP = (float) Math.max(-90F, Math.min(90F, this.pitch + my));
        float newY = (float) this.yaw + mx;
        float diffP = newP-this.pitch;
        float diffY = newY-this.yaw;
        this.pitch = newP;
        this.yaw = newY;
        this.lastPitch+=diffP;
        this.lastYaw+=diffY;
        this.strafe = movement.strafe;
        this.forward = movement.forward;
        this.jump = movement.jump > 0;
        this.sneak = movement.sneak > 0;
        movement.mX = 0;
        movement.mY = 0;
    }
    @Override
    public void tickUpdate() {
        float vel = GameMath.sqrtf(this.forward*this.forward+this.strafe*this.strafe);
        if (this.fly) {
            float var7 = 0.0F;
            if (this.sneak) {
                this.mot.y += 0.98D;
//                var7 -= 0.98F;
            }
            if (this.jump) {
                this.mot.y -= 0.98D;
//                var7 += 0.98F;
            }

            float f4 = 0.0F;
            float f5 = 0.0F;
            float f6 = 0.0F;
            float f7 = 0.0F;
            if (vel >= 0.01F)
            {
                if (vel < 1.0F)
                {
                    vel = 1.0F;
                }

                float f = -this.strafe / vel;
                float f1 = -this.forward / vel;
                float f2 = GameMath.sin(GameMath.degreesToRadians(this.yaw));
                float f3 = GameMath.cos(GameMath.degreesToRadians(this.yaw));
                f4 = f * f3;
                f5 = -f1 * f2;
                f6 = f * f2;
                f7 = f1 * f3;
            }

            float f8 = GameMath.degreesToRadians(-this.pitch);
            float f = GameMath.cos(f8);
            float f1 = -GameMath.sin(f8) * Math.signum(-this.forward);
            float f2 = f5 * f + f4;
            float f3 = GameMath.sqrtf(f5 * f5 + f7 * f7) * f1 + var7;
            float f9 = f7 * f + f6;
            float f10 = GameMath.sqrtf(GameMath.sqrtf(f2 * f2 + f9 * f9) + f3 * f3);
            if (f10 > 0.01F)
            {
                float f11 = maxSpeed / f10;
                this.mot.x += (double)(f2 * f11);
                this.mot.y += (double)(f3 * f11);
                this.mot.z += (double)(f9 * f11);
            }
        } else {
            if(vel > 0.01F)
            {
                if (vel < 1) vel = 1;
                float f = maxSpeed / vel;
                float f1 = -this.forward * f;
                float f2 = -this.strafe * f;
                float f3 = GameMath.sin(GameMath.degreesToRadians(this.yaw));
                float f4 = GameMath.cos(GameMath.degreesToRadians(this.yaw));
                this.mot.x += f1 * f4 - f2 * f3;
                this.mot.z += f2 * f4 + f1 * f3;
            }
        }
        super.tickUpdate();
        float slowdown = 0.28F;
        this.mot.x *= slowdown;
        this.mot.y *= slowdown;
        this.mot.z *= slowdown;
    }

}
