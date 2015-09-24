/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.WorldServer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface IChunkPopulator {

    /**
     * @param c
     */
    void populate(WorldServer world, Chunk c);

}
