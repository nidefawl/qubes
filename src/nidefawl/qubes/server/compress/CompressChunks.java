package nidefawl.qubes.server.compress;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.server.ChunkReader;
import nidefawl.qubes.network.packet.PacketSChunkData;
import nidefawl.qubes.network.server.ServerHandlerPlay;

public class CompressChunks implements ICompressTask {

    private Collection<Chunk> chunks;
    private int worldid;
    private int[][] coords;
    private int chunkLen;
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
        this.chunkLen = -1;
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
        if (this.hasLight)
        packet.flags |= 2;
        for (int i = 0; i < handlers.length; i++)
            this.handlers[i].sendPacket(packet);
    }

}