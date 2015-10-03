package nidefawl.qubes.noise;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseJava;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;

public class TerrainNoiseScaleJava extends TerrainNoiseScale {

    private final OpenSimplexNoise noise;
    private final int              w        = Chunk.SIZE;
    private final int              h        = World.MAX_WORLDHEIGHT;
    private double                 scaleX;
    private double                 scaleY;
    private double                 scaleZ;
    private int                    nOctaves;
    private double                 freqMult = 2.0D;
    private int                    factor   = 4;
    private int                    wLow     = (w / factor) + 1;
    private int                    hLow     = (h / factor) + 1;

    public TerrainNoiseScaleJava(long seed) {
        this.noise = new OpenSimplexNoiseJava(seed);
    }

    @Override
    public TerrainNoiseScale setOctavesFreq(int nOctaves, double freqMult) {
        this.nOctaves = nOctaves;
        this.freqMult = freqMult;
        return this;
    }

    @Override
    public TerrainNoiseScale setUpsampleFactor(int factor) {
        this.factor = factor;
        this.wLow = (this.w / factor) + 1;
        this.hLow = (this.h / factor) + 1;
        return this;
    }

    @Override
    public TerrainNoiseScale setScale(double scaleX, double scaleY, double scaleZ) {
        this.scaleX = (scaleX / gScale) * this.factor;
        this.scaleY = (scaleY / gScale) * this.factor;
        this.scaleZ = (scaleZ / gScale) * this.factor;
        return this;
    }

    double get(int x, int y, int z) {
        double d = 0.0D;
        double dAmplitude = 1.0D;
        double dFreq = 1.0D;
        for (int n = 0; n < this.nOctaves; n++) {
            double dX = x * this.scaleX * dFreq;
            double dY = y * this.scaleY * dFreq;
            double dZ = z * this.scaleZ * dFreq;
            d += this.noise.eval(dX, dY, dZ) * dAmplitude;
            dFreq *= this.freqMult;
            dAmplitude = 1D / dFreq;
        }
        return d;
    }

    public double[] gen(int cx, int cz) {
        cx /= factor;
        cz /= factor;
        double[] dn = new double[wLow * wLow * hLow];
        for (int x = 0; x < wLow; x++) {
            for (int z = 0; z < wLow; z++) {
                for (int y = 0; y < hLow; y++) {
                    double n = get(x + cx, y, z + cz);
                    dn[(z * wLow + x) * (hLow) + y] = n;

                }
            }
        }
        double[] dn2 = new double[w * w * h];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < w; z++) {
                for (int y = 0; y < h; y++) {
                    double lx = x / (double) factor;
                    double ly = y / (double) factor;
                    double lz = z / (double) factor;
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
