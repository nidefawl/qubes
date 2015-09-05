package nidefawl.qubes.network.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.network.Connection;
import nidefawl.qubes.server.GameServer;


public class NetworkServer {

	public volatile boolean                       isRunning;

    private final ListenThread                    listenThread;

    private final ArrayList<Connection>           connections    = new ArrayList<Connection>();

    private final LinkedBlockingQueue<Connection> newConnections = new LinkedBlockingQueue<Connection>();

	private final GameServer server;
	
    public long packetTimeout = 0;
	
    public long pingDelay = 1000;

	public int netVersion = 1;


    public NetworkServer(GameServer server) throws Exception {
        this.server = server;
        this.listenThread = new ListenThread(this, this.server.getConfig().port);
        this.isRunning = true;
        this.packetTimeout = this.server.getConfig().packetTimeout;
    }

	public void startListener() {
        this.listenThread.start();
	}

    public void addConnection(final Socket s) throws IOException {
        final Connection c = new Connection(s);
        final ServerHandler h = new ServerHandler(this.server, this, c);
        System.out.println("New connection: "+h.getHandlerName());
        c.setHandler(h);
        this.newConnections.add(c);
        c.startThreads();
    }

	public void halt() {
        this.isRunning = false;
        for (int a = 0; a < this.connections.size(); a++) {
            final Connection c = this.connections.get(a);
            c.disconnect(Connection.LOCAL, "Server ended");
        }
        Connection cNew = null;
        while ((cNew = this.newConnections.poll()) != null) {
            cNew.disconnect(Connection.LOCAL, "Server ended");
        }
		this.listenThread.halt();
	}


    public void update() {
        for (int a = 0; a < this.connections.size(); a++) {
            final Connection c = this.connections.get(a);
            try {
                c.update();
                if (c.finished()) {
                    c.onFinish();
                    this.connections.remove(a--);
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
        Connection cNew = null;
        while ((cNew = this.newConnections.poll()) != null) {
            this.connections.add(cNew);
        }
    }
}
