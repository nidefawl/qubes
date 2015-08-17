package nidefawl.qubes.worldgen;

import java.util.Random;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.World;

public class TestTerrain2 extends AbstractGen {

    private World world;
    private long  seed;

    public TestTerrain2(World world, long seed) {
        this.world = world;
        this.seed = seed;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        long rx = chunkX * 0x589638F52CL;
        long rz = chunkZ * 0x3F94515BD5L;
        Random rand = new Random(rx + rz);
        int heightBits = world.worldHeightBits;
        short[] blocks = new short[16*16*this.world.worldHeight];
        Chunk c = new Chunk(blocks, chunkX, chunkZ, heightBits);
        if (chunkX == 0 && chunkZ == 0) {
            generateTerrain(c);
        }
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk chunk) {
        int wh = this.world.worldHeight;
        int b = 7;
        int c = 127;
        int d = 2;
        for (int x = b-4; x < 16-b-4; x++) {
            for (int z = b; z < 16-b; z++) {
                for (int y = c; y < c+d; y++) {
                    System.out.println((chunk.getBlockX()+x)+"/"+(y)+"/"+(chunk.getBlockZ()+z));
                    chunk.blocks[y<<8|z<<4|x] = 1;
                }
            }
        }
        for (int x = b-2; x < 16-b-2; x++) {
            for (int z = b; z < 16-b; z++) {
                for (int y = c; y < c+d; y++) {
                    chunk.blocks[y<<8|z<<4|x] = 2;
                }
            }
        }
    }

}
