package nidefawl.qubes.server;

import java.io.IOException;

import nidefawl.qubes.Game;
import nidefawl.qubes.network.Connection;

public class LocalGameServer {

    GameServer server;
    private GameServer shutdownServ;
    
    public LocalGameServer() {
        this.server = new GameServer();
    }
    public boolean getStatus() {
        GameServer serv = this.server;
        return serv != null && serv.isRunning();
    }
    public void start() {
        GameServer serv = this.server;
        if (serv != null) {
            serv.stopServer(); 
        }
        serv = this.server = new GameServer(); 
        serv.config.port=21087;
        serv.config.listenAddr="localhost";
        serv.startServer();
    }
    public void stop() {
        GameServer serv = this.server;
        if (serv != null) {
            this.shutdownServ = serv;
            serv.stopServer(); 
        }
        this.server = null;
    }
    public boolean isShutdownDone() {
        GameServer serv = this.shutdownServ;
        if (serv != null) {
            return this.shutdownServ.isFinished();
        }
        return true;
    }
    public Connection newMemoryConnection() throws IOException {
        GameServer serv = this.server;
        if (serv != null) {
            return serv.networkServer.newMemoryConnection();
        }
        return null;
    }
    public boolean isReady() {
        GameServer serv = this.server;
        if (serv != null) {
            return serv.isListening();
        }
        return false;
    }
}
