package nidefawl.qubes;

import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;

public class Test {

    public static void main(String[] args) {
        testNoise();
    }

    private static void testNoise() {
        int w = 32;
        OpenSimplexNoise n = new OpenSimplexNoise(123);
        double dMin = 10000;
        double dMax = -10000;
        int octaves = 16;
        for (int x = 0; x < 1; x++) {
            for (int z = 0; z < 1; z++) {
                for (int y = 0; y < w; y++) {
                    double freq = 1;
                    double amp = 1;
                    double d = 0;
                    for (int o = 0; o < octaves; o++) {
                        d += n.eval(x*freq, y*freq, z*freq)*amp;
                        freq *= 2.0D;
                        amp /= 2.0D;
                    }
                    dMin = Math.min(d, dMin);
                    dMax = Math.max(d, dMax);
                }
            }
        }
        System.out.println(dMin+"/"+dMax);
    }
}
