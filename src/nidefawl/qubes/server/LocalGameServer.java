package nidefawl.qubes.server;

import java.io.IOException;

import nidefawl.qubes.config.ServerConfig;
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
            System.out.println("First halt previous session server");
            serv.stopServer(); 
        }
        serv = this.server = new GameServer(); 
        serv.config.port=0;
        serv.config.listenAddr="localhost";
        serv.startServer();
    }
    public String getLocalAdress() {
        GameServer serv = this.server;
        if (serv != null) {
            ServerConfig config = serv.config;
            String addr = "";
            addr += config.listenAddr;
            addr +=":";
            addr += config.port;
            return addr;
        }
        return null;
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
        if (this.shutdownServ != null) {
            if (this.shutdownServ.isFinished()) {
                this.shutdownServ = null;
                return true;
            }
            return false;
        }
        if (this.server != null)
            return false;
        return this.server == null;
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
