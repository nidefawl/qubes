package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.Handler;

public class PacketSChunkData extends AbstractPacketWorldRef {

    public int len;
    public byte[] blocks;
    public int flags;
    public int[][] coords;
    public PacketSChunkData() {
    }
    public PacketSChunkData(int id) {
        super(id);
    }

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.flags = stream.readUnsignedByte();
        this.len = stream.readInt();
        this.coords = new int[len][];
        for (int i = 0; i < len; i++) {
            this.coords[i] = new int[] { stream.readInt(), stream.readInt() };
        }
        int len = stream.readInt();
        this.blocks = new byte[len];
        stream.readFully(this.blocks);
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeByte(this.flags);
        stream.writeInt(this.len);
        for (int i = 0; i < len; i++) {
            stream.writeInt(this.coords[i][0]);
            stream.writeInt(this.coords[i][1]);
        }
        stream.writeInt(this.blocks.length);
        stream.write(this.blocks);
    }



    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this)) {
            try {

                h.handleChunkDataMulti(this, this.flags);    
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
