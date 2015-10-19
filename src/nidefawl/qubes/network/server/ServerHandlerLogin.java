package nidefawl.qubes.network.server;

import java.util.Arrays;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chat.ChannelManager;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.PlayerManager;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.WorldServer;

public class ServerHandlerLogin extends ServerHandler {

    private String name;

    public ServerHandlerLogin(GameServer server, NetworkServer netServer, Connection conn) {
        super(server, netServer, conn);
    }

    @Override
    public void handleHandshake(final PacketHandshake packetHandshake) {
        if (this.state != STATE_HANDSHAKE) {
            this.conn.disconnect(Connection.LOCAL, "Invalid packet (CLIENT STATE_HANDSHAKE)");
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
            this.conn.disconnect(Connection.LOCAL, "Invalid packet (CLIENT STATE_AUTH)");
            return;
        }
        this.name = packetAuth.name;
        this.time = System.currentTimeMillis();
        this.state = STATE_SYNC;
        this.sendPacket(new PacketAuth(name, true));
    }
    
    @Override
    public void handleClientSettings(PacketCSettings packet) {
        if (this.state != STATE_CLIENT_SETTINGS) {
            this.conn.disconnect(Connection.LOCAL, "Invalid packet (CLIENT STATE_CLIENT_SETTINGS)");
            return;
        }
        this.state = STATE_CONNECTED;
        try {
            PlayerManager mgr = this.server.getPlayerManager();
            System.out.println(this.name);
            Player exist = mgr.getPlayer(this.name);
            if (exist != null) {
                exist.kick("Another player is using your account");
                this.kick("Another player is using your account");
                return;
            }
            
            Player player = mgr.addPlayer(this.name);
            player.netHandler = new ServerHandlerPlay(player, this);
            player.setChunkLoadDistance(packet.chunkLoadDistance);
            
            this.netServer.addServerHandlerPlay(player, this, player.netHandler);
            ChannelManager mgr2 = this.server.getChatChannelMgr();
            mgr2.addUser(player);
            this.time = System.currentTimeMillis();
            
        } catch (IllegalArgumentException e) {
            this.conn.disconnect(Connection.LOCAL, "Received invalid settings");
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
    @Override
    public void handleSync(PacketSyncBlocks packetAuth) {
        if (this.state != STATE_SYNC) {
            this.conn.disconnect(Connection.LOCAL, "Invalid packet (CLIENT STATE_SYNC)");
            return;
        }
        this.time = System.currentTimeMillis();
        this.state = STATE_CLIENT_SETTINGS;
        short[] recvd = packetAuth.blockIds;
        short[] data = Block.getRegisteredIDs();
        if (!Arrays.equals(recvd, data)) {
            this.conn.disconnect(Connection.LOCAL, "Registered blocks mismatch");
            return;
        }
        PacketSyncBlocks p = new PacketSyncBlocks(data);
        this.sendPacket(p);
    }

}
