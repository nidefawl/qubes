package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.vec.Vector3f;

public class BlockFaceAttr {
    public final static int BLOCK_VERT_INT_SIZE = 9;
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
    private boolean useGlobalRenderOffset = false;
    private int tex;
    float xOff, yOff, zOff;
    private int aoMask;
    private int lightMaskSky;
    private int lightMaskBlock;
    private int type;
    private boolean reverse = false;
    private int faceDir;
    
    /**
     * @param useGlobalRenderOffset the useGlobalRenderOffset to set
     */
    public void setUseGlobalRenderOffset(boolean useGlobalRenderOffset) {
        this.useGlobalRenderOffset = useGlobalRenderOffset;
    }

    public void setTex(int tex) {
        this.tex = tex;
    }

    public void setOffset(float xOff, float yOff, float zOff) {
        this.xOff = xOff;
        this.yOff = yOff;
        this.zOff = zOff;
        if (useGlobalRenderOffset) {
            this.xOff-=Engine.GLOBAL_OFFSET.x;
            this.zOff-=Engine.GLOBAL_OFFSET.z;
        }
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
    public void rotateUV(int num) {
//        System.out.println("pre");
//        for (int i = 0; i < 4; i++)
//            System.out.printf("%d %.2f %.2f\n", i, this.v[i].u, this.v[i].v);
        for (int a = 0; a < num; a++) {
            for (int v = 0; v < 3; v++) {
                BlockFaceVert v1 = this.v[(v+1) % 4];
                BlockFaceVert v2 = this.v[v];
                float vertex1_u = v1.u;
                float vertex1_v = v1.v;
                v1.u = v2.u;
                v1.v = v2.v;
                v2.u = vertex1_u;
                v2.v = vertex1_v;
                int direction = v1.direction;
                v1.direction = v2.direction;
                v2.direction = direction;
            }
            {
                int msb = this.lightMaskBlock&0xF000;
                msb >>= (3*4);
                this.lightMaskBlock <<= 4;
                this.lightMaskBlock |= msb&0xF;
            }
            {
                int msb = this.lightMaskSky&0xF000;
                msb >>= (3*4);
                this.lightMaskSky <<= 4;
                this.lightMaskSky |= msb&0xF;
            }
            {
                int msb = this.aoMask&0xC0;
                msb >>= 6;
                this.aoMask <<= 2;
                this.aoMask |= msb&0x3;
            }
            
        }
//        System.out.println("post");
//        for (int i = 0; i < 4; i++)
//            System.out.printf("%d %.2f %.2f\n", i, this.v[i].u, this.v[i].v);
    }

    public void putSingleVert(int vert, VertexBuffer vertexBuffer) {
        BlockFaceVert v = this.v[vert];
        vertexBuffer.put(Float.floatToRawIntBits(this.xOff+v.x));
        vertexBuffer.put(Float.floatToRawIntBits(this.yOff+v.y));
        vertexBuffer.put(Float.floatToRawIntBits(this.zOff+v.z));
        vertexBuffer.put(v.normal);
        int textureHalf2 = Half.fromFloat(v.v) << 16 | Half.fromFloat(v.u);
        vertexBuffer.put(textureHalf2);
        vertexBuffer.put(v.rgba);
        vertexBuffer.put(this.tex | this.type << 16 | v.pass << (16+12)); //2x SHORT
        // BIT 0-7: 8 bit AO
        // BIT 16-18: 3 bit FACEDIR (aka blockside)
        // BIT 19-24: 6 bit VERTEXDIR 
        vertexBuffer.put(this.aoMask | this.faceDir << 16 | v.direction << 19);    
        vertexBuffer.put(this.lightMaskBlock&0xFFFF | (this.lightMaskSky&0xFFFF)<<16);
    }
    public void putShadowTexturedSingleVert(int vert, VertexBuffer vertexBuffer) {
        BlockFaceVert v = this.v[vert];
        vertexBuffer.put(Float.floatToRawIntBits(this.xOff+v.x));
        vertexBuffer.put(Float.floatToRawIntBits(this.yOff+v.y));
        vertexBuffer.put(Float.floatToRawIntBits(this.zOff+v.z));
        vertexBuffer.put(Float.floatToRawIntBits(1));
        vertexBuffer.put(Float.floatToRawIntBits(v.u)); //TODO: i think this should be halffloats
        vertexBuffer.put(Float.floatToRawIntBits(v.v));
        vertexBuffer.put(tex|this.type<<16); //2x SHORT
    }

    public void putBasicSingleVert(int vert, VertexBuffer vertexBuffer) {
        BlockFaceVert v = this.v[vert];
        vertexBuffer.put(Float.floatToRawIntBits(this.xOff+v.x));
        vertexBuffer.put(Float.floatToRawIntBits(this.yOff+v.y));
        vertexBuffer.put(Float.floatToRawIntBits(this.zOff+v.z));
        vertexBuffer.put(Float.floatToRawIntBits(1));
    }
    public void put(VertexBuffer vertexBuffer) {
//        rotateUV(1);
        int idxPos = vertexBuffer.getVertexCount();
        for (int i = 0; i < 4; i++) {
            int idx = this.reverse ? 3-(i % 4) : i % 4;
            BlockFaceVert v = this.v[idx];
            vertexBuffer.put(Float.floatToRawIntBits(this.xOff+v.x));
            vertexBuffer.put(Float.floatToRawIntBits(this.yOff+v.y));
            vertexBuffer.put(Float.floatToRawIntBits(this.zOff+v.z));
            vertexBuffer.put(v.normal);
            int textureHalf2 = Half.fromFloat(v.v) << 16 | Half.fromFloat(v.u);
            vertexBuffer.put(textureHalf2);
            vertexBuffer.put(v.rgba);
            vertexBuffer.put(this.tex | this.type << 16 | v.pass << (16+12)); //2x SHORT
            // BIT 0-7: 8 bit AO
            // BIT 16-18: 3 bit FACEDIR (aka blockside)
            // BIT 19-24: 6 bit VERTEXDIR 
            vertexBuffer.put(this.aoMask | this.faceDir << 16 | v.direction << 19);    
            vertexBuffer.put(this.lightMaskBlock&0xFFFF | (this.lightMaskSky&0xFFFF)<<16); 
            vertexBuffer.increaseVert();
        }
        vertexBuffer.putIdx(idxPos+0);
        vertexBuffer.putIdx(idxPos+1);
        vertexBuffer.putIdx(idxPos+2);
        vertexBuffer.putIdx(idxPos+2);
        vertexBuffer.putIdx(idxPos+3);
        vertexBuffer.putIdx(idxPos+0);
        vertexBuffer.increaseFace();
        
    }

    public void putBasic(VertexBuffer vertexBuffer) {
        int idxPos = vertexBuffer.getVertexCount();
        for (int i = 0; i < 4; i++) {
            int idx = this.reverse ? 3-(i % 4) : i % 4;
            BlockFaceVert v = this.v[idx];
            vertexBuffer.put(Float.floatToRawIntBits(this.xOff+v.x));
            vertexBuffer.put(Float.floatToRawIntBits(this.yOff+v.y));
            vertexBuffer.put(Float.floatToRawIntBits(this.zOff+v.z));
            vertexBuffer.put(Float.floatToRawIntBits(1));
            vertexBuffer.increaseVert();
        }
        vertexBuffer.putIdx(idxPos+0);
        vertexBuffer.putIdx(idxPos+1);
        vertexBuffer.putIdx(idxPos+2);
        vertexBuffer.putIdx(idxPos+2);
        vertexBuffer.putIdx(idxPos+3);
        vertexBuffer.putIdx(idxPos+0);
        vertexBuffer.increaseFace();
    }


    public void putShadowTextured(VertexBuffer vertexBuffer) {
        int idxPos = vertexBuffer.getVertexCount();
        for (int i = 0; i < 4; i++) {
            int idx = this.reverse ? 3-(i % 4) : i % 4;
            BlockFaceVert v = this.v[idx];
            vertexBuffer.put(Float.floatToRawIntBits(this.xOff+v.x));
            vertexBuffer.put(Float.floatToRawIntBits(this.yOff+v.y));
            vertexBuffer.put(Float.floatToRawIntBits(this.zOff+v.z));
            vertexBuffer.put(Float.floatToRawIntBits(1));
            vertexBuffer.put(Float.floatToRawIntBits(v.u));
            vertexBuffer.put(Float.floatToRawIntBits(v.v));
            vertexBuffer.put(tex|this.type<<16); //2x SHORT
            vertexBuffer.increaseVert();
        }
        vertexBuffer.putIdx(idxPos+0);
        vertexBuffer.putIdx(idxPos+1);
        vertexBuffer.putIdx(idxPos+2);
        vertexBuffer.putIdx(idxPos+2);
        vertexBuffer.putIdx(idxPos+3);
        vertexBuffer.putIdx(idxPos+0);
        vertexBuffer.increaseFace();
    }

    public void setAO(int aoMask) {
        this.aoMask = aoMask;
    }
    
    public void setType(int type) {
        this.type = type;
        for (int i = 0; i < 4; i++) {
            this.v[i].pass = Block.get(type).getRenderPass();
        }
    }
    public void setPass(int pass) {
        for (int i = 0; i < 4; i++) {
            this.v[i].pass = pass;
        }
    }
    public void flipNormal() {
        for (int i = 0; i < 4; i++) {
            this.v[i].flipNormal();
        }
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
        this.faceDir = 1+faceDir;
    }
    /**
     * @return the faceDir
     */
    public int getFaceDir() {
        return this.faceDir-1;
    }

    /**
     * 
     */
    public void calcNormal(Vector3f to) {
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
        to.set(nX, nY, nZ);
    }
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

    /**
     * @param plantNormal
     * @return 
     */
    public int packNormal(Vector3f n) {
        byte byte0 = (byte)(int)(n.x * 127F);
        byte byte1 = (byte)(int)(n.y * 127F);
        byte byte2 = (byte)(int)(n.z * 127F);
        return byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
    }

    public boolean getReverse() {
        return this.reverse;
    }


}