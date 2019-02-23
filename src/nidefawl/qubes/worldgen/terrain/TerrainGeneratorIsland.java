package nidefawl.qubes.worldgen.terrain;

import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.noise.TerrainNoiseMap2D;
import nidefawl.qubes.noise.TerrainNoiseMap2DResult;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.world.biomes.EmptyBiomeManager;
import nidefawl.qubes.worldgen.WorldGenInit;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;

public class TerrainGeneratorIsland implements ITerrainGen {
    public final static String GENERATOR_NAME = "terrain_island";

    private WorldServer world;

    private TerrainNoiseMap2D test2d;
    public TerrainGeneratorIsland(WorldServer world, long seed, WorldSettings settings) {
        this.world = world;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
        Random rand = new Random(rx + rz);
        if (test2d == null)
        test2d = new TerrainNoiseMap2D("textures/brushes/mountainRange/mountainRange_4.png", 1.0D/2048.0D, 8);
        Chunk c = new Chunk(this.world, chunkX, chunkZ);
        short[] blocks = c.getBlocks();
        byte[] water = c.getWaterMask();
        generateTerrain(c, blocks, water);
        c.checkIsEmtpy();
        return c;
    }

    public static double func2(double m, double n, double j) {
        double x = Math.abs(m - n) / (j);
        if (x > 1)
            x = 1;
        if (x < 0)
            x = 0;
        x *= x;
        x = 1 - x;
        x *= x;
        x = 1 - x;
        if (x > 1)
            x = 1;
        if (x < 0)
            x = 0;
        return 1-x;
    }

    public static double mix(double a, double b, double w) {
        if (w >= 1) {
            a = b;
        } else if (w > 0) {
            a = a + (b - a) * w;
        }
        return a;
    }

    public static double clamp10(double a) {
        return a > 1 ? 1 : a < 0 ? 0 : a;
    }

    public static double func(double m, double n, double j) {
        double x = Math.abs(m - n) / j;
        x += 1.1D;
        x *= x;
        x -= 1.1D;
        if (x > 1)
            x = 1;
        if (x < 0)
            x = 0;
        return x;
    }
    public int distSq(int x, int y, int z, int x2, int y2, int z2) {
        x = x-x2;
        z = z-z2;
        return x*x+z+z;
    }
    private void generateTerrain(Chunk c, short[] blocks, byte[] waterMask) {
        Random rand = new Random(0L);
        int wh = this.world.worldHeight;
        double dwh = wh;
        int cX = c.getBlockX();
        int cZ = c.getBlockZ();
        
        double[] dNoise = new double[16*16*wh]; 
        TerrainNoiseMap2DResult dnoise2da = test2d.generate(cX, cZ, 0, 0);
        double dStr = 44.0D;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<4|x;
                double dBaseHeight = dnoise2da.getBlur(x, z, 8);
//                dBaseHeight = dBaseHeight-0.3;
//                if (dBaseHeight < 0) {
//                    dBaseHeight/=0.3;
//                    dBaseHeight*=-1;
//                } else {
//                    dBaseHeight/=0.7;
//                    dBaseHeight = 1.0-dBaseHeight;
//                    dBaseHeight=Math.pow(dBaseHeight, 1.3f);
//                    dBaseHeight = 1.0-dBaseHeight;
//                }
                for (int y = 0; y < wh; y++) {
                    double dYH = clamp10(y/(double)2270);
                    double dBase = dStr-dStr*4.0*(y/dwh);
                    
                    
                    dBase = dBase+dBaseHeight*dStr*6;
                    dNoise[y<<8|xz] = dBase;
                }   
            }    
        }

        int waterHeight = 93;
        int stone = 0xFFFFFF;
        int undefined = 0x2FFFFFF;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<Chunk.SIZE_BITS|x;
                rand.setSeed((cX+x) * 89153 ^ (cZ+z) * 33703  + 0);
                int randSand = rand.nextInt(2);
//                int randRange = 24;
//                int randX = -rand.nextInt(randRange)+rand.nextInt(randRange);
//                int randZ = -rand.nextInt(randRange)+rand.nextInt(randRange);
                Block top = Block.grass;
                Block earth = Block.dirt;
                int a = -1;
                int curBlock = 0;
                for (int y = this.world.worldHeight - 1; y >= 0; y--) {
                    double d = dNoise[y << 8 | xz];
                    boolean wasWater = curBlock == Block.water.id;
                    boolean fromNonAir = curBlock != 0 && y+1<wh;
                    curBlock = undefined;
                    if (curBlock == undefined) {
                        if (d >= 0D) {
                            curBlock = stone;
                            if (a < 0) {
                                if (wasWater) {
                                    curBlock = Block.sand.id;
                                } else {
                                    curBlock = top.id;
                                    if (y <= waterHeight + 2) {
                                        curBlock = Block.sand.id;
                                    }
                                }

                            } else if (a < 3) {
                                curBlock = earth.id;
                            }
                            a++;
                        } else {
                            a = -1;
                            curBlock = 0;
                            if (y <= waterHeight) {
                                curBlock = Block.water.id;
                                if (fromNonAir && curBlock == Block.water.id && (y + 1 <= waterHeight)) {
                                    waterMask[(y + 1) << 8 | xz] = 1;
                                }
                            }
                        }
                    }
                    int bid = curBlock;
                    if (curBlock == stone) {
                        bid = Block.stones.getFirst().id;
                    }
                    if (curBlock == Block.water.id) {
                        blocks[y << 8 | xz] = 0;
                        waterMask[y << 8 | xz] = 1;
                        continue;
                    }
                    blocks[y << 8 | xz] = (short) bid;
                }
            }
        }
    }

    @Override
    public WorldGenInit getWorldGen(WorldServer world, long seed, WorldSettings settings) {
        WorldGenInit init = new WorldGenInit();
        init.generator = this;
        init.biomeManager = new EmptyBiomeManager(world, seed, settings);
        init.populator = new ChunkPopulator(world, seed, settings);
        return init;
    }
}
