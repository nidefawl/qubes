package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.world.IBlockWorld;

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
    public boolean extraFace;
    protected boolean resolved;
    public boolean calcLight;
    public int maskedLightSky = 0;
    public int maskedLightBlock = 0;
    public int maskedAO = 0;
    boolean isAirAbove;
    public int texture;
    public boolean renderTypeTransition;
    public int faceColor;
    
    
    public static int maskAO(int ao0, int ao1, int ao2, int ao3) {
        return ((ao3&0x3)<<6)|((ao2&0x3)<<4)|((ao1&0x3)<<2)|(ao0&0x3);
    }
    public static int mix_light(int br0, int br1, int br2, int br3) {
      // shift the upper nibble up by 4 bits so the overflow (bit 4-7) can be masked out later
      br0 = (br0&0xF) | (br0&0xF0)<<4;
      br1 = (br1&0xF) | (br1&0xF0)<<4;
      br2 = (br2&0xF) | (br2&0xF0)<<4;
      br3 = (br3&0xF) | (br3&0xF0)<<4;
      return (br0+br1+br2+br3) >> 2;
  }
    public void maskLight(int ao0, int ao1, int ao2, int ao3, Block block) {
        int sky = 0;
        sky |= (ao3>>8)&0xF; // shift down by 8, skylight is now in upper byte (mix_light shifted it there), then mask out the overflow
        sky <<= 4;
        sky |= (ao2>>8)&0xF;
        sky <<= 4;
        sky |= (ao1>>8)&0xF;
        sky <<= 4;
        sky |= (ao0>>8)&0xF;
        int self = block.getLightValue();
        int blockLight = 0;
        blockLight |= Math.max((ao3)&0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao2)&0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao1)&0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao0)&0xF, self);
        this.maskedLightBlock = blockLight;
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

    public void calcAO(IBlockWorld cache) {
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

    int vertexAO(boolean side1, boolean side2, boolean corner) {
        int ao = 3;
        if (!(side1)) 
            ao--;
        if (!(side2)) 
            ao--;
        if (!(corner)) 
            ao--;
        return ao;
    }

    /**
     * @param side2
     * @return
     */
    private boolean isOccludingId(int id) {
        Block b = Block.get(id);
        return b != null && b.isOccluding();
    }
    private boolean isOccludingAt(IBlockWorld w, int x, int y, int z) {
        int id = w.getType(x, y, z);
        Block b = Block.get(id);
        if (b != null) {
            if (b.isOccludingBlock(w, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    private void calcPosZ(IBlockWorld cache) {
        int x = this.x;
        int y = this.y;
        int z = this.z+1;
        if (isOccludingAt(cache, x, y, z)) {
            return;
        }
        boolean pp = isOccludingAt(cache, x+1, y+1, z);
        boolean np = isOccludingAt(cache, x-1, y+1, z);
        boolean pn = isOccludingAt(cache, x+1, y-1, z);
        boolean nn = isOccludingAt(cache, x-1, y-1, z);
        boolean cn = isOccludingAt(cache, x, y-1, z);
        boolean nc = isOccludingAt(cache, x-1, y, z);
        boolean cp = isOccludingAt(cache, x, y+1, z);
        boolean pc = isOccludingAt(cache, x+1, y, z);
        

        int brigthness = cache.getLight(x, y, z);
        int br_cn = cache.getLight(x, y - 1, z);
        int br_nc = cache.getLight(x - 1, y, z);
        int br_pc = cache.getLight(x + 1, y, z);
        int br_cp = cache.getLight(x, y + 1, z);
        int br_pp = br_pc;
        int br_pn = br_pc;
        int br_nn = br_nc;
        int br_np = br_nc;
        if (!(cp) || !(pc)) {
            br_pp = cache.getLight(x + 1, y + 1, z);
        }
        if (!(pc) || !(cn)) {
            br_pn = cache.getLight(x + 1, y - 1, z);
        }
        if (!(cn) || !(nc)) {
            br_nn = cache.getLight(x - 1, y - 1, z);
        }
        if (!(nc) || !(cp)) {
            br_np = cache.getLight(x - 1, y + 1, z);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brPN, brPP, brNP, Block.get(this.type));

        if (Block.get(this.type).applyAO()) {
            int ao0 = vertexAO(cn, nc, nn);
            int ao1 = vertexAO(cn, pc, pn);
            int ao2 = vertexAO(cp, pc, pp);
            int ao3 = vertexAO(cp, nc, np);
            this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
        }
    }
    
    private void calcNegZ(IBlockWorld cache) {
        int x = this.x;
        int y = this.y;
        int z = this.z - 1;
        if (isOccludingAt(cache, x, y, z)) {
            return;
        }

        
        boolean pp = isOccludingAt(cache, x + 1, y + 1, z);
        boolean np = isOccludingAt(cache, x - 1, y + 1, z);
        boolean pn = isOccludingAt(cache, x + 1, y - 1, z);
        boolean nn = isOccludingAt(cache, x - 1, y - 1, z);
        boolean cn = isOccludingAt(cache, x, y - 1, z);
        boolean nc = isOccludingAt(cache, x - 1, y, z);
        boolean cp = isOccludingAt(cache, x, y + 1, z);
        boolean pc = isOccludingAt(cache, x + 1, y, z);
        
        int brigthness = cache.getLight(x, y, z);
        int br_cn = cache.getLight(x, y - 1, z);
        int br_nc = cache.getLight(x - 1, y, z);
        int br_pc = cache.getLight(x + 1, y, z);
        int br_cp = cache.getLight(x, y + 1, z);
        int br_pp = br_pc;
        int br_pn = br_pc;
        int br_nn = br_nc;
        int br_np = br_nc;
        if (!cp || !pc) {
            br_pp = cache.getLight(x + 1, y + 1, z);
        }
        if (!cn || !pc) {
            br_pn = cache.getLight(x + 1, y - 1, z);
        }
        if (!cn || !nc) {
            br_nn = cache.getLight(x - 1, y - 1, z);
        }
        if (!cp || !nc) {
            br_np = cache.getLight(x - 1, y + 1, z);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brPN, brNN, brNP, brPP, Block.get(this.type));

        if (Block.get(this.type).applyAO()) {
            int ao0 = vertexAO(cn, pc, pn);
            int ao1 = vertexAO(cn, nc, nn);
            int ao2 = vertexAO(cp, nc, np);
            int ao3 = vertexAO(cp, pc, pp);
            this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
        }
    }

    private void calcPosX(IBlockWorld cache) {
        int x = this.x+1;
        int y = this.y;
        int z = this.z;
        if (isOccludingAt(cache, x, y, z)) {
            return;
        }
        boolean pp = isOccludingAt(cache, x, y+1, z+1);
        boolean pn = isOccludingAt(cache, x, y+1, z-1);
        boolean np = isOccludingAt(cache, x, y-1, z+1);
        boolean nn = isOccludingAt(cache, x, y-1, z-1);
        boolean nc = isOccludingAt(cache, x, y-1, z);
        boolean cn = isOccludingAt(cache, x, y, z-1);
        boolean pc = isOccludingAt(cache, x, y+1, z);
        boolean cp = isOccludingAt(cache, x, y, z+1);
        
        int brigthness = cache.getLight(x, y, z);
        int br_nc = cache.getLight(x, y-1, z);
        int br_cn = cache.getLight(x, y, z-1);
        int br_cp = cache.getLight(x, y, z+1);
        int br_pc = cache.getLight(x, y+1, z);
        int br_pp = br_cp;
        int br_pn = br_cn;
        int br_nn = br_cn;
        int br_np = br_cp;
        if (!cp || !pc) {
            br_pp = cache.getLight(x, y+1, z+1);
        }
        if (!cp || !nc) {
            br_np = cache.getLight(x, y-1, z+1);
        }
        if (!cn || !nc) {
            br_nn = cache.getLight(x, y-1, z-1);
        }
        if (!cn || !pc) {
            br_pn = cache.getLight(x, y+1, z-1);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNP, brNN, brPN, brPP, Block.get(this.type));

        if (Block.get(this.type).applyAO()) {
            int ao1 = vertexAO(cn, nc, nn);//bottom right
            int ao2 = vertexAO(cn, pc, pn); //top right
            int ao3 = vertexAO(cp, pc, pp); //top left
            int ao0 = vertexAO(cp, nc, np); //bottom left
            this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
        }
    }
    private void calcNegX(IBlockWorld cache) {
        int x = this.x-1;
        int y = this.y;
        int z = this.z;
        if (isOccludingAt(cache, x, y, z)) {
            return;
        }
        boolean pp = isOccludingAt(cache, x, y+1, z+1);
        boolean pn = isOccludingAt(cache, x, y+1, z-1);
        boolean np = isOccludingAt(cache, x, y-1, z+1);
        boolean nn = isOccludingAt(cache, x, y-1, z-1);
        boolean nc = isOccludingAt(cache, x, y-1, z);
        boolean cn = isOccludingAt(cache, x, y, z-1);
        boolean pc = isOccludingAt(cache, x, y+1, z);
        boolean cp = isOccludingAt(cache, x, y, z+1);
        
        int brigthness = cache.getLight(x, y, z);
        int br_nc = cache.getLight(x, y-1, z);
        int br_cn = cache.getLight(x, y, z-1);
        int br_cp = cache.getLight(x, y, z+1);
        int br_pc = cache.getLight(x, y+1, z);
        int br_pp = br_cp;
        int br_pn = br_cn;
        int br_nn = br_cn;
        int br_np = br_cp;
        if (!cp || !pc) {
            br_pp = cache.getLight(x, y+1, z+1);
        }
        if (!cp || !nc) {
            br_np = cache.getLight(x, y-1, z+1);
        }
        if (!cn || !nc) {
            br_nn = cache.getLight(x, y-1, z-1);
        }
        if (!cn || !pc) {
            br_pn = cache.getLight(x, y+1, z-1);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brNP, brPP, brPN, Block.get(this.type));

        if (Block.get(this.type).applyAO()) {
            int ao3 = vertexAO(cn, pc, pn);//top left
            int ao0 = vertexAO(cn, nc, nn);//bottom left
            int ao1 = vertexAO(cp, nc, np); //bottom right
            int ao2 = vertexAO(cp, pc, pp); //top right
            this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
        }
    }

    private void calcPosY(IBlockWorld cache) {
        int x = this.x;
        int y = this.y+1;
        int z = this.z;
        if (isOccludingAt(cache, x, y, z)) {
            return;
        }
        boolean pp = isOccludingAt(cache, x+1, y, z+1);
        boolean pn = isOccludingAt(cache, x+1, y, z-1);
        boolean np = isOccludingAt(cache, x-1, y, z+1);
        boolean nn = isOccludingAt(cache, x-1, y, z-1);
        boolean nc = isOccludingAt(cache, x-1, y, z);
        boolean cn = isOccludingAt(cache, x, y, z-1);
        boolean pc = isOccludingAt(cache, x+1, y, z);
        boolean cp = isOccludingAt(cache, x, y, z+1);
        

        int brigthness = cache.getLight(x, y, z);
        int br_nc = cache.getLight(x-1, y, z);
        int br_cn = cache.getLight(x, y, z-1);
        int br_cp = cache.getLight(x, y, z+1);
        int br_pc = cache.getLight(x+1, y, z);
        int br_pp = br_cp;
        int br_pn = br_cn;
        int br_nn = br_cn;
        int br_np = br_cp;
        if (!cp || !pc) {
            br_pp = cache.getLight(x+1, y, z+1);
        }
        if (!cp || !nc) {
            br_np = cache.getLight(x-1, y, z+1);
        }
        if (!cn || !nc) {
            br_nn = cache.getLight(x-1, y, z-1);
        }
        if (!cn || !pc) {
            br_pn = cache.getLight(x+1, y, z-1);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brPN, brPP, brNP, Block.get(this.type));
        

        if (Block.get(this.type).applyAO()) {
            int ao0 = vertexAO(cn, nc, nn);
            int ao3 = vertexAO(cp, nc, np);
            int ao2 = vertexAO(cp, pc, pp);
            int ao1 = vertexAO(cn, pc, pn);
            this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
        }
    }
    private void calcNegY(IBlockWorld cache) {
        int x = this.x;
        int y = this.y-1;
        int z = this.z;
        if (isOccludingAt(cache, x, y, z)) {
            return;
        }
        boolean pp = isOccludingAt(cache, x+1, y, z+1);
        boolean pn = isOccludingAt(cache, x+1, y, z-1);
        boolean np = isOccludingAt(cache, x-1, y, z+1);
        boolean nn = isOccludingAt(cache, x-1, y, z-1);
        boolean nc = isOccludingAt(cache, x-1, y, z);
        boolean cn = isOccludingAt(cache, x, y, z-1);
        boolean pc = isOccludingAt(cache, x+1, y, z);
        boolean cp = isOccludingAt(cache, x, y, z+1);
        
        int brigthness = cache.getLight(x, y, z);
        int br_nc = cache.getLight(x-1, y, z);
        int br_cn = cache.getLight(x, y, z-1);
        int br_cp = cache.getLight(x, y, z+1);
        int br_pc = cache.getLight(x+1, y, z);
        int br_pp = br_cp;
        int br_pn = br_cn;
        int br_nn = br_cn;
        int br_np = br_cp;
        if (!cp || !pc) {
            br_pp = cache.getLight(x+1, y, z+1);
        }
        if (!cp || !nc) {
            br_np = cache.getLight(x-1, y, z+1);
        }
        if (!cn || !nc) {
            br_nn = cache.getLight(x-1, y, z-1);
        }
        if (!cn || !pc) {
            br_pn = cache.getLight(x+1, y, z-1);
        }
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brPN, brPP, brNP, Block.get(this.type));
        

        if (Block.get(this.type).applyAO()) {
            int ao0 = vertexAO(cn, nc, nn);
            int ao3 = vertexAO(cp, nc, np);
            int ao2 = vertexAO(cp, pc, pp);
            int ao1 = vertexAO(cn, pc, pn);
            this.maskedAO = maskAO(ao0, ao1, ao2, ao3);
        }
    }


    public boolean mergeWith(ChunkRenderCache cache, BlockSurface c) {
        if (!this.resolved)
            this.resolve(cache);
        if (!c.resolved)
            c.resolve(cache);
        if (c.type == this.type && c.face == this.face && c.pass == this.pass && c.extraFace == this.extraFace) {
            if (this.isAirAbove != c.isAirAbove) {
                return false;
            }
            if (this.calcLight && this.maskedAO != c.maskedAO) {
                return false;
            }
            if (this.calcLight && this.maskedLightBlock != c.maskedLightBlock) {
                return false;
            }
            if (this.calcLight && this.maskedLightSky != c.maskedLightSky) {
                return false;
            }
            if (c.texture != this.texture) {
                return false;
            }
            if (c.faceColor != this.faceColor) {
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
            this.maskedAO = 0;
            this.maskedLightBlock = 0;
            this.maskedLightSky = 0;
            this.calcAO(cache);
        }
        if (this.type == Block.water.id) {
            Block b = Block.get(cache.getType(x, y+1, z));
            if (b == Block.air) {
                if (cache.getWater(x,y+1,z)>0)
                {
                    b = Block.water;
                }
            }
            this.isAirAbove = b != Block.water;
        }
        Block block = Block.get(this.type);
        int dataVal = cache.getData(x, y, z);
        this.texture = block.getTexture(this.axis<<1|this.face, dataVal, 0);
//        if (block == Block.water && cache.getWater(x, y, z) > 0) {
//            if (cache.getWater(x, y-2, z)==0) {
//                this.texture = Block.ice.getTexture(this.axis<<1|this.face, dataVal, 0);
//            }
//        }
//        this.texture = block.getFaceTexture(cache, x, y, z, dataVal, this.axis<<1|this.face, 0);
        this.faceColor = block.getFaceColor(cache, x, y, z, this.axis<<1|this.face, -1);
    }

    public void reset() {
        this.maskedAO = 0;
        this.maskedLightBlock = 0;
        this.maskedLightSky = 0;
        this.type = 0;
        this.transparent = false;
        this.extraFace = false;
        this.resolved = false;
        this.isAirAbove = false;
        this.renderTypeTransition = false;
        this.calcLight = false;
        this.type = 0;
        this.face = 0;
        this.pass = 0;
        x = y = z = 0;
        texture = 0;
        axis = 0;

    }
    public BlockSurface copy() {
        BlockSurface surface = new BlockSurface();
        surface.maskedAO = this.maskedAO;
        surface.maskedLightBlock = this.maskedLightBlock;
        surface.maskedLightSky = this.maskedLightSky;
        surface.type = this.type;
        surface.transparent = this.transparent;
        surface.extraFace = this.extraFace;
        surface.resolved = this.resolved;
        surface.isAirAbove = this.isAirAbove;
        surface.renderTypeTransition = this.renderTypeTransition;
        surface.calcLight = this.calcLight;
        surface.pass = this.pass;
        surface.texture = this.texture;
        surface.face = this.face;
        surface.faceColor = this.faceColor;
        surface.axis = this.axis;
        surface.x = this.x;
        surface.y = this.y;
        surface.z = this.z;
        return surface;
    }

}
