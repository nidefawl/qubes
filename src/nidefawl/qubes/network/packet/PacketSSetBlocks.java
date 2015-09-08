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

    public PacketSSetBlocks(int id, int x, int z, short[] pos, short[] blocks, byte[] lights) {
        super(id);
        this.chunkX = x;
        this.chunkZ = z;
        this.positions = pos;
        this.blocks = blocks;
        this.lights = lights;

    }

    public int     chunkX, chunkZ;
    public short[] positions;
    public short[] blocks;
    public byte[]  lights;

    @Override
    public void readPacket(DataInput stream) throws IOException {
        this.chunkX = stream.readInt();
        this.chunkZ = stream.readInt();
        int len = stream.readInt();
        positions = new short[len];
        blocks = new short[len];
        lights = new byte[len];
        for (int i = 0; i < len; i++) {
            positions[i] = stream.readShort();
            blocks[i] = stream.readShort();
            lights[i] = stream.readByte();
        }
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.chunkX);
        stream.writeInt(this.chunkZ);
        int len = this.positions.length;
        stream.writeInt(len);
        for (int i = 0; i < len; i++) {
            stream.writeShort(this.positions[i]);
            stream.writeShort(this.blocks[i]);
            stream.writeByte(this.lights[i]);
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
