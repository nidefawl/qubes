package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketDisconnect extends Packet {
	public int code = 0;
	public String message = "";
	public PacketDisconnect() {
	}

	public PacketDisconnect(int i, String reason) {
		this.code = i;
		this.message = reason;
	}

	@Override
	public void readPacket(DataInput stream) throws IOException {
		this.code = stream.readInt();
		this.message = readString(stream);
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
		stream.writeInt(this.code);
		writeString(this.message, stream);
	}

	@Override
	public void handle(Handler h) {
	    h.handleDisconnect(this);
	}
}
