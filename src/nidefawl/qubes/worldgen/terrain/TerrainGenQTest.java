/**
 * 
 */
package nidefawl.qubes.worldgen.terrain;

import java.util.Arrays;
import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.blockdata.BlockDataQuarterBlock;
import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TerrainGenQTest implements ITerrainGen {
    public final static String GENERATOR_NAME = "terrain_qtest";

    private WorldServer world;
    private long  seed;
    private  TerrainNoise2D noiseM2;
//    private TerrainNoiseScale nois234234e;

    public TerrainGenQTest(WorldServer world, long seed, WorldSettings settings) {
        this.world = world;
        this.seed = seed;

        Random rand = new Random(this.seed);

        {

            double scaleMix2XZ = 1.1D;
            this.noiseM2 = new TerrainNoise2D(rand.nextLong(), scaleMix2XZ, scaleMix2XZ, 2);
        }
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
        Random rand = new Random(rx + rz);
        int heightBits = world.worldHeightBits;
        Chunk c = new Chunk(world, chunkX, chunkZ, heightBits);
        short[] blocks = c.getBlocks();
        try {

            generateTerrain(c, blocks);
        } catch (Exception e) {
            e.printStackTrace();
        }
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk c, short[] blocks) {
        int nSide = 2;
        int i=16+nSide;
        int wh = this.world.worldHeight+nSide;
        double[] dNoise = generateNoise(c.getBlockX(), c.getBlockZ());
        int top = Block.stone.id;
        int earth = Block.dirt.id;
        int stone = Block.stone.id;
        int water = Block.water.id;
        short[] blocksDouble = new short[18*18*258];
        for (int x = 0; x < 18; x++) {
            for (int z = 0; z < 18; z++) {
                int a = -1;
                int xz = z << 4 | x;
                int curBlock = 0;
                for (int y = (this.world.worldHeight+2) - 1; y >= 0; y--) {
                    double d = dNoise[(y)*(i*i)+(z)*i+(x)];
                  
                    if (d >= 0D) {
                        curBlock = stone;
//                        if (d2<=0) {
                        if (a < 0) {
                            curBlock = top;
                        } else if (a < 4) {
                            curBlock = earth;
                        }
//                        }
                        a++;
                    } else {
                        a = -1;
                        curBlock = 0;
//                      if (y < 60) {
//                      curBlock = Block.water.id;
//                  }t
                        if ( y <=93) {
                            curBlock = water;
                        }
                    }
                    blocksDouble[y * 18*18  + z * 18 + (x)] = (short) curBlock;// (short) (y<93?1:0);
                }
            }
        }
//        if (false)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz = z << 4 | x;
                for (int y = (this.world.worldHeight) - 1; y >= 0; y--) {
                    int yxz1 = y << 8 | xz;
                    int yxz = (y+1) * 18*18  + (z+1) * 18 + (x+1);
                  int id = blocksDouble[yxz];
                  if (id == 0) {
                      short sa = 0;
                      for (int j = 0; j < 6; j++) {
//                          if (j>>1!=1) {
//                              continue;
//                          }
                          int xOff = Dir.getDirX(j);
                          int yOff = Dir.getDirY(j);
                          int zOff = Dir.getDirZ(j);
                          int yxz2 = (y+1+yOff) * 18*18  + (z+1+zOff) * 18 + (x+1+xOff);
                          short s = blocksDouble[yxz2];
                          if (s != 0) {
                              sa++;
                          }
                      }
                      if (sa > 0) {
                          BlockDataQuarterBlock b = null;
                          for (int x2 = 0; x2 < 2; x2++)
                          for (int z2 = 0; z2 < 2; z2++)
                          for (int y2 = 0; y2 < 2; y2++) {
                              double d = 0;
                              int r = 1;
                              for (int x3 = 0; x3 < 2; x3++)
                              for (int z3 = 0; z3 < 2; z3++)
                              for (int y3 = 0; y3 < 2; y3++) {
                                  int idx_x=(x*2+x2+(-r+x3*2*r))>>1;
                                  int idx_y=(y*2+y2+(-r+y3*2*r))>>1;
                                  int idx_z=(z*2+z2+(-r+z3*2*r))>>1;
                                  d += dNoise[(idx_y+1)*(i*i)+((idx_z+1)*i)+(idx_x+1)];
                              }
//                            int idx_x=(x*2+x2)/2;
//                            int idx_y=(y*2+y2)/2;
//                            int idx_z=(z*2+z2)/2;
//                            d += dNoise[(idx_y+1)*(i*i)+((idx_z+1)*i)+(idx_x+1)];
                            d /= 8;

                            if (d >= -0.011D) {
                                if (b == null) b = new BlockDataQuarterBlock();
                              b.setType(x2, y2, z2, top);
                          }
                      }

                          if (b != null) {
                              blocks[yxz1] = (short) Block.quarter.id;
                              c.setBlockData(x, y, z, b);
                          }

                    }
                      
                  } else {
                      blocks[yxz1] = (short) id;
                  }
//                    int mainBlock = -1;
////                    Arrays.fill(nBlocks, 0);
//                    for (int x2 = 0; x2 < 2; x2++)
//                    for (int z2 = 0; z2 < 2; z2++)
//                    for (int y2 = 0; y2 < 2; y2++) {
//                        
//                        int yxz2 = ((y << 1 | y2) << 10 | (z << 1 | z2) << 5 | (x << 1 | x2));
//
//                    }
                }
            }
        }
//        int[] nBlocks = new int[5];
//        for (int x = 0; x < 16; x++) {
//            for (int z = 0; z < 16; z++) {
//                int xz = z << 4 | x;
//                for (int y = (this.world.worldHeight) - 1; y >= 0; y--) {
//                    int yxz = y << 8 | xz;
//                    int mainBlock = -1;
//                    Arrays.fill(nBlocks, 0);
//                    boolean allSame = true;
//                    boolean hasBlocks = true;
//                    for (int x2 = 0; x2 < 2; x2++)
//                    for (int z2 = 0; z2 < 2; z2++)
//                    for (int y2 = 0; y2 < 2; y2++) {
//                        int yxz2 = ((y << 1 | y2) << 10 | (z << 1 | z2) << 5 | (x << 1 | x2));
//                        int id = blocksDouble[yxz2]&Block.BLOCK_MASK;
//                        if (mainBlock < 0)
//                            mainBlock = id;
//                        if (mainBlock != id)
//                            allSame = false;
//                        int idx = 0;
//                        if (id == top) idx = 1;
//                        else if (id == earth) idx = 2;
//                        else if (id == stone) idx = 3;
//                        else if (id == water) idx = 4;
//                        nBlocks[idx]++;
//                        if (id > 0)
//                            hasBlocks = true;
//                    }
//                    if (allSame) {
//                        blocks[yxz] = (short) mainBlock;
//                    } else if (nBlocks[0] == 0) {
//                        int id = 0;
//                        int topNr = 0;
//                        int topIdx = 0;
//                        for (int i = 1; i < 5; i++) {
//                            if (nBlocks[i] > topNr) {
//                                topNr = nBlocks[i];
//                                topIdx = i;
//                            }
//                        }
//                        switch (topIdx) {
//                            case 0:
//                                id = 0;
//                                break;
//                            case 1:
//                            case 2:
//                                id = earth;
//                                break;
//                            case 3:
//                                id = stone;
//                                break;
//                            case 4:
//                                id = water;
//                                break;
//                        }
//                        blocks[yxz] = (short) id;
//                    } else if (hasBlocks) {
//    //                        BlockDataQuarterBlock b = new BlockDataQuarterBlock();
//    //                        for (int x2 = 0; x2 < 2; x2++)
//    //                        for (int z2 = 0; z2 < 2; z2++)
//    //                        for (int y2 = 0; y2 < 2; y2++) {
//    //                            int yxz2 = ((y << 1 | y2) << 10 | (z << 1 | z2) << 5 | (x << 1 | x2));
//    //                            int id = blocksDouble[yxz2] & Block.BLOCK_MASK;
//    //                            b.setType(x2, y2, z2, id);
//    //                        }
//    //                        blocks[yxz] = (short) Block.quarter.id;
//    //                        c.setBlockData(x, y, z, b);
//                    }
//                }
//            }
//        }
//        for (int x = 0; x < 16; x++) {
//            for (int z = 0; z < 16; z++) {
//                int xz = z << 4 | x;
//                for (int y = (this.world.worldHeight) - 1; y >= 0; y--) {
//                    int yxz = y << 8 | xz;
//                    int id = blocks[yxz];
//                    if (id == earth) {
//                        blocks[yxz] = (short) top;
//                    }
//                    if (id > 0) {
//                        break;
//                    }
//                }
//            }
//        }
    }

    private double[] generateNoise(int cX, int cZ) {
        int nSide = 2;
        int i=16+nSide;
        int wh = this.world.worldHeight+nSide;
        int wh1 = this.world.worldHeight;
        double[] dNoise = new double[i*i*(wh)]; 
        for (int x = -1; x < 16+1; x++) {
            for (int z = -1; z < 16+1; z++) {
                int xz=z<<4|x;
                double dBaseHeight = noiseM2.get(cX+x, cZ+z)*4;
                double dStr = 12.0D;
                double df = this.world.worldHeight;
                for (int y = -1; y < wh1+1; y++) {
                    double dYH = clamp10((y)/(double)df);
                    double dyoff = 0.8D;
                    if (dYH >= dyoff) {
                        double dz = (dYH-dyoff)*2.0D;
                        dz = 1-dz;
                        dz*=dz*(0.23D);
                        dz = 1-dz;
                        dz /=2;
                        dYH+=dz;
//                        if (dYH < 0.5)
//                            dYH = 0.5;
                    }
                    double dBase = dStr-dYH*dStr*2.0;
                    dBase = dBase+dBaseHeight;
                    dNoise[(y+1)*(i*i)+(z+1)*i+(x+1)] = dBase;
                }   
            }    
        }


        return dNoise;
        

//        int wh = this.world.worldHeight*2;
//        
//        double[] dNoise = new double[32*32*wh]; 
//        for (int x = 0; x < 32; x++) {
//            for (int z = 0; z < 32; z++) {
//                int xz=z<<5|x;
//                double dBaseHeight = noiseM2.get(cX|x, cZ|z)*8;
//              
//                double dStr = 12.0D;
//                double df = wh;
//                for (int y = 0; y < wh; y++) {
//                    double dYH = clamp10((y)/(double)df);
//                    double dyoff = 0.8D;
//                    if (dYH >= dyoff) {
//                        double dz = (dYH-dyoff)*2.0D;
//                        dz = 1-dz;
//                        dz*=dz*(0.23D);
//                        dz = 1-dz;
//                        dz /=2;
//                        dYH+=dz;
////                        if (dYH < 0.5)
////                            dYH = 0.5;
//                    }
//                    
//                    double dBase = dStr-dYH*dStr*2.0;
//                    dBase = dBase+dBaseHeight;
//                    dNoise[y<<10|xz] = dBase;
//                }   
//            }    
//        }
//
//
//        return dNoise;
        
    }
    private double func2(double m, double n, double j) {
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
        return EmptyChunkPopulator.class;
    }


}
