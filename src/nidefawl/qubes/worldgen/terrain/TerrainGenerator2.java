package nidefawl.qubes.worldgen.terrain;


import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.world.World;

public class TerrainGenerator2 implements ITerrainGen {

    private World world;
    private long  seed;
    private  TerrainNoiseScale noise3;
    private  TerrainNoiseScale noise;
    private  TerrainNoise2D noiseM2;
    private  TerrainNoiseScale noise2;
    private  TerrainNoiseScale noise4;
    private  TerrainNoise2D noise2D;
    private  RiverNoise2D r2D;
    private  RiverNoise2D r2D2;
    private TerrainNoiseScale noise5;
//    private TerrainNoiseScale nois234234e;

    public TerrainGenerator2(World world, long seed) {
        this.world = world;
        this.seed = seed;

        Random rand = new Random(this.seed);

        {

            double scaleMixXZ = 4.80D;
            double scaleMixY = scaleMixXZ*0.4D;
            double scaleMix2XZ = 1.1D;
            double scaleMix2Y = scaleMix2XZ;
            double scaleT1XZ = 0.3D;
            double scaleT1Y = scaleT1XZ*0.3D;
            double scaleT2XZ = 4.3D;
            double scaleT2Y = scaleT1XZ*0.3D;
            double scaleT4XZ = 3.3D;
            double scaleT4Y = scaleT4XZ*0.1D;
            double scaleT5XZ = 3.6D;
            double scaleT5Y = scaleT5XZ*3.4D;
            this.noise = NoiseLib.newNoiseScale(rand.nextLong())
                    .setUpsampleFactor(4)
                    .setScale(scaleMixXZ, scaleMixY, scaleMixXZ)
                    .setOctavesFreq(3, 2.0);
//            this.noiseM2 = new TerrainNoise2D(rand.nextLong())
//                    .setUpsampleFactor(4)
//                    .setScale(scaleMix2XZ, scaleMix2XZ)
//                    .setOctavesFreq(2);
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
            this.noise4 = NoiseLib.newNoiseScale(rand.nextLong())
                    .setUpsampleFactor(4)
                    .setScale(scaleT4XZ, scaleT4Y, scaleT4XZ)
                    .setOctavesFreq(3, 2D);
            
            this.noise2D = new TerrainNoise2D(rand.nextLong(), 1, 1, 1);
            double dRScale = 0.23D;
            this.r2D = new RiverNoise2D(rand.nextLong(), dRScale, 8);
//             dRScale = 0.14D;
//            this.r2D2 = new RiverNoise2D(rand.nextLong(), dRScale, 8);
             dRScale = 2.14D;
            this.r2D2 = new RiverNoise2D(33, dRScale, 8);
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
        generateTerrain(c, blocks);
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk c, short[] blocks) {
        double[][] dn = generateNoise(c.getBlockX(), c.getBlockZ());
        double[] dNoise = dn[0];
        double[] dNoise2 = dn[1];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = Block.grass.id;
                int earth = Block.dirt.id;
                int stone = Block.stone.id;
                int a = -1;
                int xz = z << 4 | x;
                int curBlock = 0;
                for (int y = this.world.worldHeight - 1; y >= 0; y--) {
                    double d = dNoise[y << 8 | xz];
                    double d2 = dNoise2[y << 8 | xz];
                    if (d >= 0D) {
                        curBlock = stone;
                        if (d2<=0) {
                            if (a < 0) {
                                curBlock = top;
                            } else if (a < 3) {
                                curBlock = earth;
                            }
                        }
                        a++;
                    } else {
                        a = -1;
                        curBlock = 0;
//                        if (y < 60) {
//                            curBlock = Block.water.id;
//                        }
                        if (d2 > 0.5 || y <=93) {
//                            curBlock = Block.water.id;
                        }
                    }
                    blocks[y << 8 | xz] = (short) curBlock;
                }
            }
        }
    }

    private double[][] generateNoise(int cX, int cZ) {
//        Random rand = new Random(this.seed);
        int wh = this.world.worldHeight;
        
        double[] dNoise = new double[16*16*wh]; 
        double[] dNoise2 = new double[16*16*wh];
        boolean time=false;
        if (time) TimingHelper.start(1);
        RiverNoiseResult r2Dn = r2D.generate(cX, cZ);
        RiverNoiseResult r2Dn2 = r2D2.generate(cX, cZ);
        if (time) TimingHelper.end(1);
        double[] dNoise2_ = noise2.gen(cX, cZ);
        double[] dnoise5_ = noise5.gen(cX, cZ);
        double[] dnoise_ = noise.gen(cX, cZ);
        double[] dnoise3_ = noise3.gen(cX, cZ);
        double[] dnoise4_ = noise4.gen(cX, cZ);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xz=z<<4|x;
                if (time) TimingHelper.start(2);
                double dBaseHeight = (noise2D.get(cX|x, cZ|z))*1.3D;
                if (time) TimingHelper.end(2);
                if (time) TimingHelper.start(3);
                double dh2 = noiseM2.get(cX|x, cZ|z);
                double xd = 0.9D;
                dh2 = 1-clamp10((1-clamp10((dh2-(1-xd))/xd))*0.7);
                if (time) TimingHelper.end(3);
                if (time) TimingHelper.start(4);
                double dStr = 12.0D;
                double dBlurred = (clamp10(Math.pow(1+r2Dn.getBlur(x, z, 8), 1.3)-1))*44;
                double dBlurredA = (clamp10(Math.pow(1+r2Dn2.getBlur(x, z, 8), 1.3)-1))*23;
                double dBlurred2 = r2Dn.getBlur(x, z, 2)*1.8D;//r2D.getBlur(x, z, 4);
                double df = wh;
                if (time) TimingHelper.end(4);
                int idx_xz = (z * 16 + x) * (256);
                for (int y = 0; y < wh; y++) {
                    double dYH = clamp10((y)/(double)df);
                    double dyoff = 0.8D;
                    if (dYH >= dyoff) {
                        double dz = (dYH-dyoff)*2.0D;
                        dz = 1-dz;
                        dz*=dz*(0.23D);
                        dz = 1-dz;
                        dz /=4;
                        dYH+=dz;
//                        if (dYH < 0.5)
//                            dYH = 0.5;
                    }
                    dYH-=func2(144, y, 15)*0.02;
                    dYH+=func2(104, y, 25)*0.08;
//                    dYH-=func2(92, y, 12)*0.08;
//                    dYH-=func2(149, y, 12)*0.3;
//                    dYH-=func2(157, y, 12)*0.7;
//                    dYH+=func2(104, y, 12)*12.7;
                    if (x+z==0) {
//                        System.out.println(y+"/"+dYH);
                    }
                    
                    double dBase = dStr-dYH*dStr*2.0;
                    dBase = dBase+dBaseHeight;
                    if (time) TimingHelper.start(5);
                    int idx = idx_xz+y;
                    double dN = dNoise2_[idx];
                    double dN7 = dnoise5_[idx];
                    double dN1 = dnoise_[idx]*3.0D;
                    double dN2 = dnoise3_[idx]*3.0D;
                    double dN4 = (dnoise4_[idx]*0.5+0.5)*7.0D;
                    if (time) TimingHelper.end(5);
                    if (dN < 0) {
//                        dN*=0.01D;
                    }
//                    if (dN7 < 0)
//                        dN7*=0.01D;
                    dN*=34D*clamp10(func2(128, y, 55)*0.8);
                    dBase += dN*dh2;
                    double riverY = 98;
                    double dRiverH = func2(riverY -dBaseHeight*2, y, 12);
                    double riverY2 = riverY+22;
                    double dRiverH2 = y>=riverY2?1:func2(riverY2, y, 23); 
                    double xr = 0;
//                    xr+=dRiverH*dBlurred2*dStr;
//                    xr+=dRiverH2*dBlurred*dStr;
                    double dt = func2(44+dN1*4, y, 9+0.4*dN2-dN4);
                    double dt2 = func2(44+dN1*4, y, 12+0.4*dN2-dN4);
                    double cavestr = dt*dStr*dBlurredA*(1.0D+dN1*0.2D+0.2*dN2);
                  xr+=cavestr*36;

                    dBase -= xr;
                    dBase += dN7*1.7*dh2;
                    dNoise[y<<8|xz] = dBase;
                    dNoise2[y<<8|xz] = y<riverY-4?xr:0;
                    dNoise2[y<<8|xz] = dt2*dStr*dBlurredA*(1.0D+dN1*0.2D+0.2*dN2)*55;
                }   
            }    
        }

//        if (dBase >= -0.2D) {
//            c.blocks[y<<8|z<<4|x] = (short) Block.stone.id;
//        } else if (y < 100) {
//            c.blocks[y<<8|z<<4|x] = (short) Block.water.id;
//        }

        return new double[][] {dNoise, dNoise2 };
        
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
}
