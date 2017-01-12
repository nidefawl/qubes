package nidefawl.qubes.entity.ai;

import nidefawl.qubes.entity.EntityAI;
import nidefawl.qubes.util.GameMath;

public class AIMove {
    private EntityAI entity;
    private double x;
    private double y;
    private double z;
    private float speed;
    private boolean needsUpdate;
    public AIMove(EntityAI entity) {
        this.entity = entity;
    }

    public void moveTowards(double x, double y, double z, float speed) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.speed = speed;
        this.needsUpdate = true;
    }
    public void update() {
        entity.setMoveForward(0);
        if (!this.needsUpdate) {
            return;
        }
        needsUpdate = false;
        double xD = this.x-this.entity.pos.x;
        double yD = this.y-GameMath.floor(this.entity.getAabb().minY+0.5D);
        double zD = this.z-this.entity.pos.z;
        double d = xD*xD+yD*yD+zD*zD;
        if (d < 1.0E-5D) {
            return;
        }
        float f = (GameMath.atan2((float)zD, (float)xD)*180F/GameMath.PI)+90F;
//        entity.yawBodyOffset=clampAngle(entity.yawBodyOffset, f, 30);
        entity.yaw=clampAngle(entity.yaw, f, 30);
        entity.setMoveForward(this.speed);
//        entity.setMoveForward(0);
//        System.out.println("yaw "+entity.yaw);
        if (yD > 0.0D && xD*xD+zD*zD < 1.0D) {
            entity.jump=true;
        }
    }

    private float clampAngle(float yaw, float f, float f2) {
        float f3 = GameMath.wrapAngle(f-yaw);
        f3 = GameMath.clamp(f3, -f2, f2);
        return yaw+f3;
    }
}
