package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketSTrackChunk extends AbstractPacketWorldRef {
    public int x;
    public int z;
    public boolean add;

	public PacketSTrackChunk() {
	}

	public PacketSTrackChunk(int id, int x, int z, boolean add) {
	    super(id);
		this.x = x;
		this.z = z;
		this.add = add;
	}

	@Override
	public void readPacket(DataInput stream) throws IOException {
        super.readPacket(stream);
		this.x = stream.readInt();
		this.z = stream.readInt();
		this.add = stream.readByte() != 0;
		
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
        super.writePacket(stream);
        stream.writeInt(this.x);
        stream.writeInt(this.z);
        stream.writeByte(this.add?1:0);
	}

	@Override
	public void handle(Handler h) {
	    if (h.isValidWorld(this))
	        h.handleTrackChunk(this);
	}

}
