package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockGrass;
import nidefawl.qubes.block.BlockLeaves;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.Dir;

public class BlockFace {
    public final BlockSurface bs;
    public final float v0[];
    public final float v1[];
    public final float v2[];
    public final float v3[];
    public final int du[];
    public final int dv[];
    public final byte normal[];
    public int faceDir;
    private int[] pos;
    public int w;
    public int h;
    public static int encNormal(int i, int j, int k) {
//        int d = 1;
//        i *= d;
//        j *= d;
//        k *= d;
        byte byte0 = (byte)i;
        byte byte1 = (byte)j;
        byte byte2 = (byte)k;
        return byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16 | (0x01000000);
    }

    public static int encNegNormal(int i, int j, int k) {
        int d = 4;
        i *= -d;
        j *= -d;
        k *= -d;
        byte byte0 = (byte)i;
        byte byte1 = (byte)j;
        byte byte2 = (byte)k;
        return byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16 | (0x01000000);
    }

    final static int[][] faceVDirections = new int[6][];
    final static int[][] faceVDirectionsNeg = new int[6][];
    static void initDir() {
        faceVDirections[Dir.DIR_NEG_Y] = new int[] {
                encNormal(-1,  0, -1),
                encNormal(-1,  0,  1), 
                encNormal( 1,  0,  1), 
                encNormal( 1,  0, -1), 
            };
        faceVDirections[Dir.DIR_POS_Y] = new int[] {
                encNormal(-1,  0, -1),
                encNormal(-1,  0,  1), 
                encNormal( 1,  0,  1), 
                encNormal( 1,  0, -1), 
            };
        faceVDirections[Dir.DIR_NEG_X] = new int[] {
                encNormal(0, -1, -1),
                encNormal(0,  1, -1), 
                encNormal(0,  1,  1), 
                encNormal(0, -1,  1), 
            };
        faceVDirections[Dir.DIR_POS_X] = new int[] {
                encNormal(0, -1, -1),
                encNormal(0,  1, -1), 
                encNormal(0,  1,  1), 
                encNormal(0, -1,  1), 
            };
        faceVDirections[Dir.DIR_NEG_Z] = new int[] {
                encNormal(-1, -1, 0),
                encNormal( 1, -1, 0),  
                encNormal( 1,  1, 0), 
                encNormal(-1,  1, 0),
            };
        faceVDirections[Dir.DIR_POS_Z] = new int[] {
                encNormal(-1, -1, 0),
                encNormal( 1, -1, 0), 
                encNormal( 1,  1, 0), 
                encNormal(-1,  1, 0), 
            };
        
        
        

        faceVDirectionsNeg[Dir.DIR_NEG_Y] = new int[] {
                encNegNormal(-1,  0, -1),
                encNegNormal(-1,  0,  1), 
                encNegNormal( 1,  0,  1), 
                encNegNormal( 1,  0, -1), 
            };
        faceVDirectionsNeg[Dir.DIR_POS_Y] = new int[] {
                encNegNormal(-1,  0, -1),
                encNegNormal(-1,  0,  1), 
                encNegNormal( 1,  0,  1), 
                encNegNormal( 1,  0, -1), 
            };
        faceVDirectionsNeg[Dir.DIR_NEG_X] = new int[] {
                encNegNormal(0, -1, -1),
                encNegNormal(0,  1, -1), 
                encNegNormal(0,  1,  1), 
                encNegNormal(0, -1,  1), 
            };
        faceVDirectionsNeg[Dir.DIR_POS_X] = new int[] {
                encNegNormal(0, -1, -1),
                encNegNormal(0,  1, -1), 
                encNegNormal(0,  1,  1), 
                encNegNormal(0, -1,  1), 
            };
        faceVDirectionsNeg[Dir.DIR_NEG_Z] = new int[] {
                encNegNormal(-1, -1, 0),
                encNegNormal( 1, -1, 0),  
                encNegNormal( 1,  1, 0), 
                encNegNormal(-1,  1, 0),
            };
        faceVDirectionsNeg[Dir.DIR_POS_Z] = new int[] {
                encNegNormal(-1, -1, 0),
                encNegNormal( 1, -1, 0), 
                encNegNormal( 1,  1, 0), 
                encNegNormal(-1,  1, 0), 
            };
    }
    static {
        initDir();
    }


    public BlockFace(BlockSurface bs, int[] pos, int[] du, int[] dv, int u, int v, int w, int h) {
        this.bs = bs;
        this.pos = pos;
        this.du = du; 
        this.dv = dv;
        this.w = w;
        this.h = h;
        this.v0 = new float[3];
        this.v1 = new float[3];
        this.v2 = new float[3];
        this.v3 = new float[3];
        int side = bs.axis<<1|bs.face;

        if (bs.pass == 2) {
            //extend faces to fix flickering from t-junctions
            float[] fdu = new float[] { du[0], du[1], du[2] };
            float[] fdv = new float[] { dv[0], dv[1], dv[2] };
            float d = -2E-3F; 
            fdu[u] -= d;
            fdv[v] -= d;
            for (int i = 0; i < 3; i++) {
                float fPos = this.pos[i];
                if (i == u) {
                    fPos += d / 2.0F;
                }
                if (i == v) {
                    fPos += d / 2.0F;
                }
                this.v0[i] = fPos;
                this.v1[i] = fPos + fdu[i];
                this.v2[i] = fPos + fdu[i] + fdv[i];
                this.v3[i] = fPos + fdv[i];
            }

        } else { //extension happens in shader
            for (int i = 0; i < 3; i++) {
                float fPos = this.pos[i];
                this.v0[i] = fPos;
                this.v1[i] = fPos + du[i];
                this.v2[i] = fPos + du[i] + dv[i];
                this.v3[i] = fPos + dv[i];
            }
        }
        
        if (bs.type == Block.water.id && bs.isAirAbove) {
            switch (side) {
                case Dir.DIR_NEG_Y:
                    break;
                case Dir.DIR_POS_Y:
                    this.v3[1]-=0.1f;
                    this.v0[1]-=0.1f;
                    this.v1[1]-=0.1f;
                    this.v2[1]-=0.1f;
                    break;
                case Dir.DIR_NEG_Z:
                    this.v3[1]-=0.1f;
                    this.v2[1]-=0.1f;
                    break;
                case Dir.DIR_POS_Z:
                    this.v3[1]-=0.1f;
                    this.v2[1]-=0.1f;
                    break;
                case Dir.DIR_NEG_X:
                    this.v1[1]-=0.1f;
                    this.v2[1]-=0.1f;
                    break;
                case Dir.DIR_POS_X:
                    this.v1[1]-=0.1f;
                    this.v2[1]-=0.1f;
                    break;
            }
        }
        byte[] normal=null;
        switch (side) {
            case Dir.DIR_NEG_Y:
                normal = new byte[] {0,-1,0};
                break;
            case Dir.DIR_POS_Y:
                normal = new byte[] {0,1,0};
                break;
            case Dir.DIR_NEG_Z:
                normal = new byte[] {0,0,-1};
                break;
            case Dir.DIR_POS_Z:
                normal = new byte[] {0,0,1};
                break;
            case Dir.DIR_NEG_X:
                normal = new byte[] {-1,0,0};
                break;
            case Dir.DIR_POS_X:
                normal = new byte[] {1,0,0};
                break;
        }
        this.normal = normal;
    }

    /**
     * Draw basic block face
     *
     * @param attr the attr
     * @param buffer the buffer
     * @param offset the offset
     * @return the number of faces drawn (vertex count / 4)
     */
    public int drawBasic(BlockFaceAttr attr, VertexBuffer vertexBuffer) {
        attr.setReverse((this.bs.face&1)!=0);
        attr.v0.setPos(this.v0[0], this.v0[1], this.v0[2]);
        attr.v1.setPos(this.v1[0], this.v1[1], this.v1[2]);
        attr.v2.setPos(this.v2[0], this.v2[1], this.v2[2]);
        attr.v3.setPos(this.v3[0], this.v3[1], this.v3[2]);
        attr.putBasic(vertexBuffer);
        return 1;
    }

    /**
     * Draw shadow-textured block face
     *
     * @param attr the attr
     * @param buffer the buffer
     * @param offset the offset
     * @return the number of faces drawn (vertex count / 4)
     */
    public int drawShadowTextured(BlockFaceAttr attr, VertexBuffer vertexBuffer) {
        Block block = Block.get(this.bs.type);
        int tex = block.getTextureFromSide(this.faceDir);
        this.faceDir = this.bs.axis<<1|this.bs.face;
        attr.setFaceDir(faceDir);
        attr.setTex(tex);
        attr.setType(this.bs.type);
        attr.setReverse((this.bs.face&1)!=0);
        setUV(attr.v0, 0);
        attr.v0.setPos(this.v0[0], this.v0[1], this.v0[2]);
        setUV(attr.v1, 1);
        attr.v1.setPos(this.v1[0], this.v1[1], this.v1[2]);
        setUV(attr.v2, 2);
        attr.v2.setPos(this.v2[0], this.v2[1], this.v2[2]);
        setUV(attr.v3, 3);
        attr.v3.setPos(this.v3[0], this.v3[1], this.v3[2]);
        attr.putShadowTextured(vertexBuffer);
        return 1;
    }
    
    public int draw(BlockFaceAttr attr, VertexBuffer vertexBuffer) {
        attr.setNormal(this.normal[0], this.normal[1], this.normal[2]);
        Block block = Block.get(this.bs.type);
        this.faceDir = this.bs.axis<<1|this.bs.face;
        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getColorFromSide(this.faceDir);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;

        int tex = block.getTextureFromSide(this.faceDir);
        attr.setTex(tex);
        attr.setFaceDir(faceDir);
        attr.setReverse((this.bs.face&1)!=0);
//
//        attr.(0xf00000);
        attr.setAO(this.bs.maskedAO);
        attr.setLight(this.bs.maskedLightSky, this.bs.maskedLightBlock);
        attr.setType(this.bs.type);
        
        setUV(attr.v0, 0);
        attr.v0.setColorRGBAF(b * m, g * m, r * m, alpha);
        attr.v0.setPos(this.v0[0], this.v0[1], this.v0[2]);
        attr.v0.setFaceVertDir(faceVDirections[this.faceDir][0]);

        setUV(attr.v1, 1);
        attr.v1.setColorRGBAF(b * m, g * m, r * m, alpha);
        attr.v1.setPos(this.v1[0], this.v1[1], this.v1[2]);
        attr.v1.setFaceVertDir(faceVDirections[this.faceDir][1]);

        setUV(attr.v2, 2);
        attr.v2.setColorRGBAF(b * m, g * m, r * m, alpha);
        attr.v2.setPos(this.v2[0], this.v2[1], this.v2[2]);
        attr.v2.setFaceVertDir(faceVDirections[this.faceDir][2]);

        setUV(attr.v3, 3);
        attr.v3.setColorRGBAF(b * m, g * m, r * m, alpha);
        attr.v3.setPos(this.v3[0], this.v3[1], this.v3[2]);
        attr.v3.setFaceVertDir(faceVDirections[this.faceDir][3]);
        attr.put(vertexBuffer);
        if (block == Block.grass && this.faceDir != Dir.DIR_POS_Y && this.faceDir != Dir.DIR_NEG_Y) {
            int sideOverlay = BlockTextureArray.getInstance().getTextureIdx(Block.grass.id, 2);
            attr.setTex(sideOverlay);
            c = block.getColorFromSide(Dir.DIR_POS_Y);
            b = (c & 0xFF) / 255F;
            c >>= 8;
            g = (c & 0xFF) / 255F;
            c >>= 8;
            r = (c & 0xFF) / 255F;
            for (int i = 0; i < 4; i++) {
                attr.v[i].setColorRGBAF(b * m, g * m, r * m, alpha);
            }
            attr.put(vertexBuffer);
            return 2;
        }
        if (block instanceof BlockLeaves) {
//            initDir();
            int face = 1-this.bs.face;
            this.faceDir = this.bs.axis<<1|face;
            for (int i = 0; i < 4; i++) {
                attr.v[i].setFaceVertDir(faceVDirectionsNeg[this.faceDir][i]);
            }
            attr.setPass(3);
            attr.setFaceDir(faceDir);
            attr.setReverse((face&1)!=0);
            attr.setNormal(-this.normal[0], -this.normal[1], -this.normal[2]);
            attr.put(vertexBuffer);
            return 2;
        }
        return 1;
    }

    private void setUV(BlockFaceVert tess, int idx) {
        float u = 0; float v = 0;
        // 0 0 -> 0, 0
        // 0 1 -> 1, 0
        // 1 0 -> 1, 1
        // 1 1 -> 0, 1
        if (this.bs.axis==0) {
            idx = (idx+1)&0x3;
        }
        if (this.bs.axis==1) {
            idx = (idx+2)&0x3;
        }
        int y = idx>>1;
        int x = (idx&1)^y;
        if (this.bs.axis==1) {
            y = 1 - y;
            x = 1 - x;
        }
        else if (this.bs.face==1) {
            x = 1 - x;
        }
        switch (this.bs.axis) {
            case 0:
                u = this.dv[2]*x;
                v = this.du[1]*y;
                break;
            case 1:
                v = this.du[2]*x;
                u = this.dv[0]*y;
                break;
            case 2:
                u = this.du[0]*x;
                v = this.dv[1]*y;
                break;
        }
        tess.setUV(u, v);
    }
}