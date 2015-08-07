package nidefawl.game.entity;

import nidefawl.engine.vec.Vec3;
import nidefawl.game.input.Movement;

public abstract class Entity {
    public int id;
	public Vec3 pos = new Vec3();
	public Vec3 lastPos = new Vec3();
	public Vec3 mot = new Vec3();
	public Vec3 lastMot = new Vec3();
	public float yaw, lastYaw;
	public float pitch, lastPitch;
	public Entity(int id) {
		
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
        this.pos.x += this.mot.x;
        this.pos.y += this.mot.y;
        this.pos.z += this.mot.z;
	}
	
}
