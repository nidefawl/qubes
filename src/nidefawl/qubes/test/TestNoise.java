package nidefawl.qubes.test;

import nidefawl.qubes.noise.*;

public class TestNoise {

    public static void main(String[] args) {
        boolean b = NoiseLib.isLibPresent();
        if (b) {
            System.out.println("Using native library for OpenSimplexNoise");
        } else {
            System.err.println("Native library for OpenSimplexNoise not found");
        }
        double scaleMixXZ = 1;
        double scaleMixY = 1;
        long seed = 12445L;
        TerrainNoiseScale n1 = new TerrainNoiseScaleLib(seed)
                .setUpsampleFactor(8)
                .setScale(scaleMixXZ, scaleMixY, scaleMixXZ)
                .setOctavesFreq(3, 2);
        TerrainNoiseScale n2 = new TerrainNoiseScaleJava(seed)
                .setUpsampleFactor(8)
                .setScale(scaleMixXZ, scaleMixY, scaleMixXZ)
                .setOctavesFreq(3, 2);

        for (int n = 0; n < 4; n++) {
            double l1 = run(n1);
            double l2 = run(n2);
        }
        double l1 = run(n1);
        double l2 = run(n2);
        System.out.printf("lib  %.2f\n", l1);
        System.out.printf("java %.2f\n", l2);
        
    }

    private static double run(TerrainNoiseScale n1) {
        double d1 = 0;
        long start = System.nanoTime();
        for (int n = 0; n < 32; n++) {
            n1.gen(4, n);
        }
        double l = (System.nanoTime()-start) / 1000000.0D;
        return l;
    }
}
