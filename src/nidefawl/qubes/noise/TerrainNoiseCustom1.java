package nidefawl.qubes.noise;

public class TerrainNoiseCustom1 extends AbstractNoiseGen {
    private final OpenSimplexNoise noise;
    private final double scaleX;
    private final double scaleY;
    private final double scaleZ;
    private final int nOctaves;

    public TerrainNoiseCustom1(long seed, double scaleX, double scaleY, double scaleZ, int nOctaves) {
        this.noise = NoiseLib.makeGenerator(seed);
        this.scaleX = scaleX/gScale;
        this.scaleY = scaleY/gScale;
        this.scaleZ = scaleZ/gScale;
        this.nOctaves = nOctaves;
    }
    
    public double get(int x, int y, int z) {
        double d = 0.0D;
        double dAmplitude = 1.0D;
        double dFreq = 1.0D;
        for (int n = 0; n < this.nOctaves; n++) {
            double dX = x*this.scaleX*dFreq;
            double dY = y*this.scaleY*dFreq;
            double dZ = z*this.scaleZ*dFreq;
            d += this.noise.eval(dX, dY, dZ)*dAmplitude;
            dFreq *= 1.69D;
            dAmplitude = 1D/dFreq;
        }
        return d;
    }
    
}
