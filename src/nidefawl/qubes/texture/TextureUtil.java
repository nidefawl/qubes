package nidefawl.qubes.texture;

import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;
import nidefawl.qubes.noise.opennoise.SimplexValueNoise;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.util.GameMath;

public class TextureUtil {

    public static byte[] genNoise(int w) {
        byte[] data = new byte[w * w * 3];
        for (int x = 0; x < w; x++)
            for (int z = 0; z < w; z++)
                for (int y = 0; y < 3; y++) {
                    int seed = (GameMath.randomI(x * 5) - 79 + GameMath.randomI(y * 37)) * 1 + GameMath.randomI((z - 2) * 73);
                    data[(x * 64 + z) * 3 + y] = (byte) (GameMath.randomI(seed) % 128);
                }
        return data;
    }

    public byte[] genNoise2(int w, int h) {
        int noct = 8;
        long seed = 0xdeadbeefL;
        seed--;
        SimplexValueNoise n1 = new SimplexValueNoise(seed);
        OpenSimplexNoise n2 = new OpenSimplexNoise(seed);
        OpenSimplexNoise n3 = new OpenSimplexNoise(seed * 19);
        byte[] data = new byte[w * h * 3];
        TimingHelper.startSilent(123);
//        float f1 = Client.ticksran + Main.instance.partialTick;
        int iW = 2;
        double scale = 1 / 10D;
        double scale2 = scale * 5;
        double[] dNoise = new double[(w + iW * 2) * (h + iW * 2)];
        for (int iX = -iW; iX < w + iW; iX++)
            for (int iZ = -iW; iZ < h + iW; iZ++) {
                double d1 = n2.eval(iX * scale2, iZ * scale2) * 0.1D;
                double d2 = n3.eval(iX * scale2, iZ * scale2) * 0.1D;

                double d = -2D;
                double dX = (iX) * scale;
                double dZ = (iZ) * scale;
                double dN = n1.eval(dX + d1, dZ + d2);
                dN *= 0.5D;
                dN += 0.5D;
                d += (dN * 7);
                d = Math.min(1, d);
                d = Math.max(0, d);
                d = 1 - d;
                d += 0.08D;
                d = Math.pow(d, 12);
                //                d-=1D;
                d = Math.min(1, d);
                d = Math.max(0, d);
                dNoise[((iZ + iW) * (w + iW * 2)) + (iX + iW)] = d;
            }

        for (int ix = 0; ix < w; ix++)
            for (int iz = 0; iz < h; iz++) {
                double d = getBlur(dNoise, ix, iz, iW, iW, w);
                d = 1 - d;
                d = Math.pow(d, 4);
                //              d-=1D;
                d = Math.min(1, d);
                d = Math.max(0, d);
                d = 1 - d;
                int lum = (int) (d * 255);
                //              int seed = (GameMath.randomI(x*5)-79 + GameMath.randomI(y * 37)) * 1+GameMath.randomI((z-2) * 73);
                for (int y = 0; y < 3; y++) {
                    data[(iz * w + ix) * 3 + y] = (byte) lum;
                }
                //                break;
            }
        long l = TimingHelper.stopSilent(123);
        System.out.println(l);
        return data;
    }

    private double getBlur(double[] dNoise, int x, int z, int iW, int i, int w) {
        if (i == 0) {
            return dNoise[((z + iW) * (w + iW * 2)) + (x + iW)];
        }
        double d = 0;
        double[] dWeights = new double[i * i * 2 * 2];
        double dmax = 0D;
        for (int x1 = -i; x1 < i; x1++) {
            for (int z1 = -i; z1 < i; z1++) {
                double dD = GameMath.dist2d(0, 0, x1, z1);
                if (dD > dmax)
                    dmax = dD;
                dWeights[((z1 + i) * (i * 2)) + (x1 + i)] = dD;
            }
        }
        for (int j = 0; j < dWeights.length; j++) {
            dWeights[j] = 1.0D - dWeights[j] / dmax;
        }
        double weight = 0;
        for (int x1 = -i; x1 < i; x1++) {
            for (int z1 = -i; z1 < i; z1++) {
                int xx = x1 + x;
                int zz = z1 + z;
                double dWeight = dWeights[((z1 + i) * (i * 2)) + (x1 + i)];
                weight += dWeight;
                //                if (x1==-i&&z1==-i)
                //                System.out.printf("%d, %d = %.2f\n", x1, z1, dWeight);
                d += dNoise[((zz + iW) * (w + iW * 2)) + (xx + iW)] * dWeight;
            }
        }
        return d / weight;
    }
}
