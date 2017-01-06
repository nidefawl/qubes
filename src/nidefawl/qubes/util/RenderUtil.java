package nidefawl.qubes.util;

import nidefawl.qubes.gl.GLVAO;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;

public class RenderUtil {
    public static void makeCube2(VertexBuffer buf, float len) {

        for (int i = 0; i < 4; i++) {
            float x = i&1;
            float y = (i>>1)&1;
            float z = 4;
            buf.put(Float.floatToRawIntBits(x*len));
            buf.put(Float.floatToRawIntBits(y*len));
            buf.put(Float.floatToRawIntBits(z*len));
            buf.put(packNormal(0, 0, -1));
            buf.put(packTexCoord(i&1, (i>>1)&1));
            buf.put(0xffffffff);
            buf.increaseVert();
        }
        int idxPos = 0;
        buf.putIdx(idxPos+0);
        buf.putIdx(idxPos+1);
        buf.putIdx(idxPos+2);
        buf.putIdx(idxPos+2);
        buf.putIdx(idxPos+3);
        buf.putIdx(idxPos+1);
    }
    public static void makeCube(VertexBuffer buf, float len, GLVAO format) {

        float hlen= len/4.0f;
        int idxPos = 0;
        for (int i = 0; i < 6; i++) {
            final float x = Dir.getDirX(i);
            final float y = Dir.getDirY(i);
            final float z = Dir.getDirZ(i);
            for (int j = 0; j < 4; j++) {
                float xd = (j&1)==0?-1:1;
                float zd = ((j>>1)&1)==0?-1:1;
                float xOff = ((y)*xd+(z)*zd)*hlen;
                float yOff = ((z)*xd+(x)*zd)*hlen;
                float zOff = ((y)*zd+(x)*xd)*hlen;
                if (format == GLVAO.vaoModel) {
                    buf.put(Float.floatToRawIntBits(x*hlen+xOff));
                    buf.put(Float.floatToRawIntBits(y*hlen+yOff));
                    buf.put(Float.floatToRawIntBits(z*hlen+zOff));
                    buf.put(packNormal(x, y, z));
                    buf.put(packTexCoord(j&1, (j>>1)&1));
                    buf.put(0xffffffff);
                } else if (format == GLVAO.vaoStaticModel) {
                    
                    buf.put(Half.fromFloat(y*hlen+yOff) << 16 | (Half.fromFloat(x*hlen+xOff)));
                    buf.put(Half.fromFloat(0) << 16 | (Half.fromFloat(z*hlen+zOff)));
                    buf.put(packNormal(x, y, z));
                    buf.put(packTexCoord(j&1, (j>>1)&1));
                } else {
                    throw new IllegalArgumentException("Don't know how to handle that format");
                }
                buf.increaseVert();
            }
            int backFace = (i&1);
            if (backFace!=0) {
                buf.putIdx(idxPos+0);
                buf.putIdx(idxPos+1);
                buf.putIdx(idxPos+2);
                buf.putIdx(idxPos+1);
                buf.putIdx(idxPos+3);
                buf.putIdx(idxPos+2);
            } else {
                buf.putIdx(idxPos+2);
                buf.putIdx(idxPos+1);
                buf.putIdx(idxPos+0);
                buf.putIdx(idxPos+2);
                buf.putIdx(idxPos+3);
                buf.putIdx(idxPos+1);
            }
            idxPos+=4;
        }

//        byte byte0 = (byte)(int)(-1 * 128F);
//        byte byte1 = (byte)(int)(0x80);
//        System.out.println(byte1);
//        System.out.println(Integer.toHexString(byte0)+" - "+Integer.toHexString(byte1));
//        System.out.println(Integer.toBinaryString(byte0)+" - "+Integer.toBinaryString(byte1));
    }
    public static void makeSphere(VertexBuffer buf, float radius, int rings, int sectors) {

        final float R = 1.0f/(float)(rings-1);
        final float S = 1.0f/(float)(sectors-1);
        int r, s;
        double M_PI_2 = Math.PI/2.0;
        double M_PI = Math.PI;
        {
            buf.put(Float.floatToRawIntBits(0 * radius));
            buf.put(Float.floatToRawIntBits(-1 * radius));
            buf.put(Float.floatToRawIntBits(0 * radius));
            buf.put(packNormal(0, 1, 0));
            buf.put(packTexCoord(0, 0));
            buf.put(0xffffffff);
            buf.increaseVert();
        }
        for(r = 1; r < rings-1; r++) {
            for(s = 0; s < sectors; s++) {
                final float y = (float) Math.sin( -M_PI_2 + M_PI * r * R );
                final float x = (float) (Math.cos(2*M_PI * s * S) * Math.sin( M_PI * r * R ));
                final float z = (float) (Math.sin(2*M_PI * s * S) * Math.sin( M_PI * r * R ));
                buf.put(Float.floatToRawIntBits(x * radius));
                buf.put(Float.floatToRawIntBits(y * radius));
                buf.put(Float.floatToRawIntBits(z * radius));
                buf.put(packNormal(-x, -y, -z));
                buf.put(packTexCoord(s*S, r*R));
                buf.put(0xffffffff);
                buf.increaseVert();
            }
        }
        int lastIdx = buf.getVertexCount();
        {
            buf.put(Float.floatToRawIntBits(0 * radius));
            buf.put(Float.floatToRawIntBits(1 * radius));
            buf.put(Float.floatToRawIntBits(0 * radius));
            buf.put(packNormal(0, -1, 0));
            buf.put(packTexCoord(1, 1));
            buf.put(0xffffffff);
            buf.increaseVert();
        }
        for(s = 0; s < sectors-1; s++) {
            buf.putIdx(0);
            buf.putIdx(1+s);
            buf.putIdx(1+s+1);
        }
        for(r = 1; r < rings-2; r++) {
            for(s = 1; s < sectors; s++) {
                buf.putIdx((r) * sectors + s);
                buf.putIdx((r) * sectors + s + 1);
                buf.putIdx((r - 1) * sectors + s + 1);
                buf.putIdx((r) * sectors + s);
                buf.putIdx((r - 1) * sectors + s + 1);
                buf.putIdx((r - 1) * sectors + s);
            }
        }
        for(s = 0; s < sectors-1; s++) {
            buf.putIdx(lastIdx);
            buf.putIdx(lastIdx-s-1);
            buf.putIdx(lastIdx-s-2);
        }

    }
    public final static int packNormal(Vector3f v) {
        return packNormal(v.x, v.y, v.z);
    }
    public final static int packNormal(float x, float y, float z) {
        int iX = (int) Math.round(x * 128F);
        byte byte0 = (byte) (iX<-128?-128:iX>127?127:iX);
        int iY = (int) Math.round(y * 128F);
        byte byte1 = (byte) (iY<-128?-128:iY>127?127:iY);
        int iZ = (int) Math.round(z * 128F);
        byte byte2 = (byte) (iZ<-128?-128:iZ>127?127:iZ);
        int normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
        return normal;
    }
    public static int packTexCoord(float u, float v) {
        return Half.fromFloat(u) << 16 | (Half.fromFloat(v));
    }

}
