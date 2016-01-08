/**
 * 
 */
package nidefawl.qubes.worldgen.terrain.main;

import java.util.Random;

import static nidefawl.qubes.worldgen.terrain.main.TerrainGeneratorMain.*;
import nidefawl.qubes.noise.*;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.worldgen.biome.HexBiome;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SubTerrainGen2 extends SubTerrainGen {
    /**
     * @author Michael Hept 2015
     * Copyright: Michael Hept
     */
    public class NoiseData extends SubTerrainGen.SubTerrainData {

        public double[] dNoise;
        public double[] dNoise2;
        public double[] dNoise2_;
        public double[] dnoise5_;

    }
    private  TerrainNoise2D noiseM2;
    private  TerrainNoiseScale noise2;
    private  TerrainNoise2D noise2D;
    private TerrainNoiseScale noise5;
    private TerrainGeneratorMain main;
    public SubTerrainGen2(TerrainGeneratorMain t) {
        this.main = t;
        Random rand = new Random(t.seed);
        double scaleMix2XZ = 1.1D;
        double scaleT1XZ = 0.3D;
        double scaleT1Y = scaleT1XZ*0.3D;
        double scaleT5XZ = 3.6D;
        double scaleT5Y = scaleT5XZ*3.4D;
        this.noiseM2 = new TerrainNoise2D(rand.nextLong(), scaleMix2XZ, scaleMix2XZ, 2);
        this.noise2 = NoiseLib.newNoiseScale(rand.nextLong())
                .setUpsampleFactor(4)
                .setScale(scaleT1XZ, scaleT1Y, scaleT1XZ)
                .setOctavesFreq(3, 1.97D + 0.01);
        this.noise5 = NoiseLib.newNoiseScale(rand.nextLong())
                .setUpsampleFactor(4)
                .setScale(scaleT5XZ, scaleT5Y, scaleT5XZ)
                .setOctavesFreq(3, 2.59D);
        
        this.noise2D = new TerrainNoise2D(rand.nextLong(), 1, 1, 1);
    }
    public SubTerrainData prepare(int cX, int cZ) {

        double scaleT1XZ = 1D;
        double scaleT1Y = scaleT1XZ*0.1D;
        double scaleT5XZ = 1.5D;
        double scaleT5Y = scaleT5XZ*1.33D;
        this.noise2.setScale(scaleT1XZ, scaleT1Y, scaleT1XZ);
        this.noise5.setScale(scaleT5XZ, scaleT5Y, scaleT5XZ);
        int wh = this.main.world.worldHeight;
        NoiseData data = new NoiseData();
        data.dNoise = new double[16*16*wh]; 
        data.dNoise2 = new double[16*16*wh];
        data.dNoise2_ = noise2.gen(cX, cZ);
        data.dnoise5_ = noise5.gen(cX, cZ);
        return data;
    }
    public int generate(int cX, int cZ, int x, int y1, int y2, int z, HexBiome hex, SubTerrainData data, double[] d, double[] d2) {
        
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
        double dBaseHeight = (noise2D.get(cX|x, cZ|z))*1.3D;
        double dh2 = noiseM2.get(cX|x, cZ|z);
        double xd = 0.9D;
        dh2 = 1-clamp10((1-clamp10((dh2-(1-xd))/xd))*0.7);
        double dStr = 12.0D;
        double df = wh;
        int idx_xz = (z * 16 + x) * (256);
//        double hexScale1 = 1-(Math.max((distHex-gridRadius*0.9), 0)/(gridRadius*0.1));
//        if (outerScale != 0)
//        System.out.println(outerScale);

        for (int y = y1; y < y2; y++) {
            double dYH = clamp10((y+0+5*flattenScale)/(double)df);
            double dyoff = 0.8D;
            if (dYH >= dyoff) {
                double dz = (dYH-dyoff)*2.0D;
                dz = 1-dz;
                dz*=dz*(0.23D);
                dz = 1-dz;
                dz /=2;
                dYH+=dz;
            }
            
            double dBase = dStr-dYH*dStr*2.0;
            dBase = dBase+dBaseHeight;
            int idx = idx_xz+y;
            double dN = noise.dNoise2_[idx]-1;
            double dN7 = noise.dnoise5_[idx];
            dBase += dN*0.7*dh2*coreScale;
            dBase += dN7*1.7*dh2*coreScale;
            d[y] = dBase;
        }
        return 1;
    }

}
