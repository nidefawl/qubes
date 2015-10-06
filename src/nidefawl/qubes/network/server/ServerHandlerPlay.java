package nidefawl.qubes.network.server;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chat.ChannelManager;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.server.PlayerManager;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.World;
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
        if (this.state == STATE_CONNECTED) {
            this.state = STATE_PLAYING;
            WorldServer world = this.server.getWorld(player.spawnWorld);
            int flags = 0;
            if (this.player.flying) {
                flags |= 1;
            }
            sendPacket(new PacketSSpawnInWorld(world.settings, this.player.pos, flags));
            world.addPlayer(player);
            sendPacket(new PacketChatChannels(this.player.getJoinedChannels()));
        }
        super.update();
    }

    @Override
    public void handleSwitchWorld(PacketCSwitchWorld packetCSwitchWorld) {
        int idx = packetCSwitchWorld.flags;
        WorldServer[] worlds = this.server.getWorlds();
        if (idx<0||idx>=worlds.length) idx = 0;
        WorldServer worldCurrent = (WorldServer) this.player.world;
        worldCurrent.removePlayer(this.player);
        WorldServer world = worlds[idx];
        int flags = 0;
        if (this.player.flying) {
            flags |= 1;
        }
        sendPacket(new PacketSSpawnInWorld(world.settings, this.player.pos, flags));
        world.addPlayer(player);
        player.sendMessage("You are now in world "+world.getName());
    }

    @Override
    public String getHandlerName() {
        return this.handlerName;
    }
    
    @Override
    public void handleDisconnect(PacketDisconnect packetDisconnect) {
        this.conn.disconnect(Connection.REMOTE, packetDisconnect.message);
    }

    @Override
    public void onDisconnect(int from, String reason) {
        try {
            PlayerManager mgr = this.server.getPlayerManager();
            mgr.removePlayer(this.player);
            WorldServer world = (WorldServer) this.player.world;
            if (world != null) {
                world.removePlayer(this.player);
            }
            ChannelManager mgr2 = this.server.getChatChannelMgr();
            mgr2.removeUser(player);
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
        this.player.yaw = packetMovement.yaw;
        this.player.pitch = packetMovement.pitch;
    }

    public void sendPacket(Packet packet) {
        this.conn.sendPacket(packet);
    }

    public void handleSetBlock(PacketCSetBlock p) {
        player.blockPlace.tryPlace(p.pos, p.fpos, p.type, p.data, p.face);
        
    }

    public void handleSetBlocks(PacketCSetBlocks p1) {

        boolean hollow = (p1.flags & 0x1) != 0;
        int w = p1.x2 - p1.x + 1;
        int h = p1.y2 - p1.y + 1;
        int l = p1.z2 - p1.z + 1;
        if (hollow) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    {
                        int blockX = p1.x+x;
                        int blockY = p1.y+y;
                        int blockZ = p1.z ;
                        this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
                    }
                    {
                        int blockX = p1.x + x;
                        int blockY = p1.y + y;
                        int blockZ = p1.z + l-1;
                        this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
                    }
                }
            }
            for (int z = 0; z < l; z++) {
                for (int y = 0; y < h; y++) {
                    {
                        int blockX = p1.x;
                        int blockY = p1.y+y;
                        int blockZ = p1.z+z;
                        this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
                    }
                    {
                        int blockX = p1.x + w-1;
                        int blockY = p1.y + y;
                        int blockZ = p1.z + z;
                        this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
                    }
                }
            }
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < l; z++) {
//                    {
//                        int blockX = p1.x+x;
//                        int blockY = p1.y;
//                        int blockZ = p1.z+z;
//                        this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
//                    }
                    {
                        int blockX = p1.x + x;
                        int blockY = p1.y + h-1;
                        int blockZ = p1.z + z;
                        this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
                    }
                }
            }
        } else {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < l; z++) {
                    for (int y = 0; y < h; y++) {
                        if (hollow) {
                            if (x != 0 && x != w-1)
                                continue;
                            if (y != 0 && y != h-1)
                                continue;
                            if (z != 0 && z != l-1)
                                continue;
                        }
                        int blockX = p1.x + x;
                        int blockY = p1.y + y;
                        int blockZ = p1.z + z;
                        this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
//                        this.player.world.setData(blockX, blockY, blockZ, 2, Flags.MARK);
                    }
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

    public void handleChat(PacketChatMessage c) {
        String channel = c.channel.trim();
        String msg = c.message.trim();
        if (msg.startsWith("/")) {
            msg = msg.substring(1);
            this.server.getCommandHandler().handle(this.player, msg);
            return;
        }
        this.server.getChatChannelMgr().handlePlayerChat(this.player, channel, msg);
    }
}
