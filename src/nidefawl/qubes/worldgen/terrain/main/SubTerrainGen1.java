/**
 * 
 */
package nidefawl.qubes.worldgen.terrain.main;

import java.util.Random;

import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.*;
import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.worldgen.biome.HexBiome;
import nidefawl.qubes.worldgen.terrain.main.SubTerrainGen.SubTerrainData;
import nidefawl.qubes.worldgen.terrain.main.SubTerrainGen1.NoiseData;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SubTerrainGen1 extends SubTerrainGen {
    /**
     * @author Michael Hept 2015
     * Copyright: Michael Hept
     */
    public class NoiseData extends SubTerrainGen.SubTerrainData {

        public double[] dNoise;
        public double[] dNoise2;
        public RiverNoiseResult r2Dn;
        public RiverNoiseResult r2Dn2;
        public double[] dNoise2_;
        public double[] dnoise5_;
        public double[] dnoise_;
        public double[] dnoise3_;
        public double[] dnoise4_;

    }
    private  TerrainNoiseScale noise3;
    private  TerrainNoiseScale noise;
    private  TerrainNoise2D noiseM2;
    private  TerrainNoiseScale noise2;
    private  TerrainNoiseScale noise4;
    private  TerrainNoise2D noise2D;
    private  RiverNoise2D r2D;
    private  RiverNoise2D r2D2;
    private TerrainNoiseScale noise5;
    private TerrainGeneratorMain main;
    public SubTerrainGen1(TerrainGeneratorMain t) {
        this.main = t;
        Random rand = new Random(t.seed);

        {

            double scaleMixXZ = 12.80D;
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
    public SubTerrainData prepare(int cX, int cZ) {


        double scaleMixXZ = 3.80D;
        double scaleMixY = scaleMixXZ*0.4D;
        double scaleMix2XZ = 1.1D;
        double scaleMix2Y = scaleMix2XZ;
        double scaleT1XZ = 1D;
        double scaleT1Y = scaleT1XZ*0.1D;
        double scaleT2XZ = 4.3D;
        double scaleT2Y = scaleT1XZ*0.3D;
        double scaleT4XZ = 3.3D;
        double scaleT4Y = scaleT4XZ*0.1D;
        double scaleT5XZ = 1.5D;
        double scaleT5Y = scaleT5XZ*1.33D;
        this.noise.setScale(scaleMixXZ, scaleMixY, scaleMixXZ);
        this.noise2.setScale(scaleT1XZ, scaleT1Y, scaleT1XZ);
        this.noise5.setScale(scaleT5XZ, scaleT5Y, scaleT5XZ);
        this.noise3.setScale(scaleT2XZ, scaleT2Y, scaleT2XZ);
        this.noise4.setScale(scaleT4XZ, scaleT4Y, scaleT4XZ);
        int wh = this.main.world.worldHeight;
        NoiseData data = new NoiseData();
        data.dNoise = new double[16*16*wh]; 
        data.dNoise2 = new double[16*16*wh];
        data.r2Dn = r2D.generate(cX, cZ);
        data.r2Dn2 = r2D2.generate(cX, cZ);
        data.dNoise2_ = noise2.gen(cX, cZ);
        data.dnoise5_ = noise5.gen(cX, cZ);
        data.dnoise_ = noise.gen(cX, cZ);
        data.dnoise3_ = noise3.gen(cX, cZ);
        data.dnoise4_ = noise4.gen(cX, cZ);
        return data;
    }
    public double generate(int cX, int cZ, int x, int y, int z, HexBiome hex, SubTerrainData data) {
        
        NoiseData noise = (NoiseData) data;
        

        double gridRadius = this.main.biomes.hwidth;
        int wh = this.main.world.worldHeight;
        double hx = hex.getCenterX();
        double hz = hex.getCenterY();
        double distHex = GameMath.dist2d(hx, hz, cX+x, cZ+z);
        double outerDist2 = Math.min(Math.max(distHex, 0)/(gridRadius), 1);
        double outerDist = Math.max(distHex-gridRadius*smoothScale, 0);
        double outerScale = outerDist/(gridRadius*(1-smoothScale));
        outerScale = Math.min(outerScale, 1);
        double coreScale = 1-Math.min(Math.max(distHex-gridRadius*0.7, 0)/(gridRadius*0.2), 1);
        double flattenScale = 1-Math.min(Math.max(distHex-gridRadius*0.6, 0)/(gridRadius*0.6), 1);
        int xz=z<<4|x;
        double dBaseHeight = (noise2D.get(cX|x, cZ|z))*1.3D;
        double dh2 = noiseM2.get(cX|x, cZ|z);
        double xd = 0.9D;
        dh2 = 1-clamp10((1-clamp10((dh2-(1-xd))/xd))*0.7);
        double dStr = 12.0D;
        double dStr2 = 12.0D*2;
        double dBlurred = (clamp10(Math.pow(1+noise.r2Dn.getBlur(x, z, 4), 2)-1))*4;
        double dBlurredA = (clamp10(Math.pow(1+noise.r2Dn2.getBlur(x, z, 8), 0.8)-1))*4;
        double dBlurred2 = noise.r2Dn.getBlur(x, z, 4)*0.8;//r2D.getBlur(x, z, 4);
        double df = wh;
        int idx_xz = (z * 16 + x) * (256);
//        double hexScale1 = 1-(Math.max((distHex-gridRadius*0.9), 0)/(gridRadius*0.1));
//        if (outerScale != 0)
//        System.out.println(outerScale);
        int topHeight = 116;
        
            double dYH = clamp10((y+0+5*flattenScale)/(double)df);
            double dYH2 = clamp10((y+0+5)/(double)df);
            double dyoff = 0.8D;
            if (dYH >= dyoff) {
                double dz = (dYH-dyoff)*2.0D;
                dz = 1-dz;
                dz*=dz*(0.23D);
                dz = 1-dz;
                dz /=2;
                dYH+=dz;
//                if (dYH < 0.5)
//                    dYH = 0.5;
            }
//            dYH+=func2(144, y, 15)*0.2;
//            dYH+=func2(104, y, 25)*0.08;
//            dYH+=func2(114, y, 25)*0.08;
//            dYH-=func2(92, y, 12)*0.08;
//            dYH-=func2(149, y, 12)*0.3;
//            dYH-=func2(157, y, 12)*0.7;
//            dYH+=func2(104, y, 12)*12.7;
            if (x+z==0) {
//                System.out.println(y+"/"+dYH);
            }
            
            double dBase = dStr-dYH*dStr*2.0;
            dBase = dBase+dBaseHeight;
//            if (time) TimingHelper.start(5);
            int idx = idx_xz+y;
            double dN = noise.dNoise2_[idx]-1;
            double dN7 = noise.dnoise5_[idx];
            double dN1 = noise.dnoise_[idx]*3.0D;
            double dN2 = noise.dnoise3_[idx]*3.0D;
            if (dN < 0) {
//                dN*=0.01D;
            }
//            if (dN7 < 0)
//                dN7*=0.01D;
//            dN*=34D*clamp10(func2(128, y, 85)*0.8);
            dBase += dN*3.7*dh2*coreScale;;
            dBase += dN7*5.7*dh2*coreScale;;
            double riverY = 100;
            double dRiverH = func2(riverY -dBaseHeight*2, y, 12);
            double riverY2 = riverY+12;
            double dRiverH2 = y>=riverY2?1:func2(riverY2, y, 23); 
            double xr = 0;
            xr+=dRiverH*dBlurred2*dStr;
            xr+=dRiverH2*dBlurred*dStr*coreScale;
            double dt = func2(114+dN1*2, y, 7)*0.3;
            double cavestr = dt*dStr*dBlurredA*(0.5D+dN1*0.2D+0.2*dN2);
            xr+=cavestr*36;
            dBase -= xr*coreScale;
        return dBase;
    }

}