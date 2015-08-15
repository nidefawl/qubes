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
    private int[]           dims;
    private int[]           mask;
    boolean                 hasSecondPass;
    boolean                 translucentOnly  = false;
    ArrayList<Mesh> meshes = new ArrayList<Mesh>();


    public static int mod(int x, int m) {
        int result = x % m;
        return result < 0 ? result + m : result;
    }

    

    private boolean isTranslucent(int a) {
        if (a > 0 && !Block.blocksLight(a)) {
            return true;
        }
        return false;
    }
    
    private void setMask(int n, int a, int b, int b1, int b2) {
        if (isTranslucent(a)!=translucentOnly) {
            hasSecondPass = true;
            a = 0;
        }
        if (isTranslucent(b)!=translucentOnly) {
            hasSecondPass = true;
            b = 0;
        }
        if ((a != 0) == (b != 0) && (b1 == b2)) {
            mask[n] = 0;
        } else if (a != 0) {
            mask[n] = a | b1<<12;
        } else if (b != 0) {
            mask[n] = -(b | b2<<12);
        }
    }

    public ArrayList<Mesh> mesh(World world, Region region, int pass) {
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
                mask = new int[Math.max(dims[1], dims[2]) * Math.max(dims[0], dims[1])];
            }
            dims[1] = world.worldHeight;
            dims[1] = region.getHighestBlock() + 1;
            Arrays.fill(mask, 0);
            int nrm2[] = new int[] { 0, 0, 0 };
            int du[] = new int[] { 0, 0, 0 };
            int dv[] = new int[] { 0, 0, 0 };
            int x[] = new int[] { 0, 0, 0 };
            int q[] = new int[] { 0, 0, 0 };
            for (int d = 0; d < 3; ++d) {
                int i, j, k, l, w, h;
                int u = mod(d + 1, 3);
                int v = mod(d + 2, 3);
                x[0] = x[1] = x[2] = 0;
                q[0] = q[1] = q[2] = 0;
                int masklen = dims[u] * dims[v];
                q[d] = 1;
                for (x[d] = -1; x[d] < dims[d];) {
                    // Compute mask
                    int n = 0;
                    for (x[v] = 0; x[v] < dims[v]; ++x[v]) {
                        for (x[u] = 0; x[u] < dims[u]; ++x[u]) {
                            int a = (x[d] >= 0  ? region.getTypeId(x[0], x[1], x[2]) : 0);
                            int b = (x[d] < dims[d] - 1 ? region.getTypeId(x[0] + q[0], x[1] + q[1], x[2] + q[2]) : 0);
                            int b1 = (x[d] >= 0  ? region.getBiome(x[0], x[1], x[2]) : 0);
                            int b2 = (x[d] < dims[d] - 1 ? region.getBiome(x[0] + q[0], x[1] + q[1], x[2] + q[2]) : 0);
                            setMask(n, a, b, b1, b2);
                            n++;
                        }
                    }
                    // Increment x[d]
                    ++x[d];
                    // Generate mesh for mask using lexicographic ordering
                    n = 0;
                    for (j = 0; j < dims[v]; ++j) {
                        for (i = 0; i < dims[u];) {
                            int c = mask[n];
                            if (mask[n] != 0) {
                                // Compute width
                                w = 1;
                                while (n + w < masklen && c == mask[n + w] && i + w < dims[u]) {
                                    w++;
                                }
                                // Compute height (this is slightly awkward
                                boolean done = false;
                                for (h = 1; !done && j + h < dims[v]; ++h) {
                                    for (k = 0; !done && k < w; ++k) {
                                        if (c != mask[n + k + h * dims[u]]) {
                                            done = true;
                                            break;
                                        }
                                    }
                                    if (done) {
                                        break;
                                    }
                                }
                                // Add quad
                                x[u] = i;
                                x[v] = j;
                                du[0] = du[1] = du[2] = 0;
                                dv[0] = dv[1] = dv[2] = 0;
                                nrm2[0] = nrm2[1] = nrm2[2] = 0;
                                if (c > 0) {
                                    dv[v] = h;
                                    du[u] = w;
                                } else {
                                    c = -c;
                                    du[v] = h;
                                    dv[u] = w;
                                }


                                int n1 = du[1] * dv[2] - dv[1] * du[2];
                                int n2 = du[2] * dv[0] - dv[2] * du[0];
                                int n3 = du[0] * dv[1] - dv[0] * du[1];
                                boolean add = true;
                                if (isTranslucent(c&0xFFF) && !(n2 > 0)) {
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
                                    
                                    Mesh face = new Mesh(c,
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
                                // Zero-out mask
                                for (l = 0; l < h; ++l)
                                    for (k = 0; k < w; ++k) {
                                        mask[n + k + l * dims[u]] = 0;
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

}
