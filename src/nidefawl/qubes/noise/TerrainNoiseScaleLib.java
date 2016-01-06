/**
 * 
 */
package nidefawl.qubes.noise;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.UnsafeHelper;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TerrainNoiseScaleLib extends TerrainNoiseScale {
    
    native public static long native_init(long seed, int w, int h);
    native public static void native_setUpsampleFactor(long addr, int factor);
    native public static void native_setScale(long addr, double scaleX, double scaleY, double scaleZ);
    native public static void native_setOctavesFreq(long addr, int nOctaves, double freq);
    native public static void native_generateNoise(long addr, int x, int z, long buffer);
    native public static void native_free(long addr);

    private final int              w        = Chunk.SIZE;
    private final int              h        = World.MAX_WORLDHEIGHT;
    private double                 scaleX;
    private double                 scaleY;
    private double                 scaleZ;
    private int                    nOctaves;
    private double                 freqMult = 2.0D;
    private int                    factor   = 4;
    private int                    wLow     = (w / factor) + 1;
    private int                    hLow     = (h / factor) + 1;
    private long ptr;
    private long bufferAddr;

    public TerrainNoiseScaleLib(long seed) {
        this.bufferAddr = UnsafeHelper.alloc(w * h * w * 8);
        this.ptr = native_init(seed, w, h);
    }
    public TerrainNoiseScale setUpsampleFactor(int factor) {
        this.factor = factor;
        this.wLow = (this.w / factor) + 1;
        this.hLow = (this.h / factor) + 1;
        native_setUpsampleFactor(this.ptr, factor);
        return this;
    }
    public TerrainNoiseScale setScale(double scaleX, double scaleY, double scaleZ) {
        this.scaleX = (scaleX / gScale) * this.factor;
        this.scaleY = (scaleY / gScale) * this.factor;
        this.scaleZ = (scaleZ / gScale) * this.factor;
        native_setScale(this.ptr, this.scaleX, this.scaleY, this.scaleZ);
        return this;
    }
    public TerrainNoiseScale setOctavesFreq(int nOctaves, double freqMult) {
        this.nOctaves = nOctaves;
        this.freqMult = freqMult;
        native_setOctavesFreq(this.ptr, nOctaves, freqMult);
        return this;
    }

    @Override
    public synchronized double[] gen(int cx, int cz) {
        native_generateNoise(ptr, cx, cz, bufferAddr);
        double[] noise = new double[w * h * w];
        UnsafeHelper.copyDoubleArray(this.bufferAddr, noise);
        return noise;
    }

    @Override
    protected void finalize() throws Throwable {
        native_free(ptr);
        UnsafeHelper.free(this.bufferAddr);
        super.finalize();
    }
}
