package nidefawl.qubes.render;


import static nidefawl.qubes.chunk.Region.*;

import java.util.*;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Mesh;
import nidefawl.qubes.world.World;

public class Mesher {
    static class AO {
        int a0, a1, a2, a3;
    }

    private final int[]    dims;
    private BlockSurface[] mask2;
    final static BlockSurfaceAir air = new BlockSurfaceAir();
    List[] meshes = new List[WorldRenderer.NUM_PASSES];
    public Mesher() {
        for (int i = 0; i < WorldRenderer.NUM_PASSES; i++)
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
        // this part only executes when one of both faces is outside the region
        if (bs1 == null) {
            if (bs2.pass == 0) {
                mask2[n] = bs2;
            } else {
                mask2[n] = null;
            }
//            mask2[n] = null;
            return;
        }
        if (bs2 == null) {
            if (bs1.pass == 0) {
                mask2[n] = bs1;
            } else {
                mask2[n] = null;
            }
//            mask2[n] = null;
            return;
        }
//        if (bs1 != null && bs2 != null && bs1.pass != bs2.pass) {
//            if (bs1 != air && bs1.pass == 0) {
//                mask2[n] = bs1;
//                return;
//            }
//            if (bs2 != air && bs2.pass == 0) {
//                mask2[n] = bs2;
//                return;
//            }
//            mask2[n] = null;
//            return;
//        }
//        int typeA = bs1 == null ? -1 : bs1.pass+1;
//        int typeB = bs2 == null ? -1 : bs2.pass+1;
//        if (typeA == typeB) {
//            mask2[n] = null;
//        } else if (bs1 != null) {
//            mask2[n] = bs1;
//        } else if (bs2 != null) {
//            mask2[n] = bs2;
//        }
    }
    
    Region region;
    public void mesh(World world, Region region) {
        this.region = region;
        for (int i = 0; i < WorldRenderer.NUM_PASSES; i++)
            this.meshes[i].clear();
        if (!region.isEmpty()) {
            dims[0] = 16*REGION_SIZE;
            dims[1] = region.getHighestBlock() + 1;
            dims[2] = 16*REGION_SIZE;
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
                            if (c != null && c != air) {
                                // Compute width
                                int w = 1;
                                while (n + w < masklen && (mask2[n + w] != null && mask2[n + w].mergeWith(c)) && i + w < dims[u]) {
                                    w++;
                                }
                                int h = computeHeight(i, j, n, w, v, u, c);
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
//                                    add = false;
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
        if (i < 0 || i >= dims[0]) {
//            System.err.println("We need the neighbour region!");
            return air;
        }
        if (k < 0 || k >= dims[2]) {
//            System.err.println("We need the neighbour region!");
            return air;
        }
        int type = region.getTypeId(i, j, k);
        if (type > 0) {
            Block block = Block.block[type];
            BlockSurface surface = new BlockSurface();
            surface.type = type;
            surface.transparent = block.isTransparent();
            surface.pass = block.getRenderPass();
            surface.x = i;
            surface.y = j;
            surface.z = k;
            surface.face = l;
            surface.axis = axis;
            return surface;
        }
        return air;
    }


    public List<Mesh> getMeshes(int i) {
        return this.meshes[i];
    }
}
