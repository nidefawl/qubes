package nidefawl.qubes.worldgen.terrain;

import nidefawl.qubes.chunk.Chunk;

public interface ITerrainGen {

    public Chunk generateChunk(int chunkX, int chunkZ);
}
