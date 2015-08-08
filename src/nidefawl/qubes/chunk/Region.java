package nidefawl.qubes.chunk;

import static org.lwjgl.opengl.GL11.*;

import java.util.Arrays;
import java.util.LinkedList;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.DisplayList;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.RegionRenderer;
import nidefawl.qubes.vec.Mesh;
import nidefawl.qubes.world.World;

import org.lwjgl.opengl.GL11;


public class Region {
    public static final int REGION_SIZE_BITS = 2;
    public static final int REGION_SIZE      = 1 << REGION_SIZE_BITS;
    public static final int REGION_SIZE_MASK = REGION_SIZE - 1;
    
    final int               rX, rZ;
    final Chunk[][]         chunks           = new Chunk[REGION_SIZE][REGION_SIZE];
    private boolean         isRendered;
    private DisplayList         displayList;
    private int[]           facesRendered    = new int[RegionRenderer.NUM_PASSES];
    private boolean[]       hasPass          = new boolean[RegionRenderer.NUM_PASSES];
    private Mesh[][]        meshes;
    boolean                 translucentOnly  = false;
    private int[]           color            = new int[256];
    int                     lastcolor        = 0;
    public long             createTime       = System.currentTimeMillis();
    public int              index;

    private int[]           dims;
    private int[]           mask;
    boolean hasSecondPass;
    public Region(int regionX, int regionZ) {
        this.rX = regionX;
        this.rZ = regionZ;
        this.isRendered = false;
        this.displayList = null;
        meshes = new Mesh[2][];
        color[0] = 0;
        color[1] = 0xABABAB;
        color[2] = 0x187314;
        color[3] = 0x734E14;
        color[4] = 0x5C5C5C;
        color[8] = 0x2B41CF;
        color[9] = 0x2B41CF;
        color[10] = 0xE35930;
        color[11] = 0xE35930;
        color[12] = 0xEDE139;
        color[13] = 0xC9C9C9;
        color[13] = 0xC9C9C9;
        color[166] = 0x656f73;
        color[167] = 0x4a7a5b;
        color[169] = 0xABABAB;
    }

    public boolean isRendered() {
        return isRendered;
    }

    public void setRendered(boolean isRendered) {
        this.isRendered = isRendered;
    }
    private boolean isEmpty = false;

    boolean isEmpty() {
        return isEmpty;
    }
    public void generate(World world) {
        this.isEmpty = true;
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                chunks[x][z] = world.generateChunk(rX << REGION_SIZE_BITS | x, rZ << REGION_SIZE_BITS | z);
                chunks[x][z].checkIsEmtpy();
                if (!chunks[x][z].isEmpty()) {
                    this.isEmpty = false;
                }
            }
        }
    }

    public static int mod(int x, int m) {
        int result = x % m;
        return result < 0 ? result + m : result;
    }

    public void doMeshing(World world) {
        hasSecondPass = false;
        for (int pass = 0; pass < 2; pass++) {
            LinkedList<Mesh> meshes = new LinkedList<Mesh>();
            if (!isEmpty() && (pass == 0 || hasSecondPass)) {
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
                dims[1] = getHighestBlock() + 1;
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
                                int a = (x[d] >= 0  ? this.getTypeId(x[0], x[1], x[2]) : 0);
                                int b = (x[d] < dims[d] - 1 ? this.getTypeId(x[0] + q[0], x[1] + q[1], x[2] + q[2]) : 0);
                                int b1 = (x[d] >= 0  ? this.getBiome(x[0], x[1], x[2]) : 0);
                                int b2 = (x[d] < dims[d] - 1 ? this.getBiome(x[0] + q[0], x[1] + q[1], x[2] + q[2]) : 0);
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
    
                                        Mesh face = new Mesh(c,
                                                new int[] {x[0],                 x[1],                 x[2]                   },
                                                new int[] {x[0] + du[0],         x[1] + du[1],         x[2] + du[2]           },
                                                new int[] {x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2] + du[2] + dv[2]   },
                                                new int[] {x[0]         + dv[0], x[1]         + dv[1], x[2]         + dv[2]   },
                                                new byte[] {(byte) (n1 > 0 ? 1 : n1 < 0 ? -1 : 0), (byte) (n2 > 0 ? 1 : n2 < 0 ? -1 : 0), (byte) (n3 > 0 ? 1 : n3 < 0 ? -1 : 0)});
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
            this.meshes[pass] = meshes.toArray(new Mesh[meshes.size()]);
        }
//        flushBlockData();
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

    private int getHighestBlock() {
        int topBlock = 0;
        for (int i = 0; i < REGION_SIZE; i++) {
            for (int k = 0; k < REGION_SIZE; k++) {
                int y = chunks[i][k].getTopBlock();
                if (y > topBlock) {
                    topBlock = y;
                }
            }
        }
        return topBlock;
    }

    private final int getTypeId(int i, int j, int k) {
        int id = chunks[i >> 4][k >> 4].getTypeId(i & 0xF, j, k & 0xF);
//        if (isTranslucent(id))
//            return 0;
        return id;
    }

    private final int getBiome(int i, int j, int k) {
        Chunk c = chunks[i >> 4][k >> 4];
        
        return c.getBiome(i & 0xF, j, k & 0xF);
    }

    public void renderRegion(RegionRenderer renderer, float fTime, int pass) {
        if (displayList != null && displayList.list > 0)
            glCallList(displayList.list + pass);
    }

    public void flushBlockData() {
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                chunks[x][z].blocks = null;
            }
        }
    }

    public boolean hasBlockData() {
        return chunks[0][0].blocks != null;
    }

    public int getFacesRendered(int i) {
        return facesRendered[i];
    }

    public void makeRegion(RegionRenderer renderer) {
        isRendered = true;
        if (displayList == null)
            displayList = Engine.nextFreeDisplayList();
        this.lastcolor = 0;
        GL11.glColor3f(0F, 0F, 0F);
        World w = renderer.getWorld();
        int top = 110;//w.worldHeightMinusOne;
        int bottom = 10;//w.worldHeightMinusOne;
        for (int pass = 0; pass < RegionRenderer.NUM_PASSES; pass++) {
            facesRendered[pass] = 0;
            glNewList(displayList.list+pass, GL11.GL_COMPILE);
            Mesh[] meshesPass = this.meshes[pass];
            this.hasPass[pass] = meshesPass.length > 0;  
            if (this.hasPass[pass]) {
                glBegin(GL_QUADS);
                for (int a = 0; a < meshesPass.length; a++) {
                    Mesh mesh = meshesPass[a];
                    drawMesh(mesh);
                }
                glEnd();
            }
            glEndList();
            facesRendered[pass] += meshesPass.length;
        }
        this.meshes = null;
    }

    private void drawMesh(Mesh mesh) {
        if (mesh.type != lastcolor) {
            lastcolor = mesh.type;
            int block = mesh.type&0xFF;
            int biome = (mesh.type>>12)&0xFF;
//              System.out.println(block+"/"+biome);
          float m = 1F;
          float alpha = 1F;
          if (Block.water.id == block) {
              alpha = 0.8F;
          }
            int c = color[block];

            if (block == Block.grass.id) {
//                c = BiomeGenBase.byId[biome].getBiomeGrassColor(ColorizerGrass.grass);
                m = 0.3F;
            }
//            int biomeC = BiomeGenBase.byId[biome].color;

//            if (block == Block.GRASS.id) {
//                c = BiomeGenBase.byId[biome].getBiomeGrassColor(ColorizerGrass.grass, null, 0,0,0);
//                m = 0.3F;
//            }
//            int biomeC = BiomeGenBase.byId[biome].field_150609_ah;
//            float cr = ((biomeC>>16)&0xFF)/255.0F;
//            float cg = ((biomeC>>8)&0xFF)/255.0F;
//            float cb = ((biomeC>>0)&0xFF)/255.0F;
//            float br = 0.2126F*cr+0.7152F*cg+0.0722F*cb;
//            m = (m+br) / 2.0F;
            if (block == 1) {
                m = 0.8F;
            }
//                if (biome == BiomeGenBase.JUNGLE_HILLS.biomeID) {
//                    float cr = biome.
//                    m = 0.2F;
//                }
            if (c == 0) c = 0x444444;
            float b = (c&0xFF)/255F;
            c >>= 8;
            float g = (c&0xFF)/255F;
            c >>= 8;
            float r = (c&0xFF)/255F;
            GL11.glColor4f(r*m, g*m, b*m, alpha);
        }
//            if (mesh.v0[1] >= top||mesh.v1[1] >= top||mesh.v2[1] >= top||mesh.v3[1] >= top||
//                    mesh.v0[1] <= bottom||mesh.v1[1] <= bottom||mesh.v2[1] <= bottom||mesh.v3[1] <= bottom) {
//                GL11.glColor3f(0.9F, 0.4F, 0.4F);
//                lastcolor = -1;
//            }
        glNormal3f(mesh.normal[0], mesh.normal[1], mesh.normal[2]);
        glVertex3i(mesh.v0[0], mesh.v0[1], mesh.v0[2]);
        glVertex3i(mesh.v1[0], mesh.v1[1], mesh.v1[2]);
        glVertex3i(mesh.v2[0], mesh.v2[1], mesh.v2[2]);
        glVertex3i(mesh.v3[0], mesh.v3[1], mesh.v3[2]);
    }

    public void release() {
        if (displayList != null) {
            Engine.release(displayList);
            displayList = null;
        }
        this.meshes = null;
        this.isRendered = false;
    }

    public boolean hasPass(int i) {
        return this.hasPass[i];
    }
}
