/**
 * 
 */
package nidefawl.qubes.chunk.client;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ChunkManagerBenchmark extends ChunkManagerClient {

    public Chunk testChunk;
    public ChunkManagerBenchmark(World world) {
        super(world);
    }
    
    @Override
    public Chunk get(int x, int z) {
        return testChunk;
    }

}
