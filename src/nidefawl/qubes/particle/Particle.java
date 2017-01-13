package nidefawl.qubes.particle;

import nidefawl.qubes.util.BlockColl;
import nidefawl.qubes.util.CollisionQuery;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.World;

public class Particle {
    boolean dead = false;
    int maxLive = 50;
    Vec3D mot, lastMot;
    Vec3D pos, lastPos;
    Vector3f renderPos;
    Vector3f renderRot;
    Vec3D rot, lastRot;
    Vec3D rotspeed;
    Vector2f texOffset;
    float size, initSize, lastSize, renderSize;
    int tick = 0;
    public boolean noclip;
    public boolean hitGround;
    final AABB aabb = new AABB(0, 0, 0, 0, 0, 0);
    final AABB aabb2 = new AABB(0, 0, 0, 0, 0, 0);
    final AABB aabb3 = new AABB(0, 0, 0, 0, 0, 0);
    final AABB aabb4 = new AABB(0, 0, 0, 0, 0, 0);
    final CollisionQuery coll = new CollisionQuery();
    float gravity = 0.98F;
    boolean applyGravity = true;
    int lightValue = 0xF0;
    int checkTicks = 0;
    int sleepTicks = 0;
    boolean sleeping = false;
    private int lastBlock;
    
    public Particle() {
        this.rotspeed = new Vec3D();
        this.renderRot = new Vector3f();
        this.rot = new Vec3D();
        this.lastRot = new Vec3D();
        this.renderPos = new Vector3f();
        this.pos = new Vec3D();
        this.lastPos = new Vec3D();
        this.mot = new Vec3D();
        this.lastMot = new Vec3D();
        this.texOffset = new Vector2f();
    }

    protected void die() {
        this.dead = true;
    }
    
    public void setMotion(float x, float y, float z) {
        this.mot.set(x, y, z);
        this.lastMot.set(x, y, z);
    }

    public void setPos(float x, float y, float z) {
        this.pos.set(x, y, z);
        this.lastPos.set(x, y, z);
        this.aabb.centerXZ(x, y, z);
        this.pos.x = aabb.getCenterX();
        this.pos.y = aabb.minY;
        this.pos.z = aabb.getCenterZ();
    }

    public void setRot(float x, float y, float z) {
        this.rot.set(x, y, z);
        this.lastRot.set(x, y, z);
    }

    public void setRotSpeed(float x, float y, float z) {
        this.rotspeed.set(x, y, z);
    }

    public void setSize(float size) {
        this.initSize = this.size = this.lastSize = this.renderSize = size;
        size *= 0.77f;
        this.aabb.set(-size/2.0D, 0, -size/2.0D, size/2.0D, size, size/2.0D);
    }
    
    public void update(float f) {
        Vector3f.interp(lastPos, pos, f, renderPos);
        Vector3f.interp(lastRot, rot, f, renderRot);
        renderSize = lastSize + (size - lastSize) * f;
    }

    public void tickUpdate(World world) {
        this.preStep();
         
        if (world != null && sleeping&&tick%4==0) {
            aabb2.set(this.aabb);
            aabb2.expandTo(this.mot.x, this.mot.y, this.mot.z);
            if (!this.coll.queryAnyCollisions(world, aabb2, 0.1f)) {
                sleeping = false;
                sleepTicks = 0;
                
            }
        } 
        if (!sleeping) {
            this.step(world);
            double mot = this.mot.lengthSquared();
            if (mot < 0.1) {
                sleepTicks++;
            } else {
                sleepTicks = 0;
            }
        }
        this.postStep();
        if (world != null && ((!sleeping&&sleepTicks > 4) || sleeping)) {
            int iX = GameMath.floor(this.pos.x);
            int iY = GameMath.floor(this.pos.y);
            int iZ = GameMath.floor(this.pos.z);
            int curBlock = world.getType(iX, iY, iZ);
            if (!sleeping) {
                sleeping = true;
                sleepTicks = 0;
                this.lastBlock = curBlock;
                mot.scale(0);
            } else if (curBlock != lastBlock) {
//                System.out.println("wakeup2");
                sleeping = false;
                sleepTicks = 0;
            }
        }
        if (!sleeping && world != null) {
            int iX = GameMath.floor(this.pos.x);
            int iY = GameMath.floor(this.pos.y+0.3f);
            int iZ = GameMath.floor(this.pos.z);
            this.lightValue  = world.getLight(iX, iY, iZ);
        }
    }
    
    protected void preStep() {
        lastSize = size;
        lastRot.set(rot);
        lastMot.set(mot);
        lastPos.set(pos);
    }
    protected void step(World world) {
        if (this.noclip||world==null) {
            aabb.offset(this.mot.x, this.mot.y, this.mot.z);
            this.pos.x = aabb.getCenterX();
            this.pos.y = aabb.minY;
            this.pos.z = aabb.getCenterZ();
            return;
        }
        double preX = this.mot.x;
        double preZ = this.mot.z;
        double mx = this.mot.x;
        double my = this.mot.y;
        double mz = this.mot.z;
        aabb3.set(this.aabb);
        aabb3.set(this.aabb);
        aabb2.set(this.aabb3);
        aabb2.expandTo(this.mot.x, this.mot.y, this.mot.z);
        
        this.coll.query(world, aabb2);
        int a = this.coll.getNumCollisions();
        int i = 0;
//
        for (i=0; i < a; i++) {
            BlockColl coll = this.coll.get(i);
            my = coll.blockBB.getYOffset(this.aabb, my);
        }
        this.aabb.offset(0, my, 0);
        for (i=0; i < a; i++) {
            BlockColl coll = this.coll.get(i);
            mx = coll.blockBB.getXOffset(this.aabb, mx);
        }
        this.aabb.offset(mx, 0, 0);
        for (i=0; i < a; i++) {
            BlockColl coll = this.coll.get(i);
            mz = coll.blockBB.getZOffset(this.aabb, mz);
        }
        this.aabb.offset(0, 0, mz);
        if ((this.hitGround || (this.mot.y != my && this.mot.y < 0)) && (this.mot.x != mx||this.mot.z != mz)) {
            double tmpx1 = mx;
            double tmpy1 = my;
            double tmpz1 = mz;
            mx = this.mot.x;
            my = 0.5f;
            mz = this.mot.z;
            aabb4.set(aabb);
            aabb.set(aabb3);
            aabb2.set(aabb3);
            aabb2.expandTo(mx, my, mz);
            this.coll.query(world, aabb2);
             a = this.coll.getNumCollisions();
            for (i=0; i < a; i++) {
                BlockColl coll = this.coll.get(i);
                my = coll.blockBB.getYOffset(this.aabb, my);
            }
            this.aabb.offset(0, my, 0);
            for (i=0; i < a; i++) {
                BlockColl coll = this.coll.get(i);
                mx = coll.blockBB.getXOffset(this.aabb, mx);
            }
            this.aabb.offset(mx, 0, 0);
            for (i=0; i < a; i++) {
                BlockColl coll = this.coll.get(i);
                mz = coll.blockBB.getZOffset(this.aabb, mz);
            }
            this.aabb.offset(0, 0, mz);
            if (my > 0) {
                my *= -1;
                for (i=0; i < a; i++) {
                    BlockColl coll = this.coll.get(i);
                    my = coll.blockBB.getYOffset(this.aabb, my);
                }
                this.aabb.offset(0, my, 0);
            }
            if ((tmpx1*tmpx1+tmpz1*tmpz1)-(mx*mx+mz*mz)>=0){
                mx = tmpx1;
                my = tmpy1;
                mz = tmpz1;
                this.aabb.set(aabb4);
            }
        }
        
        if (this.mot.x != mx) {
            this.mot.x = 0;
        }

        if (this.mot.z != mz) {
            this.mot.z = 0;
        }
        this.hitGround = this.mot.y != my && this.mot.y < 0;
        if (this.mot.y != my) {
            this.mot.y = 0;
        }
        
//        aabb.offset(this.mot.x, this.mot.y, this.mot.z);
        this.pos.x = aabb.getCenterX();
        this.pos.y = aabb.minY;
        this.pos.z = aabb.getCenterZ();
//        double xd = this.pos.x-this.lastPos.x;
//        double zd = this.pos.z-this.lastPos.z;
//        double dist = xd*xd+zd*zd;
//        if (dist > 0) {
//            distanceMoved += GameMath.sqrtf((float) dist);    
//        }
        
    }
    protected void postStep() {
        float slowdown = 0.68F;
        float f = -getGravity();
        float fn = 0.09F;
        this.mot.x *= slowdown;
        this.mot.z *= slowdown;
        boolean fall = doesFall();
        if (fall) {
            this.mot.y = this.mot.y*(1F-fn)+f*fn;
        } else {
            this.mot.y *= slowdown;
        }
//        float fx = (float) (this.pos.x - this.lastPos.x);
//        float fz = (float) (this.pos.z - this.lastPos.z);
//        float dist = fx*fx+fz*fz;
    }
    public boolean doesFall() {
        return applyGravity;
    }

    public float getGravity() {
        return this.gravity;
    }


}
