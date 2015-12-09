package nidefawl.qubes.worldgen.terrain.main;


import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.hex.HexCell;
import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.worldgen.biome.*;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;
import nidefawl.qubes.worldgen.terrain.main.SubTerrainGen.SubTerrainData;
import sun.util.resources.CurrencyNames_ar_LB;

public class TerrainGeneratorMain implements ITerrainGen {
    public final static String GENERATOR_NAME = "terrain_rivers";

    WorldServer world;
    long  seed;
    
//    private TerrainNoiseScale nois234234e;
    HexBiomesServer biomes;
    final static double smoothScale = 0.9;;

    private Map<Biome, SubTerrainGen> map = Maps.newConcurrentMap(); 
    public TerrainGeneratorMain(WorldServer world, long seed, WorldSettings settings) {
        this.world = world;
        this.seed = seed;
        this.map.put(Biome.MEADOW_GREEN, new SubTerrainGen1(this));
        this.map.put(Biome.MEADOW_BLUE, new SubTerrainGen1(this));
        this.map.put(Biome.MEADOW_RED, new SubTerrainGen1(this));
        this.map.put(Biome.DESERT, new SubTerrainGen2(this));
        this.map.put(Biome.DESERT_RED, new SubTerrainGen2(this));
    }

    @Override
    public void init() {
        biomes = (HexBiomesServer) world.biomeManager;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
        Random rand = new Random(rx + rz);
        int heightBits = world.worldHeightBits;
        Chunk c = new Chunk(world, chunkX, chunkZ, heightBits);
        HexBiome[] hexs = new HexBiome[Chunk.SIZE*Chunk.SIZE];
        ArrayList<HexBiome> h = new ArrayList<>(); 
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                HexBiome hex = biomes.blockToHex(c.getBlockX()+x, c.getBlockZ()+z);
                hexs[z<<Chunk.SIZE_BITS|x] = hex;
                c.biomes[z<<Chunk.SIZE_BITS|x] = (byte) hex.biome.id;
                if (!h.contains(hex)) {
                    h.add(hex);
                }
            }
        }
        short[] blocks = c.getBlocks();
        generateTerrain(c, blocks, hexs, h);
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk c, short[] blocks, HexBiome[] hexs, ArrayList<HexBiome> h) {
        Map<HexBiome, SubTerrainData> map = Maps.newHashMap();
        for (HexBiome b : h) {
            SubTerrainGen g = this.map.get(b.biome);
            SubTerrainData data = g.prepare(c.getBlockX(), c.getBlockZ());
            map.put(b, data);
        }
        int wh = this.world.worldHeight;
        int cX = c.getBlockX();
        int cZ = c.getBlockZ();
        double gridRadius = biomes.hwidth;
        double[] dNoise = new double[wh*Chunk.SIZE*Chunk.SIZE];
        double[] dNoise2 = new double[wh*Chunk.SIZE*Chunk.SIZE];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<Chunk.SIZE_BITS|x;
                HexBiome hex = hexs[xz];
                double hx = hex.getCenterX();
                double hz = hex.getCenterY();
                double distHex = GameMath.dist2d(hx, hz, cX+x, cZ+z);
                double outerDist2 = Math.min(Math.max(distHex, 0)/(gridRadius), 1);
                double outerDist = Math.max(distHex-gridRadius*smoothScale, 0);
                double outerScale = outerDist/(gridRadius*(1-smoothScale));
                SubTerrainGen g = this.map.get(hex.biome);
                SubTerrainData data = map.get(hex);
                double dStr2 = 12.0D*2;
                for (int y = 0; y < wh; y++) {
                    double dYH2 = clamp10((y+0+5)/(double)wh);
                    double dBase2 = dStr2-dYH2*dStr2*2.0;
                    double gen = g.generate(cX, cZ, x, y, z, hex, data);
                    dNoise[y<<8|xz] = mix(gen, dBase2, outerScale);
                }
            }
        }

        Random rand = new Random(0L);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<Chunk.SIZE_BITS|x;
                rand.setSeed((cX+x) * 89153 ^ (cZ+z) * 33703  + 0);
                int randRange = 32;
                int randX = -rand.nextInt(randRange)+rand.nextInt(randRange);
                int randZ = -rand.nextInt(randRange)+rand.nextInt(randRange);
                HexBiome hex = hexs[xz];
                Biome b = biomes.getBiome(cX+x+randX, cZ+z+randZ);
                Block top = b.getTopBlock();
                Block earth = b.getSoilBlock();
                int stone = 0xFFFFFF;
                int a = -1;
                int curBlock = 0;

                for (int y = this.world.worldHeight - 1; y >= 0; y--) {
                    double d = dNoise[y << 8 | xz];
                    double d2 = dNoise2[y << 8 | xz];
                    if (d >= 0D) {
                        curBlock = stone;
//                        if (d2<=0) {
                            if (a < 0) {
                                curBlock = top.id;
                            } else if (a < 3) {
                                curBlock = earth.id;
                            }
//                        }
                        a++;
                    } else {
                        a = -1;
                        curBlock = 0;
//                        if (y < 60) {
//                            curBlock = Block.water.id;
//                        }
                        if (d2 > 0.5 || y <=93) {
                            curBlock = Block.water.id;
                        }
                    }
                    int bid = curBlock;
                    if (curBlock == stone) {
                        bid = getStone(this.world, cX+x, y, cZ+z, hex, rand);
                    }
                    blocks[y << 8 | xz] = (short) bid;
                }
            }
        }
    }


    public int getStone(WorldServer world, int x, int y, int z, HexBiome hex, Random rand) {
        Block b = hex.biome.getStone();
        if (b != null)
            return b.id;
        float centerX = (float) biomes.getCenterX(hex.x, hex.z);
        float centerY = (float) biomes.getCenterY(hex.x, hex.z);
        float fx = (float) biomes.getPointX(hex.x, hex.z, 0);
        float fy = (float) biomes.getPointY(hex.x, hex.z, 0);
        rand.setSeed(x * 89153 ^ z * 31 + y);
        int randRange = 8;
        float randX = -rand.nextInt(randRange)+rand.nextInt(randRange);
        float randZ = -rand.nextInt(randRange)+rand.nextInt(randRange);
        double angle = GameMath.getAngle(x-centerX+randX, z-centerY+randZ, fx-centerX, fy-centerY);
        int scaleTangent = (int) (6+6*(angle/Math.PI)); // 0 == points to corner, 1/-1 == points to half of side
        if (scaleTangent < 3) {
            return Block.basalt.id;
        }
        if (scaleTangent < 6) {
            return Block.diorite.id;
        }
        if (scaleTangent < 9) {
            return Block.marble.id;
        }
        return Block.granite.id;
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

    @Override
    public Class<? extends IChunkPopulator> getPopulator() {
        return ChunkPopulator.class;
    }

    @Override
    public Class<? extends IBiomeManager> getBiomeManager() {
        return HexBiomesServer.class;
    }
}
