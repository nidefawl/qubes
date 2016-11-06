package nidefawl.qubes.worldgen.terrain;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.worldgen.WorldGenInit;

public interface ITerrainGen {

    public Chunk generateChunk(int chunkX, int chunkZ);
    
    public WorldGenInit getWorldGen(WorldServer world, long seed, WorldSettings settings);
}
