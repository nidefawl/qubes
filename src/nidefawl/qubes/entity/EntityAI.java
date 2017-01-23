package nidefawl.qubes.entity;

import nidefawl.qubes.entity.ai.AIMove;
import nidefawl.qubes.entity.ai.AINav;
import nidefawl.qubes.entity.ai.TaskManager;
import nidefawl.qubes.util.GameMath;

public abstract class EntityAI extends Entity {

    TaskManager taskManager = new TaskManager(this);
    AINav nav = new AINav(this);
    AIMove move = new AIMove(this);
    private float moveForward;
    public boolean jump;
    private float landMovementFactor = 0.1f;
    private float airMovementFactor = 0.02f;

    public EntityAI(boolean isServerEntity) {
        super(isServerEntity);
    }
    
    
    @Override
    public void tickUpdate() {
        if (isServerSide) {
            this.taskManager.update();
//            super.tickUpdate();
//            this.animUpdate();
            this.preStep();
            this.step();
            this.nav.update();
            this.move.update();
            this.postStep();
        } else {
            this.lastYaw = this.yaw;
//            System.out.println(yawBodyOffset );
            this.lastYawBodyOffset = this.yawBodyOffset;
            this.lastPitch = this.pitch;
            this.lastMot.set(this.mot);
            this.lastPos.set(this.pos);
            this.prevDistanceMoved = this.distanceMoved;
//            this.lastYaw = this.yaw;
//            this.lastPitch = this.pitch;
//            this.lastMot.set(this.mot);
//            this.lastPos.set(this.pos);
            if (this.posticks>0) {
                if (this.posticks==1) {
                    this.pos.set(this.remotePos);
                } else {
                    this.pos.x+=(this.remotePos.x-this.pos.x)/3;
                    this.pos.y+=(this.remotePos.y-this.pos.y)/3;
                    this.pos.z+=(this.remotePos.z-this.pos.z)/3;
                }
                this.posticks--;
            }
            if (this.rotticks>0) {
                if (this.rotticks==1) {
                    this.yaw = this.remoteRotation.x;
                    this.yawBodyOffset = this.remoteRotation.y;
                    this.pitch = this.remoteRotation.z;
                } else {
                    this.yaw+=(this.remoteRotation.x-this.yaw)/3;
                    this.yawBodyOffset+=(this.remoteRotation.y-this.yawBodyOffset)/3;
                    this.pitch+=(this.remoteRotation.z-this.pitch)/3;
                }
                this.rotticks--;
            }
        }
    }

    @Override
    protected void preStep() {
        super.preStep();
        this.moveForward*=0.98f;
        float f2 = 0.91F;
        if (this.hitGround) {
            f2 = 0.546f;
        }
        float f3 = 0.16277136F / (f2*f2*f2);
        final float f4 = this.hitGround ? this.landMovementFactor*f3 : this.airMovementFactor;
        setMotion(0, this.moveForward, f4);
        f2 = 0.91f;
    }

    void setMotion(float strafe, float forward, float maxSpeed) {
        float speed = GameMath.sqrtf(strafe*strafe+forward*forward);
        if (speed >= 0.01F) {
            if (speed < 1.0F)
                speed = 1.0f;
            speed = maxSpeed / speed;
            strafe *= speed;
            forward *= speed;
            float f1 = this.yaw-this.yawBodyOffset;
            float f4 = -GameMath.sin(f1*GameMath.PI_OVER_180);
            float f5 = -GameMath.cos(f1*GameMath.PI_OVER_180);
            this.mot.x+=(strafe*f5-forward*f4);
            this.mot.z+=(forward*f5+strafe*f4);
        }
    }


    public AINav getNav() {
        return this.nav;
    }


    public AIMove getMove() {
        return this.move;
    }
    


    public void setMoveForward(float f) {
        this.moveForward=f;
    }
}
