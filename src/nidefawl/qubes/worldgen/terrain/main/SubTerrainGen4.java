/**
 * 
 */
package nidefawl.qubes.worldgen.terrain.main;

import java.util.Random;

import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.*;

import nidefawl.qubes.noise.*;
import nidefawl.qubes.noise.RiverNoise2D.RiverNoiseResult;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.biomes.HexBiome;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SubTerrainGen4 extends SubTerrainGen {
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
    public SubTerrainGen4(TerrainGeneratorMain t) {
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
            this.r2D = new RiverNoise2D(rand.nextLong(), dRScale, 8);
             dRScale = 2.14D;
            this.r2D2 = new RiverNoise2D(33, dRScale, 8);
        }
    }
    public SubTerrainData prepare(int cX, int cZ, HexBiome b) {


        NoiseData data = new NoiseData();
        data.r2Dn = r2D.generate(cX, cZ);
        data.r2Dn2 = r2D2.generate(cX, cZ);
        data.dNoise2_ = noise2.gen(cX, cZ);
        data.dnoise5_ = noise5.gen(cX, cZ);
        data.dnoise_ = noise.gen(cX, cZ);
        data.dnoise3_ = noise3.gen(cX, cZ);
        return data;
    }
    public double generate(int cX, int cZ, int x, int y1, int y2, int z, HexBiome hex, SubTerrainData data, double[] d, double[] d2) {
        
        NoiseData noise = (NoiseData) data;
        

        double gridRadius = this.main.biomes.hwidth;
        int wh = this.main.world.worldHeight;
        double hx = hex.getCenterX();
        double hz = hex.getCenterY();
        double distHex = GameMath.dist2d(hx, hz, cX+x, cZ+z);
        double outerDist = Math.max(distHex-gridRadius*smoothScale, 0);
        double outerScale = outerDist/(gridRadius*(1-smoothScale));
        outerScale = Math.min(outerScale, 1);
        double coreScale = 1-Math.min(Math.max(distHex-gridRadius*0.7, 0)/(gridRadius*0.2), 1);
        double flattenScale = 1-Math.min(Math.max(distHex-gridRadius*0.6, 0)/(gridRadius*0.6), 1);
        double dBaseHeight = (noise2D.get(cX|x, cZ|z))*14.3D;
        if (dBaseHeight < 0) {
            dBaseHeight*=0.44;
        } else {
            dBaseHeight*=1.12f;
        }
//        dBaseHeight*=Math.abs(dBaseHeight);
        
        double dh2 = noiseM2.get(cX|x, cZ|z);
        double xd = 0.9D;
        dh2 = 1-clamp10((1-clamp10((dh2-(1-xd))/xd))*0.7);
        double dStr = 120.0D;
        double dBlurred = (clamp10(Math.pow(1+noise.r2Dn.getBlur(x, z, 4), 2)-1))*4;
        double dBlurredA = (clamp10(Math.pow(1+noise.r2Dn2.getBlur(x, z, 8), 0.8)-1))*4;
        double dBlurred2 = noise.r2Dn.getBlur(x, z, 4)*0.67;//r2D.getBlur(x, z, 4);
        double df = wh;
        int idx_xz = (z * 16 + x) * (256);

        for (int y = y1; y < y2; y++) {
            double dYH = clamp10((y + 0 + 20 * flattenScale) / (double) df);
            double dyoff = 0.75D;
            if (dYH >= dyoff) {
                double dz = (dYH - dyoff) * 2.0D;
                dz = 1 - dz;
                dz *= dz * (0.05D)+0.95;
                dz = 1 - dz;
                dz /= 2;
                dYH += dz;
            }

            double dBase = dStr - dYH * dStr * 2.0;
            
            dBase = dBase + dBaseHeight*0.1+dh2*0.4;
            int idx = idx_xz + y;
            double dN = noise.dNoise2_[idx];
            double dN7 = noise.dnoise5_[idx];
            double dN1 = noise.dnoise_[idx] * 3.0D;
            double dN2 = noise.dnoise3_[idx] * 5.0D;
//            dBase += dN * 1.7 * dh2 * coreScale;
//            double f = clamp10(1+mix(1-func2(128, y, 12)*4, 1, 1-coreScale));
            double dnnn=dN1*80*(clamp10(dYH-0.4));
            double mtn=(dN7*12+dBaseHeight*8-dnnn)*(0.8+0.2*dN*dN) * 0.78f* coreScale;
            mtn*=clamp10(dYH+0.4)*clamp10(dYH+0.4);
            dBase += mtn;
            dBase -= Math.abs(mtn)*0.2*clamp10(1.2*func2(y+dN2, 150, 12));
            dBase -= Math.abs(mtn)*0.15*clamp10(0.9*func2(y+dN2, 170, 12));
            dBase -= Math.abs(mtn)*0.15*clamp10(0.9*func2(y+dN2, 140, 6));
            dBase += Math.abs(mtn)*0.35*clamp10(0.9*func2(y+dN2, 130, 12));
            dBase -= Math.abs(mtn)*0.75*clamp10(0.9*func2(y+dN2, 100, 32)+dN7*4);
//            dYH-=;
            double riverY = 100;
            double dRiverH = func2(riverY - dBaseHeight * 2, y, 12);
            double riverY2 = riverY + 12;
            double dRiverH2 = y >= riverY2 ? 1 : func2(riverY2, y, 23);
            double xr = 0;
            int iy = 90;
            dBase+=func2(iy+14 - dBaseHeight * 2, y, 33)*dN1*clamp10(dN*dN7)*33;
            dBase+=func2(iy+9 - dBaseHeight * 2, y, 12)*dN1*12;
            dBase+=func2(iy+4 - dBaseHeight * 2, y, 3)*dN1*2;
            dBase+=func2(iy - dBaseHeight * 2, y, 1)*dBlurred*1;
            xr += dRiverH * dBlurred2 * dStr;
            xr += dRiverH2 * dBlurred * dStr * coreScale;
            double dt = func2(114 + dN1 * 2, y, 7) * 0.3;
            double cavestr = dt * dStr * dBlurredA * (0.5D + dN1 * 0.2D + 0.2 * dN2);
//            xr += cavestr * 36;
            dBase -= xr * coreScale;
            d[y] = dBase;
        }
        return 1;
    }

}
