/**
 * 
 */
package nidefawl.qubes.noise.opennoise;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class OpenSimplexNoiseLib extends OpenSimplexNoise {
    native public static long noiselib_init(long seed);
    native public static void noiselib_free(long addr);
    native public static double noiselib_eval(long addr, double x, double y);
    native public static double noiselib_eval(long addr, double x, double y, double z);
    native public static double noiselib_eval(long addr, double x, double y, double z, double w);
    native public static void noiselib_generateChunk(long addr, int x, int z, long buffer);

    private long ptr;

    public OpenSimplexNoiseLib(long seed) {
        this.ptr = noiselib_init(seed);
    }

    @Override
    public double eval(double x, double y) {
        return noiselib_eval(ptr, x, y);
    }

    @Override
    public double eval(double x, double y, double z) {
        return noiselib_eval(ptr, x, y, z);
    }

    @Override
    public double eval(double x, double y, double z, double w) {
        return noiselib_eval(ptr, x, y, z, w);
    }
    @Override
    protected void finalize() throws Throwable {
        noiselib_free(ptr); //TODO: someone said this isn't good here (figure out if and why)
        super.finalize();
    }

}
