package nidefawl.qubes.worldgen.terrain;


import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.world.biomes.EmptyBiomeManager;
import nidefawl.qubes.worldgen.WorldGenInit;
import nidefawl.qubes.worldgen.populator.ChunkPopulator;
import nidefawl.qubes.worldgen.populator.EmptyChunkPopulator;

public class TerrainGeneratorOther implements ITerrainGen {
    public final static String GENERATOR_NAME = "terrain_other";

    private WorldServer world;
    private long  seed;
    private final TerrainNoise noise3;
    private final TerrainNoise noise;
    private final TerrainNoise2D noiseM2;
    private final TerrainNoiseCustom1 noise2;
    private final TerrainNoise noise4;
    private final TerrainNoise2D noise2D;
    private final RiverNoise2D r2D;
    private final RiverNoise2D r2D2;
    private TerrainNoiseCustom1 noise5;

    public TerrainGeneratorOther(WorldServer world, long seed, WorldSettings settings) {
        this.world = world;
        this.seed = seed;
        Random rand = new Random(this.seed);
        double scaleMixXZ = 1.0D;
        double scaleMixY = scaleMixXZ*0.05D;
        double scaleMix2XZ = 1.1D;
        double scaleMix2Y = scaleMix2XZ;
        double scaleT1XZ = 0.9D;
        double scaleT1Y = scaleT1XZ*0.7D;
        double scaleT2XZ = 4.3D;
        double scaleT2Y = scaleT1XZ*0.3D;
        double scaleT4XZ = 3.3D;
        double scaleT4Y = scaleT4XZ*0.1D;
        double scaleT5XZ = 5.6D;
        double scaleT5Y = scaleT5XZ*3.4D;
        this.noise = new TerrainNoise(rand.nextLong(), scaleMixXZ, scaleMixY, scaleMixXZ, 1);
        this.noiseM2 = new TerrainNoise2D(rand.nextLong(), scaleMix2XZ, scaleMix2XZ, 1);
        this.noise2 = new TerrainNoiseCustom1(rand.nextLong(), scaleT1XZ, scaleT1Y, scaleT1XZ, 3);
        this.noise5 = new TerrainNoiseCustom1(rand.nextLong(), scaleT5XZ, scaleT5Y, scaleT5XZ, 3);
        this.noise3 = new TerrainNoise(rand.nextLong(), scaleT2XZ, scaleT2Y, scaleT2XZ, 1);
        this.noise4 = new TerrainNoise(rand.nextLong(), scaleT4XZ, scaleT4Y, scaleT4XZ, 1);
        this.noise2D = new TerrainNoise2D(rand.nextLong(), 1, 1, 1);
        double dRScale = 0.1D;
        this.r2D = new RiverNoise2D(rand.nextLong(), dRScale, 8);
         dRScale = 0.8D;
        this.r2D2 = new RiverNoise2D(rand.nextLong(), dRScale, 8);
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
        Random rand = new Random(rx + rz);
        Chunk c = new Chunk(this.world, chunkX, chunkZ);
        short[] blocks = c.getBlocks();
        generateTerrain(c, blocks);
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain2(Chunk c) {
        Random rand = new Random(this.seed);
        double scaleMixXZ = 8.0D;
        double scaleMixY = scaleMixXZ*0.2D;
        double scaleMix2XZ = 4.1D;
        double scaleMix2Y = scaleMix2XZ;
        double scaleT1XZ = 22.3D;
        double scaleT1Y = scaleT1XZ*1.4D;
        double scaleT2XZ = 13.3D;
        double scaleT2Y = scaleT1XZ*0.3D;
        double scaleT4XZ = 6.3D;
        double scaleT4Y = scaleT4XZ*0.1D;
        int wh = this.world.worldHeight;
        TerrainNoise noise = new TerrainNoise(rand.nextLong(), scaleMixXZ, scaleMixY, scaleMixXZ, 1);
        TerrainNoise2D noiseM2 = new TerrainNoise2D(rand.nextLong(), scaleMix2XZ, scaleMix2XZ, 1);
        TerrainNoise noise2 = new TerrainNoise(rand.nextLong(), scaleT1XZ, scaleT1Y, scaleT1XZ, 1);
        TerrainNoise noise3 = new TerrainNoise(rand.nextLong(), scaleT2XZ, scaleT2Y, scaleT2XZ, 1);
        TerrainNoise noise4 = new TerrainNoise(rand.nextLong(), scaleT4XZ, scaleT4Y, scaleT4XZ, 1);
        TerrainNoise2D noise2D = new TerrainNoise2D(rand.nextLong(), 4, 4, 1);
        double dRScale = 0.2D;
        RiverNoise2D r2D = new RiverNoise2D(rand.nextLong(), dRScale, 8);
        
        double[] dNoise = new double[16*16*wh]; 
        int cX = c.x<<4;
        int cZ = c.z<<4;
        RiverNoiseResult r2Dn = r2D.generate(cX, cZ);
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double dBaseHeight = (noise2D.get(cX|x, cZ|z))*0.3D;
                double dh2 = 1-clamp10((0.1D+noiseM2.get(cX|x, cZ|z))*2.0D);
//                if (x+z==0) {
//                    System.out.println(dRiver);
//                }
//            if (x+z==0)
//            System.out.println(x+","+z+"/"+dBaseHeight);
                double dStr = 11.0D;
                double dN1Str = 2.0D;
                double dN2Str = 7.0D;
                double dN3Str = 4.0D;
                double dBlurred = (clamp10(Math.pow(1+r2Dn.getBlur(x, z, 8), 1.3)-1))*23;
                double dBlurred2 = r2Dn.getBlur(x, z, 2)*0.8D;//r2D.getBlur(x, z, 4);
                double df = wh*1.2D;
                for (int y = 0; y < wh; y++) {
                    int idx = y<<8|z<<4|x;
                    double dYH = clamp10(Math.pow(clamp10((y+130)/(double)df), 2.4));
                    df *=(1.0D-dh2*0.0002);
                    double dBase = dStr-dYH*dStr*2;
                    dBase += dBaseHeight*dStr*0.8D;
                    double dN = 0.0D;
                    double d = clamp10(noise.get(cX|x, y, cZ|z)*0.5D+0.5D);
                    double d4 = 1+(noise4.get(cX|x, y, cZ|z))*0.6D;
//                    double dM2 = clamp10(noiseM2.get(cX|x, y, cZ|z)*4.0D*d4);
                    double d2 = noise2.get(cX|x, y, cZ|z)*dN1Str;
                    if (d2 < 0)
                        d2*=0.7D;
                    double d3 = noise3.get(cX|x, y, cZ|z)*dN2Str;
                    if (d3 < 0)
                        d2*=0.7D;
                    dN = mix(d2, d3, d);
                    dN = mix(dN, dStr-dYH*dStr*2, dh2);
                    if (x+z==0){
//                        System.out.printf("%.2f - %.2f\n", d, dM2);
                    }
                    double dY = y < 80 ? 1 : clamp10(1-func(80, y, 22));
                    double riverY = 60-dBaseHeight*2;
                    double dRiverH = func2(riverY, y, 12);
                    riverY+=23;
                    double dRiverH2 = y>=riverY?1:func2(riverY, y, 23);
                    dBase+=dN*(1-dY);
                    dBase -= dRiverH*dBlurred2*dStr;
                    dBase -= dRiverH2*dBlurred*dStr;
                    dNoise[idx] = dBase;
                }   
            }    
        }
        
    }

    private void generateTerrain(Chunk c, short[] blocks) {
//        Random rand = new Random(this.seed);
        int wh = this.world.worldHeight;
        
        double[] dNoise = new double[16*16*wh]; 
        int cX = c.x<<4;
        int cZ = c.z<<4;
        boolean time=false;
        if (time) TimingHelper.start(1);
        RiverNoiseResult r2Dn = r2D.generate(cX, cZ);
        RiverNoiseResult r2Dn2 = r2D2.generate(cX, cZ);
        if (time) TimingHelper.end(1);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<4|x;
                if (time) TimingHelper.start(2);
                double dBaseHeight = (noise2D.get(cX|x, cZ|z))*3.3D;
                if (time) TimingHelper.end(2);
                if (time) TimingHelper.start(3);
                double dh2 = clamp10((noiseM2.get(cX|x, cZ|z))*1.3D);
                if (time) TimingHelper.end(3);
                if (time) TimingHelper.start(4);
                double dStr = 44.0D;
                double dBlurred = (clamp10(Math.pow(1+r2Dn.getBlur(x, z, 8), 1.3)-1))*23;
                double dBlurredA = (clamp10(Math.pow(1+r2Dn2.getBlur(x, z, 8), 1.3)-1))*23;
                double dBlurred2 = r2Dn.getBlur(x, z, 2)*0.8D;//r2D.getBlur(x, z, 4);
                double df = wh;
                if (time) TimingHelper.end(4);
                for (int y = 0; y < wh; y++) {
                    double dYH = clamp10((y)/(double)df);
                    if (dYH >= 0.5) {
                        double dz = (dYH-0.5D)*2.0D;
                        dz = 1-dz;
                        dz*=dz*(0.98D+dh2*0.04D);
                        dz = 1-dz;
                        dz /=4;
                        dYH+=dz;
//                        if (dYH < 0.5)
//                            dYH = 0.5;
                    }
                    double dBase = dStr-dYH*dStr*2.0;
                    double dN = noise2.get(cX|x, y, cZ|z)*2.4D;
                    dN*=24D*clamp10(func2(128, y, 66)*0.8);
                    dBase += dN;
                    dNoise[y<<8|xz] = dBase;
                }
                if (false)
                for (int y = 0; y < wh; y++) {
                    double dYH = clamp10((y)/(double)df);
                    if (dYH >= 0.5) {
                        double dz = (dYH-0.5D)*2.0D;
                        dz = 1-dz;
                        dz*=dz*(0.98D+dh2*0.04D);
                        dz = 1-dz;
                        dz /=4;
                        dYH+=dz;
//                        if (dYH < 0.5)
//                            dYH = 0.5;
                    }
                    if (x+z==0) {
//                        System.out.println(y+"/"+dYH);
                    }
                    
                    double dBase = dStr-dYH*dStr*2.0;
//                    dBase = dBase+dBaseHeight;
                    if (time) TimingHelper.start(5);
                    double dN = noise2.get(cX|x, y, cZ|z)*2.4D;
                    double dN7 = noise5.get(cX|x, y, cZ|z)*0.4D;
                    double dN1 = noise.get(cX|x, y, cZ|z)*3.0D;
                    double dN2 = noise3.get(cX|x, y, cZ|z)*3.0D;
                    double dN4 = (noise4.get(cX|x, y, cZ|z)*0.5+0.5)*7.0D;
                    if (time) TimingHelper.end(5);
//                    if (dN < 0) {
//                        dN*=0.01D;
//                    }
//                    if (dN7 < 0)
//                        dN7*=0.01D;
                    dN*=24D*clamp10(func2(128, y, 66)*0.8);
                    dBase += dN;
                    double riverY = 110 -dBaseHeight*2;
                    double dRiverH = func2(riverY, y, 12);
                    riverY+=23;
                    double dRiverH2 = y>=riverY?1:func2(riverY, y, 23); 
                    dBase -= dRiverH*dBlurred2*dStr;
                    dBase -= dRiverH2*dBlurred*dStr;
                    double dt = func2(44+dN1*4, y, 14+0.4*dN2-dN4);
                    dBase -= dt*dBlurredA*dStr*(1.0D+dN1*0.2D+0.2*dN2);
                    dBase += dN7*01.7*clamp10(func2(138, y, 22)*2.8);
                    dNoise[y<<8|xz] = dBase;
                }   
            }    
        }

//        if (dBase >= -0.2D) {
//            c.blocks[y<<8|z<<4|x] = (short) Block.stone.id;
//        } else if (y < 100) {
//            c.blocks[y<<8|z<<4|x] = (short) Block.water.id;
//        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = Block.grass.id;
                int earth = Block.dirt.id;
                int stone = Block.stones.granite.id;
                int a = -1;
                int xz=z<<4|x;
                int curBlock = 0;
                for (int y = this.world.worldHeight-1; y >= 0 ; y--) {
                    double d = dNoise[y<<8|xz];
                    if (d >= 0D) {
                        if (a < 0) {
                            curBlock = top;
                        } else if (a < 3) {
                            curBlock = earth;
                        } else {
                            curBlock = stone;
                        }
                        a++;
                    } else {
                        a = -1;
                        curBlock = 0;
                        if (y < 100) {
                            curBlock = Block.water.id;
                        }
                    }
                    blocks[y<<8|xz] = (short) curBlock;
                }
            }
        }
        
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
    public WorldGenInit getWorldGen(WorldServer world, long seed, WorldSettings settings) {
        WorldGenInit init = new WorldGenInit();
        init.generator = this;
        init.biomeManager = new EmptyBiomeManager(world, seed, settings);
//        init.populator = new ChunkPopulator(world, seed, settings);
        init.populator = new EmptyChunkPopulator(world, seed, settings);
        return init;
    }
}
