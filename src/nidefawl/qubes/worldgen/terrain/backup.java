//package nidefawl.qubes.worldgen.terrain.main;
//
//
//import java.util.ArrayList;
//import java.util.Random;
//
//import nidefawl.qubes.biome.Biome;
//import nidefawl.qubes.block.Block;
//import nidefawl.qubes.chunk.Chunk;
//import nidefawl.qubes.hex.HexCell;
//import nidefawl.qubes.noise.*;
//import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
//import nidefawl.qubes.perf.TimingHelper;
//import nidefawl.qubes.util.GameMath;
//import nidefawl.qubes.world.World;
//import nidefawl.qubes.world.WorldServer;
//import nidefawl.qubes.world.WorldSettings;
//import nidefawl.qubes.worldgen.biome.*;
//import nidefawl.qubes.worldgen.populator.ChunkPopulator;
//import nidefawl.qubes.worldgen.populator.IChunkPopulator;
//import nidefawl.qubes.worldgen.terrain.ITerrainGen;
//
//public class TerrainGeneratorMain implements ITerrainGen {
//    public final static String GENERATOR_NAME = "terrain_rivers";
//
//    private WorldServer world;
//    private long  seed;
//    private  TerrainNoiseScale noise3;
//    private  TerrainNoiseScale noise;
//    private  TerrainNoise2D noiseM2;
//    private  TerrainNoiseScale noise2;
//    private  TerrainNoiseScale noise4;
//    private  TerrainNoise2D noise2D;
//    private  RiverNoise2D r2D;
//    private  RiverNoise2D r2D2;
//    private TerrainNoiseScale noise5;
////    private TerrainNoiseScale nois234234e;
//    HexBiomesServer biomes; 
//    public TerrainGeneratorMain(WorldServer world, long seed, WorldSettings settings) {
//        this.world = world;
//        this.seed = seed;
//        Random rand = new Random(this.seed);
//
//        {
//
//            double scaleMixXZ = 12.80D;
//            double scaleMixY = scaleMixXZ*0.4D;
//            double scaleMix2XZ = 1.1D;
//            double scaleMix2Y = scaleMix2XZ;
//            double scaleT1XZ = 0.3D;
//            double scaleT1Y = scaleT1XZ*0.3D;
//            double scaleT2XZ = 4.3D;
//            double scaleT2Y = scaleT1XZ*0.3D;
//            double scaleT4XZ = 3.3D;
//            double scaleT4Y = scaleT4XZ*0.1D;
//            double scaleT5XZ = 3.6D;
//            double scaleT5Y = scaleT5XZ*3.4D;
//            this.noise = NoiseLib.newNoiseScale(rand.nextLong())
//                    .setUpsampleFactor(4)
//                    .setScale(scaleMixXZ, scaleMixY, scaleMixXZ)
//                    .setOctavesFreq(3, 2.0);
////            this.noiseM2 = new TerrainNoise2D(rand.nextLong())
////                    .setUpsampleFactor(4)
////                    .setScale(scaleMix2XZ, scaleMix2XZ)
////                    .setOctavesFreq(2);
//            this.noiseM2 = new TerrainNoise2D(rand.nextLong(), scaleMix2XZ, scaleMix2XZ, 2);
//            this.noise2 = NoiseLib.newNoiseScale(rand.nextLong())
//                    .setUpsampleFactor(4)
//                    .setScale(scaleT1XZ, scaleT1Y, scaleT1XZ)
//                    .setOctavesFreq(3, 1.97D + 0.01);
//            this.noise5 = NoiseLib.newNoiseScale(rand.nextLong())
//                    .setUpsampleFactor(4)
//                    .setScale(scaleT5XZ, scaleT5Y, scaleT5XZ)
//                    .setOctavesFreq(3, 2.59D);
//            this.noise3 = NoiseLib.newNoiseScale(rand.nextLong())
//                    .setUpsampleFactor(4)
//                    .setScale(scaleT2XZ, scaleT2Y, scaleT2XZ)
//                    .setOctavesFreq(3, 2D);
//            this.noise4 = NoiseLib.newNoiseScale(rand.nextLong())
//                    .setUpsampleFactor(4)
//                    .setScale(scaleT4XZ, scaleT4Y, scaleT4XZ)
//                    .setOctavesFreq(3, 2D);
//            
//            this.noise2D = new TerrainNoise2D(rand.nextLong(), 1, 1, 1);
//            double dRScale = 0.23D;
//            this.r2D = new RiverNoise2D(rand.nextLong(), dRScale, 8);
////             dRScale = 0.14D;
////            this.r2D2 = new RiverNoise2D(rand.nextLong(), dRScale, 8);
//             dRScale = 2.14D;
//            this.r2D2 = new RiverNoise2D(33, dRScale, 8);
//        }
//    }
//
//    @Override
//    public void init() {
//        biomes = (HexBiomesServer) world.biomeManager;
//    }
//
//    @Override
//    public Chunk generateChunk(int chunkX, int chunkZ) {
//        long rx = chunkX * 0x589638F52CL;
//        long rz = chunkZ * 0x3F94515BD5L;
//        Random rand = new Random(rx + rz);
//        int heightBits = world.worldHeightBits;
//        Chunk c = new Chunk(world, chunkX, chunkZ, heightBits);
//        HexBiome[] hexs = new HexBiome[Chunk.SIZE*Chunk.SIZE];
//        ArrayList<HexBiome> h = new ArrayList<>(); 
//        for (int x = 0; x < 16; x++) {
//            for (int z = 0; z < 16; z++) {
//                HexBiome hex = biomes.blockToHex(c.getBlockX()+x, c.getBlockZ()+z);
//                hexs[z<<Chunk.SIZE_BITS|x] = hex;
//                c.biomes[z<<Chunk.SIZE_BITS|x] = (byte) hex.biome.id;
//                if (!h.contains(hex)) {
//                    h.add(hex);
//                }
//            }
//        }
//        short[] blocks = c.getBlocks();
//        generateTerrain(c, blocks, hexs, h);
//        c.checkIsEmtpy();
//        return c;
//    }
//
//    private void generateTerrain(Chunk c, short[] blocks, HexBiome[] hexs, ArrayList<HexBiome> h) {
//        {
//
//            double scaleMixXZ = 3.80D;
//            double scaleMixY = scaleMixXZ*0.4D;
//            double scaleMix2XZ = 1.1D;
//            double scaleMix2Y = scaleMix2XZ;
//            double scaleT1XZ = 1D;
//            double scaleT1Y = scaleT1XZ*0.1D;
//            double scaleT2XZ = 4.3D;
//            double scaleT2Y = scaleT1XZ*0.3D;
//            double scaleT4XZ = 3.3D;
//            double scaleT4Y = scaleT4XZ*0.1D;
//            double scaleT5XZ = 1.5D;
//            double scaleT5Y = scaleT5XZ*1.33D;
//            this.noise.setScale(scaleMixXZ, scaleMixY, scaleMixXZ);
//            this.noise2.setScale(scaleT1XZ, scaleT1Y, scaleT1XZ);
//            this.noise5.setScale(scaleT5XZ, scaleT5Y, scaleT5XZ);
//            this.noise3.setScale(scaleT2XZ, scaleT2Y, scaleT2XZ);
//            this.noise4.setScale(scaleT4XZ, scaleT4Y, scaleT4XZ);
//        }
//        double[][] dn = generateNoise(c.getBlockX(), c.getBlockZ(), hexs);
//        double[] dNoise = dn[0];
//        double[] dNoise2 = dn[1];
//        for (int x = 0; x < 16; x++) {
//            for (int z = 0; z < 16; z++) {
//                Biome b = hexs[z<<Chunk.SIZE_BITS|x].biome;
//                int top = Block.grass.id;
//                int earth = Block.dirt.id;
//                int stone = b.getStone();
//                int a = -1;
//                int xz = z << 4 | x;
//                int curBlock = 0;
//                for (int y = this.world.worldHeight - 1; y >= 0; y--) {
//                    double d = dNoise[y << 8 | xz];
//                    double d2 = dNoise2[y << 8 | xz];
//                    if (d >= 0D) {
//                        curBlock = stone;
////                        if (d2<=0) {
//                            if (a < 0) {
//                                curBlock = top;
//                            } else if (a < 3) {
//                                curBlock = earth;
//                            }
////                        }
//                        a++;
//                    } else {
//                        a = -1;
//                        curBlock = 0;
////                        if (y < 60) {
////                            curBlock = Block.water.id;
////                        }
//                        if (d2 > 0.5 || y <=93) {
//                            curBlock = Block.water.id;
//                        }
//                    }
//                    blocks[y << 8 | xz] = (short) curBlock;
//                }
//            }
//        }
//    }
//
//    private double[][] generateNoise(int cX, int cZ, HexBiome[] hexs) {
////        Random rand = new Random(this.seed);
//        
//        
//        int wh = this.world.worldHeight;
//        
//        double[] dNoise = new double[16*16*wh]; 
//        double[] dNoise2 = new double[16*16*wh];
//        boolean time=false;
//        if (time) TimingHelper.start(1);
//        RiverNoiseResult r2Dn = r2D.generate(cX, cZ);
//        RiverNoiseResult r2Dn2 = r2D2.generate(cX, cZ);
//        if (time) TimingHelper.end(1);
//        double[] dNoise2_ = noise2.gen(cX, cZ);
//        double[] dnoise5_ = noise5.gen(cX, cZ);
//        double[] dnoise_ = noise.gen(cX, cZ);
//        double[] dnoise3_ = noise3.gen(cX, cZ);
//        double[] dnoise4_ = noise4.gen(cX, cZ);
////        double innerRadiusScale = 0.9;
//        double smoothScale = 0.9;
//        double gridRadius = biomes.hwidth;
//        for (int x = 0; x < 16; x++) {
//            for (int z = 0; z < 16; z++) {
//                 HexCell hex = hexs[z<<Chunk.SIZE_BITS|x];
//                double hx = hex.getCenterX();
//                double hz = hex.getCenterY();
//                double distHex = GameMath.dist2d(hx, hz, cX+x, cZ+z);
//                double outerDist2 = Math.min(Math.max(distHex, 0)/(gridRadius), 1);
//                double outerDist = Math.max(distHex-gridRadius*smoothScale, 0);
//                double outerScale = outerDist/(gridRadius*(1-smoothScale));
//                outerScale = Math.min(outerScale, 1);
//                double coreScale = 1-Math.min(Math.max(distHex-gridRadius*0.7, 0)/(gridRadius*0.2), 1);
//                double flattenScale = 1-Math.min(Math.max(distHex-gridRadius*0.6, 0)/(gridRadius*0.6), 1);
//                int xz=z<<4|x;
//                if (time) TimingHelper.start(2);
//                double dBaseHeight = (noise2D.get(cX|x, cZ|z))*1.3D;
//                if (time) TimingHelper.end(2);
//                if (time) TimingHelper.start(3);
//                double dh2 = noiseM2.get(cX|x, cZ|z);
//                double xd = 0.9D;
//                dh2 = 1-clamp10((1-clamp10((dh2-(1-xd))/xd))*0.7);
//                if (time) TimingHelper.end(3);
//                if (time) TimingHelper.start(4);
//                double dStr = 12.0D;
//                double dStr2 = 12.0D*2;
//                double dBlurred = (clamp10(Math.pow(1+r2Dn.getBlur(x, z, 4), 2)-1))*4;
//                double dBlurredA = (clamp10(Math.pow(1+r2Dn2.getBlur(x, z, 8), 0.8)-1))*4;
//                double dBlurred2 = r2Dn.getBlur(x, z, 4)*0.8;//r2D.getBlur(x, z, 4);
//                double df = wh;
//                if (time) TimingHelper.end(4);
//                int idx_xz = (z * 16 + x) * (256);
////                double hexScale1 = 1-(Math.max((distHex-gridRadius*0.9), 0)/(gridRadius*0.1));
////                if (outerScale != 0)
////                System.out.println(outerScale);
//                int topHeight = 116;
//                for (int y = 0; y < wh; y++) {
//                    double dYH = clamp10((y+0+5*flattenScale)/(double)df);
//                    double dYH2 = clamp10((y+0+5)/(double)df);
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
////                    dYH+=func2(144, y, 15)*0.2;
////                    dYH+=func2(104, y, 25)*0.08;
////                    dYH+=func2(114, y, 25)*0.08;
////                    dYH-=func2(92, y, 12)*0.08;
////                    dYH-=func2(149, y, 12)*0.3;
////                    dYH-=func2(157, y, 12)*0.7;
////                    dYH+=func2(104, y, 12)*12.7;
//                    if (x+z==0) {
////                        System.out.println(y+"/"+dYH);
//                    }
//                    
//                    double dBase = dStr-dYH*dStr*2.0;
//                    double dBase2 = dStr2-dYH2*dStr2*2.0;
//                    dBase = dBase+dBaseHeight;
////                    if (time) TimingHelper.start(5);
//                    int idx = idx_xz+y;
//                    double dN = dNoise2_[idx]-1;
//                    double dN7 = dnoise5_[idx];
//                    double dN1 = dnoise_[idx]*3.0D;
//                    double dN2 = dnoise3_[idx]*3.0D;
//                    double dN4 = (dnoise4_[idx]*0.5+0.5)*7.0D;
//                    if (time) TimingHelper.end(5);
//                    if (dN < 0) {
////                        dN*=0.01D;
//                    }
////                    if (dN7 < 0)
////                        dN7*=0.01D;
////                    dN*=34D*clamp10(func2(128, y, 85)*0.8);
//                    dBase += dN*3.7*dh2*coreScale;;
//                    dBase += dN7*5.7*dh2*coreScale;;
//                    double riverY = 100;
//                    double dRiverH = func2(riverY -dBaseHeight*2, y, 12);
//                    double dRiverH3 = func2(riverY -dBaseHeight*2, y, 12);
//                    double riverY2 = riverY+12;
//                    double dRiverH2 = y>=riverY2?1:func2(riverY2, y, 23); 
//                    double xr = 0;
//                    xr+=dRiverH*dBlurred2*dStr;
//                    xr+=dRiverH2*dBlurred*dStr*coreScale;
//                    double dt = func2(114+dN1*2, y, 7)*0.3;
//                    double dt2 = func2(44+dN1*4, y, 12+0.4*dN2-dN4);
//                    double cavestr = dt*dStr*dBlurredA*(0.5D+dN1*0.2D+0.2*dN2);
//                    xr+=cavestr*36;
//                    double xx=func2(topHeight-16, y, 12);
////                    xx+=func2(topHeight-06, y, 6);
////                    xx+=func2(topHeight-3, y, 4);
//                    double dEndH = xx*func2(outerDist2*100, 88, 20)*(dBlurred2+dBlurred);
//                    dBase2-=dRiverH3*dBlurred2*dStr*4;
//                    dBase2-=dRiverH3*dBlurred*dStr*4;
////                    dBase2-=cavestr*12;
//                    dBase -= xr*coreScale;
//                    dBase = mix(dBase, dBase2, outerScale+dYH*outerScale+dYH*0.4*(1-flattenScale));
//                    dBase+=dEndH*20;
////                    dBase = mix
//                    dNoise[y<<8|xz] = dBase;
////                    dNoise2[y<<8|xz] = y<riverY-4?xr:0;
//                    dNoise2[y<<8|xz] = 0;
////                    dNoise2[y<<8|xz] = dt2*dStr*dBlurredA*(1.0D+dN1*0.2D+0.2*dN2)*55;
//                }   
//            }    
//        }
//
////        if (dBase >= -0.2D) {
////            c.blocks[y<<8|z<<4|x] = (short) Block.stone.id;
////        } else if (y < 100) {
////            c.blocks[y<<8|z<<4|x] = (short) Block.water.id;
////        }
//
//        return new double[][] {dNoise, dNoise2 };
//        
//    }
//    private double func2(double m, double n, double j) {
//        double x = Math.abs(m - n) / (j);
//        if (x > 1)
//            x = 1;
//        if (x < 0)
//            x = 0;
//        x *= x;
//        x = 1 - x;
//        x *= x;
//        x = 1 - x;
//        if (x > 1)
//            x = 1;
//        if (x < 0)
//            x = 0;
//        return 1-x;
//    }
//
//    public static double mix(double a, double b, double w) {
//        if (w >= 1) {
//            a = b;
//        } else if (w > 0) {
//            a = a + (b - a) * w;
//        }
//        return a;
//    }
//
//    public static double clamp10(double a) {
//        return a > 1 ? 1 : a < 0 ? 0 : a;
//    }
//
//    public static double func(double m, double n, double j) {
//        double x = Math.abs(m - n) / j;
//        x += 1.1D;
//        x *= x;
//        x -= 1.1D;
//        if (x > 1)
//            x = 1;
//        if (x < 0)
//            x = 0;
//        return x;
//    }
//    public int distSq(int x, int y, int z, int x2, int y2, int z2) {
//        x = x-x2;
//        z = z-z2;
//        return x*x+z+z;
//    }
//
//    @Override
//    public Class<? extends IChunkPopulator> getPopulator() {
//        return ChunkPopulator.class;
//    }
//
//    @Override
//    public Class<? extends IBiomeManager> getBiomeManager() {
//        return HexBiomesServer.class;
//    }
//}
