package nidefawl.qubes.entity;

import java.util.Random;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.qmodel.QModelProperties;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.TagType;
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
    final AABB aabb3 = new AABB(0, 0, 0, 0, 0, 0);
    final AABB aabb4 = new AABB(0, 0, 0, 0, 0, 0);
    AABB dbg = new AABB();
    final CollisionQuery coll = new CollisionQuery();
    public Vector3f renderPos = new Vector3f();
    public Vector3f renderRot = new Vector3f();
	
	public Vec3D remotePos = new Vec3D();
    public Vector3f remoteRotation = new Vector3f();
    int rotticks, posticks;
    public double width;
    public double height;
    public double length;
    
    //debug vars
    public float yawfloat1, yawfloat2;
    public float yawfloat3, yawfloat4;
    public int ticks1, ticks2, ticks3;
    public float   timeJump;
    public float   timePunch;
    public boolean flagRemove = false;
    Random random = new Random();
    protected EntityProperties properties;
    public BaseStack[] equipment = new BaseStack[0];
    
	public Entity() {
        this.width = getEntityType().getWidth();
        this.height = getEntityType().getHeight();
        this.length = getEntityType().getLength();
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
	    this.preStep();
        this.step();
        this.postStep();
	}
    public boolean doesFall() {
        return !doesFly() && this.world.getChunk(GameMath.floor(this.pos.x)>>4, GameMath.floor(this.pos.z)>>4) != null;
    }
    public boolean doesFly() {
        return false;
    }
    protected void postStep() {
        float slowdown = 0.28F;
        float f = -getGravity();
        float fn = 0.11F;
        this.mot.x *= slowdown;
        this.mot.z *= slowdown;
        boolean fall = doesFall();
        if (fall) {
            this.mot.y = this.mot.y*(1F-fn)+f*fn;
        } else {
            this.mot.y *= slowdown;
        }
    }
    protected void preStep() {
        this.lastYaw = this.yaw;
        this.lastYawBodyOffset = this.yawBodyOffset;
        this.lastPitch = this.pitch;
        this.lastMot.set(this.mot);
        this.lastPos.set(this.pos);
        this.yaw = GameMath.wrapAngle(this.yaw);
        this.yawBodyOffset = GameMath.wrapAngle(this.yawBodyOffset);
        this.pitch = GameMath.wrapAngle(this.pitch);
    }
    protected void step() {
        if (this.noclip) {
            aabb.offset(this.mot.x, this.mot.y, this.mot.z);
            this.pos.x = aabb.getCenterX();
            this.pos.y = aabb.minY;
            this.pos.z = aabb.getCenterZ();
            return;
        }
//        if (this.getEntityType()==EntityType.CAT)
//            System.out.println("cat "+this.pos);
//        boolean debug = GameContext.getSide()==Side.CLIENT;
//        if (debug) {
//            Engine.worldRenderer.debugBBs.clear();
//            Engine.worldRenderer.debugBBs.put(0, aabb);
//            dbg.set(aabb);
//        }
////        aabb.set(dbg);
        aabb3.set(this.aabb);
        aabb2.set(this.aabb3);
        aabb2.expandTo(this.mot.x, this.mot.y, this.mot.z);
//        if (debug) {
//          Engine.worldRenderer.debugBBs.put(1, aabb2);
//        }
        this.coll.query(this.world, aabb2);
        int a = this.coll.getNumCollisions();
        int i = 0;
//        if (debug) {
//            for (; i < a; i++) {
//                BlockColl coll = this.coll.get(i);
//                Engine.worldRenderer.debugBBs.put(2+i, coll.blockBB);
//            }
//        }
//
        double mx = this.mot.x;
        double my = this.mot.y;
        double mz = this.mot.z;
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
            my = 1;
            mz = this.mot.z;
            aabb4.set(aabb);
            aabb.set(aabb3);
            aabb2.set(aabb3);
            aabb2.expandTo(mx, my, mz);
            this.coll.query(this.world, aabb2);
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
        if (!isUpdate && this.properties != null) {
            Tag.Compound tag = new Tag.Compound();
            tag.set("properties", this.properties.save());
            return tag;
        }
        return null;
    }


    public void readClientData(Tag tag) {
        if (tag.getType() == TagType.COMPOUND) {
            Tag.Compound compound = (Tag.Compound) tag;
//            this.name = compound.getString("name");
            Tag c = compound.get("properties");
            if (c != null)
                readProperties(c);
        }
    }
    public void readProperties(Tag tag) {
        if (tag.getType() == TagType.COMPOUND) {
            Tag.Compound compound = (Tag.Compound) tag;
            this.properties.load(compound);
        }
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

    /**
     * @param pos2
     */
    public void setRemotePos(Vec3D pos) {
        remotePos.set(pos);
        posticks=3;
    }

    /**
     * @param pitch2
     * @param yaw2
     * @param yawBodyOffset2
     */
    public void setRemoteRotation(float pitch, float yaw, float yawBodyOffset) {
        float diff = this.yaw-yaw;
        if (diff > 180) {
            this.yaw-=360;
            this.lastYaw-=360;
        } else if (diff < -180) {
            this.yaw+=360;
            this.lastYaw+=360;
        }
        diff = this.yawBodyOffset-yawBodyOffset;
        if (diff > 180) {
            this.yawBodyOffset-=360;
            this.lastYawBodyOffset-=360;
        } else if (diff < -180) {
            this.yawBodyOffset+=360;
            this.lastYawBodyOffset+=360;
        }
        diff = this.pitch-pitch;
        if (diff > 180) {
            this.pitch-=360;
            this.lastPitch-=360;
        } else if (diff < -180) {
            this.pitch+=360;
            this.lastPitch+=360;
        }
        remoteRotation.set(yaw, yawBodyOffset, pitch);
        rotticks=3;
    }

    public void remove() {
        this.flagRemove = true;
    }

    public Random getRandom() {
        return this.random;
    }

    public float getPathWeight(int rx, int ry, int rz) {
        return 0;
    }
    public AABB getAabb() {
        return this.aabb;
    }

    public void adjustRenderProps(QModelProperties renderProps, float fTime) {
        EntityProperties properties = this.properties;
        if (properties != null) {
            renderProps.setProperties(properties.properties);
        }
    }

    public EntityModel getEntityModel() {
        return null;
    }
    public EntityProperties getEntityProperties() {
        return this.properties;
    }
    public BaseStack getActiveItem(int i) {
        if (this.equipment.length > i) {
            return this.equipment[i];
        }
        return null;
    }

    public void setEquipment(BaseStack[] stacks) {
        System.out.println("setequp "+(stacks==null||stacks.length==0?"<null>":stacks[0]));
        this.equipment = stacks;
    }

}
