package nidefawl.qubes.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.network.Handler;

public class PacketSSetBlocks extends AbstractPacketWorldRef {


    public PacketSSetBlocks() {
    }

    public PacketSSetBlocks(int id, int x, int z, short[] pos, short[] blocks, byte[] lights, short[] data, BlockData[] bdata) {
        super(id);
        this.chunkX = x;
        this.chunkZ = z;
        this.positions = pos;
        this.blocks = blocks;
        this.lights = lights;
        this.data = data;
        this.bdata = bdata;
        int l = 0;
        for (int i = 0; i < this.bdata.length; i++) {
            if (this.bdata[i] != null) {
                l++;
            }
        }
        this.numBlockData = l;
    }

    public int     chunkX, chunkZ;
    public short[] positions;
    public short[] blocks;
    public byte[]  lights;
    public short[]  data;
    public BlockData[] bdata;
    private int numBlockData;

    @Override
    public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
        this.chunkX = stream.readInt();
        this.chunkZ = stream.readInt();
        int len = stream.readInt(); //TODO: MAKE THIS SMALLER SO WE INDEX BLOCKDATA NOT BY INT
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
        this.numBlockData = stream.readInt();
        if (this.numBlockData > 0) {
            this.bdata = new BlockData[len];
            for (int i = 0; i < this.numBlockData; i++) {
                int idx = stream.readInt();
                int type = stream.readByte();
                int bdataelementlen = stream.readByte()*4;
                BlockData data = BlockData.fromType(type);
                if (data == null) {
                    stream.skipBytes(bdataelementlen);
                }
                data.readDataFromStream(stream);
                this.bdata[idx] = data;
            }
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
        stream.writeInt(this.numBlockData);
        for (int i = 0; i < len; i++) {
            if (this.bdata[i] != null) {
                stream.writeInt(i);
                stream.writeByte(this.bdata[i].getTypeId());
                int l = this.bdata[i].getLength();
                stream.writeByte(l/4);
                this.bdata[i].writeDataToStream(stream);
            }
        }
    }



    @Override
    public void handle(Handler h) {
        if (h.isValidWorld(this))
        h.handleMultiBlock(this);
    }

}
