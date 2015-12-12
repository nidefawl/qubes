/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockDoublePlant;
import nidefawl.qubes.block.BlockLeaves;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.TripletLongHash;
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
        ArrayList<Block> bl4 = Lists.newArrayList();
        bl4.add(Block.tallgrass1);
        bl4.add(Block.tallgrass2);
        bl4.add(Block.grassbush);
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
                        TreeGeneratorLSystem g = TreeGenerators.get(treeType);
                        g.generate(world, x, h+1, z, rand);
                        if (treeType == 2)
                        for (Entry<Long, Integer> e : g.blocks.entrySet()) {
                            if (e.getValue() == Block.leaves.id) {
                                long l = e.getKey();
                                int x1 = TripletLongHash.getX(l);
                                int y1 = TripletLongHash.getY(l)-1-rand.nextInt(5);
                                int z1 = TripletLongHash.getZ(l);
                                int typec = world.getType(x1, y1, z1);
                                if (typec != Block.leaves.id)
                                    continue;
                                for (int k = 1; k <4; k++) { 
                                    int offx = k==0?-1:k==2?1:0;
                                    int offz = k==1?-1:k==3?1:0;
                                    int bX = x1+offx;
                                    int bY = y1;
                                    int bZ = z1+offz;
                                    int typeb = world.getType(bX, bY, bZ);
                                    if (typeb == 0) {
                                        boolean fail = false;
                                        for (int y3 = 1; y3 < 7; y3++) {
                                            if (world.getType(bX, bY-y3, bZ) != 0) {
                                                fail = true;
                                                break;
                                            }
                                        }
                                        if (!fail) {
                                            int rotData = 1;
                                            switch (k) {
                                                case 0:
                                                    rotData = 8; break;
                                                case 1:
                                                    rotData = 1; break;
                                                case 2:
                                                    rotData = 2; break;
                                                case 3:
                                                    rotData = 4; break;
                                            }
                                            int y4 = 3+rand.nextInt(4);
                                            for (int y3 = 0; y3 < y4; y3++) {
                                                world.setTypeData(bX, bY-y3, bZ, Block.vines.id, rotData, Flags.MARK);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
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
                boolean foundGround = isSoil(type);
                if (foundGround) {
                    world.setType(x, h+1, z, bg.id, Flags.MARK);
                    world.setTypeData(x, h+2, z, bg.id, 8, Flags.MARK);
                    a++;
                }   
            }
        }
        for (int i = 0; i < 70; i++) {
            int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int h = world.getHeight(x, z);
            int w = world.getWater(x, h, z);
            int min1 = world.getType(x, h-1, z);
            int min2 = world.getType(x, h-2, z);
            if (w>0 && (isSoil(min1)||min1==Block.sand.id)) {
                int idx2 = r.nextInt(bl4.size());
                Block bg = bl4.get(idx2);
                h--;
                world.setType(x, h+1, z, bg.id, Flags.MARK);
                if (bg instanceof BlockDoublePlant) {

                    world.setTypeData(x, h+2, z, bg.id, 8, Flags.MARK);
                }
            } else if (w>0&& min1==Block.air.id && (isSoil(min2)||min2==Block.sand.id)) {
                h--;
                h--;
                world.setType(x, h+1, z, Block.tallgrass2.id, Flags.MARK);
                world.setTypeData(x, h+2, z, Block.tallgrass2.id, 8, Flags.MARK);
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
