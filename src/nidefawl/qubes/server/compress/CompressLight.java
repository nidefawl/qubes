package nidefawl.qubes.server.compress;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.network.packet.PacketSLightChunk;
import nidefawl.qubes.network.server.ServerHandlerPlay;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;
import nidefawl.qubes.vec.BlockBoundingBox;
import nidefawl.qubes.world.World;

@SideOnly(value = Side.SERVER)
public class CompressLight implements ICompressTask {

    private Chunk chunks;
    private int worldid;
    private int chunkLen;
    private ServerHandlerPlay[] handlers;
    private int coordZ;
    private int coordX;
    private BlockBoundingBox box;
    private int compressionLvl;

    public CompressLight(int worldid, Chunk chunks, BlockBoundingBox box, int compressionLvl, ServerHandlerPlay... handlers) {
        this.chunks = chunks;
        this.handlers = handlers;
        this.worldid = worldid;
        this.box = box;
        this.compressionLvl = compressionLvl;
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
    public void finish(byte[] compressed, int compression) {
        PacketSLightChunk packet = new PacketSLightChunk(this.worldid);
        packet.flags = 0;
        if (compression > 0)
            packet.flags |= 1;
        packet.coordX = this.coordX;
        packet.coordZ = this.coordZ;
        packet.min = this.box.getMinHash();
        packet.max = this.box.getMaxHash();
        packet.data = compressed;
        for (int i = 0; i < handlers.length; i++) {
            ServerHandlerPlay h = handlers[i];
            if (h == null || h.finished()) {
                continue;
            }
            PlayerServer p = h.getPlayer();
            if (p == null) {
                continue;
            }
            World w = p.getWorld();
            if (w == null) {
                continue;
            }
            if (w.getId() == this.worldid) {
                h.sendPacket(packet);
            }
        }
    }

    
    @Override
    public boolean isValid() {
        for (int i = 0; i < handlers.length; i++) {
            ServerHandlerPlay h = handlers[i];
            if (h == null || h.finished()) {
                continue;
            }
            PlayerServer p = h.getPlayer();
            if (p == null) {
                continue;
            }
            World w = p.getWorld();
            if (w == null) {
                continue;
            }
            if (w.getId() == this.worldid) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCompression() {
        return this.compressionLvl;
    }

}
