package nidefawl.qubes.worldgen.terrain.main;

import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.clamp10;
import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.func2;
import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.smoothScale;

import java.util.Random;

import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.worldgen.terrain.main.SubTerrainGen.SubTerrainData;
import nidefawl.qubes.worldgen.terrain.main.SubTerrainGen4.NoiseData;

public class SubTerrainGen7 extends SubTerrainGen {
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
    public SubTerrainGen7(TerrainGeneratorMain t) {
        this.main = t;
        Random rand = new Random(t.seed);

        {
            double scaleMixXZ = 1.80D;
            double scaleMixY = scaleMixXZ*0.4D;
            double scaleT1XZ = 1.3D;
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

        double f = 0.8D;
        double harm=1.6D;
        double scaleMixXZ = f;
        double scaleMixY = scaleMixXZ*2.4D;
        double scaleT1XZ = f*harm*1;
        double scaleT1Y = scaleT1XZ*2.1D;
        double scaleT2XZ = f*harm*2;
        double scaleT2Y = scaleT1XZ*2.3D;
        double scaleT5XZ = f*harm*4;
        double scaleT5Y = scaleT5XZ*1.33D;
        double scaleMix2XZ = 1.3D;
        noise .setScale(scaleMixXZ, scaleMixY, scaleMixXZ);
        noise2.setScale(scaleT1XZ, scaleT1Y, scaleT1XZ);
        noise3 .setScale(scaleT2XZ, scaleT2Y, scaleT2XZ);
        noise5 .setScale(scaleT5XZ, scaleT5Y, scaleT5XZ);

        test2d = new TerrainNoiseMap2D("textures/brushes/mountainRange/mountainRange_4.png", 1.0D/190.0D, 8);
        NoiseData data = new NoiseData();
//        data.r2Dn = r2D.generate(cX, cZ);
//        data.r2Dn2 = r2D2.generate(cX, cZ);
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
        double dBaseHeight = noise.dnoise2da.getBlur(x, z, 2)+0.5;
        double dBaseHeight2 = noise.dnoise2da.getBlur(x, z, 8);
//        dBaseHeight = Math.pow((dBaseHeight)*1,1);
//        dBaseHeight = Math.min(1, dBaseHeight);
//        dBaseHeight = Math.max(0, dBaseHeight);
        
        dBaseHeight = 2.0*dBaseHeight-1.0;
        
        
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
            dYH = clamp10((y-12*flattenScale) / ((double) df-12*flattenScale));
            double dyoff = 0.998D;
            double dz = 0;
            if (dYH >= dyoff) {
//                dz = (dYH - dyoff)/(1.0-dyoff);
//                dz*=dz;
//                dYH+=dz*0.4D;
            }
            int idx = idx_xz + y;
            double dBase = dStr - dYH * dStr * 2.0;

            double dN1 = noise.dnoise_[idx] * 4.0D;
            double dN2 = noise.dNoise2_[idx] * 2.0D;
            double dN5 = noise.dnoise5_[idx] * 1.0D;
            double dN3 = noise.dnoise3_[idx] * 0.5D;
            if (dN5>0) {
                dN3*=dN5;
            } else {
                dN5*=-0.1;
                dN3+=dN5;
            }
            double mtn=dBaseHeight*(1)*dStr*coreScale*0.9;
            double mtn2=dBaseHeight2*(1)*dStr*coreScale*0.9;
            if (mtn < 0)
                mtn*=0.5;
            if (mtn2 < 0)
                mtn2*=0.5;
            mtn*=clamp10(dYH+0.4)*clamp10(dYH+0.4);
            dBase -= Math.abs(mtn)*0.2*clamp10(1.2*func2(y+dN3, 110, 4));
            dBase += Math.abs(mtn)*1.55*clamp10(0.9*func2(y+dN3, 140, 12));
            dBase += Math.abs(mtn)*1.65*clamp10(0.9*func2(y+dN1, 130, 6));
            dBase += Math.abs(mtn)*1.85*clamp10(0.9*func2(y+dN5, 125, 12));
            dBase+= Math.abs(mtn2)*3.75*clamp10(0.9*func2(y+dN3, 120, 32)*dN2);
//            double xn=(func2(y+dN2, 140, 14)*(dN7*dN1)*dStr*Math.max(-1, (coreScale*0.6)-(dYH*dYH)*0.85));
            double q = 28;
            dBase += dN2 * q * (coreScale*0.8+dBaseHeight2*func2(y, 140, 12)*4);
            dBase += dN5 * q * (coreScale*0.8+dBaseHeight2*func2(y, 140, 12)*4);
            dBase += dN3 * q * (coreScale*0.8+dBaseHeight2*func2(y, 140, 12)*4);
            dBase -= dN1 * q * (coreScale*0.8+dBaseHeight2*func2(y, 140, 12)*4);
            
            int iy = 110;
//            dBase+=func2(iy+14 - dBaseHeight * 2, y, 33)*xn*clamp10(dN*dN7)*33;
//            dBase+=func2(iy+9 - dBaseHeight * 2, y, 12)*xn*12;
//            dBase+=func2(iy+4 - dBaseHeight * 2, y, 3)*xn*2;
//            dBase+=func2(iy - dBaseHeight * 2, y, 1)* mtn2*3;
//            dBase+=func2(iy+20 * 2, y, 1)* (dBaseHeight2)*4;
//            System.out.println(dBaseHeight);
//          //                        d-=1D;
//            
            dBase = dBase  + mtn + (dN2+dN1)*4*clamp10(1-coreScale);
            d[y] = dBase-100*(dz);
        }
        return 1;
    }

}
