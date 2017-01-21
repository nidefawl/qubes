/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.*;
import com.google.common.collect.Lists;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.block.*;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TripletLongHash;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.world.biomes.BiomeManagerType;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.world.biomes.HexBiomesServer;
import nidefawl.qubes.world.structure.tree.Tree;
import nidefawl.qubes.worldgen.structure.GenTask;
import nidefawl.qubes.worldgen.structure.MineGen;

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
        int cX = c.getBlockX() + 8;
        int cZ = c.getBlockZ() + 8;
        Biome b;
        double distScale;
        if (this.world.getBiomeType() == BiomeManagerType.HEX) {
            HexBiome hex = this.world.getHex(cX, cZ);
            b = hex.biome;
            double distCenter = GameMath.dist2d(hex.getCenterX(), hex.getCenterY(), cX, cZ);
            distScale = Math.max(0, 1 - (distCenter / (hex.getGrid().radius * 0.8)));
            if (distScale > 0.4) {
                this.world.queueGenTask(new GenTask(this.world, c.x, c.z, new MineGen()));
            }
        }
        else {
            distScale = 1;
            b = c.getBiome(8, 8);
        }
//        System.out.println(b);
        if (b == Biome.DESERT || b == Biome.DESERT_RED) {
            return;
        }
        Random rand = new Random(c.x*2938921874L+c.z+3574985345L);
        int a = 0;
        Random r = new Random(rand.nextLong());
        ArrayList<Block> bl = Lists.newArrayList();
        for (int i = 0; i < IDMappingBlocks.HIGHEST_BLOCK_ID+1; i++) {
            Block b1 = Block.get(i);
            if (b1 != null && b1.getBlockCategory() == BlockCategory.FLOWER) {
                bl.add(b1);
            }
        }
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
        bl4.add(Block.cattail);
        bl4.add(Block.cattail);
        bl4.add(Block.grassbush);
        bl4.add(Block.aloe_vera);
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
        int nTrees = (int) (3+distScale*64);
        Block bush = Block.grassbush;
        if (rand.nextInt(44) > 22) {
            bush = Block.thingrass;
        }
        int amt = 44;
        for (int i = 0; i < amt; i++) {
            int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int h = world.getHeight(x, z);

            int type = world.getType(x, h, z);
            if (isSoil(type)) {
                if ( a <= nTrees && rand.nextInt(174) == 0) {
                    if (list != null && !list.isEmpty()) {
                        int treeType = list.get(rand.nextInt(list.size()));
                        TreeGeneratorLSystem g = TreeGenerators.get(treeType, rand);
                        if (treeType == 2) {
                            g.setVines(Block.vines);
                        }
                        g.generate(world, x, h+1, z, rand);
                        if (this.world.getBiomeType() == BiomeManagerType.HEX) {
                            Tree tree = g.getTree();
                            if (tree != null) {
                                HexBiome hex2 = ((HexBiomesServer)world.biomeManager).blockToHex(x, z);
                                hex2.registerTree(tree);
                            }
                        }
                    }
                } else {
                    world.setType(x, h+1, z, bush.id, Flags.MARK);
                    if (rand.nextInt(amt/2) == 0) {
                        bush = Block.grassbush;
                        if (rand.nextInt(44) > 22) {
                            bush = Block.thingrass;
                        }
                        if (rand.nextInt(34) > 22) {
                            bush = Block.aloe_vera;
                        }
                    }
                }
                a++;
            }   
        }
        int idx = r.nextInt(bl.size()*9);
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
        if (r.nextInt(12) == 0) {
            int idx2 = r.nextInt(bl2.size());
            Block bg = bl2.get(idx2);
            for (int i = 0; i < 60; i++) {
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
        if (r.nextInt(12) == 0) {
            int idx2 = r.nextInt(bl3.size());
            Block bg = bl3.get(idx2);
            for (int i = 0; i < 60; i++) {
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
        for (int i = 0; i < 6; i++) {
            int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int h = world.getHeight(x, z);
            if (world.getType(x, h+1, z) != 0) {
                continue;
            }
            if (world.getType(x, h, z) != 0) {
                continue;
            }
            int w1 = world.getWater(x, h+1, z);
            int w2 = world.getWater(x, h, z);
            if (w1==0&&w2>0) {
                int depth = 0;
                while (h+depth>0) {
                    int t = world.getType(x, h+depth, z);
                    if (t != 0 || world.getWater(x, h+depth, z) == 0) {
                        break;
                    }
                    depth--;
                }
                if (depth >-3) {
                    for (int j = 0; j < 6; j++) {
                        int x1 = x+rand.nextInt(8)-4;
                        int z1 = z+rand.nextInt(8)-4;
                        if (world.getType(x1, h+1, z1) != 0) {
                            continue;
                        }
                        if (world.getType(x1, h, z1) != 0) {
                            continue;
                        }
                        int w3 = world.getWater(x1, h+1, z1);
                        int w4 = world.getWater(x1, h, z1);
                        if (w3==0&&w4>0) {
                            world.setType(x1, h, z1, Block.waterlily.id, Flags.MARK);
                        }
                    }
                }
                
            }
        }
        for (int i = 0; i < 120; i++) {
            int x = c.x<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int z = c.z<<Chunk.SIZE_BITS|rand.nextInt(Chunk.SIZE);
            int h = world.getHeight(x, z);
            if (world.getType(x, h, z) != 0) {
                continue;
            }
            int w = 1;//world.getWater(x, h, z);
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
