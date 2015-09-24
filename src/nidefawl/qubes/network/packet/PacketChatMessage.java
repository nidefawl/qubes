package nidefawl.qubes.network.packet;

import java.io.*;

import nidefawl.qubes.network.Handler;

public class PacketChatMessage extends Packet {
    public String channel;
    public String message;

	public PacketChatMessage() {
	}

	public PacketChatMessage(String channel, String message) {
	    this.channel = channel;
	    this.message = message;
	}

	@Override
	public void readPacket(DataInput stream) throws IOException {
		this.channel = readString(stream, 32);
		this.message = readString(stream, 1024);
	}

	@Override
	public void writePacket(DataOutput stream) throws IOException {
        writeString(this.channel, stream);
        writeString(this.message, stream);
	}

	@Override
	public int getID() {
		return 16;
	}

	@Override
	public void handle(Handler h) {
		h.handleChat(this);
	}

}
