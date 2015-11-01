package nidefawl.qubes.meshing;

import java.nio.ByteOrder;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Dir;

public class BlockFaceVert {
    private final static boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    int                          rgba;
    float                        u;
    float                        v;
    float                        x;
    float                        y;
    float                        z;
    int dirOffset;
    int normal;
    int direction;
    

    public void setColorRGBAF(float r, float g, float b, float a) {
        float scale = 255;
        int iR = (int) (r * scale);
        int iG = (int) (g * scale);
        int iB = (int) (b * scale);
        int iA = (int) (a * 255.0F);
        int rgb;
        if (!littleEndian) {
            rgb = iB << 16 | iG << 8 | iR;
        } else {
            rgb = iR << 16 | iG << 8 | iB;
        }
        rgba = rgb | iA << 24;
    }

    public void setColorRGBA(int a) {
        rgba = a;
    }

    public void setUV(float u, float v) {
        this.u = u;
        this.v = v;
    }

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setFaceVertDir(int n) {
        dirOffset = n;
    }
    public void setDirection(int side, int vertex, boolean negative) {
        direction = (side*4+vertex)+1;
        if (negative) {
            direction += 32;
        }
    }
    public void setNoDirection() {
        direction = 0;
    }
    public void setNormal(float x, float y, float z) {

        // normalize !
        float len = x*x+y*y+z*z;
        if (len > 1E-6F) {
            len = 1.0f / GameMath.sqrtf(len);
            x *= len;
            y *= len;
            z *= len;
        }
        byte byte0 = (byte)(int)(x * 127F);
        byte byte1 = (byte)(int)(y * 127F);
        byte byte2 = (byte)(int)(z * 127F);
        int normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
        this.normal = normal;
    }
}
