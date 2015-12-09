package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class AbstractPacketWorldRef extends Packet {

    public int worldID;

    public AbstractPacketWorldRef() {
    }

    public AbstractPacketWorldRef(int id) {
        this.worldID = id;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.worldID = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.worldID);
    }

    public int getWorldId() {
        return this.worldID;
    }

}
