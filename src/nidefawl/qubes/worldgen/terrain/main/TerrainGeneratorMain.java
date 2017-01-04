package nidefawl.qubes.worldgen.terrain.main;


import java.util.*;

import com.google.common.collect.Maps;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.noise.NoiseLib;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseJava;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.world.biomes.HexBiomesServer;
import nidefawl.qubes.worldgen.WorldGenInit;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;
import nidefawl.qubes.worldgen.terrain.main.SubTerrainGen.SubTerrainData;

public class TerrainGeneratorMain implements ITerrainGen {
    public final static String GENERATOR_NAME = "terrain_main";

    WorldServer world;
    long  seed;
    
//    private TerrainNoiseScale nois234234e;
    HexBiomesServer biomes;
    final static double smoothScale = 0.85;
    OpenSimplexNoise j;
    OpenSimplexNoise j2;
    OpenSimplexNoise j4;
    OpenSimplexNoise j5;
    OpenSimplexNoise j6;
    OpenSimplexNoise j7;
    final SubTerrainGen[] gens;
    public TerrainGeneratorMain(WorldServer world, long seed, WorldSettings settings) {
        this.world = world;
        this.seed = seed;
        this.biomes = new HexBiomesServer(world, seed, settings);
        this.gens = new SubTerrainGen[] {
                new SubTerrainGenMeadow(this),
                new SubTerrainGenDesert(this),
                new SubTerrainGenSnowHills(this),
                new SubTerrainGen4(this),
                new SubTerrainGen5(this),
                new SubTerrainGen6(this),
                new SubTerrainGen7(this),
        };
//        this.map.put(Biome.MEADOW_GREEN, new SubTerrainGen1(this));
//        this.map.put(Biome.MEADOW_BLUE, new SubTerrainGen1(this));
//        this.map.put(Biome.MEADOW_RED, new SubTerrainGen1(this));
//        this.map.put(Biome.DESERT, new SubTerrainGen2(this));
//        this.map.put(Biome.DESERT_RED, new SubTerrainGen2(this));
//        this.map.put(Biome.ICE, new SubTerrainGen3(this));
//        this.map.put(Biome.MEADOW_GREEN2, new SubTerrainGen4(this));

//        for (int i = 0; i < Biome.biomes.length; i++) {
//            if (Biome.biomes[i] != null) {
//                this.map.put(Biome.biomes[i], new SubTerrainGen7(this));        
//            }
//        }
        this.j = NoiseLib.makeGenerator(seed*33703^31);
        this.j2 = NoiseLib.makeGenerator(89153^23);
        this.j4 = NoiseLib.makeGenerator(89153^23);
        this.j5 = NoiseLib.makeGenerator(824112353^23);
        this.j6 = new OpenSimplexNoiseJava(266671);
        this.j7 = new OpenSimplexNoiseJava(121661);
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
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
        byte[] water = c.getWaterMask();
        generateTerrain(c, blocks, water, hexs, h);
        c.checkIsEmtpy();
        return c;
    }
    private SubTerrainGen getTerrainGenInstance(HexBiome b) {
        if (b.biome == Biome.DESERT) {
            if (b.subtype%2 != 0) {
                return this.gens[3];
            }
            return this.gens[1];
        }
        if (b.biome == Biome.DESERT_RED) {
            if (b.subtype%2 != 0) {
                return this.gens[3];
            }
            return this.gens[1];
        }
        if (b.biome == Biome.ICE) {
            if (b.subtype%2 != 0) {
                return this.gens[4];
            }
            return this.gens[2];
        }
        if (b.subtype%3 == 1) {
            return this.gens[3];
        }
        if (b.subtype%3 == 2) {
            return this.gens[6];
        }
        return this.gens[0];
    }

    private void generateTerrain(Chunk c, short[] blocks, byte[] waterMask, HexBiome[] hexs, ArrayList<HexBiome> h) {
        Map<HexBiome, SubTerrainData> map = Maps.newHashMap();
        for (HexBiome b : h) {
            
            SubTerrainGen g = getTerrainGenInstance(b);
            SubTerrainData data = g.prepare(c.getBlockX(), c.getBlockZ(), b);
            map.put(b, data);
        }
        int wh = this.world.worldHeight;
        int cX = c.getBlockX();
        int cZ = c.getBlockZ();
        double gridRadius = biomes.hwidth*0.98;
        double[] dNoise = new double[wh*Chunk.SIZE*Chunk.SIZE];
        double[] dNoise2 = new double[wh*Chunk.SIZE*Chunk.SIZE];
        double[] dSlice = new double[wh];
        double[] dSlice2 = new double[wh];
        double noiseScale4 = 1/32.0D;
        double noiseScale6 = 1/256.0D;
        double smoothScale = 0.9;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<Chunk.SIZE_BITS|x;
                HexBiome hex = hexs[xz];
                double hx = hex.getCenterX();
                double hz = hex.getCenterY();
                double distHex = GameMath.dist2d(hx, hz, cX+x, cZ+z);
                double outerDist = Math.max(distHex-gridRadius*smoothScale, 0);
                double outerScale = clamp10(outerDist/(gridRadius*(1.0-smoothScale)));
                outerScale=1-outerScale;
                outerScale=Math.pow(outerScale, 2);
                outerScale=1-outerScale;
                SubTerrainGen g = getTerrainGenInstance(hex);
                SubTerrainData data = map.get(hex);
                double dStr2 = 12.0D*2;
                double blockNoise2 = j4.eval((cX+x)*noiseScale4, (cZ+z)*noiseScale4);
                double blockNoise3 = j5.eval((cX+x)*noiseScale6, (cZ+z)*noiseScale6);
                Arrays.fill(dSlice2, 0.0D);
                double power = 1.0D;
                if (outerScale < 1.0D)
                    power = g.generate(cX, cZ, x, 0, wh, z, hex, data, dSlice, dSlice2);
                double noiseStr = clamp10(outerScale+(1-power));
                for (int y = 0; y < wh; y++) {
                    double dYH2 = clamp10((y+0+5)/(double)wh);
                    double dBase2 = dStr2-dYH2*dStr2*2.0;
                    dBase2+=blockNoise2*blockNoise3*2.3;
                    dNoise[y<<8|xz] = mix(dSlice[y], dBase2, noiseStr);
                    dNoise2[y<<8|xz] = mix(dSlice2[y], -111, noiseStr);
                }
            }
        }

        Random rand = new Random(0L);
        double noiseScale = 1/128.0D;
        double noiseScale2 = 1/2.0D;
        double noiseScale3 = 1/12.0D;
        double noiseScale5 = 1/6.0D;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<Chunk.SIZE_BITS|x;
                rand.setSeed((cX+x) * 89153 ^ (cZ+z) * 33703  + 0);
                int randRange = 24;
                int randX = -rand.nextInt(randRange)+rand.nextInt(randRange);
                int randZ = -rand.nextInt(randRange)+rand.nextInt(randRange);
                HexBiome hex = hexs[xz];
                Biome b = biomes.getBiome(cX+x+randX, cZ+z+randZ);
                Block top = b.getTopBlock();
                Block earth = b.getSoilBlock();
                int stone = 0xFFFFFF;
                int ore = 0x1FFFFFF;
                int undefined = 0x2FFFFFF;
                int a = -1;
                int curBlock = 0;
                int q = -1;
    //                if (orenoise > 0.2)
    //                    System.out.println("orenoise "+orenoise);
                for (int y = this.world.worldHeight - 1; y >= 0; y--) {
                    double d = dNoise[y << 8 | xz];
                    double d2 = dNoise2[y << 8 | xz];
                    double d3 = 0;
                    boolean wasWater = curBlock == Block.water.id;
                    boolean fromNonAir = curBlock != 0 && y+1<wh;
                    curBlock = undefined;
//                    if (d>-2&&d<2.1&&d2 > 3) {
//                        double orenoise1 = j7.eval((cX+x)*noiseScale5, y*noiseScale5, (cZ+z)*noiseScale5);
//                        double orenoise = j6.eval((cX+x+orenoise1*4)*noiseScale3, (y+orenoise1*4)*noiseScale3, (cZ+z+orenoise1*4)*noiseScale3);
//                        curBlock = undefined;
//                        if (orenoise > 0.25) {
//                            orenoise-=0.25;
//                            curBlock = ore;
//                        }
//                    }
                    if (curBlock == undefined) {
                        if (d >= 0D) {
                            curBlock = stone;
//                            if (d2<=0) {
                                if (a < 0) {
                                    if (wasWater) {
                                        curBlock = Block.gravel.id;
//                                      curBlock = earth.id;
//                                        } else {
//                                            curBlock = top.id;
//                                        }
                                    } else {
                                        curBlock = top.id;
                                    }
                                        
                                } else if (a < 3) {
                                    curBlock = earth.id;
                                }
//                            }
                            a++;
                            q = 0;
                        } else {
                            a = -1;
                            curBlock = 0;
//                          if (y < 60) {
//                              curBlock = Block.water.id;
//                          }
                          if (d3 > 0.5 || y <=93) {
                              curBlock = Block.water.id;
                              if (fromNonAir && curBlock == Block.water.id && (y+1<=93)) {
                                  waterMask[(y+1) << 8 | xz] = 1;
                              }
                              q++;
                          } else {
                                q = 0;
                            }
                        }
                    }
                    int bid = curBlock;
                    if (curBlock == stone) {
                        bid = hex.biome.getStone(this.world, cX + x, y, cZ + z, hex, rand);
                        
//                        this.biomes
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
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<Chunk.SIZE_BITS|x;
                double blockNoise2 = j2.eval((cX+x)*noiseScale2, (cZ+z)*noiseScale2);
                double blockNoise = j.eval((cX+x+blockNoise2*32)*noiseScale, (cZ+z+blockNoise2*32)*noiseScale);
                if (blockNoise < 0.1) {
                    continue;
                }
                Biome b = biomes.getBiome(cX+x, cZ+z);
                int y = 93;
                if (b == Biome.ICE) {
                    if (blocks[(y+1) << 8 | xz] == 0 && (y-8<0||waterMask[(y-8) << 8 | xz] == 0)) {
                        blocks[y << 8 | xz] =(short) Block.ice.id;
                        c.setData(x, y, z, 7);
                        waterMask[y << 8 | xz] = 1;
//                        waterMask[y << 8 | xz]++;   
                    }
                    continue;
                }
//                if (b == Biome.ICE) {
//                    int y = 93;
//                    if (blocks[y << 8 | xz] != 0) {
//                        blocks[y << 8 | xz] =(short) Block.ice.id;
//                    }
//                } else {
//              }
          }
      }
        noiseScale2 = 1/250.0D;
      for (int x = 0; x < 16; x++) {
          for (int z = 0; z < 16; z++) {
              int xz=z<<Chunk.SIZE_BITS|x;

              if (blocks[xz] != 0) {
                  blocks[xz] = (short) Block.stones.getFirst().id;
              }
              for (int y = 0; y < wh; y++) {
                  int n = blocks[y << 8 | xz]&Block.BLOCK_MASK;
                  if (n == Block.gravel.id) {
                      int depth = 0;
                      while (y+depth<wh) {
                          int nID = blocks[(y+depth)<<8|xz]&Block.BLOCK_MASK;
                          if (nID == 0 && waterMask[(y+depth)<<8|xz] == 0) {
                              break;
                          }
                          depth++;
                      }
                      double blockNoise2 = j2.eval((cX+x)*noiseScale2, (cZ+z)*noiseScale2);
                      double blockNoise = j.eval((cX+x+blockNoise2)*noiseScale2, (cZ+z+blockNoise2)*noiseScale2);
                      blockNoise += j4.eval((cX+x)*30, (cZ+z)*30)*0.1;
                      double sandNoise = j6.eval((cX+x)*0.1, (cZ+z)*0.1)*0.5+0.5;
                          
                      blockNoise+=1.0;
                      blockNoise*=0.5;
                      blockNoise = clamp10(blockNoise);
                      if (depth>0) {
//                          System.out.println(depth);
                          if (sandNoise<0.6-clamp10(depth/8.0)*0.6) {
                              blocks[y << 8 | xz]=(short) Block.sand.id;
                              continue;
                          }
                      }
                      int m = (int) (blockNoise*140);
                      m = (m / 10) % 9;
//                      System.out.println(m);
                      c.setData(x, y, z, m);
//                      System.out.println(blockNoise);
//                      blockNoise*=10;
                      
                  }
                }
            
            }
        }
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
    public WorldGenInit getWorldGen(WorldServer world, long seed, WorldSettings settings) {
        WorldGenInit init = new WorldGenInit();
        init.generator = this;
        init.biomeManager = this.biomes;
        init.populator = new ChunkPopulator(world, seed, settings);
        return init;
    }
}
