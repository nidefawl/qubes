/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import nidefawl.qubes.chunk.Chunk;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface IChunkPopulator {

    /**
     * @param c
     */
    void populate(Chunk c);

}
