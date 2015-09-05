package nidefawl.qubes.server.compress;

import java.util.Collection;
import java.util.Set;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.server.ChunkReader;
import nidefawl.qubes.network.packet.PacketSChunkData;
import nidefawl.qubes.network.server.ServerHandler;

public class CompressChunks implements ICompressTask {

    private Collection<Chunk> chunks;
    private int worldid;
    private int[][] coords;
    private int chunkLen;
    private ServerHandler[] handlers;

    public CompressChunks(int worldid, Collection<Chunk> chunks, ServerHandler... handlers) {
        this.chunks = chunks;
        this.handlers = handlers;
        this.worldid = worldid;
    }


    public static int shortToByteArray(short[] src, byte[] dst, int offset) {
        int i = 0;
        for (; i < src.length; i++) {
            dst[offset+i*2+0] = (byte) (src[i]&0xFF);
            dst[offset+i*2+1] = (byte) ((src[i]>>8)&0xFF);
        }
        return offset+i*2;
    }

    @Override
    public int fill(byte[] tmpBuffer) {
        int offset = 0;
        int idx = 0;
        this.chunkLen = -1;
        this.coords = new int[chunks.size()][];
        for (Chunk c : chunks) {
            this.coords[idx++] = new int[] { c.x, c.z};
            short[] blocks = c.getBlocks();
            offset = shortToByteArray(blocks, tmpBuffer, offset);
            if (idx == 1) {
                this.chunkLen = offset;
            }
        }
        return offset;
    }

    @Override
    public void finish(byte[] compressed) {
        PacketSChunkData packet = new PacketSChunkData(this.worldid);
        packet.blocks = compressed;
        packet.len = this.chunks.size();
        packet.chunkLen = this.chunkLen;
        packet.coords = this.coords;
        packet.flags |= 1;
        for (int i = 0; i < handlers.length; i++)
            this.handlers[i].sendPacket(packet);
    }

}
