package nidefawl.qubes.noise;

import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;
import nidefawl.qubes.util.GameMath;

public class TerrainNoiseScale extends AbstractNoiseGen {
    private final OpenSimplexNoise noise;
    private final double scaleX;
    private final double scaleY;
    private final double scaleZ;
    private final int nOctaves;
    int                        w     = 16;
    int                        h     = 256;
    int                        scale = 4;
    int                        wLow  = (w / scale)+1;
    int                        hLow  = (h / scale)+1;

    public TerrainNoiseScale(long seed, double scaleX, double scaleY, double scaleZ, int nOctaves) {
        this.noise = new OpenSimplexNoise(seed);
        this.scaleX = (scaleX/gScale)*scale;
        this.scaleY = (scaleY/gScale)*scale;
        this.scaleZ = (scaleZ/gScale)*scale;
        this.nOctaves = nOctaves;
    }
    
     double get(int x, int y, int z) {
        double d = 0.0D;
        double dAmplitude = 1.0D;
        double dFreq = 1.0D;
        for (int n = 0; n < this.nOctaves; n++) {
            double dX = x*this.scaleX*dFreq;
            double dY = y*this.scaleY*dFreq;
            double dZ = z*this.scaleZ*dFreq;
            d += this.noise.eval(dX, dY, dZ)*dAmplitude;
            dFreq *= 1.69D;
            dAmplitude = 1D/dFreq;
        }
        return d;
    }

    public double[] gen(int cx, int cz) {
        cx /= scale;
        cz /= scale;
        double[] dn = new double[wLow * wLow * hLow];
        for (int x = 0; x < wLow; x++) {
            for (int z = 0; z < wLow; z++) {
                for (int y = 0; y < hLow; y++) {
                    double n = get(x+cx, y, z+cz);
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
