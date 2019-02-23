/**
 * 
 */
package nidefawl.qubes.world;

import nidefawl.qubes.chunk.Chunk;

/**
 * @author Michael Hept 2017
 * Copyright: Michael Hept
 */
public interface IChunkWorld extends IBlockWorld {

    void updateLightHeightMap(Chunk chunk, int i, int k, int min, int max, boolean add);

    void flagChunkLightUpdate(int x, int z);

    String getName();

    int getHeightBits();
    
}