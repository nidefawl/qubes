package nidefawl.qubes.render;


import static nidefawl.qubes.chunk.Region.*;

import static nidefawl.qubes.render.MeshedRegion.*;

import java.util.*;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionCache;
import nidefawl.qubes.vec.Mesh;
import nidefawl.qubes.world.World;

public class Mesher {

    private final int[]    dims;
    private BlockSurface[] mask2;
    final static BlockSurfaceAir air = new BlockSurfaceAir();
    @SuppressWarnings("rawtypes")
    List[] meshes = new List[WorldRenderer.NUM_PASSES];
    public Mesher() {
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
                if (bs == null || !c.mergeWith(bs)) {
                    return h;
                }
            }
            h++;
        }
        return h;
    }

    BlockSurface bs1 = null;
    BlockSurface bs2 = null;
    private void setMask2(int n) {
        // first handle everything inside of region
        if (bs1 != null && bs2 != null) {
            if (bs1 == air && bs2 == air) {
                mask2[n] = null;
                return;
            }
            if (bs1 != air && bs2 == air) {
                mask2[n] = bs1;
                return;
            }
            if (bs1 == air && bs2 != air) {
                mask2[n] = bs2;
                return;
            }
            if (bs1.pass == bs2.pass) {
                mask2[n] = null;
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
            return;
        }
        if (bs1 == null && bs2 == null) {
            System.err.println("BOTH FACES ARE OUTSIDE, UNDEFINED STATE!");
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
    
    RegionCache cache;
    private int strategy;
    public void mesh(World world, RegionCache cache) {
        this.cache = cache;
        for (int i = 0; i < this.meshes.length; i++)
            this.meshes[i].clear();
        this.strategy = 0;
        this.meshRound(world, cache);
        this.strategy = 1;
        this.meshRound(world, cache);
    }
    public void meshRound(World world, RegionCache cache) {
        Region region = cache.get(0, 0);
        if (!region.isEmpty()) {
            dims[0] = Chunk.SIZE*REGION_SIZE;
            dims[1] = region.getHighestBlock() + 1; // always correct (with neighbours)?
            dims[2] = Chunk.SIZE*REGION_SIZE;
            Arrays.fill(mask2, null);
            
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
                    
                    int n = 0;
                    for (x[v] = 0; x[v] < dims[v]; ++x[v]) {
                        for (x[u] = 0; x[u] < dims[u]; ++x[u]) {
                            bs1 = null;
                            bs2 = null;
                            if (x[axis] >= -1) {
                                bs1 = getBlockSurface(x[0], x[1], x[2], 0, axis);
                            }
                            if (x[axis] < dims[axis]) {
                                bs2 = getBlockSurface(x[0] + dir[0], x[1] + dir[1], x[2] + dir[2], 1, axis);
                            }
                            setMask2(n);
                            n++;
                        }
                    }
                    
                    ++x[axis];
                    n = 0;
                    for (int j = 0; j < dims[v]; ++j) {
                        for (int i = 0; i < dims[u];) {
                            BlockSurface c = mask2[n];
                            if (c != null && c != air && (!c.extraFace)) {
                                // Compute width
                                int w = 1;
                                while (n + w < masklen && (mask2[n + w] != null && mask2[n + w].mergeWith(c)) && i + w < dims[u]) {
                                    w++;
                                }
                                int h = computeHeight(i, j, n, w, v, u, c);
                                // Compute height (this is slightly awkward
                                boolean add = true;

                                int du[] = new int[] { 0, 0, 0 };
                                int dv[] = new int[] { 0, 0, 0 };
                                x[u] = i;
                                x[v] = j;
                                du[u] = w;
                                dv[v] = h;
                                if (add) {
                                    
                                    Mesh face = new Mesh(c, new int[] { x[0], x[1], x[2] }, du, dv, u, v, w, h);
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
                }
            }
        }
    }



    private BlockSurface getBlockSurface(int i, int j, int k, int l, int axis) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return air;
        }
        int regionX = 0;
        int regionZ = 0;
        if (i < 0) {
            i += Chunk.SIZE*REGION_SIZE;
            regionX--;
        } else if (i >= dims[0]) {
            i -= Chunk.SIZE*REGION_SIZE;
            regionX++;
        }
        if (k < 0) {
            k += Chunk.SIZE*REGION_SIZE;
            regionZ--;
        } else if (k >= dims[2]) {
            k -= Chunk.SIZE*REGION_SIZE;
            regionZ++;
        }
        Region region = this.cache.get(regionX, regionZ);
        if (region == null) {
            if (regionX==0&&regionZ==0) {
                System.err.println("REGION IS NULL ON CENTER; SHOULD NOT HAPPEN");
            }
//            System.err.println("adj missing on "+regionX+"/"+regionZ);
            return null;
        }
        int type = region.getTypeId(i, j, k);
        if (type > 0) {
            Block block = Block.block[type];
            int pass = block.getRenderPass();
            if (strategy == 1 && pass > 0) {
                return air;
            }
            BlockSurface surface = new BlockSurface();
            surface.type = type;
            surface.transparent = block.isTransparent();
            surface.x = i;
            surface.extraFace = regionX != 0 || regionZ != 0;
            surface.y = j;
            surface.z = k;
            surface.face = l;
            surface.axis = axis;
            surface.pass = pass;
            if (strategy == 1) {
                surface.type = 1;
                surface.pass = 2;
            } else {
                surface.calcAO(this.cache);
            }
            surface.x = i+region.rX*Region.REGION_SIZE_BLOCKS;
            surface.y = j;
            surface.z = k+region.rZ*Region.REGION_SIZE_BLOCKS;
            return surface;
        }
//        if (regionX!=0&&regionZ!=0) {
//            if (!region.isChunkLoaded((i>>4), (k>>4))) {
//                System.err.println("CHUNK NOT LOADED ON NEIGHBOUR");
//            }
//        }
        return air;
    }


    public List<Mesh> getMeshes(int pass) {
        return this.meshes[pass];
    }
}
