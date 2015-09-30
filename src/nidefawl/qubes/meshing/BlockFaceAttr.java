package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.util.GameMath;

public class BlockFaceAttr {
    public static boolean USE_TRIANGLES = true;
    public final static int BLOCK_VERT_INT_SIZE = 12;
    public final static int BLOCK_VERT_BYTE_SIZE = BLOCK_VERT_INT_SIZE<<2;
    public final static int BLOCK_FACE_INT_SIZE = BLOCK_VERT_INT_SIZE*4;
    public final static int BLOCK_FACE_BYTE_SIZE = BLOCK_FACE_INT_SIZE<<2;
    
    public final static int PASS_2_BLOCK_VERT_INT_SIZE = 4;
    public final static int PASS_2_BLOCK_VERT_BYTE_SIZE = PASS_2_BLOCK_VERT_INT_SIZE<<2;
    public final static int PASS_2_BLOCK_FACE_INT_SIZE = PASS_2_BLOCK_VERT_INT_SIZE*4;
    public final static int PASS_2_BLOCK_FACE_BYTE_SIZE = PASS_2_BLOCK_FACE_INT_SIZE<<2;
    
    public final static int PASS_3_BLOCK_VERT_INT_SIZE = 7;
    public final static int PASS_3_BLOCK_VERT_BYTE_SIZE = PASS_3_BLOCK_VERT_INT_SIZE<<2;
    public final static int PASS_3_BLOCK_FACE_INT_SIZE = PASS_3_BLOCK_VERT_INT_SIZE*4;
    public final static int PASS_3_BLOCK_FACE_BYTE_SIZE = PASS_3_BLOCK_FACE_INT_SIZE<<2;
    
    final public static String[] attributes = new String[] {
            "in_blockinfo",
            "in_light",
    };
    public final BlockFaceVert v0 = new BlockFaceVert();
    public final BlockFaceVert v1 = new BlockFaceVert();
    public final BlockFaceVert v2 = new BlockFaceVert();
    public final BlockFaceVert v3 = new BlockFaceVert();
    public final BlockFaceVert[] v = new BlockFaceVert[] {
            v0, v1, v2, v3
    };
    private int tex;
    float xOff, yOff, zOff;
    private int aoMask;
    private int lightMaskSky;
    private int lightMaskBlock;
    private int type;
    private boolean reverse = false;
    private int faceDir;

    public void setTex(int tex) {
        this.tex = tex;
    }

    public void setOffset(int xOff, int yOff, int zOff) {
        this.xOff = xOff;
        this.yOff = yOff;
        this.zOff = zOff;
    }

    public void setNormal(int x, int y, int z) {
        byte byte0 = (byte)(int)(x * 127F);
        byte byte1 = (byte)(int)(y * 127F);
        byte byte2 = (byte)(int)(z * 127F);
        int normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
        for (int i = 0; i < 4; i++) {
            this.v[i].normal = normal;
        }
    }

    public void setLight(int sky, int block) {
        this.lightMaskSky = sky;
        this.lightMaskBlock = block;
    }
    
    public void put(int[] rawBuffer, int index) {
        for (int i = 0; i < 4; i++) {
            int idx = this.reverse ? 3-(i % 4) : i % 4;
            BlockFaceVert v = this.v[idx];
            rawBuffer[index + 0] = Float.floatToRawIntBits(this.xOff+v.x);
            rawBuffer[index + 1] = Float.floatToRawIntBits(this.yOff+v.y);
            rawBuffer[index + 2] = Float.floatToRawIntBits(this.zOff+v.z);
            rawBuffer[index + 3] = Float.floatToRawIntBits(1);
            rawBuffer[index + 4] = v.normal;
            rawBuffer[index + 5] = Float.floatToRawIntBits(v.u);
            rawBuffer[index + 6] = Float.floatToRawIntBits(v.v);
            rawBuffer[index + 7] = v.rgba;
            rawBuffer[index + 8] = tex|this.type<<16; //2x SHORT
            rawBuffer[index + 9] = this.aoMask|this.faceDir<<16; // lower 8 bit = AO
            rawBuffer[index + 10] = this.lightMaskBlock&0xFFFF | (this.lightMaskSky&0xFFFF)<<16; 
            rawBuffer[index + 11] = v.dirOffset;
            index += BLOCK_VERT_INT_SIZE;
        }
        
    }

    public void putBasic(int[] rawBuffer, int index) {
        for (int i = 0; i < 4; i++) {
            int idx = this.reverse ? 3-(i % 4) : i % 4;
            BlockFaceVert v = this.v[idx];
            rawBuffer[index + 0] = Float.floatToRawIntBits(this.xOff+v.x);
            rawBuffer[index + 1] = Float.floatToRawIntBits(this.yOff+v.y);
            rawBuffer[index + 2] = Float.floatToRawIntBits(this.zOff+v.z);
            rawBuffer[index + 3] = Float.floatToRawIntBits(1);
            index += PASS_2_BLOCK_VERT_INT_SIZE;
        }
    }


    public void putShadowTextured(int[] rawBuffer, int index) {
        for (int i = 0; i < 4; i++) {
            int idx = this.reverse ? 3-(i % 4) : i % 4;
            BlockFaceVert v = this.v[idx];
            rawBuffer[index + 0] = Float.floatToRawIntBits(this.xOff+v.x);
            rawBuffer[index + 1] = Float.floatToRawIntBits(this.yOff+v.y);
            rawBuffer[index + 2] = Float.floatToRawIntBits(this.zOff+v.z);
            rawBuffer[index + 3] = Float.floatToRawIntBits(1);
            rawBuffer[index + 4] = Float.floatToRawIntBits(v.u);
            rawBuffer[index + 5] = Float.floatToRawIntBits(v.v);
            rawBuffer[index + 6] = tex|this.type<<16; //2x SHORT
            index += PASS_3_BLOCK_VERT_INT_SIZE;
        }
    }

    public void setAO(int aoMask) {
        this.aoMask = aoMask;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    /**
     * @param reverse the reverse to set
     */
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    /**
     * @param faceDir2
     */
    public void setFaceDir(int faceDir) {
        this.faceDir = faceDir;
    }

    /**
     * 
     */
    public int calcNormal() {
        float uX = v1.x - v0.x;
        float uY = v1.y - v0.y;
        float uZ = v1.z - v0.z;
        float vX = v2.x - v0.x;
        float vY = v2.y - v0.y;
        float vZ = v2.z - v0.z;
        float nX = uY*vZ - uZ*vY;
        float nY = uZ*vX - uX*vZ;
        float nZ = uX*vY - uY*vX;
        if (reverse) {
            nX = -nX;
            nY = -nY;
            nZ = -nZ;
        }

        // normalize !
        float len = nX*nX+nY*nY+nZ*nZ;
        if (len > 1E-6F) {
            len = 1.0f / GameMath.sqrtf(len);
            nX *= len;
            nY *= len;
            nZ *= len;
        }
        byte byte0 = (byte)(int)(nX * 127F);
        byte byte1 = (byte)(int)(nY * 127F);
        byte byte2 = (byte)(int)(nZ * 127F);
        int normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
        return normal;
    }

}