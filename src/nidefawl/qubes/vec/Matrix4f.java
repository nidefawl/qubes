package nidefawl.qubes.vec;

import java.nio.FloatBuffer;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.DumbPool;
import nidefawl.qubes.util.GameMath;

public class Matrix4f {

    final private static DumbPool<Matrix4f> pool = new DumbPool<Matrix4f>(Matrix4f.class);

    public static Matrix4f pool() {
        return pool.get();
    }
    public static Matrix4f poolZero() {
        return pool.get().setZero();
    }
    public static Matrix4f poolIdentity() {
        return pool.get().setIdentity();
    }
    public static Matrix4f pool(Matrix4f f) {
        Matrix4f v3f = pool.get();
        v3f.load(f);
        return v3f;
    }

    public float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;

    /**
     * Construct a new matrix, initialized to the identity.
     */
    public Matrix4f() {
        super();
        setIdentity();
    }

    public Matrix4f(final Matrix4f src) {
        super();
        load(src);
    }

    public Matrix4f(Vector4f q1, Vector3f t1, float s)
    {
      this.m00 = ((float)(s * (1.0D - 2.0D * q1.getY() * q1.getY() - 2.0D * q1.getZ() * q1.getZ())));
      this.m10 = ((float)(s * (2.0D * (q1.getX() * q1.getY() + q1.getW() * q1.getZ()))));
      this.m20 = ((float)(s * (2.0D * (q1.getX() * q1.getZ() - q1.getW() * q1.getY()))));

      this.m01 = ((float)(s * (2.0D * (q1.getX() * q1.getY() - q1.getW() * q1.getZ()))));
      this.m11 = ((float)(s * (1.0D - 2.0D * q1.getX() * q1.getX() - 2.0D * q1.getZ() * q1.getZ())));
      this.m21 = ((float)(s * (2.0D * (q1.getY() * q1.getZ() + q1.getW() * q1.getX()))));

      this.m02 = ((float)(s * (2.0D * (q1.getX() * q1.getZ() + q1.getW() * q1.getY()))));
      this.m12 = ((float)(s * (2.0D * (q1.getY() * q1.getZ() - q1.getW() * q1.getX()))));
      this.m22 = ((float)(s * (1.0D - 2.0D * q1.getX() * q1.getX() - 2.0D * q1.getY() * q1.getY())));

      this.m03 = t1.getX();
      this.m13 = t1.getY();
      this.m23 = t1.getZ();

      this.m30 = 0.0F;
      this.m31 = 0.0F;
      this.m32 = 0.0F;
      this.m33 = 1.0F;
    }
    /**
     * Returns a string representation of this matrix
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(m00).append(' ').append(m10).append(' ').append(m20).append(' ').append(m30).append('\n');
        buf.append(m01).append(' ').append(m11).append(' ').append(m21).append(' ').append(m31).append('\n');
        buf.append(m02).append(' ').append(m12).append(' ').append(m22).append(' ').append(m32).append('\n');
        buf.append(m03).append(' ').append(m13).append(' ').append(m23).append(' ').append(m33).append('\n');
        return buf.toString();
    }
    public String toStringShort() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("%.2f",m00)).append(' ').append(String.format("%.2f",m10)).append(' ').append(String.format("%.2f",m20)).append(' ').append(String.format("%.2f",m30)).append('\n');
        buf.append(String.format("%.2f",m01)).append(' ').append(String.format("%.2f",m11)).append(' ').append(String.format("%.2f",m21)).append(' ').append(String.format("%.2f",m31)).append('\n');
        buf.append(String.format("%.2f",m02)).append(' ').append(String.format("%.2f",m12)).append(' ').append(String.format("%.2f",m22)).append(' ').append(String.format("%.2f",m32)).append('\n');
        buf.append(String.format("%.2f",m03)).append(' ').append(String.format("%.2f",m13)).append(' ').append(String.format("%.2f",m23)).append(' ').append(String.format("%.2f",m33)).append('\n');
        return buf.toString();
    }

    /**
     * Set this matrix to be the identity matrix.
     * @return this
     */
    public Matrix4f setIdentity() {
        return setIdentity(this);
    }

    /**
     * Set the given matrix to be the identity matrix.
     * @param m The matrix to set to the identity
     * @return m
     */
    public static Matrix4f setIdentity(Matrix4f m) {
        m.m00 = 1.0f;//0
        m.m01 = 0.0f;
        m.m02 = 0.0f;
        m.m03 = 0.0f;
        m.m10 = 0.0f; //4
        m.m11 = 1.0f;//5
        m.m12 = 0.0f;
        m.m13 = 0.0f;
        m.m20 = 0.0f;
        m.m21 = 0.0f;
        m.m22 = 1.0f;//10
        m.m23 = 0.0f;
        m.m30 = 0.0f;//12
        m.m31 = 0.0f;
        m.m32 = 0.0f;
        m.m33 = 1.0f;//15

        return m;
    }

    /**
     * Set this matrix to 0.
     * @return this
     */
    public Matrix4f setZero() {
        return setZero(this);
    }

    /**
     * Set the given matrix to 0.
     * @param m The matrix to set to 0
     * @return m
     */
    public static Matrix4f setZero(Matrix4f m) {
        m.m00 = 0.0f;
        m.m01 = 0.0f;
        m.m02 = 0.0f;
        m.m03 = 0.0f;
        m.m10 = 0.0f;
        m.m11 = 0.0f;
        m.m12 = 0.0f;
        m.m13 = 0.0f;
        m.m20 = 0.0f;
        m.m21 = 0.0f;
        m.m22 = 0.0f;
        m.m23 = 0.0f;
        m.m30 = 0.0f;
        m.m31 = 0.0f;
        m.m32 = 0.0f;
        m.m33 = 0.0f;

        return m;
    }

    /**
     * Load from another matrix4f
     * @param src The source matrix
     * @return this
     */
    public Matrix4f load(Matrix4f src) {
        return load(src, this);
    }

    /**
     * Copy the source matrix to the destination matrix
     * @param src The source matrix
     * @param dest The destination matrix, or null of a new one is to be created
     * @return The copied matrix
     */
    public static Matrix4f load(Matrix4f src, Matrix4f dest) {
        if (dest == null)
            dest = new Matrix4f();
        dest.m00 = src.m00;
        dest.m01 = src.m01;
        dest.m02 = src.m02;
        dest.m03 = src.m03;
        dest.m10 = src.m10;
        dest.m11 = src.m11;
        dest.m12 = src.m12;
        dest.m13 = src.m13;
        dest.m20 = src.m20;
        dest.m21 = src.m21;
        dest.m22 = src.m22;
        dest.m23 = src.m23;
        dest.m30 = src.m30;
        dest.m31 = src.m31;
        dest.m32 = src.m32;
        dest.m33 = src.m33;

        return dest;
    }

    /**
     * Load from a float buffer. The buffer stores the matrix in column major
     * (OpenGL) order.
     *
     * @param buf A float buffer to read from
     * @return this
     */
    public Matrix4f load(FloatBuffer buf) {

        m00 = buf.get();
        m01 = buf.get();
        m02 = buf.get();
        m03 = buf.get();
        m10 = buf.get();
        m11 = buf.get();
        m12 = buf.get();
        m13 = buf.get();
        m20 = buf.get();
        m21 = buf.get();
        m22 = buf.get();
        m23 = buf.get();
        m30 = buf.get();
        m31 = buf.get();
        m32 = buf.get();
        m33 = buf.get();

        return this;
    }
    /**
     * @param mat
     * @return 
     */
    public Matrix4f load(float[] mat) {
        int i = 0;
        m00 = mat[i++];
        m10 = mat[i++];
        m20 = mat[i++];
        m30 = mat[i++];
        m01 = mat[i++];
        m11 = mat[i++];
        m21 = mat[i++];
        m31 = mat[i++];
        m02 = mat[i++];
        m12 = mat[i++];
        m22 = mat[i++];
        m32 = mat[i++];
        m03 = mat[i++];
        m13 = mat[i++];
        m23 = mat[i++];
        m33 = mat[i++];

        return this;
    }

    /**
     * Load from a float buffer. The buffer stores the matrix in row major
     * (maths) order.
     *
     * @param buf A float buffer to read from
     * @return this
     */
    public Matrix4f loadTranspose(FloatBuffer buf) {

        m00 = buf.get();
        m10 = buf.get();
        m20 = buf.get();
        m30 = buf.get();
        m01 = buf.get();
        m11 = buf.get();
        m21 = buf.get();
        m31 = buf.get();
        m02 = buf.get();
        m12 = buf.get();
        m22 = buf.get();
        m32 = buf.get();
        m03 = buf.get();
        m13 = buf.get();
        m23 = buf.get();
        m33 = buf.get();

        return this;
    }

    /**
     * Store this matrix in a float buffer. The matrix is stored in column
     * major (openGL) order.
     * @param buf The buffer to store this matrix in
     */
    public Matrix4f store(FloatBuffer buf) {
        buf.put(m00);
        buf.put(m01);
        buf.put(m02);
        buf.put(m03);
        buf.put(m10);
        buf.put(m11);
        buf.put(m12);
        buf.put(m13);
        buf.put(m20);
        buf.put(m21);
        buf.put(m22);
        buf.put(m23);
        buf.put(m30);
        buf.put(m31);
        buf.put(m32);
        buf.put(m33);
        return this;
    }

    /**
     * Store this matrix in a float buffer. The matrix is stored in column
     * major (openGL) order.
     * @param buf The buffer to store this matrix in
     */
    public Matrix4f store(float[] buf) {
        return store(buf, 0);
    }
    public Matrix4f store(float[] buf, int pos) {
        int idx = pos;
        buf[idx++] = (m00);
        buf[idx++] = (m01);
        buf[idx++] = (m02);
        buf[idx++] = (m03);
        buf[idx++] = (m10);
        buf[idx++] = (m11);
        buf[idx++] = (m12);
        buf[idx++] = (m13);
        buf[idx++] = (m20);
        buf[idx++] = (m21);
        buf[idx++] = (m22);
        buf[idx++] = (m23);
        buf[idx++] = (m30);
        buf[idx++] = (m31);
        buf[idx++] = (m32);
        buf[idx++] = (m33);
        return this;
    }

    /**
     * Store this matrix in a float buffer. The matrix is stored in row
     * major (maths) order.
     * @param buf The buffer to store this matrix in
     */
    public Matrix4f storeTranspose(FloatBuffer buf) {
        buf.put(m00);
        buf.put(m10);
        buf.put(m20);
        buf.put(m30);
        buf.put(m01);
        buf.put(m11);
        buf.put(m21);
        buf.put(m31);
        buf.put(m02);
        buf.put(m12);
        buf.put(m22);
        buf.put(m32);
        buf.put(m03);
        buf.put(m13);
        buf.put(m23);
        buf.put(m33);
        return this;
    }

    /**
     * Store the rotation portion of this matrix in a float buffer. The matrix is stored in column
     * major (openGL) order.
     * @param buf The buffer to store this matrix in
     */
    public Matrix4f store3f(FloatBuffer buf) {
        buf.put(m00);
        buf.put(m01);
        buf.put(m02);
        buf.put(m10);
        buf.put(m11);
        buf.put(m12);
        buf.put(m20);
        buf.put(m21);
        buf.put(m22);
        return this;
    }

    /**
     * Add two matrices together and place the result in a third matrix.
     * @param left The left source matrix
     * @param right The right source matrix
     * @param dest The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static Matrix4f add(Matrix4f left, Matrix4f right, Matrix4f dest) {
        if (dest == null)
            dest = new Matrix4f();

        dest.m00 = left.m00 + right.m00;
        dest.m01 = left.m01 + right.m01;
        dest.m02 = left.m02 + right.m02;
        dest.m03 = left.m03 + right.m03;
        dest.m10 = left.m10 + right.m10;
        dest.m11 = left.m11 + right.m11;
        dest.m12 = left.m12 + right.m12;
        dest.m13 = left.m13 + right.m13;
        dest.m20 = left.m20 + right.m20;
        dest.m21 = left.m21 + right.m21;
        dest.m22 = left.m22 + right.m22;
        dest.m23 = left.m23 + right.m23;
        dest.m30 = left.m30 + right.m30;
        dest.m31 = left.m31 + right.m31;
        dest.m32 = left.m32 + right.m32;
        dest.m33 = left.m33 + right.m33;

        return dest;
    }

    /**
     * Subtract the right matrix from the left and place the result in a third matrix.
     * @param left The left source matrix
     * @param right The right source matrix
     * @param dest The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static Matrix4f sub(Matrix4f left, Matrix4f right, Matrix4f dest) {
        if (dest == null)
            dest = new Matrix4f();

        dest.m00 = left.m00 - right.m00;
        dest.m01 = left.m01 - right.m01;
        dest.m02 = left.m02 - right.m02;
        dest.m03 = left.m03 - right.m03;
        dest.m10 = left.m10 - right.m10;
        dest.m11 = left.m11 - right.m11;
        dest.m12 = left.m12 - right.m12;
        dest.m13 = left.m13 - right.m13;
        dest.m20 = left.m20 - right.m20;
        dest.m21 = left.m21 - right.m21;
        dest.m22 = left.m22 - right.m22;
        dest.m23 = left.m23 - right.m23;
        dest.m30 = left.m30 - right.m30;
        dest.m31 = left.m31 - right.m31;
        dest.m32 = left.m32 - right.m32;
        dest.m33 = left.m33 - right.m33;

        return dest;
    }

    /**
     * Multiply the right matrix by the left and place the result in a third matrix.
     * @param left The left source matrix
     * @param right The right source matrix
     * @param dest The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static Matrix4f mul(Matrix4f left, Matrix4f right, Matrix4f dest) {
        if (dest == null)
            dest = new Matrix4f();

        float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
        float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
        float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
        float m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
        float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
        float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
        float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
        float m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
        float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
        float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
        float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
        float m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
        float m30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
        float m31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
        float m32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
        float m33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;

        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m03 = m03;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m13 = m13;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;
        dest.m23 = m23;
        dest.m30 = m30;
        dest.m31 = m31;
        dest.m32 = m32;
        dest.m33 = m33;

        return dest;
    }

    /**
     * Transform a Vector by a matrix and return the result in a destination
     * vector.
     * @param left The left matrix
     * @param right The right vector
     * @param dest The destination vector, or null if a new one is to be created
     * @return the destination vector
     */
    public static Vector4f transform(Matrix4f left, Vector4f right, Vector4f dest) {
        if (dest == null)
            dest = new Vector4f();

        float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30 * right.w;
        float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31 * right.w;
        float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32 * right.w;
        float w = left.m03 * right.x + left.m13 * right.y + left.m23 * right.z + left.m33 * right.w;

        dest.x = x;
        dest.y = y;
        dest.z = z;
        dest.w = w;

        return dest;
    }

    /**
     * Transform a Vector by a matrix and return the result in a destination
     * vector.
     * @param left The left matrix
     * @param right The right vector
     * @param dest The destination vector, or null if a new one is to be created
     * @return the destination vector
     */
    public static Vector3f transform(Matrix4f left, Vector3f right, Vector3f dest) {
        if (dest == null)
            dest = new Vector3f();

        float right_w = 1;
        float x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30 * right_w;
        float y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31 * right_w;
        float z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32 * right_w;
        float w = left.m03 * right.x + left.m13 * right.y + left.m23 * right.z + left.m33 * right_w;

        dest.x = x / w;
        dest.y = y / w;
        dest.z = z / w;

        return dest;
    }
    public final void transformVec(Vector3f normal)
    {
        transform(this, normal, normal);
    }

    /**
     * Transform a Vector by a matrix and return the result in a destination
     * vector.
     * @param left The left matrix
     * @param right The right vector
     * @param dest The destination vector, or null if a new one is to be created
     * @return the destination vector
     */
    public static Vector4f transformTransposed(Matrix4f left, Vector4f right, Vector4f dest) {
        if (dest == null)
            dest = new Vector4f();

        float x = left.m00 * right.x + left.m01 * right.y + left.m02 * right.z + left.m03 * right.w;
        float y = left.m10 * right.x + left.m11 * right.y + left.m12 * right.z + left.m13 * right.w;
        float z = left.m20 * right.x + left.m21 * right.y + left.m22 * right.z + left.m23 * right.w;
        float w = left.m30 * right.x + left.m31 * right.y + left.m32 * right.z + left.m33 * right.w;

        dest.x = x;
        dest.y = y;
        dest.z = z;
        dest.w = w;

        return dest;
    }

    /**
     * Transform a Vector by a matrix and return the result in a destination
     * vector.
     * @param left The left matrix
     * @param right The right vector
     * @param dest The destination vector, or null if a new one is to be created
     * @return the destination vector
     */
    public static Vector3f transformTransposed(Matrix4f left, Vector3f right, Vector3f dest) {
        if (dest == null)
            dest = new Vector3f();
        float right_w = 1;
        float x = left.m00 * right.x + left.m01 * right.y + left.m02 * right.z + left.m03 * right_w;
        float y = left.m10 * right.x + left.m11 * right.y + left.m12 * right.z + left.m13 * right_w;
        float z = left.m20 * right.x + left.m21 * right.y + left.m22 * right.z + left.m23 * right_w;
        float w = left.m30 * right.x + left.m31 * right.y + left.m32 * right.z + left.m33 * right_w;

        dest.x = x / w;
        dest.y = y / w;
        dest.z = z / w;

        return dest;
    }

    /**
     * Transpose this matrix
     * @return this
     */
    public Matrix4f transpose() {
        return transpose(this);
    }

    /**
     * Translate this matrix
     * @param vec The vector to translate by
     * @return this
     */
    public Matrix4f translate(float x, float y, float z) {
        return translate(x, y, z, this);
    }

    /**
     * Translate this matrix
     * @param vec The vector to translate by
     * @return this
     */
    public Matrix4f translate(Vector3f v) {
        return translate(v.x, v.y, v.z, this);
    }

    /**
     * Scales this matrix
     * @param vec The vector to scale by
     * @return this
     */
    public Matrix4f scale(Vector3f vec) {
        return scale(vec, this, this);
    }

    /**
     * Scales this matrix
     * @param f The vector to scale by
     * @return this
     */
    public Matrix4f scale(float s) {
        return scale(s, s, s);
    }
    public Matrix4f scale(float x, float y, float z) {
        this.m00 = this.m00 * x;
        this.m01 = this.m01 * x;
        this.m02 = this.m02 * x;
        this.m03 = this.m03 * x;
        this.m10 = this.m10 * y;
        this.m11 = this.m11 * y;
        this.m12 = this.m12 * y;
        this.m13 = this.m13 * y;
        this.m20 = this.m20 * z;
        this.m21 = this.m21 * z;
        this.m22 = this.m22 * z;
        this.m23 = this.m23 * z;
        return this;
    }

    /**
     * Scales the source matrix and put the result in the destination matrix
     * @param vec The vector to scale by
     * @param src The source matrix
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return The scaled matrix
     */
    public static Matrix4f scale(Vector3f vec, Matrix4f src, Matrix4f dest) {
        if (dest == null)
            dest = new Matrix4f();
        dest.m00 = src.m00 * vec.x;
        dest.m01 = src.m01 * vec.x;
        dest.m02 = src.m02 * vec.x;
        dest.m03 = src.m03 * vec.x;
        dest.m10 = src.m10 * vec.y;
        dest.m11 = src.m11 * vec.y;
        dest.m12 = src.m12 * vec.y;
        dest.m13 = src.m13 * vec.y;
        dest.m20 = src.m20 * vec.z;
        dest.m21 = src.m21 * vec.z;
        dest.m22 = src.m22 * vec.z;
        dest.m23 = src.m23 * vec.z;
        return dest;
    }

    /**
     * Rotates the matrix around the given axis the specified angle
     * @param angle the angle, in radians.
     * @param axis The vector representing the rotation axis. Must be normalized.
     * @return this
     */
    public Matrix4f rotate(float angle, float x, float y, float z) {
        return rotate(angle, x, y, z, this);
    }

    /**
     * Rotates the matrix around the given axis the specified angle
     * @param angle the angle, in radians.
     * @param axis The vector representing the rotation axis. Must be normalized.
     * @param dest The matrix to put the result, or null if a new matrix is to be created
     * @return The rotated matrix
     */
    public Matrix4f rotate(float angle, float x, float y, float z, Matrix4f dest) {
        return rotate(angle, x, y, z, this, dest);
    }
    public static Matrix4f convertQuaternionToMatrix4f(Quaternion q)
    {
        Matrix4f matrix = new Matrix4f();
        matrix.m00 = 1.0f - 2.0f * ( q.getY() * q.getY() + q.getZ() * q.getZ() );
        matrix.m01 = 2.0f * (q.getX() * q.getY() + q.getZ() * q.getW());
        matrix.m02 = 2.0f * (q.getX() * q.getZ() - q.getY() * q.getW());
        matrix.m03 = 0.0f;
 
        matrix.m10 = 2.0f * ( q.getX() * q.getY() - q.getZ() * q.getW() );
        matrix.m11 = 1.0f - 2.0f * ( q.getX() * q.getX() + q.getZ() * q.getZ() );
        matrix.m12 = 2.0f * (q.getZ() * q.getY() + q.getX() * q.getW() );
        matrix.m13 = 0.0f;
 
        matrix.m20 = 2.0f * ( q.getX() * q.getZ() + q.getY() * q.getW() );
        matrix.m21 = 2.0f * ( q.getY() * q.getZ() - q.getX() * q.getW() );
        matrix.m22 = 1.0f - 2.0f * ( q.getX() * q.getX() + q.getY() * q.getY() );
        matrix.m23 = 0.0f;
 
        matrix.m30 = 0;
        matrix.m31 = 0;
        matrix.m32 = 0;
        matrix.m33 = 1.0f;
  
        return matrix;
    }
    /**
     * Builds a rotation matrix.
     * 
     * @param tup
     * 
     * @return itself
     */
    public final Matrix4f setRotation( Vector3f tup )
    {
        final float cx = GameMath.cos( tup.getX() );
        final float sx = GameMath.sin( tup.getX() );
        final float cy = GameMath.cos( tup.getY() );
        final float sy = GameMath.sin( tup.getY() );
        final float cz = GameMath.cos( tup.getZ() );
        final float sz = GameMath.sin( tup.getZ() );

        this.m00=( cy * cz );
        this.m01=( cy * sz );
        this.m02=( -sy );
        
        this.m10=( sx * sy * cz - cx * sz );
        this.m11=( sx * sy * sz + cx * cz );
        this.m12=( sx * cy );
        
        this.m20=( cx * sy * cz + sx * sz );
        this.m21=( cx * sy * sz - sx * cz );
        this.m22=( cx * cy );

        this.m33=( 1.0f );
        
        return ( this );
    }
    /**
     * Rotates the source matrix around the given axis the specified angle and
     * put the result in the destination matrix.
     * @param angle the angle, in radians.
     * @param axis The vector representing the rotation axis. Must be normalized.
     * @param src The matrix to rotate
     * @param dest The matrix to put the result, or null if a new matrix is to be created
     * @return The rotated matrix
     */
    public static Matrix4f rotate(float angle, float x, float y, float z, Matrix4f src, Matrix4f dest) {
        if (dest == null)
            dest = new Matrix4f();
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        float oneminusc = 1.0f - c;
        float xy = x*y;
        float yz = y*z;
        float xz = x*z;
        float xs = x*s;
        float ys = y*s;
        float zs = z*s;

        float f00 = x*x*oneminusc+c;
        float f01 = xy*oneminusc+zs;
        float f02 = xz*oneminusc-ys;
        // n[3] not used
        float f10 = xy*oneminusc-zs;
        float f11 = y*y*oneminusc+c;
        float f12 = yz*oneminusc+xs;
        // n[7] not used
        float f20 = xz*oneminusc+ys;
        float f21 = yz*oneminusc-xs;
        float f22 = z*z*oneminusc+c;

        float t00 = src.m00 * f00 + src.m10 * f01 + src.m20 * f02;
        float t01 = src.m01 * f00 + src.m11 * f01 + src.m21 * f02;
        float t02 = src.m02 * f00 + src.m12 * f01 + src.m22 * f02;
        float t03 = src.m03 * f00 + src.m13 * f01 + src.m23 * f02;
        float t10 = src.m00 * f10 + src.m10 * f11 + src.m20 * f12;
        float t11 = src.m01 * f10 + src.m11 * f11 + src.m21 * f12;
        float t12 = src.m02 * f10 + src.m12 * f11 + src.m22 * f12;
        float t13 = src.m03 * f10 + src.m13 * f11 + src.m23 * f12;
        dest.m20 = src.m00 * f20 + src.m10 * f21 + src.m20 * f22;
        dest.m21 = src.m01 * f20 + src.m11 * f21 + src.m21 * f22;
        dest.m22 = src.m02 * f20 + src.m12 * f21 + src.m22 * f22;
        dest.m23 = src.m03 * f20 + src.m13 * f21 + src.m23 * f22;
        dest.m00 = t00;
        dest.m01 = t01;
        dest.m02 = t02;
        dest.m03 = t03;
        dest.m10 = t10;
        dest.m11 = t11;
        dest.m12 = t12;
        dest.m13 = t13;
        return dest;
    }

    /**
     * Translate this matrix and stash the result in another matrix
     * @param vec The vector to translate by
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return the translated matrix
     */
    public Matrix4f translate(float x, float y, float z, Matrix4f dest) {
        return translate(x, y, z, this, dest);
    }

    /**
     * Translate the source matrix and stash the result in the destination matrix
     * @param vec The vector to translate by
     * @param src The source matrix
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return The translated matrix
     */
    public static Matrix4f translate(float x, float y, float z, Matrix4f src, Matrix4f dest) {
        if (dest == null)
            dest = new Matrix4f();

        dest.m30 += src.m00 * x + src.m10 * y + src.m20 * z;
        dest.m31 += src.m01 * x + src.m11 * y + src.m21 * z;
        dest.m32 += src.m02 * x + src.m12 * y + src.m22 * z;
        dest.m33 += src.m03 * x + src.m13 * y + src.m23 * z;

        return dest;
    }

    /**
     * Transpose this matrix and place the result in another matrix
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return the transposed matrix
     */
    public Matrix4f transpose(Matrix4f dest) {
        return transpose(this, dest);
    }

    /**
     * Transpose the source matrix and place the result in the destination matrix
     * @param src The source matrix
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return the transposed matrix
     */
    public static Matrix4f transpose(Matrix4f src, Matrix4f dest) {
        if (dest == null)
           dest = new Matrix4f();
        float m00 = src.m00;
        float m01 = src.m10;
        float m02 = src.m20;
        float m03 = src.m30;
        float m10 = src.m01;
        float m11 = src.m11;
        float m12 = src.m21;
        float m13 = src.m31;
        float m20 = src.m02;
        float m21 = src.m12;
        float m22 = src.m22;
        float m23 = src.m32;
        float m30 = src.m03;
        float m31 = src.m13;
        float m32 = src.m23;
        float m33 = src.m33;

        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m03 = m03;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m13 = m13;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;
        dest.m23 = m23;
        dest.m30 = m30;
        dest.m31 = m31;
        dest.m32 = m32;
        dest.m33 = m33;

        return dest;
    }

    /**
     * @return the determinant of the matrix
     */
    public float determinant() {
        float f =
            m00
                * ((m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32)
                    - m13 * m22 * m31
                    - m11 * m23 * m32
                    - m12 * m21 * m33);
        f -= m01
            * ((m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32)
                - m13 * m22 * m30
                - m10 * m23 * m32
                - m12 * m20 * m33);
        f += m02
            * ((m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31)
                - m13 * m21 * m30
                - m10 * m23 * m31
                - m11 * m20 * m33);
        f -= m03
            * ((m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31)
                - m12 * m21 * m30
                - m10 * m22 * m31
                - m11 * m20 * m32);
        return f;
    }

    /**
     * Calculate the determinant of a 3x3 matrix
     * @return result
     */

    private static float determinant3x3(float t00, float t01, float t02,
                     float t10, float t11, float t12,
                     float t20, float t21, float t22)
    {
        return   t00 * (t11 * t22 - t12 * t21)
               + t01 * (t12 * t20 - t10 * t22)
               + t02 * (t10 * t21 - t11 * t20);
    }

    /**
     * Invert this matrix
     * @return this if successful, null otherwise
     */
    public Matrix4f invert() {
        return invert(this, this);
    }
    public Matrix4f invertDoublePrecision() {
        DoubleMatrix4 m4d = DoubleMatrix4.pool();
        m4d.load(this);
        m4d.invert();
        this.load(m4d);
        return this;
    }

    /**
     * Invert the source matrix and put the result in the destination
     * @param src The source matrix
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return The inverted matrix if successful, null otherwise
     */
    public static Matrix4f invert2(Matrix4f src, Matrix4f dest) {
        float values[] = new float[16];
        src.store(values);
        float determinant = src.determinant();

        if (determinant == 0) {
            dest.load(values);
            return dest;
        }
        determinant = 1.0f / determinant;
        dest.m00 = values[5] * (values[10] * values[15] - values[11] * values[14]) + values[6] * (values[11] * values[13] - values[9] * values[15])
                + values[7] * (values[9] * values[14] - values[10] * values[13]);
        dest.m01 = values[9] * (values[2] * values[15] - values[3] * values[14]) + values[10] * (values[3] * values[13] - values[1] * values[15])
                + values[11] * (values[1] * values[14] - values[2] * values[13]);
        dest.m02 = values[13] * (values[2] * values[7] - values[3] * values[6]) + values[14] * (values[3] * values[5] - values[1] * values[7])
                + values[15] * (values[1] * values[6] - values[2] * values[5]);
        dest.m03 = values[1] * (values[7] * values[10] - values[6] * values[11]) + values[2] * (values[5] * values[11] - values[7] * values[9])
                + values[3] * (values[6] * values[9] - values[5] * values[10]);
        dest.m10 = values[6] * (values[8] * values[15] - values[11] * values[12]) + values[7] * (values[10] * values[12] - values[8] * values[14])
                + values[4] * (values[11] * values[14] - values[10] * values[15]);
        dest.m11 = values[10] * (values[0] * values[15] - values[3] * values[12]) + values[11] * (values[2] * values[12] - values[0] * values[14])
                + values[8] * (values[3] * values[14] - values[2] * values[15]);
        dest.m12 = values[14] * (values[0] * values[7] - values[3] * values[4]) + values[15] * (values[2] * values[4] - values[0] * values[6])
                + values[12] * (values[3] * values[6] - values[2] * values[7]);
        dest.m13 = values[2] * (values[7] * values[8] - values[4] * values[11]) + values[3] * (values[4] * values[10] - values[6] * values[8])
                + values[0] * (values[6] * values[11] - values[7] * values[10]);
        dest.m20 = values[7] * (values[8] * values[13] - values[9] * values[12]) + values[4] * (values[9] * values[15] - values[11] * values[13])
                + values[5] * (values[11] * values[12] - values[8] * values[15]);
        dest.m21 = values[11] * (values[0] * values[13] - values[1] * values[12]) + values[8] * (values[1] * values[15] - values[3] * values[13])
                + values[9] * (values[3] * values[12] - values[0] * values[15]);
        dest.m22 = values[15] * (values[0] * values[5] - values[1] * values[4]) + values[12] * (values[1] * values[7] - values[3] * values[5])
                + values[13] * (values[3] * values[4] - values[0] * values[7]);
        dest.m23 = values[3] * (values[5] * values[8] - values[4] * values[9]) + values[0] * (values[7] * values[9] - values[5] * values[11])
                + values[1] * (values[4] * values[11] - values[7] * values[8]);
        dest.m30 = values[4] * (values[10] * values[13] - values[9] * values[14]) + values[5] * (values[8] * values[14] - values[10] * values[12])
                + values[6] * (values[9] * values[12] - values[8] * values[13]);
        dest.m32 = values[8] * (values[2] * values[13] - values[1] * values[14]) + values[9] * (values[0] * values[14] - values[2] * values[12])
                + values[10] * (values[1] * values[12] - values[0] * values[13]);
        dest.m32 = values[12] * (values[2] * values[5] - values[1] * values[6]) + values[13] * (values[0] * values[6] - values[2] * values[4])
                + values[14] * (values[1] * values[4] - values[0] * values[5]);
        dest.m33 = values[0] * (values[5] * values[10] - values[6] * values[9]) + values[1] * (values[6] * values[8] - values[4] * values[10])
                + values[2] * (values[4] * values[9] - values[5] * values[8]);
        dest.mulFloat(determinant);
        return dest;
    }
    public static Matrix4f invert(Matrix4f src, Matrix4f dest) {
        float determinant = src.determinant();

        if (determinant != 0) {
            /*
             * m00 m01 m02 m03
             * m10 m11 m12 m13
             * m20 m21 m22 m23
             * m30 m31 m32 m33
             */
            if (dest == null)
                dest = new Matrix4f();
            float determinant_inv = 1f/determinant;

            // first row
            float t00 =  determinant3x3(src.m11, src.m12, src.m13, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
            float t01 = -determinant3x3(src.m10, src.m12, src.m13, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
            float t02 =  determinant3x3(src.m10, src.m11, src.m13, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
            float t03 = -determinant3x3(src.m10, src.m11, src.m12, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
            // second row
            float t10 = -determinant3x3(src.m01, src.m02, src.m03, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
            float t11 =  determinant3x3(src.m00, src.m02, src.m03, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
            float t12 = -determinant3x3(src.m00, src.m01, src.m03, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
            float t13 =  determinant3x3(src.m00, src.m01, src.m02, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
            // third row
            float t20 =  determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m31, src.m32, src.m33);
            float t21 = -determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m30, src.m32, src.m33);
            float t22 =  determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m30, src.m31, src.m33);
            float t23 = -determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m30, src.m31, src.m32);
            // fourth row
            float t30 = -determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m21, src.m22, src.m23);
            float t31 =  determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m20, src.m22, src.m23);
            float t32 = -determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m20, src.m21, src.m23);
            float t33 =  determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m20, src.m21, src.m22);

            // transpose and divide by the determinant
            dest.m00 = t00*determinant_inv;
            dest.m11 = t11*determinant_inv;
            dest.m22 = t22*determinant_inv;
            dest.m33 = t33*determinant_inv;
            dest.m01 = t10*determinant_inv;
            dest.m10 = t01*determinant_inv;
            dest.m20 = t02*determinant_inv;
            dest.m02 = t20*determinant_inv;
            dest.m12 = t21*determinant_inv;
            dest.m21 = t12*determinant_inv;
            dest.m03 = t30*determinant_inv;
            dest.m30 = t03*determinant_inv;
            dest.m13 = t31*determinant_inv;
            dest.m31 = t13*determinant_inv;
            dest.m32 = t23*determinant_inv;
            dest.m23 = t32*determinant_inv;
            return dest;
        } else
            return null;
    }

    /**
     * Negate this matrix
     * @return this
     */
    public Matrix4f negate() {
        return negate(this);
    }

    /**
     * Negate this matrix and place the result in a destination matrix.
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return the negated matrix
     */
    public Matrix4f negate(Matrix4f dest) {
        return negate(this, dest);
    }

    /**
     * Negate this matrix and place the result in a destination matrix.
     * @param src The source matrix
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return The negated matrix
     */
    public static Matrix4f negate(Matrix4f src, Matrix4f dest) {
        if (dest == null)
            dest = new Matrix4f();

        dest.m00 = -src.m00;
        dest.m01 = -src.m01;
        dest.m02 = -src.m02;
        dest.m03 = -src.m03;
        dest.m10 = -src.m10;
        dest.m11 = -src.m11;
        dest.m12 = -src.m12;
        dest.m13 = -src.m13;
        dest.m20 = -src.m20;
        dest.m21 = -src.m21;
        dest.m22 = -src.m22;
        dest.m23 = -src.m23;
        dest.m30 = -src.m30;
        dest.m31 = -src.m31;
        dest.m32 = -src.m32;
        dest.m33 = -src.m33;

        return dest;
    }

    public final void transformVecTransposed(Vector3f normal)
    {
      float x = this.m00 * normal.x + this.m01 * normal.y + this.m02 * normal.z;
      float y = this.m10 * normal.x + this.m11 * normal.y + this.m12 * normal.z;
      normal.setZ(this.m20 * normal.x + this.m21 * normal.y + this.m22 * normal.z);
      normal.setX(x);
      normal.setY(y);
    }

    public final void mulMat(Matrix4f m1)
    {
      float lm00 = this.m00 * m1.m00 + this.m01 * m1.m10 + this.m02 * m1.m20 + this.m03 * m1.m30;

      float lm01 = this.m00 * m1.m01 + this.m01 * m1.m11 + this.m02 * m1.m21 + this.m03 * m1.m31;

      float lm02 = this.m00 * m1.m02 + this.m01 * m1.m12 + this.m02 * m1.m22 + this.m03 * m1.m32;

      float lm03 = this.m00 * m1.m03 + this.m01 * m1.m13 + this.m02 * m1.m23 + this.m03 * m1.m33;

      float lm10 = this.m10 * m1.m00 + this.m11 * m1.m10 + this.m12 * m1.m20 + this.m13 * m1.m30;

      float lm11 = this.m10 * m1.m01 + this.m11 * m1.m11 + this.m12 * m1.m21 + this.m13 * m1.m31;

      float lm12 = this.m10 * m1.m02 + this.m11 * m1.m12 + this.m12 * m1.m22 + this.m13 * m1.m32;

      float lm13 = this.m10 * m1.m03 + this.m11 * m1.m13 + this.m12 * m1.m23 + this.m13 * m1.m33;

      float lm20 = this.m20 * m1.m00 + this.m21 * m1.m10 + this.m22 * m1.m20 + this.m23 * m1.m30;

      float lm21 = this.m20 * m1.m01 + this.m21 * m1.m11 + this.m22 * m1.m21 + this.m23 * m1.m31;

      float lm22 = this.m20 * m1.m02 + this.m21 * m1.m12 + this.m22 * m1.m22 + this.m23 * m1.m32;

      float lm23 = this.m20 * m1.m03 + this.m21 * m1.m13 + this.m22 * m1.m23 + this.m23 * m1.m33;

      float lm30 = this.m30 * m1.m00 + this.m31 * m1.m10 + this.m32 * m1.m20 + this.m33 * m1.m30;

      float lm31 = this.m30 * m1.m01 + this.m31 * m1.m11 + this.m32 * m1.m21 + this.m33 * m1.m31;

      float lm32 = this.m30 * m1.m02 + this.m31 * m1.m12 + this.m32 * m1.m22 + this.m33 * m1.m32;

      float lm33 = this.m30 * m1.m03 + this.m31 * m1.m13 + this.m32 * m1.m23 + this.m33 * m1.m33;

      this.m00 = lm00;
      this.m01 = lm01;
      this.m02 = lm02;
      this.m03 = lm03;
      this.m10 = lm10;
      this.m11 = lm11;
      this.m12 = lm12;
      this.m13 = lm13;
      this.m20 = lm20;
      this.m21 = lm21;
      this.m22 = lm22;
      this.m23 = lm23;
      this.m30 = lm30;
      this.m31 = lm31;
      this.m32 = lm32;
      this.m33 = lm33;
    }
    public static Matrix4f toRotationMatrix(Quaternion q, Matrix4f dest)
    {
        if (dest == null)
            dest = new Matrix4f();
        else
            dest.setIdentity();

        // Normalize the quaternion
        q.normalise(q);

        // The length of the quaternion
        float s = 2f / q.length();

        // Convert the quaternion to matrix
        dest.m00 = 1 - s * (q.y * q.y + q.z * q.z);
        dest.m10 = s * (q.x * q.y + q.w * q.z);
        dest.m20 = s * (q.x * q.z - q.w * q.y);

        dest.m01 = s * (q.x * q.y - q.w * q.z);
        dest.m11 = 1 - s * (q.x * q.x + q.z * q.z);
        dest.m21 = s * (q.y * q.z + q.w * q.x);

        dest.m02 = s * (q.x * q.z + q.w * q.y);
        dest.m12 = s * (q.y * q.z - q.w * q.x);
        dest.m22 = 1 - s * (q.x * q.x + q.y * q.y);

        return dest;
    }
    public static Matrix4f toMatrix4f(Quaternion q) {
        Matrix4f matrix = new Matrix4f();
        matrix.m00 = 1.0f - 2.0f * (q.getY() * q.getY() + q.getZ() * q.getZ());
        matrix.m01 = 2.0f * (q.getX() * q.getY() + q.getZ() * q.getW());
        matrix.m02 = 2.0f * (q.getX() * q.getZ() - q.getY() * q.getW());
        matrix.m03 = 0.0f;

        // Second row
        matrix.m10 = 2.0f * (q.getX() * q.getY() - q.getZ() * q.getW());
        matrix.m11 = 1.0f - 2.0f * (q.getX() * q.getX() + q.getZ() * q.getZ());
        matrix.m12 = 2.0f * (q.getZ() * q.getY() + q.getX() * q.getW());
        matrix.m13 = 0.0f;

        // Third row
        matrix.m20 = 2.0f * (q.getX() * q.getZ() + q.getY() * q.getW());
        matrix.m21 = 2.0f * (q.getY() * q.getZ() - q.getX() * q.getW());
        matrix.m22 = 1.0f - 2.0f * (q.getX() * q.getX() + q.getY() * q.getY());
        matrix.m23 = 0.0f;

        // Fourth row
        matrix.m30 = 0;
        matrix.m31 = 0;
        matrix.m32 = 0;
        matrix.m33 = 1.0f;

        return matrix;
     }
    public final void setFromQuat(float a, float b, float c, float d) {
        final float n = a * a + b * b + c * c + d * d;
        final float s = (n > 0.0f) ? (2.0f / n) : 0.0f;

        final float xs = a * s, ys = b * s, zs = c * s;
        final float wx = d * xs, wy = d * ys, wz = d * zs;
        final float xx = a * xs, xy = a * ys, xz = a * zs;
        final float yy = b * ys, yz = b * zs, zz = c * zs;


        this.m00 = 1.0f - (yy + zz);
        this.m01 = xy - wz;
        this.m02 = xz + wy;
        this.m10 = xy + wz;
        this.m11 = 1.0f - (xx + zz);
        this.m12 = yz - wx;
        this.m20 = xz - wy;
        this.m21 = yz + wx;
        this.m22 = 1.0f - (xx + yy);
    }

    /**
     * @param matAbs
     * @param f
     */
    public void addWeighted(Matrix4f m, float f) {


        this.m00 += f * m.m00;
        this.m01 += f * m.m01;
        this.m02 += f * m.m02;
        this.m03 += f * m.m03;
        this.m10 += f * m.m10;
        this.m11 += f * m.m11;
        this.m12 += f * m.m12;
        this.m13 += f * m.m13;
        this.m20 += f * m.m20;
        this.m21 += f * m.m21;
        this.m22 += f * m.m22;
        this.m23 += f * m.m23;
        this.m30 += f * m.m30;
        this.m31 += f * m.m31;
        this.m32 += f * m.m32;
        this.m33 += f * m.m33;
    }

    /**
     * @param n
     */
    public void mulFloat(float f) {


        this.m00 *= f;
        this.m01 *= f;
        this.m02 *= f;
        this.m03 *= f;
        this.m10 *= f;
        this.m11 *= f;
        this.m12 *= f;
        this.m13 *= f;
        this.m20 *= f;
        this.m21 *= f;
        this.m22 *= f;
        this.m23 *= f;
    }
    /**
     * Converts a Matrix4f to a Tuple3f with Euler angles.
     * 
     * @param matrix the Matrix4f to be converted
     */

    public void toEuler(Vector3f euler )
    {
        if ( this.m10 == 1.0f )
        {
            euler.setX( 0.0f );
            euler.setY( GameMath.atan2( this.m02, this.m22 ) );
            euler.setZ( GameMath.asin( -this.m10 ) );
        }
        else if ( this.m10 == -1.0f )
        {
            euler.setX( 0.0f );
            euler.setY( GameMath.atan2( this.m02, this.m22 ) );
            euler.setZ( GameMath.asin( -this.m10 ) );
        }
        else
        {
            euler.setX( GameMath.atan2( -this.m12, this.m11 ) );
            euler.setY( GameMath.atan2( -this.m20, this.m00 ) );
            euler.setZ( GameMath.asin( this.m10 ) );
        }
    }

    public void viewVec(float x, float y, float z) {
        /*
         * 
            float fq = GameMath.sqrtf(t.x*t.x+t.z*t.z);
            float rotx = GameMath.atan2(t.x, t.z);
            float roty = GameMath.atan2(t.y, fq);
            sprite.mat.rotate(roty, 0, 1, 0);
            sprite.mat.rotate(rotx, 1, 0, 0);
         */
//        float fq = GameMath.sqrtf(t.x*t.x+t.z*t.z);
//        float rotx = GameMath.atan2(t.x, t.z);
//        float roty = GameMath.atan2(t.y, fq);
//        sprite.mat.rotate(roty, 0, 1, 0);
//        sprite.mat.rotate(rotx, 1, 0, 0);
        float rotx = -GameMath.atan2( z, x )+GameMath.PI*0.5f;
        float roty = -GameMath.atan2( y, GameMath.sqrtf(y*y+z*z));
        this.rotate(rotx, 0, 1, 0);
        this.rotate(roty, 1, 0, 0);
    }

    public void clearTranslation() {
        this.m30=0;
        this.m31=0;
        this.m32=0;
        this.m33=1;
        this.m03=0;
        this.m13=0;
        this.m23=0;
    }
    

    public Matrix4f load(DoubleMatrix4 src) {
        
        this.m00 = (float) src.m00;
        this.m01 = (float) src.m01;
        this.m02 = (float) src.m02;
        this.m03 = (float) src.m03;
        this.m10 = (float) src.m10;
        this.m11 = (float) src.m11;
        this.m12 = (float) src.m12;
        this.m13 = (float) src.m13;
        this.m20 = (float) src.m20;
        this.m21 = (float) src.m21;
        this.m22 = (float) src.m22;
        this.m23 = (float) src.m23;
        this.m30 = (float) src.m30;
        this.m31 = (float) src.m31;
        this.m32 = (float) src.m32;
        this.m33 = (float) src.m33;

        return this;
    }
}
