package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

public class PacketCInvTransaction extends Packet {
    public int       id;
    public int       action;
    public PacketCInvTransaction() {
    }

    public PacketCInvTransaction(int id, int action) {
        super();
        this.id = id;
        this.action = action;
    }


    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.id = stream.readInt();
        this.action = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.id);
        stream.writeInt(this.action);
    }

    @Override
    public void handle(Handler h) {
        h.handleInvTransaction(this);
    }

}
