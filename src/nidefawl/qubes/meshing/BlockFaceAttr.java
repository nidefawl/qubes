package nidefawl.qubes.meshing;

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
    private int normal;
    int xOff, yOff, zOff;
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
        normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
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
            rawBuffer[index + 4] = normal;
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

}