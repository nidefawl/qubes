package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketPing extends Packet {
	public long time;

	public PacketPing() {
	}

	public PacketPing(long time) {
		this.time = time;
	}

	@Override
	public void readPacket(DataInput stream) throws IOException {
		this.time = stream.readLong();
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
		stream.writeLong(this.time);
	}

	@Override
	public int getID() {
		return 1;
	}

	@Override
	public void handle(Handler h) {
		h.handlePing(this);
	}

}
