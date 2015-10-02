/**
 * 
 */
package nidefawl.qubes.noise;

import java.io.File;

import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseJava;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class NoiseLib {
    final static boolean LIB_PRESENT;
    static {
        boolean hasNoise=false;
        try {
            System.load(new File("libnativenoise.so").getAbsolutePath());    
            hasNoise = true;
        } catch (Exception e) {
            e.printStackTrace();
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
}
