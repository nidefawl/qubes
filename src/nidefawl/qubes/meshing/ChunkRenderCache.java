package nidefawl.qubes.meshing;

import java.util.Arrays;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

/**
 * Cache neighbour chunks for rendering
 * @author Michael
 *
 */
public class ChunkRenderCache implements IBlockWorld {
    public final static int WIDTH = RegionRenderer.REGION_SIZE;
    public final static int WIDTH_EXTRA = WIDTH+2;
    public final static int WIDTH_BLOCKS = WIDTH_EXTRA*Chunk.SIZE;
    final public Chunk[] chunks = new Chunk[WIDTH_EXTRA*WIDTH_EXTRA]; //TODO: are corners required?
    private IBlockWorld world;
    private int baseX;
    private int baseZ;

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
    public int getData(int i, int j, int k) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0;
        }
        Chunk region = get(i>>Chunk.SIZE_BITS, k>>Chunk.SIZE_BITS);
        return region != null ? region.getData(i&0xF, j, k&0xF) : 0;
    }

    @Override
    public BlockData getBlockData(int i, int j, int k) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return null;
        }
        Chunk region = get(i>>Chunk.SIZE_BITS, k>>Chunk.SIZE_BITS);
        return region != null ? region.getBlockData(i&0xF, j, k&0xF) : null;
    }
    
    /** call with relative coords */
    public int getType(int i, int j, int k) {

        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0;
        }
        Chunk region = get(i>>Chunk.SIZE_BITS, k>>Chunk.SIZE_BITS);
        return region != null ? region.getTypeId(i&0xF, j, k&0xF) : 0;
    }

    public int getWater(int i, int j, int k) {

        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0;
        }
        Chunk region = get(i>>Chunk.SIZE_BITS, k>>Chunk.SIZE_BITS);
        return region != null ? region.getWater(i&0xF, j, k&0xF) : 0;
    }


    public int getLight(int i, int j, int k) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0xF0;
        }
        Chunk region = get(i>>Chunk.SIZE_BITS, k>>Chunk.SIZE_BITS);
        return region != null ? region.getLight(i&0xF, j, k&0xF) : 0;
    }

    @Override
    public Biome getBiome(int i, int k) {
        Chunk region = get(i>>Chunk.SIZE_BITS, k>>Chunk.SIZE_BITS);
        return region != null ? region.getBiome(i&0xF, k&0xF) : Biome.MEADOW_GREEN;
    }

    public boolean cache(IBlockWorld world, ChunkManager mgr, MeshedRegion mr, int renderChunkX, int renderChunkZ) {
        this.world = world;
        int basechunkX = mr.rX<<RegionRenderer.REGION_SIZE_BITS;
        int basechunkZ = mr.rZ<<RegionRenderer.REGION_SIZE_BITS;
        int offsetX = mr.rX-renderChunkX;
        int offsetZ = mr.rZ-renderChunkZ;
        this.baseX = mr.rX<<RegionRenderer.REGION_SIZE_BLOCK_SIZE_BITS;
        this.baseZ = mr.rZ<<RegionRenderer.REGION_SIZE_BLOCK_SIZE_BITS;
        
        boolean minXReq = offsetX > 0;
        boolean maxXReq = offsetX < RegionRenderer.RENDER_DISTANCE-1;
        boolean minZReq = offsetZ > 0;
        boolean maxZReq = offsetZ < RegionRenderer.RENDER_DISTANCE-1;
        for (int x = -1; x < WIDTH+1; x++) {
            for (int z = -1; z < WIDTH+1; z++) {
                Chunk c = mgr.get(basechunkX+x, basechunkZ+z);
                if (c == null) {
//                    boolean dbg = mr.rX==-7&&mr.rZ==-1;
//                  System.err.println("render");
//                    if (dbg) 
//                        System.out.println("c == null @"+(basechunkX+x)+", "+(basechunkZ+z));
//                    System.out.println(c);
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

    @Override
    public boolean setType(int x, int y, int z, int type, int flags) {
        return false;
    }

    @Override
    public int getHeight(int x, int z) {
        return 0;
    }

    @Override
    public boolean setData(int x, int y, int z, int type, int render) {
        return false;
    }

    @Override
    public boolean isNormalBlock(int ix, int iy, int iz, int offsetId) {
        if (offsetId < 0) {
            offsetId = this.getType(ix, iy, iz);
        }
        return Block.get(offsetId).isNormalBlock(this, ix, iy, iz);
    }

    @Override
    public boolean setTypeData(int x, int y, int z, int type, int data, int render) {
        return false;
    }
    @Override
    public boolean setBlockData(int x, int y, int z, BlockData bd, int flags){
        return false;
    }

    @Override
    public int getBiomeFaceColor(int x, int y, int z, int faceDir, int pass, BiomeColor colorType) {
        return this.world.getBiomeFaceColor(this.baseX+x, y, this.baseZ+z, faceDir, pass, colorType);
    }

    public IBlockWorld getWorld() {
        return this.world;
    }
}
