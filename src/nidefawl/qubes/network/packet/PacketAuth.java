package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketAuth extends Packet {
	public String name;

	public PacketAuth() {
	}

	public PacketAuth(String name) {
		this.name = name;
	}

	@Override
	public void readPacket(DataInput stream) throws IOException {
		this.name = readString(stream);
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
		writeString(this.name, stream);
	}

	@Override
	public int getID() {
		return 4;
	}

	@Override
	public void handle(Handler h) {
		h.handleAuth(this);
	}

}
