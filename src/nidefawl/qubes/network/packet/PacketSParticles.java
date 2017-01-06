package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.WorldServer;

public class PacketSParticles extends AbstractPacketWorldRef {
    public final BlockPos pos;
    public int type;
    public int arg;
    public PacketSParticles() {
        pos = new BlockPos();
    }
    public PacketSParticles(WorldServer worldServer, BlockPos pos, int type, int arg) {
        super(worldServer.getId());
        this.pos = pos;
        this.type = type;
        this.arg = arg;
    }

    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this)) {
            h.handleParticles(this);
        }
    }
    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        this.pos.write(stream);
        stream.writeByte(this.type);
        stream.writeShort(this.arg);
    }
    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.pos.read(stream);
        this.type = stream.readUnsignedByte();
        this.arg = stream.readUnsignedShort();
    }

}
