package nidefawl.qubes.vec;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Tess;

public class Mesh {
    public final int type;
    public final int v0[];
    public final int v1[];
    public final int v2[];
    public final int v3[];
    public final int du[];
    public final int dv[];
    public final byte normal[];
    public int faceDir;

    public Mesh(int type, int[] v0, int[] v1, int[] v2, int[] v3, int[] du, int[] dv, byte[] normal, int faceDir) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.du = du;
        this.dv = dv;
        this.normal = normal;
        this.type = type;
        this.faceDir = faceDir;
    }

    public void draw(Tess tess) {
        tess.setNormals(this.normal[0], this.normal[1], this.normal[2]);
        Block block = Block.block[this.type & Block.BLOCK_MASK];
        int biome = (this.type >> 12) & 0xFF;
        int side = 0;
        float m = 1F;
        switch (this.faceDir) {
            case Dir.DIR_NEG_Y:
                m = 0.5F;
                break;
            case Dir.DIR_POS_Y:
                m = 1F;
                break;
            case Dir.DIR_NEG_Z:
                m = 0.8F;
                break;
            case Dir.DIR_POS_Z:
                m = 0.8F;
                break;
            case Dir.DIR_NEG_X:
                m = 0.6F;
                break;
            case Dir.DIR_POS_X:
                m = 0.6F;
                break;
        }
        float alpha = 1F;
        int c = block.getColor();
        if (block == Block.water) {
            alpha = 0.8F;
        }
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;
        tess.setColorRGBAF(b * m, g * m, r * m, alpha);
        tess.setBrightness(0xf00000);
        float xl = this.du[0] + this.du[1] + this.du[2];
        float yl = this.dv[0] + this.dv[1] + this.dv[2];
        tess.setAttr(block.getTextureFromSide(this.faceDir), 0, 0);
        tess.setUV(0, 0);
        tess.add(this.v0[0], this.v0[1], this.v0[2]);
        tess.setUV(xl, 0);
        tess.add(this.v1[0], this.v1[1], this.v1[2]);
        tess.setUV(xl, yl);
        tess.add(this.v2[0], this.v2[1], this.v2[2]);
        tess.setUV(0, yl);
        tess.add(this.v3[0], this.v3[1], this.v3[2]);
    
    }
}