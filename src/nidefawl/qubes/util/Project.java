package nidefawl.qubes.util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import nidefawl.qubes.gl.Memory;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

public class Project {


    private static final float[] IDENTITY_MATRIX =
        new float[] {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f };

    private static final FloatBuffer finalMatrix = Memory.createFloatBufferHeap(16);
    private static final FloatBuffer tempMatrix = Memory.createFloatBufferHeap(16);
    
    private static final float[] in = new float[4];
    private static final float[] out = new float[4];

    /**
     * Method gluUnproject
     *
     * @param winx
     * @param winy
     * @param winz
     * @param modelMatrix
     * @param projMatrix
     * @param viewport
     * @param obj_pos
     */
    public static boolean gluUnProject(
        float winx,
        float winy,
        float winz,
        FloatBuffer modelMatrix,
        FloatBuffer projMatrix,
        IntBuffer viewport,
        FloatBuffer obj_pos) {
        float[] in = Project.in;
        float[] out = Project.out;

        __gluMultMatricesf(modelMatrix, projMatrix, finalMatrix);

        if (!__gluInvertMatrixf(finalMatrix, finalMatrix))
            return false;

        in[0] = winx;
        in[1] = winy;
        in[2] = winz;
        in[3] = 1.0f;

        // Map x and y from window coordinates
//        System.out.println("in1 "+(in[0]-viewport.get(viewport.position() + 0))+"/"+viewport.get(viewport.position() + 2));
        in[0] = (in[0] - viewport.get(viewport.position() + 0)) / viewport.get(viewport.position() + 2);
        in[1] = (in[1] - viewport.get(viewport.position() + 1)) / viewport.get(viewport.position() + 3);

        // Map to range -1 to 1
        in[0] = in[0] * 2 - 1;
        in[1] = in[1] * 2 - 1;
        in[2] = in[2] * 2 - 1;
//        System.out.println("in1 "+in[0]+","+in[1]+","+in[2]);
        __gluMultMatrixVecf(finalMatrix, in, out);

        if (out[3] == 0.0)
            return false;

        out[3] = 1.0f / out[3];

        obj_pos.put(obj_pos.position() + 0, out[0] * out[3]);
        obj_pos.put(obj_pos.position() + 1, out[1] * out[3]);
        obj_pos.put(obj_pos.position() + 2, out[2] * out[3]);

        return true;
    }

    /**
     * @param src
     * @param inverse
     *
     * @return
     */
    private static boolean __gluInvertMatrixf(FloatBuffer src, FloatBuffer inverse) {
        int i, j, k, swap;
        float t;
        FloatBuffer temp = Project.tempMatrix;


        for (i = 0; i < 16; i++) {
            temp.put(i, src.get(i + src.position()));
        }
        __gluMakeIdentityf(inverse);

        for (i = 0; i < 4; i++) {
            /*
             * * Look for largest element in column
             */
            swap = i;
            for (j = i + 1; j < 4; j++) {
                /*
                 * if (fabs(temp[j][i]) > fabs(temp[i][i])) { swap = j;
                 */
                if (Math.abs(temp.get(j*4 + i)) > Math.abs(temp.get(i* 4 + i))) {
                    swap = j;
                }
            }

            if (swap != i) {
                /*
                 * * Swap rows.
                 */
                for (k = 0; k < 4; k++) {
                    t = temp.get(i*4 + k);
                    temp.put(i*4 + k, temp.get(swap*4 + k));
                    temp.put(swap*4 + k, t);

                    t = inverse.get(i*4 + k);
                    inverse.put(i*4 + k, inverse.get(swap*4 + k));
                    //inverse.put((i << 2) + k, inverse.get((swap << 2) + k));
                    inverse.put(swap*4 + k, t);
                    //inverse.put((swap << 2) + k, t);
                }
            }

            if (temp.get(i*4 + i) == 0) {
                /*
                 * * No non-zero pivot. The matrix is singular, which shouldn't *
                 * happen. This means the user gave us a bad matrix.
                 */
                return false;
            }

            t = temp.get(i*4 + i);
            for (k = 0; k < 4; k++) {
                temp.put(i*4 + k, temp.get(i*4 + k)/t);
                inverse.put(i*4 + k, inverse.get(i*4 + k)/t);
            }
            for (j = 0; j < 4; j++) {
                if (j != i) {
                    t = temp.get(j*4 + i);
                    for (k = 0; k < 4; k++) {
                        temp.put(j*4 + k, temp.get(j*4 + k) - temp.get(i*4 + k) * t);
                        inverse.put(j*4 + k, inverse.get(j*4 + k) - inverse.get(i*4 + k) * t);
                        /*inverse.put(
                            (j << 2) + k,
                            inverse.get((j << 2) + k) - inverse.get((i << 2) + k) * t);*/
                    }
                }
            }
        }
        return true;
    }

    /**
     * Make matrix an identity matrix
     */
    private static void __gluMakeIdentityf(FloatBuffer m) {
        int oldPos = m.position();
        m.put(IDENTITY_MATRIX);
        m.position(oldPos);
    }
    /**
     * @param a
     * @param b
     * @param r
     */
    private static void __gluMultMatricesf(FloatBuffer a, FloatBuffer b, FloatBuffer r) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r.put(r.position() + i*4 + j,
                    a.get(a.position() + i*4 + 0) * b.get(b.position() + 0*4 + j) + a.get(a.position() + i*4 + 1) * b.get(b.position() + 1*4 + j) + a.get(a.position() + i*4 + 2) * b.get(b.position() + 2*4 + j) + a.get(a.position() + i*4 + 3) * b.get(b.position() + 3*4 + j));
            }
        }
    }

    /**
     * Method __gluMultMatrixVecf
     *
     * @param finalMatrix
     * @param in
     * @param out
     */
    private static void __gluMultMatrixVecf(FloatBuffer m, float[] in, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] =
                in[0] * m.get(m.position() + 0*4 + i)
                    + in[1] * m.get(m.position() + 1*4 + i)
                    + in[2] * m.get(m.position() + 2*4 + i)
                    + in[3] * m.get(m.position() + 3*4 + i);

        }
    }

    public static void fovProjMat(float fieldOfView, float aspectRatio, float znear, float zfar, Matrix4f to) {
        float y_scale = GameMath.coTangent(GameMath.degreesToRadians(fieldOfView / 2f));
        float x_scale = y_scale / aspectRatio;
        float frustum_length = zfar - znear;

        to.setIdentity();
        to.m00 = x_scale;
        to.m11 = y_scale;
        to.m22 = -((zfar + znear) / frustum_length);
        to.m23 = -1;
        to.m32 = -((2 * znear * zfar) / frustum_length);
        to.m33 = 0;
    }
    public static void fovProjMatVk(float fieldOfView, float aspectRatio, float znear, float zfar, Matrix4f to) {
        float y_scale = GameMath.coTangent(GameMath.degreesToRadians(fieldOfView / 2f));
        float x_scale = y_scale / aspectRatio;
        float frustum_length = zfar - znear;

        to.setIdentity();
        to.m00 = x_scale;
        to.m11 = -y_scale;
        to.m22 = -((zfar + znear) / frustum_length);
        to.m23 = -1;
        to.m32 = -((2 * znear * zfar) / frustum_length);
        to.m33 = 0;
    }
    public static void fovProjMatInfInvZ(float fieldOfView, float aspectRatio, float znear, Matrix4f to) {
        float y_scale = GameMath.coTangent(GameMath.degreesToRadians(fieldOfView / 2f));
        float x_scale = y_scale / aspectRatio;

        to.setIdentity();
        to.m00 = x_scale;
        to.m11 = y_scale;
        to.m22 = 0;
        to.m23 = -1;
        to.m32 = znear;
        to.m33 = 0;
    }

    public static void orthoMat01(float left, float right, float top, float bottom, float znear, float zfar, Matrix4f to) {
        to.setIdentity();
        boolean zZeroToOne=true;
        // calculate right matrix elements
        float rm00 = 2.0f / (right - left);
        float rm11 = 2.0f / (top - bottom);
        float rm22 = (zZeroToOne ? 1.0f : 2.0f) / (znear - zfar);
        float rm30 = (left + right) / (left - right);
        float rm31 = (top + bottom) / (bottom - top);
        float rm32 = (zZeroToOne ? znear : (zfar + znear)) / (znear - zfar);

        // perform optimized multiplication
        // compute the last column first, because other columns do not depend on it
        to.m30= to.m00 * rm30 + to.m10 * rm31 + to.m20 * rm32 + to.m30;
        to.m31= to.m01 * rm30 + to.m11 * rm31 + to.m21 * rm32 + to.m31;
        to.m32= to.m02 * rm30 + to.m12 * rm31 + to.m22 * rm32 + to.m32;
        to.m33= to.m03 * rm30 + to.m13 * rm31 + to.m23 * rm32 + to.m33;
        to.m00= to.m00 * rm00;
        to.m01= to.m01 * rm00;
        to.m02= to.m02 * rm00;
        to.m03= to.m03 * rm00;
        to.m10= to.m10 * rm11;
        to.m11= to.m11 * rm11;
        to.m12= to.m12 * rm11;
        to.m13= to.m13 * rm11;
        to.m20= to.m20 * rm22;
        to.m21= to.m21 * rm22;
        to.m22= to.m22 * rm22;
        to.m23= to.m23 * rm22;
    }
    public static void orthoMat(float left, float right, float top, float bottom, float znear, float zfar, Matrix4f to) {
        to.setZero();
        to.m00 = 2.0f / (right - left);
        to.m11 = 2.0f / (top - bottom);
        to.m22 = (-2.0f) / (zfar - znear);
        to.m33 = 1.0f;
        to.m30 = -( (right+left) / (right-left) );
        to.m31 = -( (top+bottom) / (top-bottom) );
        to.m32 = -( (zfar+znear) / (zfar-znear) );
    }

    static Vector3f tmp3 = new Vector3f();
    static Vector3f tmp4 = new Vector3f();
    static Vector3f tmp5 = new Vector3f();
    public static void lookAt(float eyex, float eyey, float eyez, float centerx, float centery, float centerz, float upx, float upy, 
            float upz, Matrix4f to)
    {
        tmp3.set(upx, upy, upz);
        tmp4.set(centerx - eyex, centery - eyey, centerz - eyez);
        tmp4.normalise();
        Vector3f.cross(tmp4, tmp3, tmp5);
        tmp5.normalise();
        Vector3f.cross(tmp5, tmp4, tmp3);
        to.m00 = tmp5.x;
        to.m10 = tmp5.y;
        to.m20 = tmp5.z;
        to.m01 = tmp3.x;
        to.m11 = tmp3.y;
        to.m21 = tmp3.z;
        to.m02 = -tmp4.x;
        to.m12 = -tmp4.y;
        to.m22 = -tmp4.z;
        to.translate(-eyex, -eyey, -eyez);
//        normalize(forward);
//        cross(forward, up, side);
//        normalize(side);
//        cross(side, forward, up);
//        __gluMakeIdentityf(matrix);
//        matrix.put(0, side[0]);
//        matrix.put(4, side[1]);
//        matrix.put(8, side[2]);
//        matrix.put(1, up[0]);
//        matrix.put(5, up[1]);
//        matrix.put(9, up[2]);
//        matrix.put(2, -forward[0]);
//        matrix.put(6, -forward[1]);
//        matrix.put(10, -forward[2]);
//        GL11.glMultMatrix(matrix);
//        GL11.glTranslatef(-eyex, -eyey, -eyez);
    }

    public static void fovProjMatInfInvZVk(float fieldOfView, float aspectRatio, float znear, Matrix4f to) {
        float y_scale = GameMath.coTangent(GameMath.degreesToRadians(fieldOfView / 2f));
        float x_scale = y_scale / aspectRatio;

        to.setIdentity();
        to.m00 = x_scale;
        to.m11 = -y_scale;
        to.m22 = -1;
        to.m23 = -1;
        to.m32 = -znear;
        to.m33 = 0;
    }
}
