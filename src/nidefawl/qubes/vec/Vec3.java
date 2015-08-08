package nidefawl.qubes.vec;

import java.nio.FloatBuffer;

import nidefawl.qubes.util.GameMath;

public class Vec3 {
    public float x, y, z;

    public Vec3() {
    }

    public Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos toBlock() {
        return new BlockPos(GameMath.floor(this.x), GameMath.floor(this.y), GameMath.floor(this.z));
    }

    public void set(Vec3 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float lengthSquared() {
        return (this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public Vec3 translate(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public static Vec3 add(Vec3 left, Vec3 right, Vec3 dest) {
        if (dest == null) {
            return new Vec3(left.x + right.x, left.y + right.y, left.z + right.z);
        }
        dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
        return dest;
    }

    public static Vec3 sub(Vec3 left, Vec3 right, Vec3 dest) {
        if (dest == null) {
            return new Vec3(left.x - right.x, left.y - right.y, left.z - right.z);
        }
        dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
        return dest;
    }

    public static Vec3 cross(Vec3 left, Vec3 right, Vec3 dest) {
        if (dest == null) {
            dest = new Vec3();
        }
        dest.set(left.y * right.z - (left.z * right.y), right.x * left.z - (right.z * left.x), left.x * right.y - (left.y * right.x));

        return dest;
    }

    public Vec3 negate() {
        this.x = (-this.x);
        this.y = (-this.y);
        this.z = (-this.z);
        return this;
    }

    public Vec3 negate(Vec3 dest) {
        if (dest == null)
            dest = new Vec3();
        dest.x = (-this.x);
        dest.y = (-this.y);
        dest.z = (-this.z);
        return dest;
    }

    public Vec3 normalise(Vec3 dest) {
        float l = length();

        if (dest == null)
            dest = new Vec3(this.x / l, this.y / l, this.z / l);
        else {
            dest.set(this.x / l, this.y / l, this.z / l);
        }
        return dest;
    }

    public static float dot(Vec3 left, Vec3 right) {
        return (left.x * right.x + left.y * right.y + left.z * right.z);
    }

    public static float angle(Vec3 a, Vec3 b) {
        float dls = dot(a, b) / a.length() * b.length();
        if (dls < -1.0F)
            dls = -1.0F;
        else if (dls > 1.0F)
            dls = 1.0F;
        return (float) Math.acos(dls);
    }

    public Vec3 load(FloatBuffer buf) {
        this.x = buf.get();
        this.y = buf.get();
        this.z = buf.get();
        return this;
    }

    public Vec3 scale(float scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;

        return this;
    }

    public Vec3 store(FloatBuffer buf) {
        buf.put(this.x);
        buf.put(this.y);
        buf.put(this.z);

        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append("Vec3[");
        sb.append(this.x);
        sb.append(", ");
        sb.append(this.y);
        sb.append(", ");
        sb.append(this.z);
        sb.append(']');
        return sb.toString();
    }

    public final float getX() {
        return this.x;
    }

    public final float getY() {
        return this.y;
    }

    public final void setX(float x) {
        this.x = x;
    }

    public final void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getZ() {
        return this.z;
    }

    public final float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public final Vec3 normalise() {
        float len = length();
        if (len != 0.0F) {
            float l = 1.0F / len;
            return scale(l);
        }
        throw new IllegalStateException("vec with len <= 0");
    }
}
