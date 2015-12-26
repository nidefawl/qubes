package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketAuth extends Packet {
	public String name;
    public boolean success;

	public PacketAuth() {
	}

	public PacketAuth(String name) {
		this.name = name;
	}

	/**
     * @param name2
     * @param b
     */
    public PacketAuth(String name, boolean success) {
        this.name = name;
        this.success = success;
    }

    @Override
	public void readPacket(DataInput stream) throws IOException {
		this.name = readString(stream);
		this.success = stream.readByte() != 0;
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
		writeString(this.name, stream);
		stream.writeByte(this.success ? 1 : 0);
	}

	@Override
	public void handle(Handler h) {
		h.handleAuth(this);
	}

}
