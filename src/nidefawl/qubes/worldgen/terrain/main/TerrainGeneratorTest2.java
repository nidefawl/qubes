package nidefawl.qubes.worldgen.terrain.main;


import java.util.*;

import com.google.common.collect.Maps;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseJava;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.world.biomes.EmptyBiomeManager;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.worldgen.WorldGenInit;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;
import nidefawl.qubes.worldgen.terrain.main.SubTerrainGen.SubTerrainData;

public class TerrainGeneratorTest2 implements ITerrainGen {
    public final static String GENERATOR_NAME = "terrain_test2";

    WorldServer world;
    long  seed;
    
    final static double smoothScale = 0.85;
    OpenSimplexNoise j;
    OpenSimplexNoise j2;
    OpenSimplexNoise j4;
    OpenSimplexNoise j5;
    OpenSimplexNoise j6;
    OpenSimplexNoise j7;

    private  TerrainNoiseScale noise3;
    private  TerrainNoiseScale noise;
    private  TerrainNoise2D noiseM2;
    private  TerrainNoiseScale noise2;
    private  TerrainNoise2D noise2D;
    private  RiverNoise2D r2D;
    private  RiverNoise2D r2D2;
    private TerrainNoiseScale noise5;
    private TerrainGeneratorLight main;
    
    public TerrainGeneratorTest2(WorldServer world, long seed, WorldSettings settings) {
        this.world = world;
        this.seed = seed;
        this.j = NoiseLib.makeGenerator(seed*33703^31);
        this.j2 = NoiseLib.makeGenerator(89153^23);
        this.j4 = NoiseLib.makeGenerator(89153^23);
        this.j5 = NoiseLib.makeGenerator(824112353^23);
        this.j6 = new OpenSimplexNoiseJava(266671);
        this.j7 = new OpenSimplexNoiseJava(121661);
        Random rand = new Random(seed);

        {

            double scaleMixXZ = 12.80D;
            double scaleMixY = scaleMixXZ*0.4D;
            double scaleMix2XZ = 1.1D;
            double scaleT1XZ = 0.3D;
            double scaleT1Y = scaleT1XZ*0.3D;
            double scaleT2XZ = 4.3D;
            double scaleT2Y = scaleT1XZ*0.3D;
            double scaleT5XZ = 3.6D;
            double scaleT5Y = scaleT5XZ*3.4D;
            this.noise = NoiseLib.newNoiseScale(rand.nextLong())
                    .setUpsampleFactor(4)
                    .setScale(scaleMixXZ, scaleMixY, scaleMixXZ)
                    .setOctavesFreq(3, 2.0);
            this.noiseM2 = new TerrainNoise2D(rand.nextLong(), scaleMix2XZ, scaleMix2XZ, 2);
            this.noise2 = NoiseLib.newNoiseScale(rand.nextLong())
                    .setUpsampleFactor(4)
                    .setScale(scaleT1XZ, scaleT1Y, scaleT1XZ)
                    .setOctavesFreq(3, 1.97D + 0.01);
            this.noise5 = NoiseLib.newNoiseScale(rand.nextLong())
                    .setUpsampleFactor(4)
                    .setScale(scaleT5XZ, scaleT5Y, scaleT5XZ)
                    .setOctavesFreq(3, 2.59D);
            this.noise3 = NoiseLib.newNoiseScale(rand.nextLong())
                    .setUpsampleFactor(4)
                    .setScale(scaleT2XZ, scaleT2Y, scaleT2XZ)
                    .setOctavesFreq(3, 2D);
            
            this.noise2D = new TerrainNoise2D(rand.nextLong(), 1, 1, 1);
            double dRScale = 0.23D;
            this.r2D = new RiverNoise2D(rand.nextLong(), dRScale, 8);
             dRScale = 2.14D;
            this.r2D2 = new RiverNoise2D(33, dRScale, 8);
        }
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        int heightBits = world.worldHeightBits;
        Chunk c = new Chunk(world, chunkX, chunkZ, heightBits);
        short[] blocks = c.getBlocks();
        byte[] water = c.getWaterMask();
        generateTerrain(c, blocks, water);
        c.checkIsEmtpy();
        return c;
    }
    public static class NoiseData  {

        public RiverNoiseResult r2Dn;
        public RiverNoiseResult r2Dn2;
        public double[] dNoise2_;
        public double[] dnoise5_;
        public double[] dnoise_;
        public double[] dnoise3_;

    }

    private void generateTerrain(Chunk c, short[] blocks, byte[] waterMask) {
        Map<HexBiome, SubTerrainData> map = Maps.newHashMap();
        int wh = this.world.worldHeight;
        int cX = c.getBlockX();
        int cZ = c.getBlockZ();
        double[] dNoise = new double[wh*Chunk.SIZE*Chunk.SIZE];
        double[] dNoise2 = new double[wh*Chunk.SIZE*Chunk.SIZE];

        double scaleMixXZ = 0.80D;
        double scaleMixY = scaleMixXZ*1.7D;
        double scaleT1XZ = 0.8D;
        double scaleT1Y = scaleT1XZ*4.1D;
        double scaleT2XZ =0.8D;
        double scaleT2Y = scaleT1XZ*4.3;
        double scaleT5XZ =0.9D;
        double scaleT5Y = scaleT5XZ*6.1;
        this.noise.setScale(scaleMixXZ, scaleMixY, scaleMixXZ);
        this.noise2.setScale(scaleT1XZ, scaleT1Y, scaleT1XZ);
        this.noise5.setScale(scaleT5XZ, scaleT5Y, scaleT5XZ);
        this.noise3.setScale(scaleT2XZ, scaleT2Y, scaleT2XZ);
        NoiseData data = new NoiseData();
        double dRScale = 1.43D;
        this.r2D = new RiverNoise2D(214, dRScale, 8);
        data.r2Dn = r2D.generate(cX, cZ);
         dRScale = 4.23D;
        this.r2D2 = new RiverNoise2D(144, dRScale, 8);
        this.noise2D = new TerrainNoise2D(12412, 0.77, 0.77, 1);
        data.r2Dn2 = r2D2.generate(cX, cZ);
        data.dNoise2_ = noise2.gen(cX, cZ);
        data.dnoise5_ = noise5.gen(cX, cZ);
        data.dnoise_ = noise.gen(cX, cZ);
        data.dnoise3_ = noise3.gen(cX, cZ);

        double randomHeight = 0.004D;
        double tallness = 12.8D;
        double tallnessBase = 0.15D;
        double heightOffset = 27.0D;
        double dStr = 44.0D;
        double yBrr = 110;
        double yBrr2 = 115;
        double brrrIntens = 0.8;
        double brrrIntens2 = 0.2;
        double riverY = 87;
        double riverY2 = riverY +1;
        double riverStr = 0.0;
        double cavePos = 110;
        double caveStrength = 44;
        
        

        double gridRadius = 255;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double dBaseHeight = (tallnessBase+(noise2D.get(cX|x, cZ|z)))*tallness;
                double dh2 = noiseM2.get(cX|x, cZ|z);
                double xd = 0.9D;
                dh2 = 1-clamp10((1-clamp10((dh2-(1-xd))/xd))*0.7);
                double dBlurred = clamp10(data.r2Dn.getBlur(x, z, 8)*4)*0.4;
                double dBlurredA = (clamp10(Math.pow(1+data.r2Dn2.getBlur(x, z, 4), 1.2)-1))*4;
                double dBlurred2 = clamp10(data.r2Dn.getBlur(x, z, 2))*0.25;
                double df = wh;
                int idx_xz = (z * 16 + x) * (256);
                double flattenScale = 0;
                double coreScale = 0.7;
                dStr = 12;
                
                for (int y = 0; y < wh; y++) {
                    double dYH = clamp10((y + 0 + heightOffset * flattenScale) / (double) df);
                    double dyoff = 0.6D;
                    if (dYH >= dyoff) {
                        dYH*=1.0D+(dYH-dyoff);
    //                    double dz = (dYH - dyoff) * 2.0D;
    //                    dz = 1 - dz;
    //                    dz *= dz * (0.23D);
    //                    dz = 1 - dz;
    //                    dz /= 2;
    //                    dYH += dz;
                    }
                    int idx = idx_xz + y;
                    int xz=z<<Chunk.SIZE_BITS|x;
                    double dN1 = data.dnoise_[idx] * 32.9D;
                    dYH = mix(dYH, dN1, randomHeight*coreScale);
                    double dBase = dStr - dYH * dStr * 2.0;
                    dBase = dBase + dBaseHeight;
                    double dN = data.dNoise2_[idx] * 0.7;
                    double dN7 = data.dnoise5_[idx];
                    double dN2 = data.dnoise3_[idx] * 2.0D;
                    dBase += dN * 3.7 * dh2 * coreScale;
                    dBase += dN7 * 5.7 * dh2 * coreScale;
                    double dRiverH = func2(riverY - dBaseHeight * 1.6D, y, 24);
                    double dRiverH2 = func2(riverY2, y, 23);
                    double xr = 0;
                    xr += dRiverH * dBlurred2 * dStr * coreScale*1.2*riverStr;
                    xr += dRiverH2 * dBlurred * dStr * coreScale*1.7*riverStr;
                    double dt = func2(cavePos + dN1, y, 4.4D) * 0.25;
                    dt *= clamp10((y-95.0D)/20.0D);
                    double cavestr = dt * dStr * dBlurredA * (0.5D + dN1 * 0.7D + 0.2 * dN2);
                    if (cavestr<0)cavestr=0;
                    xr += cavestr * caveStrength;
                    double brrr = y >= yBrr ? 1: func2(yBrr, y, 12);
                    double brrr2 = y >= yBrr2 ? 1: func2(yBrr2, y, 12);
                    if (brrr < 0) brrr = 0;
                    if (brrr2 < 0) brrr2 = 0;
                    dBase -= brrr*dh2*dStr*0.5*brrrIntens;
                    dBase -= brrr2*dN2*dStr*0.5*brrrIntens2;
                    dBase -= xr * coreScale;
                    dBase=mix(dBase, -dStr, clamp10((y-(wh-20))/20.0D));
                    dNoise[y<<8|xz] = dBase;
                    dNoise2[y<<8|xz] = cavestr * 36;
                }
                
            }    
        }

        Random rand = new Random(0L);
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
                Biome b = this.world.biomeManager.getBiome(cX+x+randX, cZ+z+randZ);
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
                                        curBlock = Block.stones.getFirst().id;
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
                    if (curBlock == stone ) {
                        bid = Block.stones.getFirst().id;//getStone(this.world, cX+x, y, cZ+z, hex, rand);
                        if (d2>0) {
                          double orenoise1 = j7.eval((cX+x)*noiseScale5, y*noiseScale5, (cZ+z)*noiseScale5);
                          double orenoise = j6.eval((cX+x+orenoise1*4)*noiseScale3, (y+orenoise1*4)*noiseScale3, (cZ+z+orenoise1*4)*noiseScale3);

//                            System.out.println("ore at "+(cX+x)+","+y+","+(cZ+z));
                            //todo: add mapping stone<->ore
                          if (orenoise > 0.3)
                            bid = Block.ores.getFirst().id;
                        //                            for (Block b2 : Block.ores.getBlocks()) {
                        //                                if (b2.gette
                        //                            }
                        }
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
//        for (int x = 0; x < 16; x++) {
//            for (int z = 0; z < 16; z++) {
//                int xz=z<<Chunk.SIZE_BITS|x;
//                double blockNoise2 = j2.eval((cX+x)*noiseScale2, (cZ+z)*noiseScale2);
//                double blockNoise = j.eval((cX+x+blockNoise2*32)*noiseScale, (cZ+z+blockNoise2*32)*noiseScale);
//                if (blockNoise < 0.1) {
//                    continue;
//                }
//                HexBiome hex = hexs[xz];
//                Biome b = biomes.getBiome(cX+x, cZ+z);
//                int y = 93;
//                if (b == Biome.ICE) {
//                    if (blocks[(y+1) << 8 | xz] == 0 && (y-8<0||waterMask[(y-8) << 8 | xz] == 0)) {
//                        blocks[y << 8 | xz] =(short) Block.ice.id;
//                        c.setData(x, y, z, 7);
//                        waterMask[y << 8 | xz] = 1;
////                        waterMask[y << 8 | xz]++;   
//                    }
//                    continue;
//                }
////                if (b == Biome.ICE) {
////                    int y = 93;
////                    if (blocks[y << 8 | xz] != 0) {
////                        blocks[y << 8 | xz] =(short) Block.ice.id;
////                    }
////                } else {
////              }
//          }
//      }
        noiseScale2 = 1/250.0D;
      for (int x = 0; x < 16; x++) {
          for (int z = 0; z < 16; z++) {
              int xz=z<<Chunk.SIZE_BITS|x;

              if (blocks[xz] != 0) {
                  blocks[xz] = (short) Block.stones.getFirst().id;
              }
              for (int y = 0; y < wh; y++) {
                  int n = blocks[y << 8 | xz]&Block.BLOCK_MASK;
                  if (n == Block.stones.getFirst().id) {
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
        init.biomeManager = new EmptyBiomeManager(world, seed, settings);
        init.populator = new ChunkPopulator(world, seed, settings);
        return init;
    }
}
