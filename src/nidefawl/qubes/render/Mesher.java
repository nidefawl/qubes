package nidefawl.qubes.render;


import static nidefawl.qubes.chunk.Region.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Mesh;
import nidefawl.qubes.world.World;

public class Mesher {
    private static final int SOUTH      = 0;
    private static final int NORTH      = 1;
    private static final int EAST       = 2;
    private static final int WEST       = 3;
    private static final int TOP        = 4;
    private static final int BOTTOM     = 5;
    private static final int CHUNK_WIDTH     = 16<<Region.REGION_SIZE_BITS;    
    static class AO {
        int a0, a1, a2, a3;
    }
    private int[]           dims;
    private int[]           mask;
    private BlockSurface[]           mask2;
    boolean                 hasSecondPass;
    boolean                 translucentOnly  = false;
    ArrayList<Mesh> meshes = new ArrayList<Mesh>();


    private boolean isTranslucent(int a) {
        if (a > 0 && !Block.blocksLight(a)) {
            return true;
        }
        return false;
    }


    private int computeHeight2(int i, int j, int n, int w, int v, int u, BlockSurface c) {
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


    private int computeHeight(int i, int j, int n, int w, int v, int u, int c) {
        int h = 1;
        while (j + h < dims[v]) {
            for (int k = 0; k < w; ++k) {
                if (c != mask[n + k + h * dims[u]]) {
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
        if (bs1 != null && bs1.transparent!=translucentOnly) {
            hasSecondPass = true;
            bs1 = null;
        }
        if (bs2 != null && bs2.transparent!=translucentOnly) {
            hasSecondPass = true;
            bs2 = null;
        }
        if ((bs1 != null) == (bs2 != null)) {
            mask2[n] = null;
        } else if (bs1 != null) {
            mask2[n] = bs1;
        } else if (bs2 != null) {
            mask2[n] = bs2;
        }
    }
    /**
     * 
     */
    Region region;
    public ArrayList<Mesh> mesh2(World world, Region region, int pass) {
        this.region = region;
        if (pass == 0) {
            hasSecondPass = false;
        }
        meshes.clear();
        if (!region.isEmpty() && (pass == 0 || hasSecondPass)) {
            translucentOnly = pass == 1;
            // block index of first chunk in this region of REGION_SIZE * REGION_SIZE chunks
    //        int xOff = rX << (REGION_SIZE_BITS + 4);
    //        int zOff = rZ << (REGION_SIZE_BITS + 4);

            // Sweep over 3-axes
            if (dims == null) {
                dims = new int[] { 16 * REGION_SIZE, world.worldHeight, 16 * REGION_SIZE };
                mask2 = new BlockSurface[Math.max(dims[1], dims[2]) * Math.max(dims[0], dims[1])];
            }
            dims[1] = world.worldHeight;
            dims[1] = region.getHighestBlock() + 1;
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
                            if (x[axis] >= 0) {
                                bs1 = getBlockSurface(x[0], x[1], x[2], 0, axis);
                            }
                            if (x[axis] < dims[axis] - 1) {
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
                            if (c != null) {
                                // Compute width
                                int w = 1;
                                while (n + w < masklen && (mask2[n + w] != null && mask2[n + w].mergeWith(c)) && i + w < dims[u]) {
                                    w++;
                                }
                                int h = computeHeight2(i, j, n, w, v, u, c);
                                // Compute height (this is slightly awkward
                                boolean done = false;
                                boolean add = true;

                                int du[] = new int[] { 0, 0, 0 };
                                int dv[] = new int[] { 0, 0, 0 };
                                // Add quad
                                if (c.face == 1) {
                                    x[u] = i+w;
                                    x[v] = j;
                                    du[u] = -w;
                                    dv[v] = h;
                                } else {
                                    x[u] = i;
                                    x[v] = j;
                                    du[u] = w;
                                    dv[v] = h;
                                }

                                int n1 = du[1] * dv[2] - dv[1] * du[2];
                                int n2 = du[2] * dv[0] - dv[2] * du[0];
                                int n3 = du[0] * dv[1] - dv[0] * du[1];
                                if (c.transparent && !(n2 > 0)) {
                                    add = false;
//                                    continue;
                                }
                                /*
                                Mesh face = new Mesh(c,
                                        new int[] {xOff + x[0],                 x[1],                 zOff + x[2]                   },
                                        new int[] {xOff + x[0] + du[0],         x[1] + du[1],         zOff + x[2] + du[2]           },
                                        new int[] {xOff + x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], zOff + x[2] + du[2] + dv[2]   },
                                        new int[] {xOff + x[0]         + dv[0], x[1]         + dv[1], zOff + x[2]         + dv[2]   },
                                        new byte[] {(byte) (n1 > 0 ? 1 : n1 < 0 ? -1 : 0), (byte) (n2 > 0 ? 1 : n2 < 0 ? -1 : 0), (byte) (n3 > 0 ? 1 : n3 < 0 ? -1 : 0)});
                                */
                                if (add) {
                                    int faceDir = Dir.DIR_POS_X;
                                    if (n1 < 0) faceDir = Dir.DIR_NEG_X;
                                    if (n2 > 0) faceDir = Dir.DIR_POS_Y;
                                    if (n2 < 0) faceDir = Dir.DIR_NEG_Y;
                                    if (n3 > 0) faceDir = Dir.DIR_POS_Z;
                                    if (n3 < 0) faceDir = Dir.DIR_NEG_Z;
                                    
                                    Mesh face = new Mesh(c.type,
                                            new int[] {x[0],                 x[1],                 x[2]                   },
                                            new int[] {x[0] + du[0],         x[1] + du[1],         x[2] + du[2]           },
                                            new int[] {x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2] + du[2] + dv[2]   },
                                            new int[] {x[0]         + dv[0], x[1]         + dv[1], x[2]         + dv[2]   },
                                            new int[] {du[0], du[1], du[2]   },
                                            new int[] {dv[0], dv[1], dv[2]   },
                                            new byte[] {(byte) (n1 > 0 ? 1 : n1 < 0 ? -1 : 0), (byte) (n2 > 0 ? 1 : n2 < 0 ? -1 : 0), (byte) (n3 > 0 ? 1 : n3 < 0 ? -1 : 0)},
                                            faceDir);
                                    meshes.add(face);
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
        return meshes;
    }



    private BlockSurface getBlockSurface(int i, int j, int k, int l, int axis) {
        int type = region.getTypeId(i, j, k);
        if (type > 0) {

            BlockSurface surface = new BlockSurface();
            surface.type = type;
            surface.transparent = isTranslucent(type);
            surface.x = i;
            surface.y = j;
            surface.z = k;
            surface.face = l;
            surface.axis = axis;
            return surface;
        }
        return null;
    }
}
