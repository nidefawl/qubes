package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;

public class BlockSurface {

    public BlockSurface() {
    }
    public boolean transparent;
    public int type;
    public int face;
    public int axis;
    public int x;
    public int y;
    public int z;
    public int pass;
    public Chunk chunk;
    public boolean extraFace;
    protected boolean resolved;
    public boolean calcLight;
    public int maskedLightSky = 0;
    public int maskedLightBlock = 0;
    public int maskedAO = 0;
    public static int maskAO(int ao0, int ao1, int ao2, int ao3) {
        return ((ao3&0x3)<<6)|((ao2&0x3)<<4)|((ao1&0x3)<<2)|(ao0&0x3);
    }
    private int mix_light(int br0, int br1, int br2, int br3) {
      // shift the upper nibble up by 4 bits so the overflow (bit 4-7) can be masked out later
      br0 = (br0&0xF) | (br0&0xF0)<<4;
      br1 = (br1&0xF) | (br1&0xF0)<<4;
      br2 = (br2&0xF) | (br2&0xF0)<<4;
      br3 = (br3&0xF) | (br3&0xF0)<<4;
      return (br0+br1+br2+br3) >> 2;
  }
    public void maskLight(int ao0, int ao1, int ao2, int ao3) {
        int sky = 0;
        sky |= (ao3>>8)&0xF; // shift down by 8, skylight is now in upper byte (mix_light shifted it there), then mask out the overflow
        sky <<= 4;
        sky |= (ao2>>8)&0xF;
        sky <<= 4;
        sky |= (ao1>>8)&0xF;
        sky <<= 4;
        sky |= (ao0>>8)&0xF;
        int block = 0;
        block |= (ao3)&0xF;
        block <<= 4;
        block |= (ao2)&0xF;
        block <<= 4;
        block |= (ao1)&0xF;
        block <<= 4;
        block |= (ao0)&0xF;
        this.maskedLightBlock = block;
        this.maskedLightSky = sky;
    }

    
    
    final static int[][] offset = new int[][] {
            new int[] {
                    1,  0,  0,
                },
            new int[] {
                   -1,  0,  0,
                },
            new int[] {
                    0,  1,  0,
                },
            new int[] {
                    0, -1,  0,
                },
            new int[] {
                    0,  0,  1
                },
            new int[] {
                    0,  0, -1
                },
    };
    private void calcLight(ChunkRenderCache cache) {
//        int face = this.axis<<1|this.face;
//        int[] off = offset[face];
//        int n = getLight(cache, off[0], off[1], off[2]);
//        int sky = n>>4;
//        this.maskedLightSky = maskLight(sky, sky, sky, sky);
    }
    

    private int getLight(ChunkRenderCache cache, int i, int j, int k) {
        int x = this.x+i;
        int y = this.y+j;
        int z = this.z+k;
        int type = cache.getTypeId(x, y, z);
        if (type == 0 || Block.block[type].isTransparent()) {
            return cache.getLight(x, y, z);
        }
        return 0;
    }


    public void calcAO(ChunkRenderCache cache) {
        if (Block.block[this.type].applyAO()) {
            int face = this.axis<<1|this.face;
            switch (face) {
                case 0:
                    calcPosX(cache);
                    return;
                case 1:
                    calcNegX(cache);
                    return;
                case 2:
                    calcPosY(cache);
                    return;
                case 3:
                    calcNegY(cache);
                    return;
                case 4:
                    calcPosZ(cache);
                    return;
                case 5:
                    calcNegZ(cache);
                    return;
            }
        }
    }

    int vertexAO(int side1, int side2, int corner) {
        int ao = 3;
        if (!isOpaque(side1)) 
            ao--;
        if (!isOpaque(side2)) 
            ao--;
        if (!isOpaque(corner)) 
            ao--;
        return ao;
    }

    private boolean isOpaque(int side) {
        return side > 0 && !Block.block[side].isTransparent();
    }

    private void calcPosZ(ChunkRenderCache cache) {
        int x = this.x;
        int y = this.y;
        int z = this.z+1;
        if (isOpaque(cache.getTypeId(x, y, z))) {
            return;
        }
        int pp = cache.getTypeId(x+1, y+1, z);
        int np = cache.getTypeId(x-1, y+1, z);
        int pn = cache.getTypeId(x+1, y-1, z);
        int nn = cache.getTypeId(x-1, y-1, z);
        int cn = cache.getTypeId(x, y-1, z);
        int nc = cache.getTypeId(x-1, y, z);
        int cp = cache.getTypeId(x, y+1, z);
        int pc = cache.getTypeId(x+1, y, z);
        

        int brigthness = cache.getLight(x, y, z);
        int br_cn = cache.getLight(x, y - 1, z);
        int br_nc = cache.getLight(x - 1, y, z);
        int br_pc = cache.getLight(x + 1, y, z);
        int br_cp = cache.getLight(x, y + 1, z);
        int br_pp = br_pc;
        int br_pn = br_pc;
        int br_nn = br_nc;
        int br_np = br_nc;
        if (!isOpaque(cp) || !isOpaque(pc)) {
            br_pp = cache.getLight(x + 1, y + 1, z);
        }
        if (!isOpaque(cp) || !isOpaque(nc)) {
            br_pn = cache.getLight(x + 1, y - 1, z);
        }
        if (!isOpaque(cn) || !isOpaque(nc)) {
            br_nn = cache.getLight(x - 1, y - 1, z);
        }
        if (!isOpaque(cn) || !isOpaque(pc)) {
            br_np = cache.getLight(x - 1, y + 1, z);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brPN, brPP, brNP);
        
        int ao0 = vertexAO(cn, nc, nn);
        int ao1 = vertexAO(cn, pc, pn);
        int ao2 = vertexAO(cp, pc, pp);
        int ao3 = vertexAO(cp, nc, np);
        this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
    }
    
    private void calcNegZ(ChunkRenderCache cache) {
        int x = this.x;
        int y = this.y;
        int z = this.z - 1;
        if (isOpaque(cache.getTypeId(x, y, z))) {
            return;
        }
        int absX = (this.x|(cache.get(0, 0).x)<<4);
        int absZ = (this.z|(cache.get(0, 0).z)<<4);

        
        int pp = cache.getTypeId(x + 1, y + 1, z);
        int np = cache.getTypeId(x - 1, y + 1, z);
        int pn = cache.getTypeId(x + 1, y - 1, z);
        int nn = cache.getTypeId(x - 1, y - 1, z);
        int cn = cache.getTypeId(x, y - 1, z);
        int nc = cache.getTypeId(x - 1, y, z);
        int cp = cache.getTypeId(x, y + 1, z);
        int pc = cache.getTypeId(x + 1, y, z);
        
        int brigthness = cache.getLight(x, y, z);
        int br_cn = cache.getLight(x, y - 1, z);
        int br_nc = cache.getLight(x - 1, y, z);
        int br_pc = cache.getLight(x + 1, y, z);
        int br_cp = cache.getLight(x, y + 1, z);
        int br_pp = br_pc;
        int br_pn = br_pc;
        int br_nn = br_nc;
        int br_np = br_nc;
        if (!isOpaque(cp) || !isOpaque(pc)) {
            br_pp = cache.getLight(x + 1, y + 1, z);
        }
        if (!isOpaque(cp) || !isOpaque(nc)) {
            br_pn = cache.getLight(x + 1, y - 1, z);
        }
        if (!isOpaque(cn) || !isOpaque(nc)) {
            br_nn = cache.getLight(x - 1, y - 1, z);
        }
        if (!isOpaque(cn) || !isOpaque(pc)) {
            br_np = cache.getLight(x - 1, y + 1, z);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brPN, brPP, brNP);
        
        int ao1 = vertexAO(cn, pc, pn);
        int ao0 = vertexAO(cn, nc, nn);
        int ao3 = vertexAO(cp, nc, np);
        int ao2 = vertexAO(cp, pc, pp);
        this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
    }

    private void calcPosX(ChunkRenderCache cache) {
        int x = this.x+1;
        int y = this.y;
        int z = this.z;
        if (isOpaque(cache.getTypeId(x, y, z))) {
            return;
        }
        int pp = cache.getTypeId(x, y+1, z+1);
        int pn = cache.getTypeId(x, y+1, z-1);
        int np = cache.getTypeId(x, y-1, z+1);
        int nn = cache.getTypeId(x, y-1, z-1);
        int nc = cache.getTypeId(x, y-1, z);
        int cn = cache.getTypeId(x, y, z-1);
        int pc = cache.getTypeId(x, y+1, z);
        int cp = cache.getTypeId(x, y, z+1);
        
        int brigthness = cache.getLight(x, y, z);
        int br_nc = cache.getLight(x, y-1, z);
        int br_cn = cache.getLight(x, y, z-1);
        int br_cp = cache.getLight(x, y, z+1);
        int br_pc = cache.getLight(x, y+1, z);
        int br_pp = br_cp;
        int br_pn = br_cn;
        int br_nn = br_cn;
        int br_np = br_cp;
        if (!isOpaque(cp) || !isOpaque(pc)) {
            br_pp = cache.getLight(x, y+1, z+1);
        }
        if (!isOpaque(cp) || !isOpaque(nc)) {
            br_np = cache.getLight(x, y-1, z+1);
        }
        if (!isOpaque(cn) || !isOpaque(nc)) {
            br_nn = cache.getLight(x, y-1, z-1);
        }
        if (!isOpaque(cn) || !isOpaque(pc)) {
            br_pn = cache.getLight(x, y+1, z-1);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNP, brNN, brPN, brPP);
        
        int ao1 = vertexAO(cn, nc, nn);//bottom right
        int ao2 = vertexAO(cn, pc, pn); //top right
        int ao3 = vertexAO(cp, pc, pp); //top left
        int ao0 = vertexAO(cp, nc, np); //bottom left
        this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
    }
    private void calcNegX(ChunkRenderCache cache) {
        int x = this.x-1;
        int y = this.y;
        int z = this.z;
        if (isOpaque(cache.getTypeId(x, y, z))) {
            return;
        }
        int pp = cache.getTypeId(x, y+1, z+1);
        int pn = cache.getTypeId(x, y+1, z-1);
        int np = cache.getTypeId(x, y-1, z+1);
        int nn = cache.getTypeId(x, y-1, z-1);
        int nc = cache.getTypeId(x, y-1, z);
        int cn = cache.getTypeId(x, y, z-1);
        int pc = cache.getTypeId(x, y+1, z);
        int cp = cache.getTypeId(x, y, z+1);
        
        int brigthness = cache.getLight(x, y, z);
        int br_nc = cache.getLight(x, y-1, z);
        int br_cn = cache.getLight(x, y, z-1);
        int br_cp = cache.getLight(x, y, z+1);
        int br_pc = cache.getLight(x, y+1, z);
        int br_pp = br_cp;
        int br_pn = br_cn;
        int br_nn = br_cn;
        int br_np = br_cp;
        if (!isOpaque(cp) || !isOpaque(pc)) {
            br_pp = cache.getLight(x, y+1, z+1);
        }
        if (!isOpaque(cp) || !isOpaque(nc)) {
            br_np = cache.getLight(x, y-1, z+1);
        }
        if (!isOpaque(cn) || !isOpaque(nc)) {
            br_nn = cache.getLight(x, y-1, z-1);
        }
        if (!isOpaque(cn) || !isOpaque(pc)) {
            br_pn = cache.getLight(x, y+1, z-1);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNP, brNN, brPN, brPP);
        
        int ao2 = vertexAO(cn, pc, pn);//top left
        int ao1 = vertexAO(cn, nc, nn);//bottom left
        int ao0 = vertexAO(cp, nc, np); //bottom right
        int ao3 = vertexAO(cp, pc, pp); //top right
        this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
    }

    private void calcPosY(ChunkRenderCache cache) {
        int x = this.x;
        int y = this.y+1;
        int z = this.z;
        if (isOpaque(cache.getTypeId(x, y, z))) {
            return;
        }
        int pp = cache.getTypeId(x+1, y, z+1);
        int pn = cache.getTypeId(x+1, y, z-1);
        int np = cache.getTypeId(x-1, y, z+1);
        int nn = cache.getTypeId(x-1, y, z-1);
        int nc = cache.getTypeId(x-1, y, z);
        int cn = cache.getTypeId(x, y, z-1);
        int pc = cache.getTypeId(x+1, y, z);
        int cp = cache.getTypeId(x, y, z+1);
        

        int brigthness = cache.getLight(x, y, z);
        int br_nc = cache.getLight(x-1, y, z);
        int br_cn = cache.getLight(x, y, z-1);
        int br_cp = cache.getLight(x, y, z+1);
        int br_pc = cache.getLight(x+1, y, z);
        int br_pp = br_cp;
        int br_pn = br_cn;
        int br_nn = br_cn;
        int br_np = br_cp;
        if (!isOpaque(cp) || !isOpaque(pc)) {
            br_pp = cache.getLight(x+1, y, z+1);
        }
        if (!isOpaque(cp) || !isOpaque(nc)) {
            br_np = cache.getLight(x-1, y, z+1);
        }
        if (!isOpaque(cn) || !isOpaque(nc)) {
            br_nn = cache.getLight(x-1, y, z-1);
        }
        if (!isOpaque(cn) || !isOpaque(pc)) {
            br_pn = cache.getLight(x+1, y, z-1);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brNP, brPP, brPN);
        
        
        int ao0 = vertexAO(cn, nc, nn);
        int ao1 = vertexAO(cp, nc, np);
        int ao2 = vertexAO(cp, pc, pp);
        int ao3 = vertexAO(cn, pc, pn);
        this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
    }
    private void calcNegY(ChunkRenderCache cache) {
        int x = this.x;
        int y = this.y-1;
        int z = this.z;
        if (isOpaque(cache.getTypeId(x, y, z))) {
            return;
        }
        int pp = cache.getTypeId(x+1, y, z+1);
        int pn = cache.getTypeId(x+1, y, z-1);
        int np = cache.getTypeId(x-1, y, z+1);
        int nn = cache.getTypeId(x-1, y, z-1);
        int nc = cache.getTypeId(x-1, y, z);
        int cn = cache.getTypeId(x, y, z-1);
        int pc = cache.getTypeId(x+1, y, z);
        int cp = cache.getTypeId(x, y, z+1);
        
        int brigthness = cache.getLight(x, y, z);
        int br_nc = cache.getLight(x-1, y, z);
        int br_cn = cache.getLight(x, y, z-1);
        int br_cp = cache.getLight(x, y, z+1);
        int br_pc = cache.getLight(x+1, y, z);
        int br_pp = br_cp;
        int br_pn = br_cn;
        int br_nn = br_cn;
        int br_np = br_cp;
        if (!isOpaque(cp) || !isOpaque(pc)) {
            br_pp = cache.getLight(x+1, y, z+1);
        }
        if (!isOpaque(cp) || !isOpaque(nc)) {
            br_np = cache.getLight(x-1, y, z+1);
        }
        if (!isOpaque(cn) || !isOpaque(nc)) {
            br_nn = cache.getLight(x-1, y, z-1);
        }
        if (!isOpaque(cn) || !isOpaque(pc)) {
            br_pn = cache.getLight(x+1, y, z-1);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brNP, brPP, brPN);
        
        
        int ao1 = vertexAO(cp, nc, np);
        int ao0 = vertexAO(cn, nc, nn);
        int ao3 = vertexAO(cn, pc, pn);
        int ao2 = vertexAO(cp, pc, pp);
        this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
    }


    public boolean mergeWith(ChunkRenderCache cache, BlockSurface c) {
        if (!this.resolved)
            this.resolve(cache);
        if (!c.resolved)
            c.resolve(cache);
        if (c.type == this.type && c.face == this.face && c.pass == this.pass && c.extraFace == this.extraFace) {
            if (this.calcLight && this.maskedAO != c.maskedAO) {
                return false;
            }
            if (this.calcLight && this.maskedLightBlock != c.maskedLightBlock) {
                return false;
            }
            if (this.calcLight && this.maskedLightSky != c.maskedLightSky) {
                return false;
            }
//            if (this.hasAO) {
//                if (this.ao0 != )
//            }
//            if (this.ao0 == c.ao0 && this.ao1 == c.ao1 && this.ao2 == c.ao2 && this.ao3 == c.ao3) {
//                this.light0 == c.light0
//            }
            return true;
        }
        return false;
    }

    void resolve(ChunkRenderCache cache) {
        this.resolved = true;
        if (this.calcLight) {
            this.calcAO(cache);
            this.calcLight(cache);
        }
    }

    public void reset() {
        this.maskedAO = 0;
        this.maskedLightBlock = 0;
        this.maskedLightSky = 0;
        this.type = 0;
        this.transparent = false;
        this.chunk = null;
        this.transparent = false;
        this.extraFace = false;
        this.calcLight = false;
        this.type = 0;
        this.face = 0;
        this.pass = 0;
        x = y = z = 0;
        axis = 0;

    }

}
