package nidefawl.qubes.server.compress;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.chunk.blockdata.BlockDataSliced;
import nidefawl.qubes.chunk.server.ChunkReader;
import nidefawl.qubes.network.packet.PacketSChunkData;
import nidefawl.qubes.network.server.ServerHandlerPlay;
import nidefawl.qubes.util.ByteArrIO;

public class CompressChunks implements ICompressTask {

    private Collection<Chunk> chunks;
    private int worldid;
    private int[][] coords;
    private ServerHandlerPlay[] handlers;
    private boolean hasLight;

    public CompressChunks(int worldid, Collection<Chunk> chunks, ServerHandlerPlay[] handlers, boolean hasLight) {
        this.chunks = chunks;
        this.handlers = handlers;
        this.worldid = worldid;
        this.hasLight = hasLight;
    }


    public static void shortToByteArray(short[] src, byte[] dst, int offset) {
        for (int i = 0; i < src.length; i++) {
            dst[offset+i*2+0] = (byte) (src[i]&0xFF);
            dst[offset+i*2+1] = (byte) ((src[i]>>8)&0xFF);
        }
    }

    @Override
    public int fill(byte[] tmpBuffer) {
        int offset = 0;
        int idx = 0;
        this.coords = new int[chunks.size()][];
        for (Chunk c : chunks) {
            this.coords[idx++] = new int[] { c.x, c.z};
            short[] blocks = c.getBlocks();
            shortToByteArray(blocks, tmpBuffer, offset);
            offset += blocks.length*2;
            if (hasLight) {
                byte[] light = c.getBlockLight();
                System.arraycopy(light, 0, tmpBuffer, offset, light.length);
//              Arrays.fill(tmpBuffer, offset, offset+light.length, (byte)0xFF);
                offset += light.length;
            }
            byte[] biomes = c.biomes;
            System.arraycopy(biomes, 0, tmpBuffer, offset, biomes.length);
            offset += biomes.length;
            byte[] waterMask = c.waterMask;
            System.arraycopy(waterMask, 0, tmpBuffer, offset, waterMask.length);
            offset += waterMask.length;
            short[][] heightArrays = c.blockMetadata.getArrays();
            short slices = 0;
            for (int i = 0; i < heightArrays.length; i++) {
                short[] slice = heightArrays[i];
                if (slice != null) {
                    slices |= (1<<i);
                }
            }
            tmpBuffer[offset+0] = (byte) (slices&0xFF);
            tmpBuffer[offset+1] = (byte) ((slices>>8)&0xFF);
            offset+=2;
            for (int i = 0; i < heightArrays.length; i++) {
                short[] slice = heightArrays[i];
                if (slice != null) {
                    shortToByteArray(slice, tmpBuffer, offset);
                    offset += slice.length*2;
                }
            }
            BlockData[][] dataSlices = c.blockData.getArrays();
            slices = 0;
            for (int i = 0; i < dataSlices.length; i++) {
                if (dataSlices[i] != null) {
                    slices |= (1<<i);
                }
            }
            tmpBuffer[offset+0] = (byte) (slices&0xFF);
            tmpBuffer[offset+1] = (byte) ((slices>>8)&0xFF);
            offset+=2;
            for (int i = 0; i < dataSlices.length; i++) {
                BlockData[] slice = dataSlices[i];
                if (slice != null) {
                    int n = 0;
                    int len = 0;
                    for (int a = 0; a < slice.length; a++) {
                        if (slice[a] != null) {
                            n++;
                            len += slice[a].getLength();
                        }
                    }
                    len += BlockData.HEADER_SIZE*n;
                    offset += ByteArrIO.writeInt(tmpBuffer, offset, len);
                    offset += ByteArrIO.writeShort(tmpBuffer, offset, n);
                    for (int a = 0; a < slice.length; a++) {
                        if (slice[a] != null) {
                            offset += ByteArrIO.writeShort(tmpBuffer, offset, a);
                            offset += slice[a].writeHeader(tmpBuffer, offset);
                            offset += slice[a].writeData(tmpBuffer, offset);
                        }
                    }
                }
            }
        }
        return offset;
    }

    @Override
    public void finish(byte[] compressed) {
        PacketSChunkData packet = new PacketSChunkData(this.worldid);
        packet.blocks = compressed;
        packet.len = this.chunks.size();
        packet.coords = this.coords;
        packet.flags |= 1;
        if (this.hasLight)
        packet.flags |= 2;
        for (int i = 0; i < handlers.length; i++)
            this.handlers[i].sendPacket(packet);
    }

}
