package nidefawl.qubes.meshing;

import java.nio.ByteOrder;

public class BlockFaceVert {
    private final static boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    int                          rgba;
    float                        u;
    float                        v;
    float                        x;
    float                        y;
    float                        z;
    int dirOffset;

    public void setColorRGBAF(float r, float g, float b, float a) {
        int iR = (int) (r * 255.0F);
        int iG = (int) (g * 255.0F);
        int iB = (int) (b * 255.0F);
        int iA = (int) (a * 255.0F);
        int rgb;
        if (!littleEndian) {
            rgb = iB << 16 | iG << 8 | iR;
        } else {
            rgb = iR << 16 | iG << 8 | iB;
        }
        rgba = rgb | iA << 24;
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
}
