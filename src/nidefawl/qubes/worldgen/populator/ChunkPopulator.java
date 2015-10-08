/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ChunkPopulator implements IChunkPopulator {
    public final static String POPULATOR_NAME = "default";

    private final WorldServer world;
    final TreeGen1 tree = new TreeGen1();
    
    public ChunkPopulator(WorldServer world, long l, WorldSettings settings) {
        this.world = world;
    }
    
    @Override
    public void populate(Chunk c) {
        Random rand = new Random();
        int a = 0;
        for (int i = 0; i < 255; i++) {
            int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int h = world.getHeight(x, z);

            int type = world.getType(x, h, z);
            if (isSoil(type)) {
                if ( a <= 5 && rand.nextInt(3) == 0) {
                    TreeGenerators.rand(rand).generate(world, x, h+1, z, rand);
                } else {
                    world.setType(x, h+1, z, Block.longgrass.id, Flags.MARK);
                }
                a++;
            }   
            
//                tree.generate(world, x, h, z, rand);
        }
    }
    /**
     * @param type
     * @return
     */
    private boolean isSoil(int type) {
        return type == Block.dirt.id || type == Block.grass.id;
    }

}
