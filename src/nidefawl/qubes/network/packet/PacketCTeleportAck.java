package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

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
        this.sync = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(sync);
    }



    @Override
    public void handle(Handler h) {
        h.handleTeleportAck(this);   
    }

}
