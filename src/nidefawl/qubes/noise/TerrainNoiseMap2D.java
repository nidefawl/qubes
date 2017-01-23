package nidefawl.qubes.noise;

import nidefawl.qubes.util.GameMath;

public class TerrainNoiseMap2D extends AbstractNoiseGen {
    final static int CHUNK_SIZE = 16;
    final static double[][]         dWeights = new double[8][];
    static {
        double dmax = 0D;
        for (int i = 0; i < dWeights.length; i++) {
            int iW = 1+i;
            dWeights[i] = new double[iW * iW * 2 * 2];
            for (int x1 = -iW; x1 < iW; x1++) {
                for (int z1 = -iW; z1 < iW; z1++) {
                    double dD = GameMath.dist2d(0, 0, x1, z1);
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
    NoiseMap2D map;
    private final double           scale;
    private int maxI;
    private int size;
    public TerrainNoiseMap2D(String string, double scaleX, int maxI) {
        map = new NoiseMap2D(string);
        this.scale = scaleX;
        int w = CHUNK_SIZE;
        int size = w+maxI*2;
        this.size = size*size;
        this.maxI = maxI;
    }

    public TerrainNoiseMap2DResult generate(int x, int z, double hx, double hz) {
        TerrainNoiseMap2DResult r = new TerrainNoiseMap2DResult(size, maxI);
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        
        double w = map.getWidth()/2.0D;
        double h = map.getHeight()/2.0D;
        
        for (int iX = -maxI; iX < CHUNK_SIZE + maxI; iX++)
            for (int iZ = -maxI; iZ < CHUNK_SIZE + maxI; iZ++) {
              double d1, d2;
              d1 = d2 = 0;
              double d = -1D;
              int blockX = iX+x;
              int blockZ = iZ+z;
              double relativeX = blockX-hx;
              double relativeZ = blockZ-hz;
              relativeX*=scale;
              relativeZ*=scale;
              int dX = GameMath.floor(w+w*relativeX);
              int dZ = GameMath.floor(h+h*relativeZ);
//              System.err.println(w+"/"+dX+"/"+relativeX);
              d = map.evalI(dX, dZ);
              min = Math.min(d, min);
              max = Math.max(d, max);
//              d += Math.abs(dN) * 10;
//              d *= 0.5D;
//              d += 0.5D;
//              d = Math.min(1, d);
//              d = Math.max(0, d);
//              d = 1 - d;
//              d += 0.08D;
                r.dNoise[((iZ + maxI) * (CHUNK_SIZE + maxI * 2)) + (iX + maxI)] = d;
            }
//        System.out.println(min+"/"+max);
        return r;
    }
}
