/**
 * 
 */
package nidefawl.qubes.worldgen.terrain.main;

import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.*;

import java.util.Random;

import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.biomes.HexBiome;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SubTerrainGenMeadow extends SubTerrainGen {
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
    public SubTerrainGenMeadow(TerrainGeneratorMain t) {
        this.main = t;
        Random rand = new Random(t.seed);

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
    public SubTerrainData prepare(int cX, int cZ, HexBiome b) {
/*
 * 
        double scaleMixXZ = 0.40D;
        double scaleMixY = scaleMixXZ*0.7D;
        double scaleT1XZ = 12.7D;
        double scaleT1Y = scaleT1XZ*0.01D;
        double scaleT2XZ = 2.6D;
        double scaleT2Y = scaleT1XZ*0.001;
        double scaleT5XZ =6.5D;
        double scaleT5Y = scaleT5XZ*12;
 */

        double scaleMixXZ = 0.80D;
        double scaleMixY = scaleMixXZ*1.7D;
        double scaleT1XZ = 2.4D;
        double scaleT1Y = scaleT1XZ*4.1D;
        double scaleT2XZ =2.3D;
        double scaleT2Y = scaleT1XZ*4.3;
        double scaleT5XZ =2.7D;
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
        this.noise2D = new TerrainNoise2D(12412, 2, 2, 1);
        data.r2Dn2 = r2D2.generate(cX, cZ);
        data.dNoise2_ = noise2.gen(cX, cZ);
        data.dnoise5_ = noise5.gen(cX, cZ);
        data.dnoise_ = noise.gen(cX, cZ);
        data.dnoise3_ = noise3.gen(cX, cZ);
        return data;
    }
    public double generate(int cX, int cZ, int x, int y1, int y2, int z, HexBiome hex, SubTerrainData data, double[] d, double[] d2) {
        
        NoiseData noise = (NoiseData) data;

/*
 *         double randomHeight = 0.18D;
        double tallness = 12.4D;
        double tallnessBase = 0.05D;
        double heightOffset = 7.0D;
        double dStr = 32.0D;
        double yBrr = 130;
        double yBrr2 = 130;
        double brrrIntens = 1.0D;
        double brrrIntens2 = 1.0D;
        double riverY = 92;
        double riverY2 = riverY + 12;
        double riverStr = 0.8D;
 */
//        double randomHeight = 0.18D;
//        double tallness = 12.4D;
//        double tallnessBase = 0.05D;
//        double heightOffset = 12.0D;
//        double dStr = 32.0D;
//        double yBrr = 130;
//        double yBrr2 = 120;
//        double brrrIntens = 1.0D;
//        double brrrIntens2 = 0.5D;
//        double riverY = 92;
//        double riverY2 = riverY + 12;
//        double riverStr = 0.8D;
//        double caveStrength = 2000D;

        double randomHeight = 0.0D;
        double tallness = 4.8D;
        double tallnessBase = 0.25D;
        double heightOffset = 17.0D;
        double dStr = 44.0D;
        double yBrr = 110;
        double yBrr2 = 115;
        double brrrIntens = 0.63D;
        double brrrIntens2 = 0.13D;
        double riverY = 87;
        double riverY2 = riverY +1;
        double riverStr = 0.68D;
        double cavePos = 110;
        double caveStrength = 44;
        
        
        
        double gridRadius = this.main.biomes.hwidth;
        int wh = this.main.world.worldHeight;
        double hx = hex.getCenterX();
        double hz = hex.getCenterY();
        double distHex = GameMath.dist2d(hx, hz, cX+x, cZ+z);
        double distScale = clamp10((distHex-gridRadius*0.5)/((gridRadius*0.5)*smoothScale));
        double outerDist = Math.max(distHex-gridRadius*smoothScale, 0);
        double outerScale = outerDist/(gridRadius*(1-smoothScale));
        outerScale = Math.min(outerScale, 1);
        double coreScale = 1-Math.min(Math.max(distHex-gridRadius*0.7, 0)/(gridRadius*0.2), 1);
        double flattenScale = 1-Math.min(Math.max(distHex-gridRadius*0.6, 0)/(gridRadius*0.6), 1);
        double dBaseHeight = (tallnessBase+(noise2D.get(cX|x, cZ|z)))*tallness*coreScale;
        double dh2 = noiseM2.get(cX|x, cZ|z);
        double xd = 0.9D;
        dh2 = 1-clamp10((1-clamp10((dh2-(1-xd))/xd))*0.7);
        double dBlurred = clamp10(noise.r2Dn.getBlur(x, z, 8)*4)*0.4;
        double dBlurredA = (clamp10(Math.pow(1+noise.r2Dn2.getBlur(x, z, 4), 1.2)-1))*4;
        double dBlurred2 = clamp10(noise.r2Dn.getBlur(x, z, 2))*0.25;
        double df = wh;
        int idx_xz = (z * 16 + x) * (256);
        dStr = 17+distScale*40;
        for (int y = y1; y < y2; y++) {
            double dYH = clamp10((y + 0 + heightOffset * flattenScale) / (double) df);
            double dyoff = 0.8D;
            if (dYH >= dyoff) {
                dYH*=1.0D+(dYH-dyoff);
//                double dz = (dYH - dyoff) * 2.0D;
//                dz = 1 - dz;
//                dz *= dz * (0.23D);
//                dz = 1 - dz;
//                dz /= 2;
//                dYH += dz;
            }
            int idx = idx_xz + y;
            double dN1 = noise.dnoise_[idx] * 14.9D;
            dYH = mix(dYH, dN1, randomHeight*coreScale);
            double dBase = dStr - dYH * dStr * 2.0;
            dBase = dBase + dBaseHeight;
            double dN = noise.dNoise2_[idx] * 0.7;
            double dN7 = noise.dnoise5_[idx];
            double dN2 = noise.dnoise3_[idx] * 2.0D;
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
            dBase=mix(dBase, -dStr, clamp10((y-(y2-20))/20.0D));
            d[y] = dBase;
            d2[y] = cavestr * 36;
        }
        return 1;
    }

}
