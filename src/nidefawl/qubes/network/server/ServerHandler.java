package nidefawl.qubes.network.server;

import java.net.InetSocketAddress;

import nidefawl.qubes.config.GameServer;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.IHandler;
import nidefawl.qubes.network.packet.*;

public class ServerHandler implements IHandler {
	final static int STATE_HANDSHAKE = 0;
	final static int STATE_VERSION = 1;
	final static int STATE_AUTH = 2;
	final static int STATE_CONNECTED = 3;

	int state = STATE_HANDSHAKE;
	public Connection conn;
	private final InetSocketAddress addr;
	private final String name;
	private long time;
	private GameServer server;
	private NetworkServer netServer;

	public ServerHandler(GameServer server, NetworkServer netServer, final Connection c) {
		this.conn = c;
		this.addr = this.conn.getAddr();
		this.name = this.addr.getHostString();
		this.time = System.currentTimeMillis();
		this.server = server;
		this.netServer = netServer;
	}

	@Override
	public boolean isServerSide() {
		return true;
	}

	@Override
	public void handleHandshake(final PacketHandshake packetHandshake) {
		if (this.state != STATE_HANDSHAKE) {
			this.conn.disconnect("Invalid packet");
			return;
		}
		if (packetHandshake.version != this.netServer.netVersion) {
			this.conn.disconnect("Invalid version. Server is on version "+this.netServer.netVersion);
			return;
		}
		this.state = STATE_CONNECTED;
		this.conn.sendPacket(new PacketHandshake(this.netServer.netVersion));
		this.time = System.currentTimeMillis();
	}

	@Override
	public void update() {
		if (state != STATE_CONNECTED) {
			if (System.currentTimeMillis() - time > netServer.packetTimeout) {
				this.conn.disconnect("Packet timed out");
				return;
			}
		} else {
			if (System.currentTimeMillis() - time > netServer.pingDelay) {
				this.time = System.currentTimeMillis();
	            this.conn.sendPacket(new PacketPing(System.nanoTime()));
			}
		}
	}

	@Override
	public void handlePing(PacketPing p) {
		
	}


	@Override
	public String getHandlerName() {
		return this.name;
	}

	@Override
	public void handleDisconnect(PacketDisconnect packetDisconnect) {
		System.out.println("client send disconnect");
		this.conn.disconnect("Client sent disconnect: "+packetDisconnect.message);
	}
}
