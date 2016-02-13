package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

public class PacketCSetProperty extends Packet {

    public int propVal;
    public int propId;
    public PacketCSetProperty() {
    }
    public PacketCSetProperty(int id, int val) {
        this.propId = id;
        this.propVal = val;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.propId = stream.readInt();
        this.propVal = stream.readInt();
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.propId);
        stream.writeInt(this.propVal);
    }

    @Override
    public void handle(Handler h) {
        h.handleSetProperty(this);
    }

}
