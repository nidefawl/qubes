package nidefawl.qubes.noise;

import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;

public class RiverNoise2D extends AbstractNoiseGen {
    public final static class RiverNoiseResult {
        private final double[] dNoise;
        private final int size;
        private final int maxI;
        public RiverNoiseResult(int size, int maxI) {
            this.size = size; this.maxI = maxI;
            this.dNoise = new double[this.size];
        }

        public double getNoise(double[] dNoise, int x, int z, int iW, int w) {
            return dNoise[((z + iW) * (w + iW * 2)) + (x + iW)];
        }
        public double getBlur(int x, int z, int w) {
            if (w == 0) {
                return dNoise[((z + maxI) * (CHUNK_SIZE + maxI * 2)) + (x + maxI)];
            }
            double d = 0;
            double weight = 0;
            for (int x1 = -w; x1 < w; x1++) {
                for (int z1 = -w; z1 < w; z1++) {
                    int xx = x1 + x;
                    int zz = z1 + z;
                    double dWeight = dWeights[w-1][((z1 + w) * (w * 2)) + (x1 + w)];
                    weight += dWeight;
                    //                if (x1==-i&&z1==-i)
                    //                System.out.printf("%d, %d = %.2f\n", x1, z1, dWeight);
                    d += dNoise[((zz + maxI) * (CHUNK_SIZE + maxI * 2)) + (xx + maxI)] * dWeight;
                }
            }
            return d / weight;
        }
    }
    final static int CHUNK_SIZE = 16;
    private final static double[][]         dWeights = new double[8][];
    static {
        double dmax = 0D;
        for (int i = 0; i < dWeights.length; i++) {
            int iW = 1+i;
            dWeights[i] = new double[iW * iW * 2 * 2];
            for (int x1 = -iW; x1 < iW; x1++) {
                for (int z1 = -iW; z1 < iW; z1++) {
                    double dD = dist2d(0, 0, x1, z1);
                    if (dD > dmax)
                        dmax = dD;
                    dWeights[i][((z1 + iW) * (iW * 2)) + (x1 + iW)] = dD;
                }
            }
            for (int j = 0; j < dWeights[i].length; j++) {
                dWeights[i][j] = 1.0D - dWeights[i][j] / dmax;
            }
        }
    }
    private final OpenSimplexNoise n1;
    private final double           scale;
    private int maxI;
    private int size;
    public RiverNoise2D(long seed, double scaleX, int maxI) {
        this.n1 = NoiseLib.makeGenerator(seed);
        this.scale = scaleX / gScale;
        int w = CHUNK_SIZE;
        int size = w+maxI*2;
        this.size = size*size;
        this.maxI = maxI;
    }

    public static double dist2d(double x, double z, double xx, double zz) {
        x = x - xx;
        z = z - zz;
        return Math.sqrt(x * x + z * z);
    }

    public RiverNoiseResult generate(int x, int z) {
        RiverNoiseResult r = new RiverNoiseResult(size, maxI);
        double scale2 = scale * 5;
        for (int iX = -maxI; iX < CHUNK_SIZE + maxI; iX++)
            for (int iZ = -maxI; iZ < CHUNK_SIZE + maxI; iZ++) {
//                double d1 = n2.eval((iX+x) * scale2, (iZ+z) * scale2) * 0.1D;
//                double d2 = n3.eval((iX+x) * scale2, (iZ+z) * scale2) * 0.1D;
                double d1, d2;
                d1 = d2 = 0;
                double d = -1D;
                double dX = (iX+x) * scale;
                double dZ = (iZ+z) * scale;
                double dN = n1.eval(dX + d1, dZ + d2);
                d += Math.abs(dN) * 10;
                d *= 0.5D;
                d += 0.5D;
                d = Math.min(1, d);
                d = Math.max(0, d);
                d = 1 - d;
                d += 0.08D;
                d = Math.pow(d, 12);
                //                        d-=1D;
                d = Math.min(1, d);
                d = Math.max(0, d);
                r.dNoise[((iZ + maxI) * (CHUNK_SIZE + maxI * 2)) + (iX + maxI)] = d;
            }
        return r;
    }

}
