package nidefawl.qubes.noise;

public abstract class TerrainNoiseScale extends AbstractNoiseGen {

    public abstract double[] gen(int cx, int cz);
    public abstract TerrainNoiseScale setUpsampleFactor(int factor);
    public abstract TerrainNoiseScale setScale(double scaleX, double scaleY, double scaleZ);
    public abstract TerrainNoiseScale setOctavesFreq(int nOctaves, double freqMult);

}
