package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

public class PacketSSetBlock extends AbstractPacketWorldRef {
    public PacketSSetBlock() {
    }

    public PacketSSetBlock(int id, int x, int y, int z, int type) {
        super(id);
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

    public int x, y, z;
    public int type;

    @Override
    public void readPacket(DataInput stream) throws IOException {
        x = stream.readInt();
        y = stream.readInt();
        z = stream.readInt();
        type = stream.readInt();

    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(x);
        stream.writeInt(y);
        stream.writeInt(z);
        stream.writeInt(type);
    }

    @Override
    public int getID() {
        return 10;
    }

    @Override
    public void handle(Handler h) {
        h.handleBlock(this);
    }

}
