/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class EmptyChunkPopulator implements IChunkPopulator {
    public final static String POPULATOR_NAME = "empty";
    
    public EmptyChunkPopulator(WorldServer world, long l, WorldSettings settings) {
        
    }
    
    @Override
    public void populate(Chunk c) {
        // TODO Auto-generated method stub
        
    }

}
