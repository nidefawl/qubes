package nidefawl.qubes.noise;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseJava;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;

public class TerrainNoiseScaleJava extends TerrainNoiseScale {
    
    private final OpenSimplexNoise noise;
    private final double           scaleX;
    private final double           scaleY;
    private final double           scaleZ;
    private final int              nOctaves;

    public TerrainNoiseScaleJava(long seed, double scaleX, double scaleY, double scaleZ, int nOctaves) {
        this.noise = new OpenSimplexNoiseJava(seed);
        this.scaleX = (scaleX / gScale) * scale;
        this.scaleY = (scaleY / gScale) * scale;
        this.scaleZ = (scaleZ / gScale) * scale;
        this.nOctaves = nOctaves;
    }

    double get(int x, int y, int z, double freq) {
        double d = 0.0D;
        double dAmplitude = 1.0D;
        double dFreq = 1.0D;
        for (int n = 0; n < this.nOctaves; n++) {
            double dX = x * this.scaleX * dFreq;
            double dY = y * this.scaleY * dFreq;
            double dZ = z * this.scaleZ * dFreq;
            d += this.noise.eval(dX, dY, dZ) * dAmplitude;
            dFreq *= freq;
            dAmplitude = 1D / dFreq;
        }
        return d;
    }

    public double[] gen(int cx, int cz, double freq) {
        cx /= scale;
        cz /= scale;
        double[] dn = new double[wLow * wLow * hLow];
        for (int x = 0; x < wLow; x++) {
            for (int z = 0; z < wLow; z++) {
                for (int y = 0; y < hLow; y++) {
                    double n = get(x + cx, y, z + cz, freq);
                    dn[(z * wLow + x) * (hLow) + y] = n;

                }
            }
        }
        double[] dn2 = new double[w * w * h];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < w; z++) {
                for (int y = 0; y < h; y++) {
                    double lx = x / (double) scale;
                    double ly = y / (double) scale;
                    double lz = z / (double) scale;
                    int ix = GameMath.floor(lx);
                    int iy = GameMath.floor(ly);
                    int iz = GameMath.floor(lz);
                    double fx = lx - ix;
                    double fy = ly - iy;
                    double fz = lz - iz;
                    double n = 0;
                    for (int i = 0; i < 8; i++) {
                        int x1 = (i & 1);
                        int y1 = ((i & 2) >> 1);
                        int z1 = ((i & 4) >> 2);
                        double nx = x1 == 0 ? 1.0D - fx : fx;
                        double ny = y1 == 0 ? 1.0D - fy : fy;
                        double nz = z1 == 0 ? 1.0D - fz : fz;
                        n += dn[((iz + z1) * wLow + ix + x1) * (hLow) + iy + y1] * nx * ny * nz;
                    }
                    dn2[(z * w + x) * (h) + y] = n;
                }
            }
        }
        return dn2;
    }
}
