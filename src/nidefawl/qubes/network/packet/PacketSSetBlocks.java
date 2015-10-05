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

    public PacketSSetBlocks(int id, int x, int z, short[] pos, short[] blocks, byte[] lights, short[] data) {
        super(id);
        this.chunkX = x;
        this.chunkZ = z;
        this.positions = pos;
        this.blocks = blocks;
        this.lights = lights;
        this.data = data;
    }

    public int     chunkX, chunkZ;
    public short[] positions;
    public short[] blocks;
    public byte[]  lights;
    public short[]  data;

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.chunkX = stream.readInt();
        this.chunkZ = stream.readInt();
        int len = stream.readInt();
        positions = new short[len];
        blocks = new short[len];
        data = new short[len];
        lights = new byte[len];
        for (int i = 0; i < len; i++) {
            positions[i] = stream.readShort();
            blocks[i] = stream.readShort();
            data[i] = stream.readShort();
            lights[i] = stream.readByte();
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(this.chunkX);
        stream.writeInt(this.chunkZ);
        int len = this.positions.length;
        stream.writeInt(len);
        for (int i = 0; i < len; i++) {
            stream.writeShort(this.positions[i]);
            stream.writeShort(this.blocks[i]);
            stream.writeShort(this.data[i]);
            stream.writeByte(this.lights[i]);
        }
    }

    @Override
    public int getID() {
        return 11;
    }

    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this))
        h.handleMultiBlock(this);
    }

}
