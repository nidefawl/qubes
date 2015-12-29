package nidefawl.qubes.chunk.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import nidefawl.qubes.block.Block;
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

    private Compound writeChunk(Chunk c) throws IOException {
        Compound cmp = new Compound();
        cmp.setInt("version", 2);
        cmp.setInt("x", c.x);
        cmp.setInt("z", c.z);
        short[] blocks = c.getBlocks();
        byte[] byteBlocks = shortToByteArray(blocks);
        Tag.Compound blockSHortDataCompound = c.blockMetadata.writeToTag();
        if (blockSHortDataCompound != null)
            cmp.set("blockdata", blockSHortDataCompound);
        Tag.Compound blockDataCompound = c.blockData.writeToTag();
        if (blockDataCompound != null) {
            cmp.set("blockdataext", blockDataCompound);
        }
        cmp.setByteArray("blocks", byteBlocks);
        byte[] blockLight = c.getBlockLight();
        byte[] blockLight2 = new byte[blockLight.length];
        System.arraycopy(blockLight, 0, blockLight2, 0, blockLight2.length);
        cmp.setByteArray("blockLight", blockLight2);
        byte[] waterMask = c.getWaterMask();
        byte[] waterMask2 = new byte[waterMask.length];
        System.arraycopy(waterMask, 0, waterMask2, 0, waterMask2.length);
        cmp.setByteArray("waterMask", waterMask2);
        cmp.setBoolean("isLit", c.isLit);
        cmp.setBoolean("isPopulated", c.isPopulated);
        return cmp;
    }

    private Chunk readChunk(World world, int x, int z, Compound t) throws IOException {
        int version = t.getInt("version");
        if (version != 2) {
            System.err.println("Not loading version "+version+" chunk at "+x+"/"+z);
            return null;
        }
        Chunk c = new Chunk(world, x, z, world.worldHeightBits);
        Tag.ByteArray bytearray = t.getByteArray("blocks");
        byte[] byteBlocks = bytearray.getArray();
        readBlocks(byteBlocks, c.getBlocks());
        Tag blockDataCompound = t.get("blockdata");
        if (blockDataCompound != null) {
            c.blockMetadata.readFromTag((Tag.Compound) blockDataCompound);    
        }
        blockDataCompound = t.get("blockdataext");
        if (blockDataCompound != null) {
            c.blockData.readFromTag((Tag.Compound) blockDataCompound);    
        }
        Tag.ByteArray waterMaskTag = t.getByteArray("waterMask");
        byte[] waterMask = waterMaskTag.getArray();
        System.arraycopy(waterMask, 0, c.getWaterMask(), 0, waterMask.length);
        
        Tag.ByteArray blockLightArr = t.getByteArray("blockLight");
        byte[] blockLight = blockLightArr.getArray();
        System.arraycopy(blockLight, 0, c.getBlockLight(), 0, blockLight.length);
        c.isPopulated = t.getBoolean("isPopulated");
        c.isLit = t.getBoolean("isLit");
        return c;
    }

    public static void byteToShortArray(byte[] blocks, short[] dst) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] = (short) ( (blocks[i*2+0]&0xFF) | ((blocks[i*2+1]&0xFF)<<8) );
        }
    }
    public static void readBlocks(byte[] blocks, short[] dst) {
        for (int i = 0; i < dst.length; i++) {
            short block = (short) ( (blocks[i*2+0]&0xFF) | ((blocks[i*2+1]&0xFF)<<8) );
            int iblock = block & Block.BLOCK_MASK;
            if (Block.get(iblock) == null) {
                dst[i] = 0;
            } else {
                dst[i] = block;
            }
        }
    }


    public static byte[] shortToByteArray(short[] blocks) {
        byte[] bytes = new byte[blocks.length*2];
        return shortToByteArray(blocks, bytes);
    }

    public static byte[] shortToByteArray(short[] blocks, byte[] bytes) {
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


    static byte[] temp = new byte[1024];
    /** MAKE SURE TO CALL THIS FROM A SINGLE THREAD ONLY **/
    public static void shortArrayFromStream(short[] data, DataInput in) throws IOException {
        in.readFully(temp, 0, data.length*2);
        byteToShortArray(temp, data);
        
    }

    /** MAKE SURE TO CALL THIS FROM A SINGLE THREAD ONLY **/
    public static void shorArrayToStream(short[] data, DataOutput out) throws IOException {
        shortToByteArray(data, temp);
        out.write(temp, 0, data.length>>1);
    }
}
