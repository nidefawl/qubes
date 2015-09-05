package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;
import nidefawl.qubes.vec.BlockPos;

public class PacketCSetBlocks extends AbstractPacketWorldRef {

    public int x, y, z;
    public int x2, y2, z2;
    public int type;
    public PacketCSetBlocks() {
    }

    public PacketCSetBlocks(int id, BlockPos pos1, BlockPos pos2, int type) {
        super(id);
        this.x = pos1.x;
        this.y = pos1.y;
        this.z = pos1.z;
        this.x2 = pos2.x;
        this.y2 = pos2.y;
        this.z2 = pos2.z;
        this.type = type;
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        x = stream.readInt();
        y = stream.readInt();
        z = stream.readInt();
        x2 = stream.readInt();
        y2 = stream.readInt();
        z2 = stream.readInt();
        type = stream.readInt();

    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(x);
        stream.writeInt(y);
        stream.writeInt(z);
        stream.writeInt(x2);
        stream.writeInt(y2);
        stream.writeInt(z2);
        stream.writeInt(type);
    }

    @Override
    public int getID() {
        return 9;
    }

    @Override
    public void handle(Handler h) {
        h.handleSetBlocks(this);
    }

}
