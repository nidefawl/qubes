package nidefawl.qubes.network.client;

import java.io.IOException;

import nidefawl.qubes.Game;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.network.packet.PacketHandshake;
import nidefawl.qubes.util.GameError;

public class ThreadConnect implements Runnable {
    String          host;
    int             port;
    private Thread  thread;
    public IOException exception;
    public boolean finished;
    public boolean cancelled = false;
    public boolean connected;
    String stateStr = "Connecting...";

    public ThreadConnect(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        NetworkClient client = null;
        try {
            stateStr = "Connecting to "+host+":"+port;
            client = new NetworkClient(host, (short) port);
            client.sendPacket(new PacketHandshake(client.netVersion));
            ClientHandler handler = client.getClient();
            String dots = "...";
            while (client.isConnected() && !cancelled) {
                int ticks = Game.ticksran/10;
                stateStr = "Authenticating"+(dots.substring(0, 1+(ticks%3)));
                client.processLogin();
                Thread.sleep(50);
                if (handler.getState() == ClientHandler.STATE_CONNECTED) {
                    this.connected = true;
                    Game.instance.setConnection(client);
                    stateStr = "Connected!";
                    break;
                }
            }
        } catch (IOException e) {
            stateStr = "Failed connecting: "+e.getMessage();
            this.exception = e;
        } catch (Exception e) {
            ErrorHandler.setException(new GameError("Exception in connect thread", e));
        } finally {
            try {
                if (!connected && client != null) {
                    if (!cancelled)
                        stateStr = client.getClient().getDisconnectReason();
                    client.disconnect("Failed connecting to server");
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            finished = true;
        }
    }

    public void startThread() {
        thread = new Thread(this);
        thread.setName("NetworkConnectThread");
        thread.setDaemon(true);
        thread.start();
    }

    public String getState() {
        return stateStr;
    }
    public void cancel() {
        cancelled = true;
    }
}
