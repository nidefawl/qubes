package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;

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
    public int ao0 = 0;
    public int ao1 = 0;
    public int ao2 = 0;
    public int ao3 = 0;
    public boolean extraFace;
    protected boolean resolved;
    public boolean hasAO;
    
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
        this.ao0 = vertexAO(cn, nc, nn);
        this.ao1 = vertexAO(cn, pc, pn);
        this.ao2 = vertexAO(cp, pc, pp);
        this.ao3 = vertexAO(cp, nc, np);
    }

    private void calcNegZ(ChunkRenderCache cache) {
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
        this.ao1 = vertexAO(cn, pc, pn);
        this.ao0 = vertexAO(cn, nc, nn);
        this.ao3 = vertexAO(cp, nc, np);
        this.ao2 = vertexAO(cp, pc, pp);
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
        this.ao1 = vertexAO(cn, nc, nn);//bottom right
        this.ao2 = vertexAO(cn, pc, pn); //top right
        this.ao3 = vertexAO(cp, pc, pp); //top left
        this.ao0 = vertexAO(cp, nc, np); //bottom left
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
        this.ao3 = vertexAO(cn, pc, pn);//top left
        this.ao0 = vertexAO(cn, nc, nn);//bottom left
        this.ao1 = vertexAO(cp, nc, np); //bottom right
        this.ao2 = vertexAO(cp, pc, pp); //top right
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
        this.ao0 = vertexAO(cn, nc, nn);
        this.ao1 = vertexAO(cp, nc, np);
        this.ao2 = vertexAO(cp, pc, pp);
        this.ao3 = vertexAO(cn, pc, pn);
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
        this.ao1 = vertexAO(cp, nc, np);
        this.ao0 = vertexAO(cn, nc, nn);
        this.ao3 = vertexAO(cn, pc, pn);
        this.ao2 = vertexAO(cp, pc, pp);
    }


    public boolean mergeWith(ChunkRenderCache cache, BlockSurface c) {
        if (!this.resolved)
            this.resolve(cache);
        if (!c.resolved)
            c.resolve(cache);
        if (c.type == this.type && c.face == this.face && c.pass == this.pass && c.extraFace == this.extraFace) {
            return this.ao0 == c.ao0 && this.ao1 == c.ao1 && this.ao2 == c.ao2 && this.ao3 == c.ao3;
        }
        return false;
    }

    void resolve(ChunkRenderCache cache) {
        this.resolved = true;
        if (this.hasAO) {
            this.calcAO(cache);
        }
    }

    public void reset() {
        ao0 = 0;
        ao1 = 0;
        ao2 = 0;
        ao3 = 0;
        this.type = 0;
        this.transparent = false;
        this.chunk = null;
        this.transparent = false;
        this.extraFace = false;
        this.type = 0;
        this.face = 0;
        this.pass = 0;
        x = y = z = 0;
        axis = 0;

    }

}
