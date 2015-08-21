package nidefawl.qubes;

import java.util.Random;

import nidefawl.qubes.noise.TerrainNoise;
import nidefawl.qubes.util.GameMath;

public class TestNoise {

    public static void main(String[] args) {
        TestNoise t = new TestNoise(new Random(1));
        {

            long l = System.nanoTime();
            int nLoops = 122;
            for (int i = 0; i < nLoops; i++) {
                t.gen(0, 0);
            }
            l = (System.nanoTime() - l) / 1000;
            l /= nLoops;
            System.out.println(""+l+" mikros");
        }
        {

            long l = System.nanoTime();
            int nLoops = 122;
            for (int i = 0; i < nLoops; i++) {
                t.genD(0, 0);
            }
            l = (System.nanoTime() - l) / 1000;
            l /= nLoops;
            System.out.println(""+l+" mikros");
        }

    }

    private final TerrainNoise noise;
    private final TerrainNoise noise2;
    private final TerrainNoise noise3;
    private final TerrainNoise noise4;
    int                        w     = 16;
    int                        h     = 256;
    int                        scale = 4;
    int                        wLow  = (w / scale)+1;
    int                        hLow  = (h / scale)+1;

    public TestNoise(Random rand) {
        double scaleMixXZ = 1;
        double scaleMixY = 1;
        this.noise = new TerrainNoise(rand.nextLong(), scaleMixXZ, scaleMixY, scaleMixXZ, 1);
        this.noise2 = new TerrainNoise(rand.nextLong(), scaleMixXZ, scaleMixY, scaleMixXZ, 1);
        this.noise3 = new TerrainNoise(rand.nextLong(), scaleMixXZ, scaleMixY, scaleMixXZ, 1);
        this.noise4 = new TerrainNoise(rand.nextLong(), scaleMixXZ, scaleMixY, scaleMixXZ, 1);
    }

    /**
     * 
     * @param cx chunk block coord
     * @param cz chunk block coord
     * @return 
     */
    private double[] genD(int cx, int cz) {
        double[] dn2 = new double[w * w * h];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < w; z++) {
                for (int y = 0; y < h; y++) {
                    double n = noise.get(x+cx, y, z+cz);
                    n += noise2.get(x+cx, y, z+cz);
                    n += noise3.get(x+cx, y, z+cz);
                    n += noise4.get(x+cx, y, z+cz);
                    n /= 4.0D;
                    dn2[idxH(x, y, z)] = n;
                }
            }
        }
        return dn2;
    }
    private double[] gen(int cx, int cz) {
        cx /= scale;
        cz /= scale;
        double[] dn = new double[wLow * wLow * hLow];
        for (int x = 0; x < wLow; x++) {
            for (int z = 0; z < wLow; z++) {
                for (int y = 0; y < hLow; y++) {
                    double n = noise.get(x+cx, y, z+cz);
                    n += noise2.get(x+cx, y, z+cz);
                    n += noise3.get(x+cx, y, z+cz);
                    n += noise4.get(x+cx, y, z+cz);
                    n /= 4.0D;
                    dn[idxL(x, y, z)] = n;

                }
            }
        }
        double[] dn2 = new double[w * w * h];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < w; z++) {
                for (int y = 0; y < h; y++) {
                    dn2[idxH(x, y, z)] = getInterp(x, y, z, scale, dn);
                }
            }
        }
        return dn2;
    }
    private int idxL(int x, int y, int z) {
        return (z * wLow + x) * (hLow) + y;
    }
    private int idxH(int x, int y, int z) {
        return (z * w + x) * (h) + y;
    }
    private double getInterp(int x, int y, int z, int scale, double[] dn) {
        double lx = x/(double)scale;
        double ly = y/(double)scale;
        double lz = z/(double)scale;
        int ix = GameMath.floor(lx);
        int iy = GameMath.floor(ly);
        int iz = GameMath.floor(lz);
        double fx = lx-ix;
        double fy = ly-iy;
        double fz = lz-iz;
        double n = 0;
        for (int i = 0; i < 8; i++) {
            int x1 = (i&1);
            int y1 = ((i&2)>>1);
            int z1 = ((i&4)>>2);
            double nx = x1 == 0 ? 1.0D - fx : fx;
            double ny = y1 == 0 ? 1.0D - fy : fy;
            double nz = z1 == 0 ? 1.0D - fz : fz;
            n += dn[idxL(ix+x1, iy+y1, iz+z1)]*nx*ny*nz;
        }
        return n;
    }
}
