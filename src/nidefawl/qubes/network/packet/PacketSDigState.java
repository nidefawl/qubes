package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

public class PacketSDigState extends AbstractPacketWorldRef {
    
    public PacketSDigState() {
    }

    public int stage;
    
    public PacketSDigState(int worldid, int stage) {
        super(worldid);
        this.stage = stage;
    }
    
    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.stage = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(this.stage);
    }



    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this))
            h.handleServerDigState(this);
    }

}
