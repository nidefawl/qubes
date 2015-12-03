package nidefawl.qubes.entity;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.util.BlockColl;
import nidefawl.qubes.util.CollisionQuery;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.World;

public abstract class Entity {
    static int NEXT_ENT_ID=0;
    public int id = ++NEXT_ENT_ID;
    public World world;
	public Vec3D pos = new Vec3D();
	public Vec3D lastPos = new Vec3D();
	public Vec3D mot = new Vec3D();
	public Vec3D lastMot = new Vec3D();
	public float yaw, lastYaw;
    public float yawBodyOffset, lastYawBodyOffset;
	public float pitch, lastPitch;
	public boolean noclip;
    public boolean hitGround;
    final AABB aabb = new AABB(0, 0, 0, 0, 0, 0);
    final AABB aabb2 = new AABB(0, 0, 0, 0, 0, 0);
    AABB dbg = new AABB();
    final CollisionQuery coll = new CollisionQuery();
    public Vector3f renderPos = new Vector3f();
    public Vector3f renderRot = new Vector3f();
	
    private double width;
    private double height;
    private double length;
    
    //debug vars
    public float yawfloat1, yawfloat2;
    public float yawfloat3, yawfloat4;
    public int ticks1, ticks2, ticks3;
    
	public Entity() {
		this.width = 0.8D;
		this.height = 1.6D;
		this.length = 0.8D;
		this.aabb.set(-width/2.0D, 0, -length/2.0D, width/2.0D, height, length/2.0D);
	}
	
	@Override
	public int hashCode() {
		return this.id;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Entity ? ((Entity)obj).id == this.id : false;
	}
	
	public void tickUpdate() {
	    if (this.yaw > 360)
	        this.yaw -= 360;
	    if (this.yaw < 0)
	        this.yaw += 360;
        this.lastYaw = this.yaw;
        this.lastYawBodyOffset = this.yawBodyOffset;
        this.lastPitch = this.pitch;
        this.lastMot.set(this.mot);
        this.lastPos.set(this.pos);
        this.step();
	}

    protected void step() {
        if (this.noclip) {
            aabb.offset(this.mot.x, this.mot.y, this.mot.z);
            this.pos.x = aabb.getCenterX();
            this.pos.y = aabb.minY;
            this.pos.z = aabb.getCenterZ();
            return;
        }
//        Engine.worldRenderer.debugBBs.clear();
//        Engine.worldRenderer.debugBBs.put(0, aabb);
//        dbg.set(aabb);
////        aabb.set(dbg);
        aabb2.set(this.aabb);
        aabb2.expandTo(this.mot.x, this.mot.y, this.mot.z);
//        Engine.worldRenderer.debugBBs.put(1, aabb2);
        this.coll.query(this.world, aabb2);
        int a = this.coll.getNumCollisions();
        int i = 0;
//        for (; i < a; i++) {
//            BlockColl coll = this.coll.get(i);
//            Engine.worldRenderer.debugBBs.put(2+i, coll.blockBB);
//        }
//
        int y1=GameMath.floor(aabb.minY);
        double mx = this.mot.x;
        double my = this.mot.y;
        double mz = this.mot.z;
        int n = 2;
        for (i=0; i < a; i++) {
            BlockColl coll = this.coll.get(i);
            my = coll.blockBB.getYOffset(this.aabb, my);
        }
        this.aabb.offset(0, my, 0);
        for (i=0; i < a; i++) {
            BlockColl coll = this.coll.get(i);
            mx = coll.blockBB.getXOffset(this.aabb, mx);
//            if (coll.y == y1)
//              Engine.worldRenderer.debugBBs.put(n++, coll.blockBB);
                
        }
        this.aabb.offset(mx, 0, 0);
        for (i=0; i < a; i++) {
            BlockColl coll = this.coll.get(i);
            mz = coll.blockBB.getZOffset(this.aabb, mz);
        }
        this.aabb.offset(0, 0, mz);
        
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
    }

    public float getGravity() {
        return 0.98F;
    }

    public void move(Vec3D v) {
        move(v.x, v.y, v.z);
    }

    public void move(Vector3f v) {
        move(v.x, v.y, v.z);
    }
    public void move(double x, double y, double z) {
        this.pos.x = x;
        this.pos.y = y;
        this.pos.z = z;
        this.lastPos.set(this.pos);
        this.aabb.centerXZ(x, y, z);
    }
    /**
     * @param yaw
     * @param pitch
     */
    public void setYawPitch(float yaw, float pitch) {
        this.yaw = yaw;
        this.yawBodyOffset = 0;
        this.pitch = pitch;
    }

    /**
     * @return
     */
    public Tag writeClientData(boolean isUpdate) {
        return null;
    }

    public void readClientData(Tag tag) {
    }

    public abstract EntityType getEntityType();


    /**
     * @return
     */
    public int getLookDir() {
        float f = (this.yaw%360.0F)/90.0f;
        int dir = GameMath.floor(f+0.5f)&3;
        if (dir == 1)
            return Dir.DIR_POS_Z;
        if (dir == 2)
            return Dir.DIR_NEG_X;
        if (dir == 3)
            return Dir.DIR_NEG_Z;
        return Dir.DIR_POS_X;
    }

    public World getWorld() {
        return this.world;
    }

    /**
     * @param i
     * @param fTime
     * @return 
     */
    public Vector3f getRenderPos(float fTime) {
        float px = (float) (this.lastPos.x + (this.pos.x - this.lastPos.x) * fTime) + 0;
        float py = (float) (this.lastPos.y + (this.pos.y - this.lastPos.y) * fTime) + 0;
        float pz = (float) (this.lastPos.z + (this.pos.z - this.lastPos.z) * fTime) + 0;
        renderPos.set(px, py, pz);
        return renderPos;
    }

    /**
     * @param i
     * @param fTime
     * @return 
     */
    public Vector3f getRenderRot(float fTime) {
        float difHeadYaw = (this.yawBodyOffset - this.lastYawBodyOffset);
        difHeadYaw = difHeadYaw > 180 ? -(360-difHeadYaw) : difHeadYaw;
        difHeadYaw = difHeadYaw < -180 ? (360+difHeadYaw) : difHeadYaw;
        float eHeadYaw = (float) (this.lastYawBodyOffset + (difHeadYaw) * fTime) + 0;
        float difYaw = (this.yaw - this.lastYaw);
        difYaw = difYaw > 180 ? -(360-difYaw) : difYaw;
        difYaw = difYaw < -180 ? (360+difYaw) : difYaw;
        float eYaw = (float) (this.lastYaw + (difYaw) * fTime) + 0;
        float difPitch = (this.pitch - this.lastPitch);
        difPitch = difPitch > 180 ? -(360-difPitch) : difPitch;
        difPitch = difPitch < -180 ? (360+difPitch) : difPitch;
        float ePitch = (float) (this.lastPitch + (difPitch) * fTime) + 0;
        renderRot.set(eHeadYaw, eYaw, ePitch);
        return renderRot;
    }
}
