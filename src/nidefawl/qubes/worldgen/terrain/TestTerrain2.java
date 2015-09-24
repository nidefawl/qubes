package nidefawl.qubes.worldgen.terrain;

import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.world.World;

public class TestTerrain2 implements ITerrainGen {

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
        Chunk c = new Chunk(this.world, chunkX, chunkZ, heightBits);
        short[] blocks = c.getBlocks();
        generateTerrain(c, blocks);
        c.checkIsEmtpy();
        return c;
    }

    private void generateTerrain(Chunk chunk, short[] blocks) {
        int wh = this.world.worldHeight;
        int b = 7;
        int c = 127;
        int d = 2;
//        for (int x = b-4; x < 16-b-4; x++) {
//            for (int z = b; z < 16-b; z++) {
//                for (int y = c; y < c+d; y++) {
//                    blocks[y<<8|z<<4|x] = (short) Block.sand.id;
//                }
//            }
//        }
//      for (int x = b-2; x < 16-b-2; x++) {
//      for (int z = b; z < 16-b; z++) {
//          for (int y = c; y < c+d; y++) {
//              blocks[y<<8|z<<4|x] = (short) Block.sand.id;
//          }
//      }
//  }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 128d; y++) {
                    blocks[y << 8 | z << 4 | x] = (short) Block.sand.id;
                }
            }
        }
    }

}
