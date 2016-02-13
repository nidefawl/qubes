package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.util.ByteArrIO;

public class PacketSyncBlocks extends Packet {
    public short[] blockIds;
    
    
    
	public PacketSyncBlocks() {
	}

	public PacketSyncBlocks(short[] blockIds) {
	    this.blockIds = blockIds;
	}

    @Override
    public void readPacket(DataInput stream) throws IOException {
        int len = stream.readInt();
        if (len > Block.NUM_BLOCKS) {
            throw new IOException("Invalid packet len >= NUM_BLOCKS ("+len+" >= "+Block.NUM_BLOCKS+")");
        }
        this.blockIds = new short[len];
        byte[] byteData = new byte[this.blockIds.length*2];
        stream.readFully(byteData);
        ByteArrIO.byteToShortArray(byteData, this.blockIds);
        
    }

    @Override
    public void writePacket(DataOutput stream) throws IOException {
        stream.writeInt(this.blockIds.length);
	    byte[] data = ByteArrIO.shortToByteArray(this.blockIds);;
	    stream.write(data);
	}

	@Override
	public void handle(Handler h) {
		h.handleSync(this);
	}

}
