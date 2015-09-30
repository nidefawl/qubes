/**
 * 
 */
package nidefawl.qubes.meshing;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.World;

import static nidefawl.qubes.meshing.BlockFaceAttr.*;
import static nidefawl.qubes.render.region.RegionRenderer.*;
import static nidefawl.qubes.render.WorldRenderer.*;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class BlockRenderer {

    private ChunkRenderCache ccache;
    private World            w;
    private BlockFaceAttr    attr;
    int[]                    buffer;

    /**
     * @param w
     * @param buffer 
     * @param ccache
     */
    public void preRender(World w, int[] buffer, ChunkRenderCache ccache, BlockFaceAttr attr) {
        this.w = w;
        this.ccache = ccache;
        this.attr = attr;
        this.buffer = buffer;
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

    public int render(int renderPass, int ix, int iy, int iz, int bufferIdx) {
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
                    return renderPlant(block, ix, iy, iz, bufferIdx);
            }
        }
        return 0;
    }

    int renderPlant(Block block, int ix, int iy, int iz, int bufferIdx) {
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
        bufferIdx += BlockFaceAttr.BLOCK_FACE_INT_SIZE;
//        return 1;
        
        attr.setReverse(true);
        {
            float nx=-nside;
            float nz=nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }
        
        
        attr.put(buffer, bufferIdx);
        bufferIdx += BlockFaceAttr.BLOCK_FACE_INT_SIZE;

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
        bufferIdx += BlockFaceAttr.BLOCK_FACE_INT_SIZE;
        attr.setReverse(true);
        {
            float nx=-nside;
            float nz=-nside;
            attr.v1.setNormal(nx, ny, nz); // set upward normal
            attr.v2.setNormal(nx, ny, nz); // set upward normal
        }

        
        attr.put(buffer, bufferIdx);
        bufferIdx += BlockFaceAttr.BLOCK_FACE_INT_SIZE;
        return 4;
    }
}
