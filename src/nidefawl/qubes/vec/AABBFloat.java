package nidefawl.qubes.vec;

import nidefawl.qubes.util.RayTrace;

public class AABBFloat {
    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;
    public AABBFloat(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public AABBFloat() {
    }

    public float getWidth() {
        return this.maxX-this.minX;
    }
    public float getHeight() {
        return this.maxY-this.minY;
    }
    public float getLength() {
        return this.maxZ-this.minZ;
    }
    
    public void offset(float x, float y, float z) {
        this.minX += x;
        this.maxX += x;
        this.minY += y;
        this.maxY += y;
        this.minZ += z;
        this.maxZ += z;
    }
    
    public void expandTo(float x, float y, float z) {
        if (x < 0)
        this.minX += x;
        if (x > 0)
        this.maxX += x;
        if (y < 0)
        this.minY += y;
        if (y > 0)
        this.maxY += y;
        if (z < 0)
        this.minZ += z;
        if (z > 0)
        this.maxZ += z;
    }

    
    public void expand(float x, float y, float z) {
        this.minX -= x;
        this.maxX += x;
        this.minY -= y;
        this.maxY += y;
        this.minZ -= z;
        this.maxZ += z;
    }
    
    public void set(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }
    public AABBFloat copy() {
        return new AABBFloat(minX, minY, minZ, maxX, maxY, maxZ);
    }
    public void set(AABBFloat b) {
        this.set(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }
    
    public float getCenterX() {
        return this.minX+getWidth()/2.0F;
    }
    
    public float getCenterY() {
        return this.minY+getHeight()/2.0F;
    }
    
    public float getCenterZ() {
        return this.minZ+getLength()/2.0F;
    }

    public void centerXZ(float x, float y, float z) {
        float w = getWidth()/2.0F;
        float l = getLength()/2.0F;
        float h = getHeight();
        set(x-w, y, z-l, x+w, y+h, z+l);
    }

    public boolean intersects(AABBFloat b) {
        if (b.maxX < this.minX) return false;
        if (b.maxY < this.minY) return false;
        if (b.maxZ < this.minZ) return false;
        if (this.maxX < b.minX) return false;
        if (this.maxY < b.minY) return false;
        if (this.maxZ < b.minZ) return false;
        return true;
    }
    /**
     * @param aabb
     * @return
     */
    public boolean intersects(AABB b) {
        if (b.maxX < this.minX) return false;
        if (b.maxY < this.minY) return false;
        if (b.maxZ < this.minZ) return false;
        if (this.maxX < b.minX) return false;
        if (this.maxY < b.minY) return false;
        if (this.maxZ < b.minZ) return false;
        return true;
    }


    @Override
    public String toString() {
        return "AABB["+String.format("%.2f %.2f %.2f - %.2f %.2f %.2f", minX, minY, minZ, maxX, maxY, maxZ)+"]";
    }

    /**
     * Ray-AABB collision test
     * taken from http://gamedev.stackexchange.com/a/18459
     * Adapted to figure out side we collide with
     * dirFrac is 1.0f / direction (with fixed denormals/NaN/Inf)
     * @param rayTrace 
     * 
     * @param origin
     * @param dirfrac (1.0f / direction) 
     * @param dirFrac2 
     * @return 
     */
    public boolean raytrace(RayTrace rayTrace, Vector3f origin, Vector3f direction, Vector3f dirfrac) {
        float t1 = (this.minX - origin.x)*dirfrac.x;
        float t2 = (this.maxX - origin.x)*dirfrac.x;
        float t3 = (this.minY - origin.y)*dirfrac.y;
        float t4 = (this.maxY - origin.y)*dirfrac.y;
        float t5 = (this.minZ - origin.z)*dirfrac.z;
        float t6 = (this.maxZ - origin.z)*dirfrac.z;

        float tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        // if tmax < 0, ray (line) is intersecting AABB, but whole AABB is behing us
        if (tmax < 0) {
            return false;
        }
        
        float minX = t1;
        float minY = t3;
        float minZ = t5;
        int iMinX = Dir.DIR_NEG_X;
        int iMinY = Dir.DIR_NEG_Y;
        int iMinZ = Dir.DIR_NEG_Z;
        if (min(t2, minX)) {
            minX = t2;
            iMinX = Dir.DIR_POS_X;
        }
        if (min(t4, minY)) {
            minY = t4;
            iMinY = Dir.DIR_POS_Y;
        }
        if (min(t6, minZ)) {
            minZ = t6;
            iMinZ = Dir.DIR_POS_Z;
        }
        
        float tmin = minX;
        int iMin = iMinX;
        if (max(minY, tmin)) {
            tmin = minY;
            iMin = iMinY;
        }
        if (max(minZ, tmin)) {
            tmin = minZ;
            iMin = iMinZ;
        }

        // if tmin > tmax, ray doesn't intersect AABB
        if (tmin > tmax)
        {
            return false;
        }
        
        rayTrace.setIntersection(origin, direction, tmin, iMin);
        return true;
    }
    //Min / Max functions copied from java.lang.Math
    private static long negativeZeroFloatBits = Float.floatToIntBits(-0.0f);
    public static boolean min(float a, float b) {
        if (a != a) return true;   // a is NaN
        if ((a == 0.0f) && (b == 0.0f)
            && (Float.floatToIntBits(b) == negativeZeroFloatBits)) {
            return false;
        }
        return (a <= b);
    }
    public static boolean max(float a, float b) {
        if (a != a) return true;   // a is NaN
        if ((a == 0.0f) && (b == 0.0f)
            && (Float.floatToIntBits(a) == negativeZeroFloatBits)) {
            return false;
        }
        return (a >= b);
    }

}

