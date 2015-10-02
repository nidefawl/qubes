package nidefawl.qubes.noise;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.World;

public abstract class TerrainNoiseScale extends AbstractNoiseGen {
    final static int               w     = Chunk.SIZE;
    final static int               h     = World.MAX_WORLDHEIGHT;
    final static int               scale = 4;
    final static int               wLow  = (w / scale) + 1;
    final static int               hLow  = (h / scale) + 1;

    public static TerrainNoiseScale build(long seed, double scaleX, double scaleY, double scaleZ, int nOctaves) {
        return new TerrainNoiseScaleJava(seed, scaleX, scaleY, scaleZ, nOctaves);
    }

    public abstract double[] gen(int cx, int cz, double freq);

}
