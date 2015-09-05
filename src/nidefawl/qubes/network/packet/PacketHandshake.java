package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketHandshake extends Packet {
	public int version;

	public PacketHandshake() {
	}
	
	public PacketHandshake(int version) {
		this.version = version;
	}

	@Override
	public void readPacket(DataInput stream) throws IOException {
		this.version = stream.readInt();
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
		stream.writeInt(this.version);
	}

	@Override
	public int getID() {
		return 2;
	}

	@Override
	public void handle(Handler h) {
		h.handleHandshake(this);
	}

}
