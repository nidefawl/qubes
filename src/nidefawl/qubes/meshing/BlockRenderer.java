/**
 * 
 */
package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockStairs;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.World;

import static nidefawl.qubes.meshing.BlockFaceAttr.*;
import static nidefawl.qubes.render.region.RegionRenderer.*;
import static nidefawl.qubes.render.WorldRenderer.*;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class BlockRenderer {
    final static int[][] faceVDirections = BlockFace.faceVDirections;
    BlockSurface bs = new BlockSurface();
    private ChunkRenderCache ccache;
    private World            w;
    private BlockFaceAttr    attr;
    int[]                    buffer;
    int bufferIdx;
    int faceSize;
    final private AABBFloat bb = new AABBFloat();
    
    int[]       bufferShadow;
    int         shadowBufferIndex;
    private int shadowDrawMode;
    private int shadowFaceSize;
    public int  numShadowFaces;
    
    public void setDefaultBounds() {
        bb.set(0, 0, 0, 1, 1, 1);
    }

    /**
     * @param w
     * @param buffer 
     * @param faceSize 
     * @param ccache
     */
    public void preRender(World w, int[] buffer, int bufferIdx, int faceSize, ChunkRenderCache ccache, BlockFaceAttr attr) {
        this.w = w;
        this.ccache = ccache;
        this.attr = attr;
        this.buffer = buffer;
        this.faceSize = faceSize;
        this.bufferIdx = bufferIdx;
    }

    public void setShadowBuffer(int[] bufferShadow, int shadowBufferIndex, int shadowDrawMode, int shadowFaceSize) {
        this.bufferShadow = bufferShadow;
        this.shadowBufferIndex = shadowBufferIndex;
        this.shadowFaceSize = shadowFaceSize;
        this.shadowDrawMode = shadowDrawMode;
        this.numShadowFaces = 0;
    }

    public static int maskAO(int ao0, int ao1, int ao2, int ao3) {
        return ((ao3 & 0x3) << 6) | ((ao2 & 0x3) << 4) | ((ao1 & 0x3) << 2) | (ao0 & 0x3);
    }

    public static int mix_light(int br0, int br1, int br2, int br3) {
        // shift the upper nibble up by 4 bits so the overflow (bit 4-7) can be masked out later
        br0 = (br0 & 0xF) | (br0 & 0xF0) << 4;
        br1 = (br1 & 0xF) | (br1 & 0xF0) << 4;
        br2 = (br2 & 0xF) | (br2 & 0xF0) << 4;
        br3 = (br3 & 0xF) | (br3 & 0xF0) << 4;
        return (br0 + br1 + br2 + br3) >> 2;
    }

    public void maskLight(int ao0, int ao1, int ao2, int ao3, Block block) {
        int sky = 0;
        sky |= (ao3 >> 8) & 0xF;// shift down by 8, skylight is now in upper byte (mix_light shifted it there), then mask out the overflow
        sky <<= 4;
        sky |= (ao2 >> 8) & 0xF;
        sky <<= 4;
        sky |= (ao1 >> 8) & 0xF;
        sky <<= 4;
        sky |= (ao0 >> 8) & 0xF;
        int self = block.getLightValue();
        int blockLight = 0;
        blockLight |= Math.max((ao3) & 0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao2) & 0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao1) & 0xF, self);
        blockLight <<= 4;
        blockLight |= Math.max((ao0) & 0xF, self);
        attr.setLight(sky, blockLight);
    }

    private boolean isOccluding(int id) {
        return id > 0 && Block.block[id].isOccluding();
    }

    int getLight(int ix, int iy, int iz) {
        int i = ix & REGION_SIZE_BLOCKS_MASK;
        int k = iz & REGION_SIZE_BLOCKS_MASK;
        int chunkX = i >> Chunk.SIZE_BITS;
        int chunkZ = k >> Chunk.SIZE_BITS;
        Chunk chunk = ccache.get(chunkX, chunkZ);
        return chunk.getLight(i & 0xF, iy, k & 0xF);
    }

    public int render(int renderPass, int ix, int iy, int iz) {
        //        ccache
        int i = ix & REGION_SIZE_BLOCKS_MASK;
        int j = iy & SLICE_HEIGHT_BLOCK_MASK;
        int k = iz & REGION_SIZE_BLOCKS_MASK;
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            System.err.println("j == " + j + " should not happen");
            return 0;
        }
        int chunkX = i >> Chunk.SIZE_BITS;
        int chunkZ = k >> Chunk.SIZE_BITS;
        Chunk chunk = ccache.get(chunkX, chunkZ);
        if (chunk == null) {
            System.err.println("CHUNK IS NULL should not happen");
            return 0;
        }
        int type = chunk.getTypeId(i & 0xF, iy, k & 0xF);
        if (type < 1) {
            System.err.println("type == " + type + " should not happen (" + i + "," + j + "," + k + " - " + ix + "," + iy + "," + iz + ")");
            return 0;
        }
        //        light = chunk.getTypeId(i & 0xF, iy, k & 0xF);
        Block block = Block.block[type];
        int renderType = block.getRenderType();
        if (renderPass == PASS_LOD) {
            switch (renderType) {
                case 1:
                    return renderPlant(block, ix, iy, iz);
                case 2:
                    return renderBlock(block, ix, iy, iz); //SLAB
                case 3:
                    return renderStairs(block, ix, iy, iz);
            }
        }
        return 0;
    }


    void renderXNeg(Block block, float x, float y, float z) {
        attr.setNormal(-1, 0, 0);
        attr.v2.setUV(bb.maxZ, bb.maxY);
        attr.v3.setUV(bb.maxZ, bb.minY);
        attr.v0.setUV(bb.minZ, bb.minY);
        attr.v1.setUV(bb.minZ, bb.maxY);
        attr.v0.setPos(x + bb.minX, y + bb.minY, z + bb.minZ);
        attr.v1.setPos(x + bb.minX, y + bb.maxY, z + bb.minZ);
        attr.v2.setPos(x + bb.minX, y + bb.maxY, z + bb.maxZ);
        attr.v3.setPos(x + bb.minX, y + bb.minY, z + bb.maxZ);
    }

    void renderXPos(Block block, float x, float y, float z) {
        attr.setNormal(1, 0, 0);
        attr.v0.setUV(1-bb.minZ, bb.minY);
        attr.v1.setUV(1-bb.minZ, bb.maxY);
        attr.v2.setUV(1-bb.maxZ, bb.maxY);
        attr.v3.setUV(1-bb.maxZ, bb.minY);
        attr.v0.setPos(x + bb.maxX, y + bb.minY, z + bb.minZ);
        attr.v1.setPos(x + bb.maxX, y + bb.maxY, z + bb.minZ);
        attr.v2.setPos(x + bb.maxX, y + bb.maxY, z + bb.maxZ);
        attr.v3.setPos(x + bb.maxX, y + bb.minY, z + bb.maxZ);
    }
    void renderZNeg(Block block, float x, float y, float z) {
        attr.setNormal(0, 0, -1);
        attr.v0.setUV(1-bb.minX, bb.maxY);
        attr.v1.setUV(1-bb.minX, bb.minY);
        attr.v2.setUV(1-bb.maxX, bb.minY);
        attr.v3.setUV(1-bb.maxX, bb.maxY);
        attr.v0.setPos(x + bb.minX, y + bb.maxY, z + bb.minZ);
        attr.v1.setPos(x + bb.minX, y + bb.minY, z + bb.minZ);
        attr.v2.setPos(x + bb.maxX, y + bb.minY, z + bb.minZ);
        attr.v3.setPos(x + bb.maxX, y + bb.maxY, z + bb.minZ);
    }
    void renderZPos(Block block, float x, float y, float z) {
        attr.setNormal(0, 0, 1);
        attr.v0.setUV(bb.minX, bb.maxY);
        attr.v1.setUV(bb.minX, bb.minY);
        attr.v2.setUV(bb.maxX, bb.minY);
        attr.v3.setUV(bb.maxX, bb.maxY);
        attr.v0.setPos(x + bb.minX, y + bb.maxY, z + bb.maxZ);
        attr.v1.setPos(x + bb.minX, y + bb.minY, z + bb.maxZ);
        attr.v2.setPos(x + bb.maxX, y + bb.minY, z + bb.maxZ);
        attr.v3.setPos(x + bb.maxX, y + bb.maxY, z + bb.maxZ);
    }
    void renderYPos(Block block, float x, float y, float z) {
        attr.setNormal(0, 1, 0);// set upward normal
        //TODO: handle uv rotation
        attr.v0.setUV(bb.maxX, bb.minZ);
        attr.v1.setUV(bb.minX, bb.minZ);
        attr.v2.setUV(bb.minX, bb.maxZ);
        attr.v3.setUV(bb.maxX, bb.maxZ);
        
        attr.v0.setPos(x + bb.maxX, y + bb.maxY, z + bb.minZ);
        attr.v1.setPos(x + bb.minX, y + bb.maxY, z + bb.minZ);
        attr.v2.setPos(x + bb.minX, y + bb.maxY, z + bb.maxZ);
        attr.v3.setPos(x + bb.maxX, y + bb.maxY, z + bb.maxZ);
    }


    void renderYNeg(Block block, float x, float y, float z) {
        attr.setNormal(0, -1, 0);// set downward normal
        attr.v0.setUV(bb.maxX, bb.minZ);
        attr.v1.setUV(bb.minX, bb.minZ);
        attr.v2.setUV(bb.minX, bb.maxZ);
        attr.v3.setUV(bb.maxX, bb.maxZ);
        attr.v0.setPos(x + bb.maxX, y + bb.minY, z + bb.minZ);
        attr.v1.setPos(x + bb.minX, y + bb.minY, z + bb.minZ);
        attr.v2.setPos(x + bb.minX, y + bb.minY, z + bb.maxZ);
        attr.v3.setPos(x + bb.maxX, y + bb.minY, z + bb.maxZ);
    }
    
    int renderFace(Block block, int faceDir, float x, float y, float z) {
        switch (faceDir) {
            case 1:
                renderXNeg(block, x, y, z);
                break;
            case 0:
                renderXPos(block, x, y, z);
                break;
            case 3:
                renderYNeg(block, x, y, z);
                break;
            case 2:
                renderYPos(block, x, y, z);
                break;
            case 5:
                renderZNeg(block, x, y, z);
                break;
            case 4:
                renderZPos(block, x, y, z);
                break;
        }
        attr.put(buffer, bufferIdx);
        bufferIdx += this.faceSize;
        if (this.shadowDrawMode == 1) {
            attr.putShadowTextured(bufferShadow, this.shadowBufferIndex);
        } else {
            attr.putBasic(bufferShadow, this.shadowBufferIndex);
        }
        this.shadowBufferIndex += this.shadowFaceSize;
        this.numShadowFaces++;
        return 1;
    }
    private int renderStairs(Block block, int ix, int iy, int iz) {
        int data = this.w.getData(ix, iy, iz);
        int rot = data&3;
        int topBottom = (data&0x4)>>2;
//      
        int axis = (rot%2)==0?0:2;
        int side = rot/2;
        setBounds(0,0,0,1,1,1);
        int f = 0;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        setBounds(0,0,0,1,0.5f,1);
        if (topBottom>0){

            bb.minY+=0.5f;
            bb.maxY+=0.5f;
        }
        side = 1-side;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        axis = 2-axis;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        side = 1-side;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        side = 1-topBottom;
        axis = 1;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        BlockStairs.setStairBB(this.bb, (rot+2)&3, 1-topBottom, 0);
        side = topBottom;
        axis = 1;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        BlockStairs.setStairBB(this.bb, rot, topBottom, 1);
        side = topBottom;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        axis = (rot%2)==0?0:2;
        side = 1-(rot/2);
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        axis = 2-axis;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        side = 1-side;
        f+=getAndRenderBlockFace(block, ix, iy, iz, axis, side);
        return f;
    }
    /**
     * @param i
     * @param j
     * @param k
     * @param l
     * @param m
     */
    private void setBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.bb.minX = minX;
        this.bb.minY = minY;
        this.bb.minZ = minZ;
        this.bb.maxX = maxX;
        this.bb.maxY = maxY;
        this.bb.maxZ = maxZ;
    }

    int renderBlock(Block block, int ix, int iy, int iz) {
        setBlockBounds(block, ix, iy, iz);
        int f = 0;
        for (int n = 0; n < 6; n++) {
            f += getAndRenderBlockFace(block, ix, iy, iz, n/2, n%2);
        }
        return f;
    }

    /**
     * @param n
     */
    private void setFaceColor(Block block, int faceDir, float x, float y, float z) {
        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getColorFromSide(faceDir);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;
        int tex = block.getTextureFromSide(faceDir);
        attr.setTex(tex);
        attr.setFaceDir(faceDir);
        attr.setReverse((this.bs.face&1)!=0);
        attr.setAO(bs.maskedAO);
        attr.setLight(bs.maskedLightSky, bs.maskedLightBlock);
        attr.setType(bs.type);
        for (int v = 0; v < 4; v++) {
            attr.v[v].setColorRGBAF(b * m, g * m, r * m, alpha);
            int idx = v;
            if (faceDir/2>0) {
                idx = (idx + 3) % 4;
            }
            attr.v[v].setFaceVertDir(faceVDirections[faceDir][idx]);
        }
        
    }

    /**
     * @param block
     * @param ix
     * @param iy
     * @param iz
     */
    private void setBlockBounds(Block block, int ix, int iy, int iz) {
        AABBFloat f = block.getRenderBlockBounds(w, ix, iy, iz, this.bb);
        if (f == null) {
            setDefaultBounds();
        }
    }
    private int getAndRenderBlockFace(Block block, int ix, int iy, int iz, int axis, int side) {
        BlockSurface surface = getSingleBlockSurface(block, ix, iy, iz, axis, side);
        if (surface != null) {
            int faceDir = axis<<1|side;
            setFaceColor(block, faceDir, ix, iy, iz);
            return renderFace(block, faceDir, ix, iy, iz);    
        }
        return 0;
    }
    
    private BlockSurface getSingleBlockSurface(Block block, int ix, int iy, int iz, int axis, int side) {
        int offx = axis == 0 ? 1-side*2 : 0;
        int offy = axis == 1 ? 1-side*2 : 0;
        int offz = axis == 2 ? 1-side*2 : 0;
        int neighbour = this.w.getType(ix+offx, iy+offy, iz+offz);
        Block b = Block.get(neighbour);
        if (b != null && !b.isFaceVisible(this.w, ix+offx, iy+offy, iz+offz, axis, side, block, bb)) {
            return null;
        }
        int data = this.w.getData(ix, iy, iz); //TODO: this is queried multiple times
        bs.x = ix & REGION_SIZE_BLOCKS_MASK;
        bs.y = iy;
        bs.z = iz & REGION_SIZE_BLOCKS_MASK;
        bs.axis = axis;
        bs.face = side;
        bs.type = block.id;
        bs.data = data;
        bs.transparent = block.isTransparent();
        bs.pass = block.getRenderPass();
        bs.extraFace = false;
        bs.calcLight = true;
        bs.isLeaves = false;
        bs.calcAO(this.ccache);
        return bs;
    }

    int renderPlant(Block block, int ix, int iy, int iz) {
        int brigthness = getLight(ix, iy, iz);

        float m = 1F;
        float alpha = block.getAlpha();
        int c = block.getColorFromSide(Dir.DIR_POS_Y);
        float b = (c & 0xFF) / 255F;
        c >>= 8;
        float g = (c & 0xFF) / 255F;
        c >>= 8;
        float r = (c & 0xFF) / 255F;

        int tex = block.getTextureFromSide(Dir.DIR_POS_Y);
        attr.setAO(0);
        attr.setTex(tex);
        attr.setFaceDir(Dir.DIR_POS_Y);

        int br_pp = brigthness;
        int br_cp = brigthness;
        int br_pc = brigthness;
        int br_nc = brigthness;
        int br_np = brigthness;
        int br_nn = brigthness;
        int br_cn = brigthness;
        int br_pn = brigthness;
        int brPP = mix_light(brigthness, br_pp, br_cp, br_pc);
        int brNP = mix_light(brigthness, br_np, br_cp, br_nc);
        int brNN = mix_light(brigthness, br_nn, br_cn, br_nc);
        int brPN = mix_light(brigthness, br_pn, br_cn, br_pc);
        maskLight(brNN, brPN, brPP, brNP, block);
        attr.setType(block.id);
      attr.setNormal(0, 1, 0); // set upward normal

        final long multiplier = 0x5DEECE66DL;
        final long addend = 0xBL;
        final long mask = (1L << 48) - 1;

        float x = ix;
        float y = iy;
        float z = iz;
        long seed = (ix * 5591 + iy * 19 + iz * 7919);
        long iR = ((multiplier * seed + addend) & mask);
        float fR = 0.6F;
        int n = 12;
        int ma = (1 << n) - 1;
        x += ((iR & ma) / (float) (ma)) * fR - 0.5f * fR;
        z += (((iR >> n) & ma) / (float) (ma)) * fR - 0.5f * fR;

        float h = 1f;
        float w = 1f;
        float sideOffset = 1 - w;
        for (int v = 0; v < 4; v++) {
            attr.v[v].setColorRGBAF(b * m, g * m, r * m, alpha);
            attr.v[v].setFaceVertDir(0);
        }

        attr.v0.setUV(sideOffset, 0);
        attr.v0.setPos(x + sideOffset, y, z + sideOffset);

        attr.v1.setUV(sideOffset, h);
        attr.v1.setPos(x + sideOffset, y + h, z + sideOffset);

        attr.v2.setUV(1 - sideOffset, h);
        attr.v2.setPos(x + 1 - sideOffset, y + h, z + 1 - sideOffset);

        attr.v3.setUV(1 - sideOffset, 0);
        attr.v3.setPos(x + 1 - sideOffset, y, z + 1 - sideOffset);
        
        
        attr.setReverse(false);
        float nup=0.9f;
        float nside=0.3f;
        float ny=nup;
        {
            float nx=nside;
            float nz=-nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }
        
        attr.put(buffer, bufferIdx);
        bufferIdx += this.faceSize;
//        return 1;
        
        attr.setReverse(true);
        {
            float nx=-nside;
            float nz=nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }
        
        
        attr.put(buffer, bufferIdx);
        bufferIdx += this.faceSize;

        attr.v0.setUV(sideOffset, 0);
        attr.v0.setPos(x + 1 - sideOffset, y, z + sideOffset);

        attr.v1.setUV(sideOffset, h);
        attr.v1.setPos(x + 1 - sideOffset, y + h, z + sideOffset);

        attr.v2.setUV(1 - sideOffset, h);
        attr.v2.setPos(x + sideOffset, y + h, z + 1 - sideOffset);

        attr.v3.setUV(1 - sideOffset, 0);
        attr.v3.setPos(x + sideOffset, y, z + 1 - sideOffset);

        attr.setReverse(false);
        {
            float nx=nside;
            float nz=nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }
        
        
        attr.put(buffer, bufferIdx);
        bufferIdx += this.faceSize;
        attr.setReverse(true);
        {
            float nx=-nside;
            float nz=-nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }

        
        attr.put(buffer, bufferIdx);
        bufferIdx += this.faceSize;
        return 4;
    }
}
