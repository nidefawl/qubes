package nidefawl.qubes.network.client;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
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
import nidefawl.qubes.vec.BlockBoundingBox;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.WorldClient;
import nidefawl.qubes.world.WorldSettingsClient;

public class ClientHandler extends Handler {

    int state = STATE_HANDSHAKE;

    public final NetworkClient client;
    private long               time;
    public final static long   timeout = 5000;

    public ClientHandler(final NetworkClient client) {
        this.client = client;
        this.time = System.currentTimeMillis();
    }

    @Override
    public boolean isServerSide() {
        return false;
    }

    @Override
    public void update() {
        if (state != STATE_CONNECTED) {
            if (System.currentTimeMillis() - time > timeout) {
                this.client.disconnect("Packet timed out");
                return;
            }
        } else {
        }
    }

    @Override
    public void handlePing(PacketPing p) {
        this.client.sendPacket(new PacketPing(p.time));
    }

    @Override
    public void handleHandshake(PacketHandshake packetHandshake) {
        if (this.state != STATE_HANDSHAKE) {
            this.client.disconnect("Invalid packet");
            return;
        }
        if (packetHandshake.version != this.client.netVersion) {
            this.client.disconnect("Invalid version. Client is on version " + this.client.netVersion + ", Server on " + packetHandshake.version);
            return;
        }
        this.state = STATE_AUTH;
        this.time = System.currentTimeMillis();
        this.client.sendPacket(new PacketAuth(Game.instance.getProfile().getName()));
    }

    @Override
    public String getHandlerName() {
        return "Client";
    }

    @Override
    public void handleDisconnect(PacketDisconnect packetDisconnect) {
        this.client.onKick(packetDisconnect.code, packetDisconnect.message);
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
            this.client.disconnect("Invalid packet");
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
        this.client.sendPacket(packet);
    }

    public static void byteToShortArray(byte[] blocks, short[] dst, int offset) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] = (short) ( (blocks[offset+i*2+0]&0xFF) | ((blocks[offset+i*2+1]&0xFF)<<8) );
        }
    }

    @Override
    public void handleChunkDataMulti(PacketSChunkData packet, int flags) {
        int[][] coords = packet.coords;
        int len = coords.length;
        byte[] decompressed = packet.blocks;
        if ((flags&1)!=0) {
            decompressed = inflate(packet.blocks);
        }
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
            short[] dst = c.getBlocks();
            byteToShortArray(decompressed, dst, offset);
            offset += dst.length*2;
            if ((flags&2)!=0) {
                byte[] light = c.getBlockLight();
                System.arraycopy(decompressed, offset, light, 0, light.length);
                offset += light.length;
            }
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
        short[] positions = p.positions;
        short[] blocks = p.blocks;
        byte[] lights = p.lights;
        int len = positions.length;
        for (int i = 0; i < len; i++) {
            short pos = positions[i];
            short type = blocks[i];
            byte light = lights[i];
            int x = TripletShortHash.getX(pos);
            int y = TripletShortHash.getY(pos);
            int z = TripletShortHash.getZ(pos);
            c.setType(x&Chunk.MASK, y, z&Chunk.MASK, type);
            c.setLight(x&Chunk.MASK, y, z&Chunk.MASK, -1, light&0xFF);
            this.world.flagBlock(p.chunkX<<Chunk.SIZE_BITS|x, y, p.chunkZ<<Chunk.SIZE_BITS|z);      
        }
    }

    public void handleLightChunk(PacketSLightChunk packet) {
        Chunk c = this.chunkManager.get(packet.coordX, packet.coordZ);
        if (c == null) {
            System.err.println("Failed recv. light data, chunk is not loaded: " + packet.coordX + "/" + packet.coordZ);
            return;
        }
        BlockBoundingBox box = BlockBoundingBox.fromShorts(packet.min, packet.max);
        if(283>>4==packet.coordX&&(-523)>>4==packet.coordZ) {
            if (box.contains(283&0xF, 137, -523&0xF)) {
                System.err.println("light is flagged333");
            }
        }
//        System.out.println("light "+packet.coordX+"/"+packet.coordZ+" - "+box);
        byte[] decompressed = inflate(packet.data);
        c.setLights(decompressed, box);
        Engine.regionRenderer.flagChunk(c.x, c.z); //TODO: do not flag whole y-slice

    }

}