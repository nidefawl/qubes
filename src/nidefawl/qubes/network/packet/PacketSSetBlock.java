package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

public class PacketSSetBlock extends AbstractPacketWorldRef {

    public PacketSSetBlock() {
    }

    public PacketSSetBlock(int id, int x, int y, int z, int type, int light) {
        super(id);
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.light = light;
    }

    public int x, y, z;
    public int type;
    private int light;

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        x = stream.readInt();
        y = stream.readInt();
        z = stream.readInt();
        type = stream.readInt();
        light = stream.readUnsignedByte();

    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(x);
        stream.writeInt(y);
        stream.writeInt(z);
        stream.writeInt(type);
        stream.writeByte(light);
    }



    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this))
        h.handleBlock(this);
    }

}
