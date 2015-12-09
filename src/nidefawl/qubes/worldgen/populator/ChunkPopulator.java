/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.biome.Biome;
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
//        if (1==1)return;
        Biome b = this.world.biomeManager.getBiome(c.getBlockX()+8, c.getBlockZ()+8);
        if (b == Biome.DESERT || b == Biome.DESERT_RED)
            return;
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
//      }
      bl.add(Block.flower_violet);
      bl.add(Block.flower_fmn_black);
      bl.add(Block.flower_fmn_blue);
      bl.add(Block.flower_compositae_camille);
      bl.add(Block.flower_compositae_milkspice);
      bl.add(Block.flower_oxmorina_blue);
      bl.add(Block.flower_poppy);
      bl.add(Block.flower_rose);
      bl.add(Block.flower_compositae_pinkpanther);
      bl.add(Block.flower_compositae_tigerteeth);
      ArrayList<Block> bl2 = Lists.newArrayList();
      bl2.add(Block.fern1);
      bl2.add(Block.fern2);
      bl2.add(Block.fern3);
      bl2.add(Block.fern4);
      ArrayList<Block> bl3 = Lists.newArrayList();
      bl3.add(Block.tallgrass1);
        bl3.add(Block.tallgrass2);
        List<Integer> list = new ArrayList<Integer>();
        list.add(0);
        list.add(0);
        list.add(0);
        list.add(0);
        list.add(1);
        list.add(1);
        list.add(1);
        list.add(1);
        list.add(2);
        list.add(2);
        list.add(2);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(4);
        list.add(4);
        list.add(4);
        for (int i = 0; i < 133; i++) {
            int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int h = world.getHeight(x, z);

            int type = world.getType(x, h, z);
            if (isSoil(type)) {
                if ( a <= 4 && rand.nextInt(24) == 0) {
                    if (list != null && !list.isEmpty()) {
                        int treeType = list.get(rand.nextInt(list.size()));
                        IWorldGen g = TreeGenerators.get(treeType);
                        g.generate(world, x, h+1, z, rand);
                    }
                } else {
                    Block bg = Block.grassbush;
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
        if (r.nextInt(30) == 0) {
            int idx2 = r.nextInt(bl2.size());
            Block bg = bl2.get(idx2);
            for (int i = 0; i < 20; i++) {
                int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
                int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
                int h = world.getHeight(x, z);
                int type = world.getType(x, h, z);
                if (isSoil(type)) {
                    world.setType(x, h+1, z, bg.id, Flags.MARK);
                    world.setTypeData(x, h+2, z, bg.id, 8, Flags.MARK);
                    a++;
                }   
            }
        }
        else if (r.nextInt(30) == 0) {
            int idx2 = r.nextInt(bl3.size());
            Block bg = bl3.get(idx2);
            for (int i = 0; i < 20; i++) {
                int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
                int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
                int h = world.getHeight(x, z);
                int type = world.getType(x, h, z);
                if (isSoil(type)) {
                    world.setType(x, h+1, z, bg.id, Flags.MARK);
                    world.setTypeData(x, h+2, z, bg.id, 8, Flags.MARK);
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
