package nidefawl.qubes.network.server;

import java.net.InetSocketAddress;

import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketDisconnect;
import nidefawl.qubes.network.packet.PacketPing;
import nidefawl.qubes.server.GameServer;

public abstract class ServerHandler extends Handler {

    protected int                     state = STATE_HANDSHAKE;
    protected final Connection        conn;
    protected final InetSocketAddress addr;
    protected final String            handlerName;
    protected long                    time;
    protected GameServer              server;
    protected NetworkServer           netServer;

    public ServerHandler(GameServer server, NetworkServer netServer, Connection conn) {
        this.conn = conn;
        this.addr = this.conn.getAddr();
        this.handlerName = this.addr.getHostString();
        this.time = System.currentTimeMillis();
        this.server = server;
        this.netServer = netServer;
    }
    public InetSocketAddress getAddr() {
        return addr;
    }

    @Override
    public boolean isServerSide() {
        return true;
    }

    @Override
    public void update() {
        this.conn.validateConnection();
        int max = 10;
        Packet p;
        while ((p = this.conn.pollPacket()) != null) {
            p.handle(this);
            if (max--<=0)break;
        }
        if (state < STATE_CONNECTED) {
            if (System.currentTimeMillis() - time > netServer.packetTimeout) {
                this.conn.disconnect(Connection.LOCAL, "Packet timed out");
                return;
            }
        } else {
            if (System.currentTimeMillis() - time > netServer.pingDelay) {
                this.time = System.currentTimeMillis();
                sendPacket(new PacketPing(System.nanoTime()));
            }
        }
        if (this.conn.finished()) {
            int from = this.conn.getDisconnectFrom();
            String reason = this.conn.getDisconnectReason();
            while ((p = this.conn.pollPacket()) != null) {
                if (p instanceof PacketDisconnect) {
                    from = Connection.REMOTE;
                    reason = ((PacketDisconnect)p).message;
                }
            }
            onDisconnect(from, reason);
            System.out.println(getHandlerName()+" disconnected: "+reason);
        }
    }

    @Override
    public void handleDisconnect(PacketDisconnect packetDisconnect) {
        this.conn.disconnect(Connection.REMOTE, "Client sent disconnect: " + packetDisconnect.message);
    }

    @Override
    public void handlePing(PacketPing p) {

    }

    @Override
    public String getHandlerName() {
        return this.handlerName;
    }


    public void sendPacket(Packet packet) {
        this.conn.sendPacket(packet);
    }

    public void kick(String string) {
        this.conn.disconnect(Connection.LOCAL, string);
    }

    public boolean finished() {
        return this.conn.finished();
    }
}
