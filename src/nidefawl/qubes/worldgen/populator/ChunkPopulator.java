/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.ArrayList;
import java.util.Random;

import com.google.common.collect.Lists;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockLeaves;
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
        Random r = new Random();
        ArrayList<Block> bl = Lists.newArrayList();
//        for (Block b : Block.block) {
//            if (b != null && b.getRenderType() == 1) {
//                if (b.getName().toLowerCase().contains("web"))
//                    continue;
//                if (b.getName().toLowerCase().contains("sap"))
//                    continue;
//                bl.add(b);
//            }
//        }
        bl.add(Block.flower_violet);
        bl.add(Block.flower_fmn_black);
        bl.add(Block.flower_fmn_blue);
        bl.add(Block.flower_compositae_camille);
        bl.add(Block.flower_compositae_milkspice);
        IWorldGen g = TreeGenerators.rand(rand);
        for (int i = 0; i < 133; i++) {
            int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int h = world.getHeight(x, z);

            int type = world.getType(x, h, z);
            if (isSoil(type)) {
                if ( a <= 4 && rand.nextInt(24) == 0) {
                    g.generate(world, x, h+1, z, rand);
                } else {
                    Block bg = Block.grassbush;
//                    if (b>10&&rand.nextInt(10) == 0) {
//                        bg = bl.get(r.nextInt(bl.size()));
//                    }
                    world.setType(x, h+1, z, bg.id, Flags.MARK);
                }
                a++;
            }   
        }
        int idx = r.nextInt(bl.size()*8);
        if (idx < bl.size()) {
            Block bg = bl.get(idx);
            for (int i = 0; i < 20; i++) {
                int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
                int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
                int h = world.getHeight(x, z);
                int type = world.getType(x, h, z);
                if (isSoil(type)) {
                    world.setType(x, h+1, z, bg.id, Flags.MARK);
                    a++;
                }   
            }
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
