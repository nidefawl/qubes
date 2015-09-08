package nidefawl.qubes.worldgen;

import java.util.Random;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.World;

public class TestTerrain extends AbstractGen {

    private World world;
    private long  seed;

    public TestTerrain(World world, long seed) {
        this.world = world;
        this.seed = seed;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
        Random rand = new Random(rx + rz);
        int heightBits = world.worldHeightBits;
        Chunk c = new Chunk(this.world, chunkX, chunkZ, heightBits);
        short[] blocks = c.getBlocks();
        generateTerrain(c, blocks);
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk chunk, short[] blocks) {
        int wh = this.world.worldHeight;
        int b = 4;
        int c = 60;
        int d = 8;
        for (int x = b; x < 16-b; x++) {
            for (int z = b; z < 16-b; z++) {
                for (int y = c; y < c+d; y++) {
                    blocks[y<<8|z<<4|x] = 1;
                }
            }
        }
         b = 0;
         c = 40;
         d = 2;
        for (int x = b; x < 16-b; x++) {
            for (int z = b; z < 16-b; z++) {
                for (int y = c; y < c+d; y++) {
                    blocks[y<<8|z<<4|x] = 1;
                }
            }
        }
    }

}
