/**
 * 
 */
package nidefawl.qubes.noise;

import java.io.File;

import org.lwjgl.system.Platform;

import nidefawl.qubes.noise.opennoise.OpenSimplexNoise;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseJava;
import nidefawl.qubes.noise.opennoise.OpenSimplexNoiseLib;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class NoiseLib {
    final static boolean LIB_PRESENT;

    static {
        LIB_PRESENT = create();
    }

    public static boolean create() {
        String name;
        boolean bitness = System.getProperty("os.arch").indexOf("64") > -1;
        switch ( Platform.get() ) {
            case LINUX:
                name = bitness?"libnativenoise.x64.so.1":"libnativenoise.x86.so.1";
                break;
            case WINDOWS:
                name = bitness?"nativenoise.x64.dll":"nativenoise.x86.dll";
                break;
            default:
                throw new IllegalStateException();
        }
        File f = new File(name);
        if (!f.exists()) {
            f = new File("../Game/lib/noise/", name);
        }
        if (f.exists()) {
            System.load(f.getAbsolutePath());
            return true;
        }
        return false;
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
