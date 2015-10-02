/**
 * 
 */
package nidefawl.qubes.noise;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class OpenSimplexNoiseLib extends OpenSimplexNoise {
    native public static long init(long seed);
    native public static void free(long addr);
    native public static double eval(long addr, double x, double y);
    native public static double eval(long addr, double x, double y, double z);
    native public static double eval(long addr, double x, double y, double z, double w);
    native public static void generateChunk(long addr, int x, int z, long buffer);

    private long ptr;

    public OpenSimplexNoiseLib(long seed) {
        this.ptr = init(seed);
    }

    @Override
    public double eval(double x, double y) {
        return eval(ptr, x, y);
    }

    @Override
    public double eval(double x, double y, double z) {
        return eval(ptr, x, y, z);
    }

    @Override
    public double eval(double x, double y, double z, double w) {
        return eval(ptr, x, y, z, w);
    }
    @Override
    protected void finalize() throws Throwable {
        free(ptr); //TODO: someone said this isn't good here (figure out if and why)
        super.finalize();
    }

}
