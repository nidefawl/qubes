package nidefawl.qubes.util;

import java.nio.FloatBuffer;

import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Quaternion;

public class GameMath {

    public static float cos(float f) {
        return (float)Math.cos(f);
    }
    public static float sin(float f) {
        return (float)Math.sin(f);
    }
    public static int floor(double d)
    {
        int x = (int) d;
        return d < x ? x - 1 : x;
    }
    final static float[] m   = new float[16];
    final static float[] inv = new float[16];
    public static void invertMat4x(FloatBuffer matin, FloatBuffer matout) {
        float det;
        int i;

        if (matin.remaining() == 0) {
            matin.flip();
        }
        if (matout.remaining() == 0) {
            matout.flip();
        }
        for (i = 0; i < 16; ++i) {
            m[i] = matin.get(i);
        }
        if (matin.remaining() == 0) {
            matin.flip();
        }
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];

        det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if ((double)det != 0.0) {
            for (i = 0; i < 16; ++i) {
                matout.put(inv[i] / det);
            }
        }  else {
            for (i = 0; i < 16; ++i) {
                matout.put(0F);
            }
        }
        matout.flip();
    }


    public static Matrix4f convertQuaternionToMatrix4f(Quaternion q, Matrix4f out) {
        out.m00 = 1.0f - 2.0f * (q.getY() * q.getY() + q.getZ() * q.getZ());
        out.m01 = 2.0f * (q.getX() * q.getY() + q.getZ() * q.getW());
        out.m02 = 2.0f * (q.getX() * q.getZ() - q.getY() * q.getW());
        out.m03 = 0.0f;

        // Second row
        out.m10 = 2.0f * (q.getX() * q.getY() - q.getZ() * q.getW());
        out.m11 = 1.0f - 2.0f * (q.getX() * q.getX() + q.getZ() * q.getZ());
        out.m12 = 2.0f * (q.getZ() * q.getY() + q.getX() * q.getW());
        out.m13 = 0.0f;

        // Third row
        out.m20 = 2.0f * (q.getX() * q.getZ() + q.getY() * q.getW());
        out.m21 = 2.0f * (q.getY() * q.getZ() - q.getX() * q.getW());
        out.m22 = 1.0f - 2.0f * (q.getX() * q.getX() + q.getY() * q.getY());
        out.m23 = 0.0f;

        // Fourth row
        out.m30 = 0;
        out.m31 = 0;
        out.m32 = 0;
        out.m33 = 1.0f;
        return out;
    }
   public static float coTangent(float angle) {
       return (float)(1f / Math.tan(angle));
   }
    
   public static float degreesToRadians(float degrees) {
       return degrees * (float)(Math.PI / 180d);
   }

    public static float sqrtf(float f) {
        return (float) Math.sqrt(f);
    }

    public static int randomI(long seed) {
        seed ^= (seed << 21);
        seed ^= (seed >>> 35);
        seed ^= (seed << 4);
        return (int) seed;
    }
    public static int lhToZ(long l) {
        return (int) (l&0xFFFFFFFF) + Integer.MIN_VALUE;
    }
    public static int lhToX(long l) {
        return (int) (l >> 32);
    }
    public static long toLong(int x, int z) {
        return ((long) x << 32) | ((long)z-Integer.MIN_VALUE);
    }
    public static double dist2d(double x, double z, double xx, double zz) {
        x = x - xx;
        z = z - zz;
        return Math.sqrt(x*x+z*z);
    }
    public static int log2(int x) {
        int y,v;
        v = x;
        y = -1;
        while (v>0) {
            v >>=1;
            y++;
        }
        return y;
    }
    public static int signum(float dy) {
        return dy < 0 ? -1 : dy > 0 ? 1 : 0;
    }
    public static boolean isNormalFloat(float f) {
        return f != Float.NaN && Math.abs(f) > 1.0E-5F;
    }

    public static float mod(float value, float modulus) {
      return (value % modulus + modulus) % modulus;
    }
    public final static float PI_OVER_180 = 0.0174532925f;
    
    public static float clamp(float e, float f, float g) {
        return e < f ? f : e > g ? g : e;
    }
    public static int distSq3Di(int x, int y, int z, int x2, int y2, int z2) {
        x = x2-x;
        y = y2-y;
        z = z2-z;
        return x*x+y*y+z*z;
    }
    /**
     * @param base
     * @param f
     * @return
     */
    public static final float powf(float base, float f) {
        return (float) Math.pow(base, f);
    }
    /**
     * @param f
     * @param i
     * @return
     */
    public static float pow(float base, int exp) {
        if (exp == 0) {
            return 1.0F;
        }
        if (exp <= 0) {
            base = 1.0F / base;
            exp = -exp;
        }
        float temp = base;
        exp--;
        while (true) {
            if ((exp & 0x1) != 0) {
                base *= temp;
            }
            exp >>= 1;
            if (exp == 0) {
                break;
            }
            temp *= temp;
        }
        return base;

    }
}
