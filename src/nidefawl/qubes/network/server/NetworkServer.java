package nidefawl.qubes.network.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.ReaderThread;
import nidefawl.qubes.network.WriterThread;
import nidefawl.qubes.server.GameServer;

public class NetworkServer {

    public volatile boolean isRunning;

    private final ListenThread listenThread;

    private final GameServer server;

    public long packetTimeout = 0;

    public long pingDelay = 1000;

    public int                                  netVersion    = 1;
    private final ArrayList<ServerHandlerPlay>  handlersPlay  = Lists.newArrayList();
    private final ArrayList<ServerHandlerLogin> handlersLogin = Lists.newArrayList();

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
        c.startThreads();
        final ServerHandlerLogin loginHandler = new ServerHandlerLogin(this.server, this, c);
        this.handlersLogin.add(loginHandler);
        System.out.println("New connection: " + loginHandler.getHandlerName());
    }

    public void halt() {
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
            try {
                ServerHandlerLogin loginHandler = this.handlersLogin.get(a);
                loginHandler.update();
                if (loginHandler.finished()) {
                    this.handlersLogin.remove(a--);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (int a = 0; a < this.handlersPlay.size(); a++) {
            try {
                ServerHandlerPlay playerHandler = this.handlersPlay.get(a);
                playerHandler.update();
                if (playerHandler.finished()) {
                    this.handlersPlay.remove(a--);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
