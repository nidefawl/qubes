package nidefawl.qubes.network.server;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.server.PlayerManager;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.WorldServer;

public class ServerHandlerPlay extends ServerHandler {
    protected final Player player;

    public ServerHandlerPlay(Player player, ServerHandlerLogin login) {
        super(login.server, login.netServer, login.conn);
        this.player = player;
        this.state = login.state;
    }

    @Override
    public void handlePing(PacketPing p) {

    }
    @Override
    public void update() {
        super.update();
        if (this.state == STATE_CONNECTED) {
            this.state = STATE_PLAYING;
            WorldServer world = (WorldServer) this.player.world;
            int flags = 0;
            if (this.player.flying) {
                flags |= 1;
            }
            sendPacket(new PacketSSpawnInWorld(world.getId(), this.player.pos, flags, world.getUUID(), world.getSeed(), world.getTime()));
        }
    }

    @Override
    public String getHandlerName() {
        return this.name;
    }
    
    @Override
    public void handleDisconnect(PacketDisconnect packetDisconnect) {
        this.conn.disconnect(Connection.REMOTE, packetDisconnect.message);
    }

    @Override
    public void onDisconnect(int from, String reason) {
        try {
            if (this.player != null) {
                PlayerManager mgr = this.server.getPlayerManager();
                mgr.removePlayer(this.player);
            }
        } catch (Exception e) {
            ErrorHandler.setException(new GameError("Failed removing player", e));
        }
    }

    @Override
    public void handleMovement(PacketCMovement packetMovement) {
        this.player.pos = packetMovement.pos;
        boolean hitground = (packetMovement.flags & 0x1) != 0;
        boolean fly = (packetMovement.flags & 0x2) != 0;
        this.player.hitGround = hitground;
        this.player.flying = fly;
    }

    public void sendPacket(Packet packet) {
        this.conn.sendPacket(packet);
    }

    public void handleSetBlock(PacketCSetBlock p) {
        if (p.y < 0 || p.y >= this.player.world.worldHeight)
            return;
        this.player.world.setType(p.x, p.y, p.z, p.type, Flags.MARK);

    }

    public void handleSetBlocks(PacketCSetBlocks p1) {

        int w = p1.x2 - p1.x + 1;
        int h = p1.y2 - p1.y + 1;
        int l = p1.z2 - p1.z + 1;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < l; z++) {
                    int blockX = p1.x + x;
                    int blockY = p1.y + y;
                    int blockZ = p1.z + z;
                    this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
                }
            }

        }
    }

    @Override
    public boolean isValidWorld(AbstractPacketWorldRef packet) {
        return packet.getWorldId() == this.player.world.getId();
    }

    public Player getPlayer() {
        return this.player;
    }

}
