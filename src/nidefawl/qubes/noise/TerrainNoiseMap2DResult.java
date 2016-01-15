package nidefawl.qubes.noise;

public final class TerrainNoiseMap2DResult {
    final double[] dNoise;
    private final int size;
    private final int maxI;
    public TerrainNoiseMap2DResult(int size, int maxI) {
        this.size = size; this.maxI = maxI;
        this.dNoise = new double[this.size];
    }

    public double getNoise(double[] dNoise, int x, int z, int iW, int w) {
        return dNoise[((z + iW) * (w + iW * 2)) + (x + iW)];
    }
    public double getBlur(int x, int z, int w) {
        if (w == 0) {
            return dNoise[((z + maxI) * (TerrainNoiseMap2D.CHUNK_SIZE + maxI * 2)) + (x + maxI)];
        }
        double d = 0;
        double weight = 0;
        for (int x1 = -w; x1 < w; x1++) {
            for (int z1 = -w; z1 < w; z1++) {
                int xx = x1 + x;
                int zz = z1 + z;
                double dWeight = TerrainNoiseMap2D.dWeights[w-1][((z1 + w) * (w * 2)) + (x1 + w)];
                weight += dWeight;
                //                if (x1==-i&&z1==-i)
                //                System.out.printf("%d, %d = %.2f\n", x1, z1, dWeight);
                d += dNoise[((zz + maxI) * (TerrainNoiseMap2D.CHUNK_SIZE + maxI * 2)) + (xx + maxI)] * dWeight;
            }
        }
        return d / weight;
    }
}