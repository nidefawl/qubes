package nidefawl.qubes.vec;

import java.nio.FloatBuffer;

import nidefawl.qubes.util.GameMath;

public class Vec3D {
    public double x, y, z;

    public Vec3D() {
    }

    public Vec3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3D(Vec3D v) {
        this.set(v);
    }

    public BlockPos toBlock() {
        return new BlockPos(GameMath.floor(this.x), GameMath.floor(this.y), GameMath.floor(this.z));
    }

    public void set(Vec3D v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double lengthSquared() {
        return (this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public Vec3D translate(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public static Vec3D add(Vec3D left, Vec3D right, Vec3D dest) {
        if (dest == null) {
            return new Vec3D(left.x + right.x, left.y + right.y, left.z + right.z);
        }
        dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
        return dest;
    }

    public static Vec3D sub(Vec3D left, Vec3D right, Vec3D dest) {
        if (dest == null) {
            return new Vec3D(left.x - right.x, left.y - right.y, left.z - right.z);
        }
        dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
        return dest;
    }

    public static Vec3D cross(Vec3D left, Vec3D right, Vec3D dest) {
        if (dest == null) {
            dest = new Vec3D();
        }
        dest.set(left.y * right.z - (left.z * right.y), right.x * left.z - (right.z * left.x), left.x * right.y - (left.y * right.x));

        return dest;
    }

    public Vec3D negate() {
        this.x = (-this.x);
        this.y = (-this.y);
        this.z = (-this.z);
        return this;
    }

    public Vec3D negate(Vec3D dest) {
        if (dest == null)
            dest = new Vec3D();
        dest.x = (-this.x);
        dest.y = (-this.y);
        dest.z = (-this.z);
        return dest;
    }

    public Vec3D normalise(Vec3D dest) {
        double l = length();

        if (dest == null)
            dest = new Vec3D(this.x / l, this.y / l, this.z / l);
        else {
            dest.set(this.x / l, this.y / l, this.z / l);
        }
        return dest;
    }

    public static double dot(Vec3D left, Vec3D right) {
        return (left.x * right.x + left.y * right.y + left.z * right.z);
    }

    public static double angle(Vec3D a, Vec3D b) {
        double dls = dot(a, b) / a.length() * b.length();
        if (dls < -1.0F)
            dls = -1.0F;
        else if (dls > 1.0F)
            dls = 1.0F;
        return (double) Math.acos(dls);
    }

    public Vec3D load(FloatBuffer buf) {
        this.x = buf.get();
        this.y = buf.get();
        this.z = buf.get();
        return this;
    }

    public Vec3D scale(double scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;

        return this;
    }

    public Vec3D store(FloatBuffer buf) {
        buf.put((float) this.x);
        buf.put((float) this.y);
        buf.put((float) this.z);
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

    public final double getX() {
        return this.x;
    }

    public final double getY() {
        return this.y;
    }

    public final void setX(double x) {
        this.x = x;
    }

    public final void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getZ() {
        return this.z;
    }

    public final double length() {
        return (double) Math.sqrt(lengthSquared());
    }

    public final Vec3D normalise() {
        double len = length();
        if (len != 0.0F) {
            double l = 1.0F / len;
            return scale(l);
        }
        throw new IllegalStateException("vec with len <= 0");
    }

    public Vec3D offset(double x, double y, double z) {
        return new Vec3D(this.x+x, this.y+y, this.z+z);
    }

    public void set(Vector3f pos) {
        set(pos.x, pos.y, pos.z);
    }

}
