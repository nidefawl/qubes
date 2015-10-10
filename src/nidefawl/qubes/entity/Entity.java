package nidefawl.qubes.entity;

import nidefawl.qubes.util.BlockColl;
import nidefawl.qubes.util.CollisionQuery;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;
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
	public float pitch, lastPitch;
	public boolean noclip;
    final AABB aabb = new AABB(0, 0, 0, 0, 0, 0);
    final AABB aabb2 = new AABB(0, 0, 0, 0, 0, 0);
    final CollisionQuery coll = new CollisionQuery();
	
    private double width;
    private double height;
    private double length;
    
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
        this.lastPitch = this.pitch;
        this.lastMot.set(this.mot);
        this.lastPos.set(this.pos);
        this.step();
	}

    AABB dbg = new AABB();
    public boolean hitGround;
    private void step() {
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
        this.pitch = pitch;
    }

	
}
