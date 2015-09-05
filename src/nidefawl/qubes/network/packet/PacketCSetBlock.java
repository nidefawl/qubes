package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

public class PacketCSetBlock extends AbstractPacketWorldRef {
    public PacketCSetBlock() {
    }

    public int x, y, z;
    public int type;

    public PacketCSetBlock(int id, int x, int y, int z, int type) {
        super(id);
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

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
        return 8;
    }

    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this)) {
            h.handleSetBlock(this);    
        }   
    }

}
