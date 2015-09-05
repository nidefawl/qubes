package nidefawl.qubes.network.client;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.client.ChunkManagerClient;
import nidefawl.qubes.chunk.server.ChunkReader;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.TripletShortHash;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.WorldClient;
import nidefawl.qubes.world.WorldSettingsClient;

public class ClientHandler extends Handler {

    int state = STATE_HANDSHAKE;

    public final NetworkClient client;
    private long               time;
    private final Connection   conn;
    public final static long   timeout = 5000;

    public ClientHandler(final NetworkClient client, Connection conn) {
        this.client = client;
        this.time = System.currentTimeMillis();
        this.conn = conn;
    }

    @Override
    public boolean isServerSide() {
        return false;
    }

    @Override
    public void update() {
        if (state != STATE_CONNECTED) {
            if (System.currentTimeMillis() - time > timeout) {
                this.conn.disconnect(Connection.LOCAL, "Packet timed out");
                return;
            }
        } else {
        }
    }

    @Override
    public void handlePing(PacketPing p) {
        this.conn.sendPacket(new PacketPing(p.time));
    }

    @Override
    public void handleHandshake(PacketHandshake packetHandshake) {
        if (this.state != STATE_HANDSHAKE) {
            this.conn.disconnect(Connection.LOCAL, "Invalid packet");
            return;
        }
        if (packetHandshake.version != this.client.netVersion) {
            this.conn.disconnect(Connection.LOCAL, "Invalid version. Client is on version " + this.client.netVersion + ", Server on " + packetHandshake.version);
            return;
        }
        this.state = STATE_AUTH;
        this.time = System.currentTimeMillis();
        this.conn.sendPacket(new PacketAuth(Game.instance.getProfile().getName()));
    }

    @Override
    public String getHandlerName() {
        return "Client";
    }

    @Override
    public void handleDisconnect(PacketDisconnect packetDisconnect) {
        this.conn.disconnect(Connection.REMOTE, "Server sent disconnect: " + packetDisconnect.message);
    }

    @Override
    public void onFinish() {
        System.out.println("Lost connection: " + getHandlerName());
    }

    public int getState() {
        return this.state;
    }

    int disconnectFrom   = -1;
    String disconnectReason = null;

    private WorldClient world;

    private PlayerSelf player;

    private ChunkManagerClient chunkManager;

    @Override
    public void onDisconnect(int from, String reason) {
        if (disconnectFrom < 0 || from == Connection.REMOTE) {
            this.disconnectFrom = from;
            this.disconnectReason = reason;
        }
    }
    public String getDisconnectReason() {
        String str = disconnectReason;
        this.disconnectReason = null;
        return str;
    }

    @Override
    public void handleJoinGame(PacketSSpawnInWorld packetJoinGame) {
        if (this.state != STATE_AUTH) {
            this.conn.disconnect(Connection.LOCAL, "Invalid packet");
            return;
        }
        this.state = STATE_CONNECTED;
        this.time = System.currentTimeMillis();
        this.world = new WorldClient(new WorldSettingsClient(packetJoinGame.id, packetJoinGame.uuid, packetJoinGame.seed, packetJoinGame.time));
        this.chunkManager = (ChunkManagerClient) this.world.getChunkManager();
        this.player = new PlayerSelf(this, Game.instance.getProfile().getName());
        this.player.setFly((packetJoinGame.flags & 0x1) != 0);
        this.world.addEntity(player);
        this.player.move(packetJoinGame.pos);
        Game.instance.setWorld(this.world);
        Game.instance.setPlayer(player);
    }

    public void sendPacket(Packet packet) {
        this.conn.sendPacket(packet);
    }

    public static short[] byteToShortArray(byte[] blocks, int offset, int len) {
        short[] shorts = new short[len/2];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) ( (blocks[offset+i*2+0]&0xFF) | ((blocks[offset+i*2+1]&0xFF)<<8) );
        }
        return shorts;
    }

    @Override
    public void handleChunkDataMulti(PacketSChunkData packet, boolean b) {
        int[][] coords = packet.coords;
        int len = coords.length;
        byte[] decompressed = inflate(packet.blocks);
        int offset = 0;
        int singleChunkByteSize = packet.chunkLen;
        for (int i = 0; i < len; i++) {
            int[] pos = coords[i];
            Chunk c = this.chunkManager.getOrMake(pos[0], pos[1]);
            if (c == null) {
                System.err.println("Failed recv. getOrMake returned null for chunk position "+pos[0]+"/"+pos[1]);
                offset += singleChunkByteSize;
                continue;
            }
            short[] blocks = byteToShortArray(decompressed, offset, singleChunkByteSize);
            offset += singleChunkByteSize;
            c.setBlocks(blocks);
            Engine.regionRenderer.flagChunk(c.x, c.z);
        }
    }

    //TODO: move decompression to thread
    final Inflater inflate = new Inflater();
    final int i10Meg = 10*1024*1024;

    byte[] tmpBuffer = new byte[i10Meg];
    private byte[] inflate(byte[] blocks) {
        inflate.reset();
        inflate.setInput(blocks);
        byte[] out = null;
        try {
            int len = inflate.inflate(tmpBuffer);
            out = new byte[len];
            System.arraycopy(tmpBuffer, 0, out, 0, len);
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        return out;
    }

    @Override
    public boolean isValidWorld(AbstractPacketWorldRef packet) {
        return packet.getWorldId() == this.world.getId();
    }

    public void handleBlock(PacketSSetBlock p) {
        this.world.setType(p.x, p.y, p.z, p.type, Flags.MARK);
    }

    public void handleMultiBlock(PacketSSetBlocks p) {
        Chunk c = this.world.getChunk(p.chunkX, p.chunkZ);
        if (c == null) {
            System.err.println("Cannot process PacketSSetBlocks, chunk is not loaded");
            return;
        }
        for (int i = 0; i < p.len; i++) {
            short pos = p.positions.get(i);
            short type = p.types.get(i);
            int x = TripletShortHash.getX(pos);
            int y = TripletShortHash.getY(pos);
            int z = TripletShortHash.getZ(pos);
            this.world.setType(p.chunkX<<Chunk.SIZE_BITS|x, y, p.chunkZ<<Chunk.SIZE_BITS|z, type, Flags.MARK);            
        }
    }
}
