package nidefawl.qubes.chunk.server;

import java.io.IOException;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.TagReader;
import nidefawl.qubes.world.World;
import nidefawl.qubes.nbt.Tag.Compound;

public class ChunkReader {
    final ChunkManagerServer mgr;
    final RegionFileCache    fileCache;

    public ChunkReader(ChunkManagerServer mgr, RegionFileCache fileCache) {
        this.mgr = mgr;
        this.fileCache = fileCache;
    }

    public Chunk loadChunk(World world, int x, int z) {
        RegionFile f = this.fileCache.getRegionFileChunk(x, z);
        try {
            byte[] data = f.readChunk(x, z);
            if (data.length > 0) {
                Compound t = (Compound) TagReader.readTagFromCompressedBytes(data);
                return readChunk(world, x, z, t);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveChunk(Chunk c) {
        RegionFile f = this.fileCache.getRegionFileChunk(c.x, c.z);
        try {
            Compound tag = writeChunk(c);
            byte[] data = TagReader.writeTagToCompresedBytes(tag);
            f.writeChunk(c.x, c.z, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Compound writeChunk(Chunk c) {
        Compound cmp = new Compound();
        cmp.setInt("version", 1);
        cmp.setInt("x", c.x);
        cmp.setInt("z", c.z);
        short[] blocks = c.getBlocks();
        byte[] byteBlocks = shortToByteArray(blocks);
        cmp.setByteArray("blocks", byteBlocks);
        return cmp;
    }

    private Chunk readChunk(World world, int x, int z, Compound t) {
        Chunk c = new Chunk(x, z, world.worldHeightBits);
        Tag.ByteArray bytearray = t.getByteArray("blocks");
        byte[] byteBlocks = bytearray.getArray();
        short[] blocks = byteToShortArray(byteBlocks);
        c.setBlocks(blocks);
        return c;
    }


    public static byte[] shortToByteArray(short[] blocks) {
        byte[] bytes = new byte[blocks.length*2];
        for (int i = 0; i < blocks.length; i++) {
            bytes[i*2+0] = (byte) (blocks[i]&0xFF);
            bytes[i*2+1] = (byte) ((blocks[i]>>8)&0xFF);
        }
        return bytes;
    }

    public static short[] byteToShortArray(byte[] blocks) {
        short[] shorts = new short[blocks.length/2];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) ( (blocks[i*2+0]&0xFF) | ((blocks[i*2+1]&0xFF)<<8) );
        }
        return shorts;
    }
}
