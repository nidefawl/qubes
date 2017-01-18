package nidefawl.qubes.network.client;

import java.io.IOException;
import java.net.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.MemoryConnection;
import nidefawl.qubes.network.TCPConnection;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketDisconnect;
import nidefawl.qubes.server.GameServer;

public class NetworkClient {

    private final ClientHandler handler;
    private final Connection    conn;
    public int netVersion = Packet.NET_VERSION;

    public NetworkClient(final String host, final short port) throws UnknownHostException, IOException {
        this(host, port, false);
    }
    public NetworkClient(final String host, final short port, boolean isLocalAttempt) throws UnknownHostException, IOException {
        Connection conn = null;
        if (isLocalAttempt) {
            try {
                conn = Game.instance.server.newMemoryConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (conn == null) {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), 10000);
            s.setSoTimeout(10000);
            conn = new TCPConnection(s);
        }
        this.conn = conn;
        this.handler = new ClientHandler(this);
        this.conn.startThreads();
    }
    

    public void update() {
        if (this.conn.isConnected()) {
            this.conn.validateConnection();
        }
        if (this.conn.isConnected()) {
            this.handler.handlePackets(this.conn.getIncoming());
            this.handler.update();
        }
        if (this.conn.finished()) {
            int from = this.conn.getDisconnectFrom();
            String reason = this.conn.getDisconnectReason();
            Packet p;
            while ((p = this.conn.pollPacket()) != null) {
                if (p instanceof PacketDisconnect) {
                    from = Connection.REMOTE;
                    reason = ((PacketDisconnect)p).message;
                }
            }
            handler.onDisconnect(from, reason);
            System.out.println(handler.getHandlerName()+" disconnected: "+reason);
        }
    }
    public void processLogin() {
        if (this.conn.isConnected()) {
            this.conn.validateConnection();
            final Packet p = this.conn.pollPacket();
            if (p != null) {
                p.handle(this.handler);
            }
        }
        if (this.conn.finished()) {
            int from = this.conn.getDisconnectFrom();
            String reason = this.conn.getDisconnectReason();
            Packet p;
            while ((p = this.conn.pollPacket()) != null) {
                if (p instanceof PacketDisconnect) {
                    from = Connection.REMOTE;
                    reason = ((PacketDisconnect)p).message;
                }
            }
            handler.onDisconnect(from, reason);
            System.out.println(handler.getHandlerName()+" disconnected: "+reason);
        }
    
    }
    public ClientHandler getClient() {
        return handler;
    }

	public boolean isConnected() {
		return this.conn.isConnected();
	}


	public void sendPacket(Packet packet) {
		this.conn.sendPacket(packet);
	}
	
	public void disconnect(String reason) {
	    this.conn.disconnect(Connection.LOCAL, reason);
	}


    public void onKick(int code, String message) {
        this.conn.disconnect(Connection.REMOTE, message);
    }

}
