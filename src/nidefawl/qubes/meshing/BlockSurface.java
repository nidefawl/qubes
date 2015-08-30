package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionCache;

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
    public Region region;
    public int ao0 = 0;
    public int ao1 = 0;
    public int ao2 = 0;
    public int ao3 = 0;
    public boolean rotateVertex;
    public boolean extraFace;
    
    public void calcAO(RegionCache cache) {
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
        } else {
            ao0 = 2;
            ao1 = 2;
            ao2 = 2;
            ao3 = 2;
        }
    }

    int vertexAO(int side1, int side2, int corner) {
        int ao = 2;
        if (isOpaque(side1)) 
            ao--;
        if (isOpaque(side2)) 
            ao--;
        if (ao>0&&isOpaque(corner)) 
            ao--;
        return ao;
    }

    private boolean isOpaque(int side) {
        return side > 0 && !Block.block[side].isTransparent();
    }

    private void calcPosZ(RegionCache cache) {
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
        this.ao0 = vertexAO(cn, nc, nn);
        this.ao1 = vertexAO(cn, pc, pn);
        this.ao2 = vertexAO(cp, pc, pp);
        this.ao3 = vertexAO(cp, nc, np);
    }

    private void calcNegZ(RegionCache cache) {
        int x = this.x;
        int y = this.y;
        int z = this.z - 1;
        if (isOpaque(cache.getTypeId(x, y, z))) {
            return;
        }
        int pp = cache.getTypeId(x + 1, y + 1, z);
        int np = cache.getTypeId(x - 1, y + 1, z);
        int pn = cache.getTypeId(x + 1, y - 1, z);
        int nn = cache.getTypeId(x - 1, y - 1, z);
        int cn = cache.getTypeId(x, y - 1, z);
        int nc = cache.getTypeId(x - 1, y, z);
        int cp = cache.getTypeId(x, y + 1, z);
        int pc = cache.getTypeId(x + 1, y, z);
        this.ao0 = vertexAO(cn, pc, pn);
        this.ao1 = vertexAO(cn, nc, nn);
        this.ao2 = vertexAO(cp, nc, np);
        this.ao3 = vertexAO(cp, pc, pp);
    }

    private void calcPosX(RegionCache cache) {
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
        this.ao0 = vertexAO(cn, nc, nn);//bottom right
        this.ao1 = vertexAO(cn, pc, pn); //top right
        this.ao2 = vertexAO(cp, pc, pp); //top left
        this.ao3 = vertexAO(cp, nc, np); //bottom left
    }
    private void calcNegX(RegionCache cache) {
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
        this.ao0 = vertexAO(cn, pc, pn);//top left
        this.ao1 = vertexAO(cn, nc, nn);//bottom left
        this.ao2 = vertexAO(cp, nc, np); //bottom right
        this.ao3 = vertexAO(cp, pc, pp); //top right
    }
    private void calcPosY(RegionCache cache) {
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
        this.ao0 = vertexAO(cn, nc, nn);
        this.ao1 = vertexAO(cp, nc, np);
        this.ao2 = vertexAO(cp, pc, pp);
        this.ao3 = vertexAO(cn, pc, pn);
    }
    private void calcNegY(RegionCache cache) {
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
        this.ao0 = vertexAO(cp, nc, np);
        this.ao1 = vertexAO(cn, nc, nn);
        this.ao2 = vertexAO(cn, pc, pn);
        this.ao3 = vertexAO(cp, pc, pp);
    }


    public boolean mergeWith(BlockSurface c) {
        if (c.type == this.type && c.face == this.face && c.pass == this.pass && c.extraFace == this.extraFace) {
            return this.ao0 == c.ao0 && this.ao1 == c.ao1 && this.ao2 == c.ao2 && this.ao3 == c.ao3;
//            if (Math.abs(this.x-c.x)>3||Math.abs(this.z-c.z)>3) {
//                System.out.println("no");
//                return false;
//            }
//            int bface = ;
//            if (bface==2) {
//                return false;
//            }
//            return true;
        }
        return false;
    }

}
