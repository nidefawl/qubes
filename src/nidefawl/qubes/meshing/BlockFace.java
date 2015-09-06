package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Tess;
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
        float d = 0;//-1E-3F;
//        if (bs.pass == 2) {
//            d = -0.03F;
//        }
//        d = 0;
        float[] fdu = new float[] { du[0], du[1], du[2] };
        float[] fdv = new float[] { dv[0], dv[1], dv[2] };
        fdu[u] -= d;
        fdv[v] -= d;
        for (int i = 0; i < 3; i++) {
            float fPos = this.pos[i];
            if (i == u) {
                fPos += d/2.0F;
            }
            if (i == v) {
                fPos += d/2.0F;
            }
            this.v0[i] = fPos;
            this.v1[i] = fPos + fdu[i];
            this.v2[i] = fPos + fdu[i] + fdv[i];
            this.v3[i] = fPos + fdv[i];
        }
        int side = bs.axis<<1|bs.face;
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


    public void drawBasic(BlockFaceAttr attr) {
        attr.v0.setPos(this.v0[0], this.v0[1], this.v0[2]);
        attr.v1.setPos(this.v1[0], this.v1[1], this.v1[2]);
        attr.v2.setPos(this.v2[0], this.v2[1], this.v2[2]);
        attr.v3.setPos(this.v3[0], this.v3[1], this.v3[2]);
    }
    
    public void draw(BlockFaceAttr attr) {
        attr.setNormal(this.normal[0], this.normal[1], this.normal[2]);
        Block block = Block.block[this.bs.type & Block.BLOCK_MASK];
        int side = this.bs.axis<<1|this.bs.face;
        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getColor();
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;

        int tex = block.getTextureFromSide(side);
        attr.setTex(tex);

        attr.setBrightness(0xf00000);
        
        attr.setAO(this.bs.ao0, this.bs.ao1, this.bs.ao2, this.bs.ao3);
        
        setUV(attr.v0, 0);
        attr.v0.setColorRGBAF(b * m, g * m, r * m, alpha);
        attr.v0.setPos(this.v0[0], this.v0[1], this.v0[2]);

        setUV(attr.v1, 1);
        attr.v1.setColorRGBAF(b * m, g * m, r * m, alpha);
        attr.v1.setPos(this.v1[0], this.v1[1], this.v1[2]);

        setUV(attr.v2, 2);
        attr.v2.setColorRGBAF(b * m, g * m, r * m, alpha);
        attr.v2.setPos(this.v2[0], this.v2[1], this.v2[2]);

        setUV(attr.v3, 3);
        attr.v3.setColorRGBAF(b * m, g * m, r * m, alpha);
        attr.v3.setPos(this.v3[0], this.v3[1], this.v3[2]);
        
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
        int y = idx>>1;
        int x = (idx&1)^y;
        switch (this.bs.axis) {
            case 0:
                u = this.dv[2]*x;
                v = this.du[1]*y;
                break;
            case 1:
                u = this.du[2]*x;
                v = this.dv[0]*y;
                break;
            case 2:
                u = this.du[0]*x;
                v = this.dv[1]*y;
                break;
        }
        tess.setUV(u, v);
    }
}