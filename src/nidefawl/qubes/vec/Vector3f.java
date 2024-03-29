package nidefawl.qubes.vec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.FloatBuffer;

import nidefawl.qubes.network.StreamIO;
import nidefawl.qubes.util.DumbPool;

public class Vector3f implements StreamIO, IVec3 {
    
    final public static Vector3f ZERO = new Vector3f();
    final public static Vector3f ONE = new Vector3f(1);

    final private static DumbPool<Vector3f> pool = new DumbPool<Vector3f>(Vector3f.class);
    
    public static Vector3f pool() {
        return pool.get();
    }
    public static Vector3f pool(Vector3f f) {
        Vector3f v3f = pool.get();
        v3f.set(f);
        return v3f;
    }
    public static Vector3f pool(float x, float y, float z) {
        Vector3f v3f = pool.get();
        v3f.set(x, y, z);
        return v3f;
    }
    
    public float x, y, z;

    /**
     * Constructor for Vector3f.
     */
    public Vector3f() {
        super();
    }

    /**
     * Constructor
     */
    public Vector3f(Vector3f src) {
        set(src.x, src.y, src.z);
    }

    /**
     * Constructor
     */
    public Vector3f(float x, float y, float z) {
        set(x, y, z);
    }

    public Vector3f(Vec3D pos) {
        set((float)pos.x, (float)pos.y, (float)pos.z);
    }

    public Vector3f(double d) {
        this((float)d, (float)d, (float)d);
    }

    /* (non-Javadoc)
     * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
     */
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /* (non-Javadoc)
     * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
     */
    public void set(Vector3f src) {
        set(src.x, src.y, src.z);
    }

    /* (non-Javadoc)
     * @see org.lwjgl.util.vector.WritableVector3f#set(float, float, float)
     */
    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public void add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
    }

    /**
     * @return the length squared of the vector
     */
    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    /**
     * Translate a vector
     * 
     * @param x
     *            The translation in x
     * @param y
     *            the translation in y
     * @return this
     */
    public Vector3f translate(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    /**
     * Add a vector to another vector and place the result in a destination vector.
     * 
     * @param left
     *            The LHS vector
     * @param right
     *            The RHS vector
     * @param dest
     *            The destination vector, or null if a new vector is to be created
     * @return the sum of left and right in dest
     */
    public static Vector3f add(Vector3f left, Vector3f right, Vector3f dest) {
        if (dest == null)
            return new Vector3f(left.x + right.x, left.y + right.y, left.z + right.z);
        else {
            dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
            return dest;
        }
    }

    /**
     * Subtract a vector from another vector and place the result in a destination vector.
     * 
     * @param left
     *            The LHS vector
     * @param right
     *            The RHS vector
     * @param dest
     *            The destination vector, or null if a new vector is to be created
     * @return left minus right in dest
     */
    public static Vector3f sub(Vector3f left, Vector3f right, Vector3f dest) {
        if (dest == null)
            return new Vector3f(left.x - right.x, left.y - right.y, left.z - right.z);
        else {
            dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
            return dest;
        }
    }

    /**
     * The cross product of two vectors.
     *
     * @param left
     *            The LHS vector
     * @param right
     *            The RHS vector
     * @param dest
     *            The destination result, or null if a new vector is to be created
     * @return left cross right
     */
    public static Vector3f cross(Vector3f left, Vector3f right, Vector3f dest) {

        if (dest == null)
            dest = new Vector3f();

        dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);

        return dest;
    }

    /**
     * Negate a vector
     * 
     * @return this
     */
    public Vector3f negate() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /**
     * Negate a vector and place the result in a destination vector.
     * 
     * @param dest
     *            The destination vector or null if a new vector is to be created
     * @return the negated vector
     */
    public Vector3f negate(Vector3f dest) {
        if (dest == null)
            dest = new Vector3f();
        dest.x = -x;
        dest.y = -y;
        dest.z = -z;
        return dest;
    }

    /**
     * Normalise this vector and place the result in another vector.
     * 
     * @param dest
     *            The destination vector, or null if a new vector is to be created
     * @return the normalised vector
     */
    public Vector3f normalise(Vector3f dest) {
        float l = length();

        if (dest == null)
            dest = new Vector3f(x / l, y / l, z / l);
        else
            dest.set(x / l, y / l, z / l);

        return dest;
    }

    /**
     * The dot product of two vectors is calculated as v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
     * 
     * @param left
     *            The LHS vector
     * @param right
     *            The RHS vector
     * @return left dot right
     */
    public static float dot(Vector3f left, Vector3f right) {
        return left.x * right.x + left.y * right.y + left.z * right.z;
    }

    /**
     * Calculate the angle between two vectors, in radians
     * 
     * @param a
     *            A vector
     * @param b
     *            The other vector
     * @return the angle between the two vectors, in radians
     */
    public static float angle(Vector3f a, Vector3f b) {
        float dls = dot(a, b) / (a.length() * b.length());
        if (dls < -1f)
            dls = -1f;
        else if (dls > 1.0f)
            dls = 1.0f;
        return (float) Math.acos(dls);
    }

    /* (non-Javadoc)
     * @see org.lwjgl.vector.Vector#load(FloatBuffer)
     */
    public Vector3f load(FloatBuffer buf) {
        x = buf.get();
        y = buf.get();
        z = buf.get();
        return this;
    }

    /* (non-Javadoc)
     * @see org.lwjgl.vector.Vector#scale(float)
     */
    public Vector3f scale(float scale) {

        x *= scale;
        y *= scale;
        z *= scale;

        return this;

    }

    /* (non-Javadoc)
     * @see org.lwjgl.vector.Vector#store(FloatBuffer)
     */
    public Vector3f store(FloatBuffer buf) {

        buf.put(x);
        buf.put(y);
        buf.put(z);

        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append("Vector3f[");
        sb.append(x);
        sb.append(", ");
        sb.append(y);
        sb.append(", ");
        sb.append(z);
        sb.append(']');
        return sb.toString();
    }

    /**
     * @return x
     */
    public final float getX() {
        return x;
    }

    /**
     * @return y
     */
    public final float getY() {
        return y;
    }

    /**
     * Set X
     * 
     * @param x
     */
    public final void setX(float x) {
        this.x = x;
    }

    /**
     * Set Y
     * 
     * @param y
     */
    public final void setY(float y) {
        this.y = y;
    }

    /**
     * Set Z
     * 
     * @param z
     */
    public void setZ(float z) {
        this.z = z;
    }

    /* (Overrides)
     * @see org.lwjgl.vector.ReadableVector3f#getZ()
     */
    public float getZ() {
        return z;
    }

    /**
     * @return the length of the vector
     */
    public final float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    /**
     * Normalise this vector
     * 
     * @return this
     */
    public final Vector3f normalise() {
        float len = length();
        if (len != 0.0f) {
            float l = 1.0f / len;
            return scale(l);
        } else
            throw new IllegalStateException("Zero length vector");
    }

    public final Vector3f normaliseZero() {
        float len = length();
        if (len > 1E-8F)
            scale(1.0f / len);
        else
            set(0, 0, 0);
        return this;
    }

    public Vector3f normaliseNull() {
        float len = length();
        if (len > 1E-8F) {
            float l = 1.0f / len;
            return scale(l);
        }
        return null;
    }

    public Vector3f scaleN(float f) {
        return new Vector3f(this).scale(f);
    }

    /**
     * @param v
     */
    public void set(Vec3D v) {
        set((float) v.x, (float) v.y, (float) v.z);
    }

    /**
     * @param dir
     */
    public void addVec(Vector3f dir) {
        this.x+=dir.x;
        this.y+=dir.y;
        this.z+=dir.z;
    }

    /**
     * @param camX
     * @param camY
     * @param camZ
     * @return
     */
    public float distanceSq(float camX, float camY, float camZ) {
        camX -= this.x;
        camY -= this.y;
        camZ -= this.z;
        return camX*camX+camY*camY+camZ*camZ;
    }
    public float distance(Vector3f other) {
        Vector3f tmp = pool();
        tmp.x = this.x - other.x;
        tmp.y = this.y - other.y;
        tmp.z = this.z - other.z;
        return tmp.x*tmp.x+tmp.y*tmp.y+tmp.z*tmp.z;
    }
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeFloat(this.x);
        out.writeFloat(this.y);
        out.writeFloat(this.z);
    }
    @Override
    public void read(DataInput in) throws IOException {
        this.x = in.readFloat();
        this.y = in.readFloat();
        this.z = in.readFloat();
    }

    /**
     * @return
     */
    public Vector3f copy() {
        return new Vector3f(this);
    }

    /**
     * @param param
     */
    public void subtract(Vector3f param) {
        Vector3f.sub(this, param, this);
    }

    public static void interp(Vector3f a, Vector3f b, float f, Vector3f to) {
        to.set(
                a.x+(b.x-a.x)*f,
                a.y+(b.y-a.y)*f,
                a.z+(b.z-a.z)*f
                );
    }
    public static void interp(Vec3D a, Vec3D b, float f, Vector3f to) {
        to.set(
                (float)(a.x+(b.x-a.x)*f),
                (float)(a.y+(b.y-a.y)*f),
                (float)(a.z+(b.z-a.z)*f)
                );
    }

    public void setElement(int i, float f) {
        switch (i) {
            case 0:
                this.x = f;
                break;
            case 1:
                this.y = f;
                break;
            case 2:
                this.z = f;
                break;
        }
    }

    public Vector3f add(IVec3 v) {
        this.x += v.x();
        this.y += v.y();
        this.z += v.z();
        return this;
    }

    public float x() {
        return x;
    }
    public float y() {
        return y;
    }
    public float z() {
        return z;
    }
}
