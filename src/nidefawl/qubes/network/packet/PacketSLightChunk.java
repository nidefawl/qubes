package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

public class PacketSLightChunk extends AbstractPacketWorldRef {

    public int coordX, coordZ;
    public byte[] data;
    public short min, max;
    public PacketSLightChunk() {
    }
    public PacketSLightChunk(int id) {
        super(id);
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.coordX = stream.readInt();
        this.coordZ = stream.readInt();
        this.min = stream.readShort();
        this.max = stream.readShort();
        int len = stream.readUnsignedShort();
        this.data = new byte[len];
        stream.readFully(this.data);
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(this.coordX);
        stream.writeInt(this.coordZ);
        stream.writeShort(this.min);
        stream.writeShort(this.max);
        stream.writeShort(this.data.length);
        stream.write(this.data);
    }



    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this)) {
            h.handleLightChunk(this);    
        }
    }

}
