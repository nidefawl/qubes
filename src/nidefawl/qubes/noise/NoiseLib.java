/**
 * 
 */
package nidefawl.qubes.noise;

import java.io.File;

import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseJava;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseLib;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class NoiseLib {
    final static boolean LIB_PRESENT;

    static {
        boolean hasNoise = false;
        try {
            System.load(new File("libnativenoise.so").getAbsolutePath());
            hasNoise = true;
        } catch (UnsatisfiedLinkError e) {
            //SWALLOW
        }
        try {
            System.load(new File("../lib/noise/nativenoise.dll").getAbsolutePath());
            hasNoise = true;
        } catch (UnsatisfiedLinkError e) {
            //SWALLOW
        }
        try {
            System.load(new File("./lib/noise/nativenoise.dll").getAbsolutePath());
            hasNoise = true;
        } catch (UnsatisfiedLinkError e) {
            //SWALLOW
        }
        LIB_PRESENT = hasNoise;
    }

    public static boolean isLibPresent() {
        return LIB_PRESENT;
    }

    public static OpenSimplexNoise makeGenerator(long seed) {
        if (LIB_PRESENT) {
            return new OpenSimplexNoiseLib(seed);
        }
        return new OpenSimplexNoiseJava(seed);
    }

    public static TerrainNoiseScale newNoiseScale(long seed) {
        if (LIB_PRESENT) {
            return new TerrainNoiseScaleLib(seed);
        }
        return new TerrainNoiseScaleJava(seed);
    }
}
