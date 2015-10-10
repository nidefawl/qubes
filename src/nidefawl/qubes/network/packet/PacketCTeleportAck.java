package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class PacketCTeleportAck extends AbstractPacketWorldRef {
    public PacketCTeleportAck() {
    }
    public int sync;

    public PacketCTeleportAck(int id, int sync) {
        super(id);
        this.sync = sync;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        sync = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(sync);
    }

    @Override
    public int getID() {
        return 21;
    }

    @Override
    public void handle(Handler h) {
        h.handleTeleportAck(this);   
    }

}
