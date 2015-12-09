package nidefawl.qubes.network;

import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.qubes.network.packet.*;

public abstract class Handler {
    public final static int STATE_HANDSHAKE = 0;
    public final static int STATE_AUTH      = 1;
    public final static int STATE_SYNC = 2;
    public final static int STATE_CLIENT_SETTINGS = 3;
    public final static int STATE_CONNECTED = 4;
    public final static int STATE_PLAYING = 5;

    public abstract boolean isServerSide();

    public abstract void update();

    public abstract String getHandlerName();

    public abstract boolean isValidWorld(AbstractPacketWorldRef packet);

    public abstract void onDisconnect(int from, String reason);
    
    public void handleHandshake(PacketHandshake packetHandshake) {
    }

    public void handlePing(PacketPing p) {
    }


    public void handleDisconnect(PacketDisconnect packetDisconnect) {
    }
    
    public void handleSpawnInWorld(PacketSSpawnInWorld packetJoinGame) {
    }

    public void handleAuth(PacketAuth packetAuth) {
    }

    public void handleMovement(PacketCMovement packetMovement) {
    }


    public void handleChunkDataMulti(PacketSChunkData packetChunkDataMulti, int light) {
    }

    public void handleSetBlock(PacketCSetBlock packetCSetBlock) {
    }

    public void handleSetBlocks(PacketCSetBlocks packetCSetBlocks) {
        
    }

    public void handleBlock(PacketSSetBlock packetSSetBlock) {
    }

    public void handleMultiBlock(PacketSSetBlocks packetSSetBlocks) {
    }

    public void handlePackets(LinkedBlockingQueue<Packet> incoming) {
        Packet p;
        while ((p = incoming.poll()) != null) {
            p.handle(this);
        }
    }

    public void handleLightChunk(PacketSLightChunk packetSLightChunk) {
    }

    /**
     * @param packetSTrackChunk
     */
    public void handleTrackChunk(PacketSTrackChunk packetSTrackChunk) {
        
    }

    /**
     * @param packet
     */
    public void handleClientSettings(PacketCSettings packet) {
    }

    /**
     * @param packetCSwitchWorld
     */
    public void handleSwitchWorld(PacketCSwitchWorld packetCSwitchWorld) {
        
    }

    /**
     * @param packetChatMessage
     */
    public void handleChat(PacketChatMessage packetChatMessage) {
        
    }

    /**
     * @param packetChatChannels
     */
    public void handleChannels(PacketChatChannels packetChatChannels) {
        
    }

    /**
     * @param packetSWorldTime
     */
    public void handleWorldTime(PacketSWorldTime packetSWorldTime) {
        
    }

    /**
     * @param packetSTeleport
     */
    public void handleTeleport(PacketSTeleport packetSTeleport) {
        
    }

    /**
     * @param packetSyncBlocks
     */
    public void handleSync(PacketSyncBlocks packetSyncBlocks) {
    }

    /**
     * @param packetCTeleportAck
     */
    public void handleTeleportAck(PacketCTeleportAck packetCTeleportAck) {
        
    }

    /**
     * @param packetSEntityUnTrack
     */
    public void handleEntityUntrack(PacketSEntityUnTrack packetSEntityUnTrack) {
    }

    /**
     * @param packetSEntityTrack
     */
    public void handleEntityTrack(PacketSEntityTrack packetSEntityTrack) {
    }

    /**
     * @param packetSEntityMove
     */
    public void handleEntityMove(PacketSEntityMove packetSEntityMove) {
    }

    /**
     * @param packetSWorldBiomes
     */
    public void handleWorldBiomes(PacketSWorldBiomes packetSWorldBiomes) {
    }
}
