package nidefawl.qubes.entity;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.BlockColl;
import nidefawl.qubes.util.CollisionQuery;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.Vec3;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.world.World;

public abstract class Entity {
    public int id;
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
    
	public Entity(int id) {
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
            this.pos.x += this.mot.x;
            this.pos.y += this.mot.y;
            this.pos.z += this.mot.z;
            return;
        }
        this.mot.y -= getGravity();
//        Engine.worldRenderer.debugBBs.clear();
        dbg.set(aabb);
//        aabb.set(dbg);
        aabb2.set(this.dbg);
        aabb2.expandTo(this.mot.x, this.mot.y, this.mot.z);
        this.coll.query(this.world, aabb2);
        int a = this.coll.getNumCollisions();

        double mx = this.mot.x;
        double my = this.mot.y;
        double mz = this.mot.z;
        if (a > 0) {
            int i = 0;
            for (; i < a; i++) {
                BlockColl coll = this.coll.get(i);
                my = coll.blockBB.getYOffset(this.dbg, my);
            }
            dbg.offset(0, my, 0);
            for (i=0; i < a; i++) {
                BlockColl coll = this.coll.get(i);
                mx = coll.blockBB.getXOffset(this.dbg, mx);
            }
            dbg.offset(mx, 0, 0);
            for (i=0; i < a; i++) {
                BlockColl coll = this.coll.get(i);
                mz = coll.blockBB.getZOffset(this.dbg, mz);
            }
            dbg.offset(0, 0, mz);
        } else {
        }

        if (this.mot.x != mx)
            this.mot.x = 0;
        this.hitGround = this.mot.y != my && this.mot.y < 0;
        if (this.mot.y != my) {
            this.mot.y = 0;
            this.pos.y = dbg.minY;
        } else {

            this.pos.y += this.mot.y;
        }
        if (this.mot.z != mz)
            this.mot.z = 0;
        this.pos.x += this.mot.x;
        this.pos.z += this.mot.z;
        aabb.centerXZ(this.pos.x, this.pos.y, this.pos.z);
    }

    public float getGravity() {
        return 0.98F;
    }

    public void move(float x, float y, float z) {
        this.pos.x = x;
        this.pos.y = y;
        this.pos.z = z;
        this.lastPos.set(this.pos);
        this.aabb.centerXZ(x, y, z);
    }
	
}
