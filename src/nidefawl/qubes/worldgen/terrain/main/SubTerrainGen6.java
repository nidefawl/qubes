package nidefawl.qubes.worldgen.terrain.main;

import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.clamp10;
import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.func2;
import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.smoothScale;

import java.util.Random;

import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.biomes.HexBiome;

public class SubTerrainGen6 extends SubTerrainGen {
    /**
     * @author Michael Hept 2015
     * Copyright: Michael Hept
     */
    public class NoiseData extends SubTerrainGen.SubTerrainData {

        public RiverNoiseResult r2Dn;
        public RiverNoiseResult r2Dn2;
        public double[] dNoise2_;
        public double[] dnoise5_;
        public double[] dnoise_;
        public double[] dnoise3_;
        public TerrainNoiseMap2DResult dnoise2da;

    }
    private  TerrainNoiseScale noise3;
    private  TerrainNoiseScale noise;
    private  TerrainNoise2D noiseM2;
    private  TerrainNoiseScale noise2;
    private  TerrainNoise2D noise2D;
    private  RiverNoise2D r2D;
    private  RiverNoise2D r2D2;
    private TerrainNoiseScale noise5;
    private TerrainGeneratorMain main;
    private TerrainNoiseMap2D test2d;
    public SubTerrainGen6(TerrainGeneratorMain t) {
        this.main = t;
        Random rand = new Random(t.seed);

        {

            double scaleMixXZ = 1.80D;
            double scaleMixY = scaleMixXZ*0.4D;
            double scaleT1XZ = 2D;
            double scaleT1Y = scaleT1XZ*0.1D;
            double scaleT2XZ = 1.3D;
            double scaleT2Y = scaleT1XZ*0.3D;
            double scaleT5XZ = 1.5D;
            double scaleT5Y = scaleT5XZ*1.33D;
            double scaleMix2XZ = 1.3D;
            this.noise = NoiseLib.newNoiseScale(rand.nextLong())
                    .setUpsampleFactor(4)
                    .setScale(scaleMixXZ, scaleMixY, scaleMixXZ)
                    .setOctavesFreq(3, 2.0);
            this.noiseM2 = new TerrainNoise2D(rand.nextLong(), scaleMix2XZ, scaleMix2XZ, 4);
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
            double n2dscale=1.3;
            this.noise2D = new TerrainNoise2D(rand.nextLong(), n2dscale, n2dscale, 4);
            double dRScale = 0.23D;
//            this.r2D = new RiverNoise2D(rand.nextLong(), dRScale, 8);
             dRScale = 2.14D;
//            this.r2D2 = new RiverNoise2D(33, dRScale, 8);
        }
    }
    public SubTerrainData prepare(int cX, int cZ, HexBiome hex) {


        test2d = new TerrainNoiseMap2D("textures/brushes/mountainRange/OJYl2Po.png", 1.0D/150.0D, 8);
        NoiseData data = new NoiseData();
//        data.r2Dn = r2D.generate(cX, cZ);
//        data.r2Dn2 = r2D2.generate(cX, cZ);
        double scaleMixXZ = 1.80D;
        double scaleMixY = scaleMixXZ*0.4D;
        double scaleT1XZ = 1.3D;
        double scaleT1Y = scaleT1XZ*0.1D;
        double scaleT2XZ = 1.3D;
        double scaleT2Y = scaleT1XZ*0.3D;
        double scaleT5XZ = 1.5D;
        double scaleT5Y = scaleT5XZ*1.33D;
        double scaleMix2XZ = 1.3D;
        noise2.setScale(scaleT1XZ, scaleT1Y, scaleT1XZ);
        data.dNoise2_ = noise2.gen(cX, cZ);
        data.dnoise5_ = noise5.gen(cX, cZ);
        data.dnoise_ = noise.gen(cX, cZ);
        data.dnoise3_ = noise3.gen(cX, cZ);
        double hx = hex.getCenterX();
        double hz = hex.getCenterY();
        data.dnoise2da = test2d.generate(cX, cZ, hx, hz);
        return data;
    }
    public double generate(int cX, int cZ, int x, int y1, int y2, int z, HexBiome hex, SubTerrainData data, double[] d, double[] d2) {
        
        NoiseData noise = (NoiseData) data;
        

        double gridRadius = this.main.biomes.hwidth*0.98;
        int wh = this.main.world.worldHeight;
        double hx = hex.getCenterX();
        double hz = hex.getCenterY();
        double distHex = GameMath.dist2d(hx, hz, cX+x, cZ+z);
        double outerDist = Math.max(distHex-gridRadius*smoothScale, 0);
        double outerScale = outerDist/(gridRadius*(1-smoothScale));
        outerScale = Math.min(outerScale, 1);
        double coreScale = Math.min(Math.max(gridRadius*0.85-distHex, 0)/(gridRadius*0.12), 1);
        double flattenScale = 1-Math.min(Math.max(distHex-gridRadius*0.8, 0)/(gridRadius*0.4), 1);
        double dBaseHeight = noise.dnoise2da.getBlur(x, z, 4);
        double dBaseHeight2 = noise.dnoise2da.getBlur(x, z, 8);
        dBaseHeight = Math.pow((dBaseHeight)*1,1);
        dBaseHeight = Math.min(1, dBaseHeight);
        dBaseHeight = Math.max(0, dBaseHeight);
        
        dBaseHeight = 2.0*dBaseHeight-1.0;
        dBaseHeight2 = 2.0*dBaseHeight2-1.0;
        
//        if (dBaseHeight < 0) {
//            dBaseHeight*=0.44;
//        } else {
//            dBaseHeight*=1.12f;
//        }
//      dBaseHeight*=Math.abs(dBaseHeight);

        double xd = 0.9D;
        double dStr = 220.0D;
        double df = wh;
        int idx_xz = (z * 16 + x) * (256);

        for (int y = y1; y < y2; y++) {
            double dYH = clamp10((y) / (double) df);
            dYH = clamp10((y-2*flattenScale) / ((double) df-2*flattenScale));
            double dyoff = 0.998D;
            double dz = 0;
            if (dYH >= dyoff) {
//                dz = (dYH - dyoff)/(1.0-dyoff);
//                dz*=dz;
//                dYH+=dz*0.4D;
            }
            int idx = idx_xz + y;
            double dBase = dStr - dYH * dStr * 2.0;

            double dN = noise.dNoise2_[idx] * 5.0D;
            double dN7 = noise.dnoise5_[idx] * 5.0D;
            double dN1 = noise.dnoise_[idx] * 3.0D;
            double dN2 = noise.dnoise3_[idx] * 5.0D;
            if (dN7>0) {
                dN2*=dN7;
            } else {
                dN7*=-0.1;
                dN2+=dN7;
            }
//            dBase += dN * 1.7 * dh2 * coreScale;
//            double f = clamp10(1+mix(1-func2(128, y, 12)*4, 1, 1-coreScale));
            double dnnn=dN1*80*(clamp10(dYH-0.4));
            double mtn=dBaseHeight*(1)*dStr*coreScale*0.9;
            double mtn2=dBaseHeight2*(1)*dStr*coreScale*2.9;
            if (mtn < 0)
                mtn*=0.5;
            if (mtn2 < 0)
                mtn2*=0.5;
//            mtn*=clamp10(dYH+0.4)*clamp10(dYH+0.4);
            dBase -= Math.abs(mtn)*0.2*clamp10(1.2*func2(y+dN2, 150, 12));
            dBase += Math.abs(mtn)*0.55*clamp10(0.9*func2(y+dN2, 170, 12));
            dBase += Math.abs(mtn)*0.65*clamp10(0.9*func2(y+dN1, 140, 6));
            dBase += Math.abs(mtn)*0.35*clamp10(0.9*func2(y+dN7, 130, 12));
            dBase+= Math.abs(mtn2)*3.75*clamp10(0.9*func2(y+dN2, 60, 32)*dN);
//            dBase+=(Math.max(dN, 0)+Math.max(dN1, 0)+Math.max(dN7, 0))*dStr*Math.max(0, (coreScale*0.3)-(dYH*dYH*dYH*dYH)*0.55);
            int iy = 70;
//            dBase+=func2(iy+14 - dBaseHeight * 2, y, 33)*dN1*clamp10(dN*dN7)*33;
//            dBase+=func2(iy+9 - dBaseHeight * 2, y, 12)*dN1*12;
//            dBase+=func2(iy+4 - dBaseHeight * 2, y, 3)*dN1*2;
//            dBase+=func2(iy - dBaseHeight * 2, y, 1)* mtn*1;
//            dBase+=func2(iy+20 * 2, y, 1)* (dBaseHeight2)*4;
//            System.out.println(dBaseHeight);
//          //                        d-=1D;
//            
            dBase = dBase  + mtn ;
            d[y] = dBase-4000*(dz);
        }
        return 1;
    }

}
