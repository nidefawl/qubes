package nidefawl.qubes.meshing;

import static nidefawl.qubes.render.region.RegionRenderer.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.perf.TimingHelper2;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.World;

public class Mesher {

    private final int[]    dims;
    private BlockSurface[] mask2;
    private final  BlockSurfaceAir air = new BlockSurfaceAir();
    private final  BlockSurfaceHidden hidden = new BlockSurfaceHidden();
    
 // THIS WILL NOT WORK WITH REGIONS > 32x32x32 (needs to be bigger data type + bigger array size)
    private final  short[] renderTypeBlocks = new short[1<<16]; 
    private int nextBlockIDX = 0;
    
    //TODO: implement cache to make memory efficient
    private final BlockSurface[] scratchPad = new BlockSurface[2048]; //shoudln't exceed 32*32*2 (2 sides) block faces
    int scratchpadidx = 0;
    @SuppressWarnings("rawtypes")
    List[] meshes = new List[WorldRenderer.NUM_PASSES];
    public Mesher() {
        for (int i = 0; i < scratchPad.length; i++)
            this.scratchPad[i] = new BlockSurface();
        for (int i = 0; i < meshes.length; i++)
            this.meshes[i] = new ArrayList<>();
        this.dims = new int[3];
        this.mask2 = new BlockSurface[World.MAX_WORLDHEIGHT*World.MAX_WORLDHEIGHT]; // max world height sqared
    }



    private int computeHeight(int i, int j, int n, int w, int v, int u, BlockSurface c) {
        int h = 1;
        while (j + h < dims[v]) {
            for (int k = 0; k < w; ++k) {
                BlockSurface bs = mask2[n + k + h * dims[u]];
                if (bs == null || !c.mergeWith(cache, bs)) {
                    return h;
                }
            }
            h++;
        }
        return h;
    }

    BlockSurface bs1 = null;
    BlockSurface bs2 = null;
    final private AABBFloat fullBB = new AABBFloat(0, 0, 0, 1, 1, 1);
    private BlockSurface[] extraWaterFaces = new BlockSurface[4500];
    private int extraIdx;
    private void setMask2(int n, int[] x, int[] dir, int axis) {
        // first handle everything inside of region
        if (bs1 != null && bs2 != null) {
            if (bs1 == air && bs2 == air) {
                mask2[n] = null;
                return;
            }
            if (bs1 == hidden && bs2 == hidden) {
                mask2[n] = null;
                return;
            }
            if (bs1 != air && bs2 == air) {
                
                //fix double faces on top of slice
                int rY = bs1.y >> SLICE_HEIGHT_BLOCK_BITS;
                if (rY < HEIGHT_SLICES - 1 && bs1.axis == 1 && rY == ySlice && (bs1.y & SLICE_HEIGHT_BLOCK_MASK) == SLICE_HEIGHT_BLOCK_MASK) {
                    mask2[n] = null;
                } else
                mask2[n] = bs1;
                return;
            }
            if (bs1 == air && bs2 != air) {
                //fix double faces on bottom of slice
                int rY = bs2.y >> SLICE_HEIGHT_BLOCK_BITS;
                if (rY > 0 && bs2.axis == 1 && rY == ySlice && (bs2.y & SLICE_HEIGHT_BLOCK_MASK) == 0) {
                    mask2[n] = null;
                } else
                mask2[n] = bs2;
                return;
            }
            if (bs1.transparent && !bs2.transparent) {
                if (bs1.axis == 1 && bs1.face == 0)
                    extraWaterFaces[extraIdx++]=bs1;
                mask2[n] = bs2;
                return;
            }
            if (bs2.transparent && !bs1.transparent) {
                if (bs2.axis == 1 && bs2.face == 0)
                    extraWaterFaces[extraIdx++]=bs2;
                mask2[n] = bs1;
                return;
            }
            if ((bs1.renderTypeTransition || bs2.renderTypeTransition)) {
                
                BlockSurface from = bs1.renderTypeTransition ? bs2 : bs1;
                BlockSurface to = from == bs1 ? bs2 : bs1;
                int side = from == bs1 ? 0 : 1;
                Block b = Block.get(to.type);
                Block bfrom = Block.get(from.type);
                if (b != null && !b.isFaceVisible(this.cache, to.x, to.y, to.z, axis, side, bfrom, fullBB)) {
                    mask2[n] = null;
                    return;
                }
                mask2[n] = from;
                return;
            }
//            if (bs1.pass == bs2.pass && ((!bs1.transparent && !bs2.transparent)  || bs1.pass > 0)) {
//                mask2[n] = null;
//                return;
//            }
            
            if (bs1.pass == bs2.pass && bs1.transparent == bs2.transparent) {
                mask2[n] = null;
                return;
            }
            
            if (bs1.pass == 0 && !bs1.transparent) {
                mask2[n] = bs1;
                return;
            }
            
            if (bs2.pass == 0 && !bs2.transparent) {
                mask2[n] = bs2;
                return;
            }
            
            if (bs1.pass == 0) {
                mask2[n] = bs1;
                return;
            }
            if (bs2.pass == 0) {
                mask2[n] = bs2;
                return;
            }
            
            System.err.println("transition from non-air to non-air, non of both have pass == 0, UNDEFINED STATE!");
            System.err.println(this.strategy+" - "+Block.get(bs1.type)+"/"+Block.get(bs2.type)+"/"+bs1.transparent+"/"+bs2.transparent+"/"+bs1.pass+"/"+bs2.pass);
            
            return;
        }
        if (bs1 == null && bs2 == null) {
            System.err.println("BOTH FACES ARE OUTSIDE, UNDEFINED STATE!");
            return;
        }
        // this part only executes when one of both faces are outside the region
        if (bs1 == null) { // neighbour is not loaded
            if (bs2.pass>=0) {
//                bs2.extraFace=true;
                mask2[n] = bs2;
            }
            return;
        }
        if (bs2 == null) {
            if (bs1.pass>=0) {
//                bs1.extraFace=true;
                mask2[n] = bs1;
            }
            return;
        }
    }
    
    ChunkRenderCache cache;
    private int strategy;
    private int yPos;
    private int ySlice;
    final static boolean MEASURE = false;
    public static int avgUsage = -1;
    public void mesh(ChunkRenderCache ccache, int rY) {
        scratchpadidx = 0;
        this.nextBlockIDX = 0;
        this.ySlice = rY;
        this.yPos = rY<<RegionRenderer.SLICE_HEIGHT_BLOCK_BITS;
        this.cache = ccache;
        for (int i = 0; i < this.meshes.length; i++)
            this.meshes[i].clear();
        this.strategy = 0;
        if (MEASURE) TimingHelper2.startSec("mesh0");
        this.meshRound(ccache);
        this.strategy = 1;
        if (MEASURE) TimingHelper2.endStart("mesh1");
        this.meshRound(ccache);
        if (MEASURE) TimingHelper2.endSec();
        if (scratchpadidx > avgUsage) {
            System.out.println("new max = "+scratchpadidx+" ("+this.extraIdx+"/"+this.nextBlockIDX+")");
            for (int i = 0; i < WorldRenderer.NUM_PASSES; i++) {
                System.out.println("meshes["+i+"].size() = "+meshes[i].size());
            }
        }
        avgUsage = Math.max(scratchpadidx,avgUsage);
    }

    private void meshRound(ChunkRenderCache ccache) {
        extraIdx=0;
        dims[0] = RegionRenderer.REGION_SIZE_BLOCKS;
        dims[1] = RegionRenderer.SLICE_HEIGHT_BLOCKS;
        dims[2] = RegionRenderer.REGION_SIZE_BLOCKS;
        Arrays.fill(mask2, null);
        final int[] min = new int[] {-1, -1, -1};
        final int[] max = new int[] {0, 0, 0};
        int x[] = new int[] { 0, 0, 0 };
        int dir[] = new int[] { 0, 0, 0 };
        for (int axis = 0; axis < 3; ++axis) {
            int u = (axis + 1) % 3;
            int v = (axis + 2) % 3;
            x[0] = x[1] = x[2] = 0;
            dir[0] = dir[1] = dir[2] = 0;
            int masklen = dims[u] * dims[v];
            dir[axis] = 1;
            for (x[axis] = -1; x[axis] < dims[axis];) {
                scratchpadidx = 0;
                if (MEASURE) TimingHelper2.startSec("masq");
                int n = 0;
                for (x[v] = 0; x[v] < dims[v]; ++x[v]) {
                    for (x[u] = 0; x[u] < dims[u]; ++x[u]) {
                        bs1 = null;
                        bs2 = null;
                        if (x[axis] >= min[axis]) {
                            if (MEASURE) TimingHelper2.startSec("getBlockSurface");
                            bs1 = getBlockSurface(x[0], x[1], x[2], 0, axis);
                            if (MEASURE) TimingHelper2.endSec();
                        }
                        if (x[axis] < dims[axis]-max[axis]) {
                            if (MEASURE) TimingHelper2.startSec("getBlockSurface");
                            bs2 = getBlockSurface(x[0] + dir[0], x[1] + dir[1], x[2] + dir[2], 1, axis);
                            if (MEASURE) TimingHelper2.endSec();
                        }
                        setMask2(n, x, dir, axis);
                        n++;
                    }
                }
                if (MEASURE) TimingHelper2.endStart("compute");
                
                ++x[axis];
                n = 0;
                for (int j = 0; j < dims[v]; ++j) {
                    for (int i = 0; i < dims[u];) {
                        BlockSurface c = mask2[n];
                        if (c != null && c != air && c != hidden && (!c.extraFace) && (!c.renderTypeTransition)) {
                            // Compute width
                            int w = 1;
                            while (n + w < masklen && (mask2[n + w] != null && mask2[n + w].mergeWith(ccache, c)) && i + w < dims[u]) {
                                w++;
                            }
                            int h = computeHeight(i, j, n, w, v, u, c);
                            
                            boolean add = true;

                            int du[] = new int[] { 0, 0, 0 };
                            int dv[] = new int[] { 0, 0, 0 };
                            x[u] = i;
                            x[v] = j;
                            du[u] = w;
                            dv[v] = h;
                            if (add) {
                                if (!c.resolved)
                                    c.resolve(ccache);
                                c = c.copy();
                                BlockFace face = new BlockFace(c, new int[] { x[0], x[1], x[2] }, du, dv, u, v, w, h);
                                meshes[c.pass].add(face);
                            }

                            // Zero-out mask2
                            for (int l = 0; l < h; ++l)
                                for (int k = 0; k < w; ++k) {
                                    mask2[n + k + l * dims[u]] = null;
                                }
                            // Increment counters and continue
                            i += w;
                            n += w;
                        } else {
                            ++i;
                            ++n;
                        }
                    }
                }
                for (int j = 0; j < extraIdx; ++j) {
                    BlockSurface c = extraWaterFaces[j];
                    if (!c.resolved)
                        c.resolve(ccache);
                    int w = 1;
                    int h = 1;
                    int du[] = new int[] { 0, 0, 0 };
                    int dv[] = new int[] { 0, 0, 0 };
                    x[u] = c.z;
                    x[v] = c.x;
                    du[u] = w;
                    dv[v] = h;
                    c = c.copy();
                    BlockFace face = new BlockFace(c, new int[] { x[0], x[1], x[2] }, du, dv, u, v, w, h);
                    meshes[c.pass].add(face);
                }
                extraIdx = 0;
                if (MEASURE) TimingHelper2.endSec();
            }
        }
    
    }



    private BlockSurface getBlockSurface(int i, int j, int k, int l, int axis) {
        j += yPos;
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return air;
        }
        int chunkX = i>>Chunk.SIZE_BITS;
        int chunkZ = k>>Chunk.SIZE_BITS;
//                            System.out.println("block "+i+"/"+k+" is in chunk "+chunkX+"/"+chunkZ);
        boolean center = chunkX >= 0 && chunkX < ChunkRenderCache.WIDTH && chunkZ >= 0 && chunkZ < ChunkRenderCache.WIDTH;
        Chunk chunk = this.cache.get(chunkX, chunkZ);
        if (chunk == null) {
//          if (center) {
//          System.err.println("CHUNK IS NULL SHOULD NOT HAPPEN");
//      }
//      System.err.println("adj missing on "+regionX+"/"+regionZ);
            return null;
        }
        int type = chunk.getTypeId(i&0xF, j, k&0xF);
        boolean a = chunk.getWater(i&0xF, j, k&0xF)>0;
        if (a && (type != Block.ice.id)) {
            if (type > 0) {
                if (center && strategy == 0 && axis == 0 && l == 0) {
                    renderTypeBlocks[this.nextBlockIDX++] = 
                            (short) (j<<(REGION_SIZE_BLOCK_SIZE_BITS*2)|i<<(REGION_SIZE_BLOCK_SIZE_BITS)|k);
                }
            }
            type = Block.water.id;
//            if (chunk.getWater(i&0xF, j-2, k&0xF)==0) {
//                type = Block.ice.id;
//            }
        }
        if (type > 0) {
            Block block = Block.get(type);
            int pass = block.getRenderPass();
            int renderType = block.getRenderType();
            boolean renderTypeTransition = false;
            boolean flagged = false;
            if (renderType != 0) {
                if (center && strategy == 0 && axis == 0 && l == 0&&!flagged) {
                    renderTypeBlocks[this.nextBlockIDX++] = 
                            (short) (j<<(REGION_SIZE_BLOCK_SIZE_BITS*2)|i<<(REGION_SIZE_BLOCK_SIZE_BITS)|k);
                }
                if (!block.renderMeshedAndNormal()) {
                    if (block.isTransparent()) {
                        return air;
                    }
//                    if (strategy == 1)
//                        return hidden;
                    renderTypeTransition = true;
                }
            }
            if (strategy == 1 && block.getRenderShadow()<1) {
                return air;
            }
            BlockSurface surface = next();
            surface.type = type;
            surface.transparent = block.isTransparent();
            surface.x = i;
            surface.y = j;
            surface.z = k;
            surface.extraFace = !center;
            surface.face = l;
            surface.axis = axis;
            surface.pass = pass;
            surface.renderTypeTransition = renderTypeTransition;
            if (strategy == 1) {
                if (!surface.transparent && !renderTypeTransition) {
                    surface.type = 1;
                }
                surface.pass = 2;
                surface.transparent = false;
            } else {
                surface.calcLight = true;
            }
            return surface;
        }
        return air;
    }


    private BlockSurface next() {
        BlockSurface surface = scratchPad[scratchpadidx++];
        surface.reset();
        return surface;
    }



    public List<BlockFace> getMeshes(int pass) {
        return this.meshes[pass];
    }



    /**
     * @return
     */
    public int getRenderType1Blocks() {
        return this.nextBlockIDX;
    }



    /**
     * @param j
     * @return 
     */
    public short getBlockPos(int j) {
        return renderTypeBlocks[j];
    }
}
