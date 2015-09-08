package nidefawl.qubes.server.compress;

import java.util.Arrays;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.network.packet.PacketSLightChunk;
import nidefawl.qubes.network.server.ServerHandlerPlay;
import nidefawl.qubes.vec.BlockBoundingBox;

public class CompressLight implements ICompressTask {

    private Chunk chunks;
    private int worldid;
    private int chunkLen;
    private ServerHandlerPlay[] handlers;
    private int coordZ;
    private int coordX;
    private BlockBoundingBox box;

    public CompressLight(int worldid, Chunk chunks, BlockBoundingBox box, ServerHandlerPlay... handlers) {
        this.chunks = chunks;
        this.handlers = handlers;
        this.worldid = worldid;
        this.box = box;
    }

    @Override
    public int fill(byte[] tmpBuffer) {
        int offset = 0;
        this.chunkLen = -1;
        this.coordX = this.chunks.x;
        this.coordZ = this.chunks.z;
        byte[] light = this.chunks.getLights(this.box);
        System.arraycopy(light, 0, tmpBuffer, offset, light.length);
        this.chunkLen = light.length;
        return light.length;
    }

    @Override
    public void finish(byte[] compressed) {
        PacketSLightChunk packet = new PacketSLightChunk(this.worldid);
        packet.coordX = this.coordX;
        packet.coordZ = this.coordZ;
        packet.min = this.box.getMinHash();
        packet.max = this.box.getMaxHash();
        packet.data = compressed;
        for (int i = 0; i < handlers.length; i++)
            this.handlers[i].sendPacket(packet);
    }

}
