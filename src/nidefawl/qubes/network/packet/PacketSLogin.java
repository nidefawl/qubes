package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketSLogin extends Packet {

	public PacketSLogin() {
	}


    @Override
	public void readPacket(DataInput stream) throws IOException {
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
	}

	@Override
	public void handle(Handler h) {
		h.handleLogin(this);
	}

}
