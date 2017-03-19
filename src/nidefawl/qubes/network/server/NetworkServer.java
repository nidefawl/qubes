package nidefawl.qubes.network.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.MemoryConnection;
import nidefawl.qubes.network.TCPConnection;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.server.GameServer;

public class NetworkServer {

    public volatile boolean isRunning;

    private final ListenThread listenThread;

    private final GameServer server;

    public long packetTimeout = 0;

    public long pingDelay = 1000;

    public int                                  netVersion    = Packet.NET_VERSION;
    private final ArrayList<ServerHandlerPlay>  handlersPlay  = Lists.newArrayList();
    private final ArrayList<ServerHandlerLogin> handlersLogin = Lists.newArrayList();

    public NetworkServer(GameServer server) throws Exception {
        this.server = server;
        this.listenThread = new ListenThread(this, this.server.getConfig());
        this.isRunning = true;
        this.packetTimeout = this.server.getConfig().packetTimeout;
    }

    public void startListener() {
        this.listenThread.start();
    }

    public void addConnection(final Socket s) throws IOException {

        final Connection c = new TCPConnection(s);
        c.startThreads();
        final ServerHandlerLogin loginHandler = new ServerHandlerLogin(this.server, this, c);
        this.handlersLogin.add(loginHandler);
        System.out.println("New connection: " + loginHandler.getHandlerName());
    }
    public Connection newMemoryConnection() throws IOException {
        MemoryConnection connOtherEnd = new MemoryConnection(true);
        MemoryConnection connHere = new MemoryConnection(false);
        connOtherEnd.setOtherEnd(connHere);
        connHere.setOtherEnd(connOtherEnd);
        final ServerHandlerLogin loginHandler = new ServerHandlerLogin(this.server, this, connHere);
        this.handlersLogin.add(loginHandler);
        System.out.println("New connection: " + loginHandler.getHandlerName());
        return connOtherEnd;
    }

    public void halt() {
        System.out.println("halting network server "+handlersLogin+","+handlersPlay);
        this.isRunning = false;
        this.listenThread.halt();
        for (int a = 0; a < this.handlersLogin.size(); a++) {
            final ServerHandlerLogin c = this.handlersLogin.get(a);
            c.kick("Server ended");
        }
        for (int a = 0; a < this.handlersPlay.size(); a++) {
            final ServerHandlerPlay c = this.handlersPlay.get(a);
            c.kick("Server ended");
        }
    }

    public void update() {
        for (int a = 0; a < this.handlersLogin.size(); a++) {
            ServerHandlerLogin loginHandler = null;
            try {
                loginHandler = this.handlersLogin.get(a);
                loginHandler.update();
                if (loginHandler.finished()) {
                    this.handlersLogin.remove(a--);
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    if (loginHandler != null) {
                        loginHandler.kick("Server error");
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        for (int a = 0; a < this.handlersPlay.size(); a++) {
            ServerHandlerPlay playerHandler = null;
            try {
                playerHandler = this.handlersPlay.get(a);
                playerHandler.update();
                if (playerHandler.finished()) {
                    this.handlersPlay.remove(a--);
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    if (playerHandler != null) {
                        playerHandler.kick("Server error");
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    public void addServerHandlerPlay(Player player, ServerHandlerLogin handlerLogin, ServerHandlerPlay handlerPlay) {
        handlersPlay.add(handlerPlay);
    }

    public ArrayList<ServerHandlerLogin> getLoginHandlers() {
        return handlersLogin;
    }
    public ArrayList<ServerHandlerPlay> getHandlers() {
        return handlersPlay;
    }
}
