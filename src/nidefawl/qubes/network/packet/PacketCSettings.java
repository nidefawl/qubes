package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketCSettings extends Packet {
    public int chunkLoadDistance;

	public PacketCSettings() {
	}

	public PacketCSettings(int chunkLoadDistance) {
		this.chunkLoadDistance = chunkLoadDistance;
	}

	@Override
	public void readPacket(DataInput stream) throws IOException {
		this.chunkLoadDistance = stream.readInt();
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
		stream.writeInt(this.chunkLoadDistance);
	}

	@Override
	public void handle(Handler h) {
		h.handleClientSettings(this);
	}

}
