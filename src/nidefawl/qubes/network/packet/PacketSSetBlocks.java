package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.network.Handler;

public class PacketSSetBlocks extends AbstractPacketWorldRef {
    public PacketSSetBlocks() {
    }

    public PacketSSetBlocks(int id, int x, int z) {
        super(id);
        this.chunkX = x;
        this.chunkZ = z;

    }

    public int         len;
    public int         chunkX, chunkZ;
    public List<Short> positions;
    public List<Short> types;

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.len = stream.readInt();
        this.chunkX = stream.readInt();
        this.chunkZ = stream.readInt();
        this.positions = Lists.newArrayList();
        this.types = Lists.newArrayList();
        for (int i = 0; i < this.len; i++) {
            this.positions.add(stream.readShort());
            this.types.add(stream.readShort());
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.len);
        stream.writeInt(this.chunkX);
        stream.writeInt(this.chunkZ);
        for (int i = 0; i < this.len; i++) {
            stream.writeShort(this.positions.get(i));
            stream.writeShort(this.types.get(i));
        }
    }

    @Override
    public int getID() {
        return 11;
    }

    @Override
    public void handle(Handler h) {
        h.handleMultiBlock(this);
    }

}
