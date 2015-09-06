package nidefawl.qubes.network.server;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.PlayerManager;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.WorldServer;

public class ServerHandlerLogin extends ServerHandler {

    public ServerHandlerLogin(GameServer server, NetworkServer netServer, Connection conn) {
        super(server, netServer, conn);
    }

    @Override
    public void handleHandshake(final PacketHandshake packetHandshake) {
        if (this.state != STATE_HANDSHAKE) {
            this.conn.disconnect(Connection.LOCAL, "Invalid packet");
            return;
        }
        if (packetHandshake.version != this.netServer.netVersion) {
            this.conn.disconnect(Connection.LOCAL, "Invalid version. Server is on version " + this.netServer.netVersion);
            return;
        }
        this.state = STATE_AUTH;
        sendPacket(new PacketHandshake(this.netServer.netVersion));
        this.time = System.currentTimeMillis();
    }

    @Override
    public void onDisconnect(int from, String reason) {
    }

    @Override
    public void handleAuth(PacketAuth packetAuth) {
        if (this.state != STATE_AUTH) {
            this.conn.disconnect(Connection.LOCAL, "Invalid packet");
            return;
        }
        this.time = System.currentTimeMillis();
        this.state = STATE_CONNECTED;
        try {
            PlayerManager mgr = this.server.getPlayerManager();
            Player player = mgr.addPlayer(packetAuth.name);
            player.netHandler = new ServerHandlerPlay(player, this);
            this.netServer.addServerHandlerPlay(player, this, player.netHandler);
        } catch (Exception e) {
            this.conn.disconnect(Connection.LOCAL, "Failed loading player data");
            ErrorHandler.setException(new GameError("Failed adding player", e));
        }
    }

    @Override
    public boolean isValidWorld(AbstractPacketWorldRef packet) {
        return false;
    }
    
    @Override
    public boolean finished() {
        return this.state == STATE_CONNECTED || super.finished();
    }
    
    @Override
    public void handleDisconnect(PacketDisconnect packetDisconnect) {
        this.conn.disconnect(Connection.REMOTE, packetDisconnect.message);
    }

}
