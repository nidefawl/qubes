package nidefawl.qubes.network.client;

import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.IHandler;
import nidefawl.qubes.network.packet.PacketDisconnect;
import nidefawl.qubes.network.packet.PacketHandshake;
import nidefawl.qubes.network.packet.PacketPing;

public class ClientHandler implements IHandler {
	final static int STATE_HANDSHAKE = 0;
	final static int STATE_VERSION = 1;
	final static int STATE_AUTH = 2;
	final static int STATE_CONNECTED = 3;

	int state = STATE_HANDSHAKE;

	public final NetworkClient client;
	private long time;
	private final Connection conn;
	public final static long timeout = 5000;

	public ClientHandler(final NetworkClient client, Connection conn) {
		this.client = client;
		this.time = System.currentTimeMillis();
		this.conn = conn;
	}

	@Override
	public boolean isServerSide() {
		return false;
	}

	@Override
	public void update() {
		if (state != STATE_CONNECTED) {
			if (System.currentTimeMillis() - time > timeout) {
				this.conn.disconnect("Packet timed out");
				return;
			}
		} else {
		}
	}

	@Override
	public void handlePing(PacketPing p) {
		this.conn.sendPacket(new PacketPing(p.time));
	}

	@Override
	public void handleHandshake(PacketHandshake packetHandshake) {
		if (this.state != STATE_HANDSHAKE) {
			this.conn.disconnect("Invalid packet");
			return;
		}
		if (packetHandshake.version != this.client.netVersion) {
			this.conn.disconnect("Invalid version. Client is on version "+this.client.netVersion+", Server on "+packetHandshake.version);
			return;
		}
		this.state = STATE_CONNECTED;
		this.time = System.currentTimeMillis();
	}

	@Override
	public String getHandlerName() {
		return "Client";
	}


	@Override
	public void handleDisconnect(PacketDisconnect packetDisconnect) {
		this.conn.disconnect("Server sent disconnect: "+packetDisconnect.message);
	}
}
