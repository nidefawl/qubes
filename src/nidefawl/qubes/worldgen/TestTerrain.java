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
    public Chunk getChunkAt(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
        Random rand = new Random(rx + rz);
        int heightBits = world.worldHeightBits;
        short[] blocks = new short[16*16*this.world.worldHeight];
        Chunk c = new Chunk(blocks, chunkX, chunkZ, heightBits);
        generateTerrain(c);
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk chunk) {
        int wh = this.world.worldHeight;
        int b = 4;
        int c = 60;
        int d = 8;
        for (int x = b; x < 16-b; x++) {
            for (int z = b; z < 16-b; z++) {
                for (int y = c; y < c+d; y++) {
                    chunk.blocks[y<<8|z<<4|x] = 1;
                }
            }
        }
         b = 0;
         c = 40;
         d = 2;
        for (int x = b; x < 16-b; x++) {
            for (int z = b; z < 16-b; z++) {
                for (int y = c; y < c+d; y++) {
                    chunk.blocks[y<<8|z<<4|x] = 1;
                }
            }
        }
    }

}
