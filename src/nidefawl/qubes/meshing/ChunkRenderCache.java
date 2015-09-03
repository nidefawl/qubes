package nidefawl.qubes.meshing;

import java.util.Arrays;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.Region;
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
    public final static int WIDTH = Region.REGION_SIZE;
    public final static int WIDTH_EXTRA = WIDTH+2;
    public final static int WIDTH_BLOCKS = WIDTH_EXTRA*Chunk.SIZE;
    final public Chunk[] regions = new Chunk[WIDTH_EXTRA*WIDTH_EXTRA]; //TODO: are corners required?

    public void set(int x, int z, Chunk region) {
        this.regions[(x+1)*WIDTH_EXTRA+(z+1)] = region;
    }
    
    public Chunk get(int x, int z) {
        return this.regions[(x+1)*WIDTH_EXTRA+(z+1)];
    }

    public void flush() {
        Arrays.fill(this.regions, null);
    }

    /** call with relative coords */
    public int getTypeId(int i, int j, int k) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0;
        }
        int chunkX = 0;
        int chunkZ = 0;
        if (i < 0) {
            i += WIDTH_BLOCKS;
            chunkX--;
        } else if (i >= WIDTH_BLOCKS) {
            i -= WIDTH_BLOCKS;
            chunkX++;
        }
        if (k < 0) {
            k += WIDTH_BLOCKS;
            chunkZ--;
        } else if (k >= WIDTH_BLOCKS) {
            k -= WIDTH_BLOCKS;
            chunkZ++;
        }
        Chunk region = get(chunkX, chunkZ);
        return region != null ? region.getTypeId(i&0xF, j, k&0xF) : 0;
    }

    public boolean cache(WorldClient world, MeshedRegion mr, int renderChunkX, int renderChunkZ) {
        int basechunkX = mr.rX<<Region.REGION_SIZE_BITS;
        int basechunkZ = mr.rZ<<Region.REGION_SIZE_BITS;
        int offsetX = mr.rX-renderChunkX;
        int offsetZ = mr.rZ-renderChunkZ;
        boolean minXReq = offsetX > 0;
        boolean maxXReq = offsetX < RegionRenderer.RENDER_DISTANCE-1;
        boolean minZReq = offsetZ > 0;
        boolean maxZReq = offsetZ < RegionRenderer.RENDER_DISTANCE-1;
        ChunkManager mgr = world.getChunkManager();
        for (int x = -1; x < WIDTH+2; x++) {
            for (int z = -1; z < WIDTH+2; z++) {
                Chunk c = mgr.get(basechunkX+x, basechunkZ+z);
                if (c == null) {
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
