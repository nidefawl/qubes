package nidefawl.qubes.path;

import nidefawl.qubes.entity.EntityAI;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TripletIntHash;
import nidefawl.qubes.util.TripletLongHash;
import nidefawl.qubes.vec.Vec3D;

public class PathPoint {

    public int x;
    public int y;
    public int z;
    public float totalPathDistance;
    public float distanceToNext;
    public float distanceToTarget;
    public boolean isFirst;
    public int pos;
    public PathPoint previous;

    public PathPoint(int x, int y, int z) {
        this.x=x;
        this.y=y;
        this.z=z;
        this.pos = -1;
    }

    public float distanceTo(PathPoint end) {
        float f = GameMath.distSq3Di(x, y, z, end.x, end.y, end.z);
        return f < 0.01f? 0: GameMath.sqrtf(f);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PathPoint) {
            PathPoint p2 = (PathPoint)obj;
            return this.x==p2.x&&this.y==p2.y&&this.z==p2.z;
        }
        return false;
    }
    @Override
    public int hashCode() {
        return TripletIntHash.toHash(x, y, z);
    }

    public boolean inUse() {
        return this.pos>=0;
    }

    public void getPosition(EntityAI entity, Vec3D to) {
        to.set(this.x+GameMath.ceil(entity.width)*0.5f, this.y, this.z+GameMath.ceil(entity.length)*0.5f);
    }
    
    @Override
    public String toString() {
        return "PathPoint["+x+","+y+","+z+",pos="+pos+"]";
    }

}
