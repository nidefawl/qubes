package nidefawl.qubes.meshing;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;

import java.util.Arrays;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

/**
 * Cache neighbour chunks for rendering
 * @author Michael
 *
 */
public class ChunkRenderCache {
    public final static int WIDTH = RegionRenderer.REGION_SIZE;
    public final static int WIDTH_EXTRA = WIDTH+2;
    public final static int WIDTH_BLOCKS = WIDTH_EXTRA*Chunk.SIZE;
    final public Chunk[] chunks = new Chunk[WIDTH_EXTRA*WIDTH_EXTRA]; //TODO: are corners required?

    public void set(int x, int z, Chunk region) {
        this.chunks[(x+1)*WIDTH_EXTRA+(z+1)] = region;
    }
    
    public Chunk get(int x, int z) {
        return this.chunks[(x+1)*WIDTH_EXTRA+(z+1)];
    }

    public void flush() {
        Arrays.fill(this.chunks, null);
    }
    /** call with relative coords */
    public int getTypeId(int i, int j, int k) {

        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0;
        }
        Chunk region = get(i>>Chunk.SIZE_BITS, k>>Chunk.SIZE_BITS);
        return region != null ? region.getTypeId(i&0xF, j, k&0xF) : 0;
    }

    public int getLight(int i, int j, int k) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0xF0;
        }
        Chunk region = get(i>>Chunk.SIZE_BITS, k>>Chunk.SIZE_BITS);
        return region != null ? region.getLight(i&0xF, j, k&0xF) : 0;
    }

    public boolean cache(WorldClient world, MeshedRegion mr, int renderChunkX, int renderChunkZ) {
        int basechunkX = mr.rX<<RegionRenderer.REGION_SIZE_BITS;
        int basechunkZ = mr.rZ<<RegionRenderer.REGION_SIZE_BITS;
        int offsetX = mr.rX-renderChunkX;
        int offsetZ = mr.rZ-renderChunkZ;
        
        boolean minXReq = offsetX > 0;
        boolean maxXReq = offsetX < RegionRenderer.RENDER_DISTANCE-1;
        boolean minZReq = offsetZ > 0;
        boolean maxZReq = offsetZ < RegionRenderer.RENDER_DISTANCE-1;
        ChunkManager mgr = world.getChunkManager();
        for (int x = -1; x < WIDTH+1; x++) {
            for (int z = -1; z < WIDTH+1; z++) {
                Chunk c = mgr.get(basechunkX+x, basechunkZ+z);
                if (c == null) {
//                    boolean dbg = mr.rX==-7&&mr.rZ==-1;
//                  System.err.println("render");
//                    if (dbg) 
//                        System.out.println("c == null @"+(basechunkX+x)+", "+(basechunkZ+z));
                    if (x >= 0 && x < WIDTH && z >= 0 && z < WIDTH) {
                        return false;
                    }
                    if (x < 0 && minXReq)
                        return false;
                    if (z < 0 && minZReq)
                        return false;
                    if (x >= WIDTH && maxXReq)
                        return false;
                    if (z >= WIDTH && maxZReq)
                        return false;
                } else {
                }
                set(x, z, c);
            }
        }
        return true;
    }

    public Chunk getWest() {
        return get(-1, 0);
    }
    public Chunk getEast() {
        return get(WIDTH, 0);
    }
    public Chunk getNorth() {
        return get(0, -1);
    }
    public Chunk getSouth() {
        return get(0, WIDTH);
    }
    
}
