package nidefawl.qubes.network.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.packet.Packet;

public class NetworkClient {

    private final ClientHandler client;
    private final Connection    conn;
    public int netVersion = 1;

    public NetworkClient(final String host, final short port) throws UnknownHostException, IOException {
    	Socket s = new Socket();
    	s.connect(new InetSocketAddress(host, port), 500);
    	s.setSoTimeout(5000);
        this.conn = new Connection(s);
        this.client = new ClientHandler(this, this.conn);
        this.conn.setHandler(this.client);
        this.conn.startThreads();
    }
    

    public void update() {
        if (this.conn.isConnected()) {
            this.conn.update();
        }
        if (this.conn.finished()) {
            this.conn.disconnect("Disconnected");
        }
    
    }


	public boolean isConnected() {
		return this.conn.isConnected();
	}


	public void sendPacket(Packet packet) {
		this.conn.sendPacket(packet);
	}

}
