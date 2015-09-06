package nidefawl.qubes.meshing;

import java.nio.ByteOrder;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.vec.Dir;

public class BlockFaceAttr {
    public final static int BLOCK_VERT_INT_SIZE = 12;
    public final static int BLOCK_VERT_BYTE_SIZE = BLOCK_VERT_INT_SIZE<<2;
    public final static int BLOCK_FACE_INT_SIZE = BLOCK_VERT_INT_SIZE*4;
    public final static int BLOCK_FACE_BYTE_SIZE = BLOCK_FACE_INT_SIZE<<2;
    public final static int PASS_2_BLOCK_VERT_INT_SIZE = 4;
    public final static int PASS_2_BLOCK_VERT_BYTE_SIZE = PASS_2_BLOCK_VERT_INT_SIZE<<2;
    public final static int PASS_2_BLOCK_FACE_INT_SIZE = PASS_2_BLOCK_VERT_INT_SIZE*4;
    public final static int PASS_2_BLOCK_FACE_BYTE_SIZE = PASS_2_BLOCK_FACE_INT_SIZE<<2;
    public final BlockFaceVert v0 = new BlockFaceVert();
    public final BlockFaceVert v1 = new BlockFaceVert();
    public final BlockFaceVert v2 = new BlockFaceVert();
    public final BlockFaceVert v3 = new BlockFaceVert();
    public final BlockFaceVert[] v = new BlockFaceVert[] {
            v0, v1, v2, v3
    };
    private int tex;
    private int normal;
    private int brightness;
    int xOff, yOff, zOff;

    public void setTex(int tex) {
        this.tex = tex;
    }

    public void setOffset(int xOff, int yOff, int zOff) {
        this.xOff = xOff;
        this.yOff = yOff;
        this.zOff = zOff;
    }

    public void setNormal(byte x, byte y, byte z) {
        byte byte0 = (byte)(int)(x * 127F);
        byte byte1 = (byte)(int)(y * 127F);
        byte byte2 = (byte)(int)(z * 127F);
        normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
    }
    
    public void setBrightness(int i) {
        brightness = i;
    }
    
    public void put(int[] rawBuffer, int index, int pass) {
        for (int i = 0; i < 4; i++) { //TODO: HANDLE REORDER/bs.rotateVertex
            BlockFaceVert v = this.v[i];
            rawBuffer[index + 0] = Float.floatToRawIntBits(this.xOff+v.x);
            rawBuffer[index + 1] = Float.floatToRawIntBits(this.yOff+v.y);
            rawBuffer[index + 2] = Float.floatToRawIntBits(this.zOff+v.z);
            rawBuffer[index + 3] = Float.floatToRawIntBits(1);
            rawBuffer[index + 4] = normal;
            rawBuffer[index + 5] = Float.floatToRawIntBits(v.u);
            rawBuffer[index + 6] = Float.floatToRawIntBits(v.v);
            rawBuffer[index + 7] = v.rgba;
            rawBuffer[index + 8] = brightness;
            rawBuffer[index + 9] = tex; //2x SHORT
            rawBuffer[index + 10] = 0; // 2x 16 bit
            index += BLOCK_VERT_INT_SIZE;
        }
        
    }

    public void putBasic(int[] rawBuffer, int index, int pass) {
        for (int i = 0; i < 4; i++) { //TODO: HANDLE REORDER/bs.rotateVertex
            BlockFaceVert v = this.v[i];
            rawBuffer[index + 0] = Float.floatToRawIntBits(this.xOff+v.x);
            rawBuffer[index + 1] = Float.floatToRawIntBits(this.yOff+v.y);
            rawBuffer[index + 2] = Float.floatToRawIntBits(this.zOff+v.z);
            rawBuffer[index + 3] = Float.floatToRawIntBits(1);
            index += PASS_2_BLOCK_VERT_INT_SIZE;
        }
    }

}