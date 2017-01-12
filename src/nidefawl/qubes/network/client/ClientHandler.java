package nidefawl.qubes.network.client;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.PlayerProfile;
import nidefawl.qubes.async.AsyncTaskThread;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.async.AsyncTask;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chat.client.ChatManager;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkDataSliced2;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.chunk.blockdata.BlockDataSliced;
import nidefawl.qubes.chunk.client.ChunkManagerClient;
import nidefawl.qubes.crafting.CraftingManager;
import nidefawl.qubes.crafting.CraftingManagerClient;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.EntityType;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.GuiSelectWorld;
import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.io.ByteArrIO;
import nidefawl.qubes.io.network.DataListType;
import nidefawl.qubes.io.network.WorldInfo;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.BlockBoundingBox;
import nidefawl.qubes.world.IWorldSettings;
import nidefawl.qubes.world.WorldClient;
import nidefawl.qubes.world.WorldSettingsClient;
import nidefawl.qubes.world.biomes.IBiomeManager;

public class ClientHandler extends Handler {
    public final static long   timeout = 5000;

    int state = STATE_HANDSHAKE;
    String disconnectReason = null;
    private long               time;
    int disconnectFrom   = -1;


    public final NetworkClient client;
    private ChunkManagerClient chunkManager;
    private PlayerSelf         player;
    private WorldClient        world;

    //TODO: move decompression to thread
    final Inflater inflate = new Inflater();
    final int i10Meg = 20*1024*1024;
    byte[] tmpBuffer = new byte[i10Meg];
    public ArrayList<WorldInfo> worldList = Lists.newArrayList();

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
        if (state < STATE_CONNECTED || state > STATE_LOBBY) {
            if (System.currentTimeMillis() - time > timeout) {
                this.client.disconnect("Client Packet timed out");
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
            this.client.disconnect("Invalid packet (STATE_HANDSHAKE)");
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
    public void handleSync(PacketSyncBlocks packetAuth) {
        if (this.state != STATE_SYNC) {
            this.client.disconnect("Invalid packet (STATE_SYNC)");
            return;
        }
        this.state = STATE_CLIENT_SETTINGS;
        this.time = System.currentTimeMillis();
        PlayerProfile profile = Game.instance.getProfile();
        this.player = new PlayerSelf(this, profile);
        this.sendPacket(new PacketCSettings(Game.instance.settings.chunkLoadDistance));
    }
    public void handleLogin(PacketSLogin packetSLogin) {
        if (this.state != STATE_CLIENT_SETTINGS) {
            this.client.disconnect("Invalid packet (STATE_CLIENT_SETTINGS)");
            return;
        }
        this.state = STATE_CONNECTED;
    }
    @Override
    public void handleAuth(PacketAuth packetAuth) {
        if (this.state != STATE_AUTH) {
            this.client.disconnect("Invalid packet (STATE_AUTH)");
            return;
        }
        this.state = STATE_SYNC;
        this.time = System.currentTimeMillis();
        if (packetAuth.success) {
            PlayerProfile profile = Game.instance.getProfile();
            profile.setName(packetAuth.name);
            short[] data = Block.getRegisteredIDs();
            PacketSyncBlocks p = new PacketSyncBlocks(data);
            this.sendPacket(p);
        } else {
            this.client.onKick(Connection.REMOTE, "Invalid auth");
        }
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
    public void handleSpawnInWorld(PacketSSpawnInWorld packetJoinGame) {
        if (this.state < STATE_CLIENT_SETTINGS) {
            this.client.disconnect("Invalid packet (STATE_CLIENT_SETTINGS)");
            return;
        }
        this.state = STATE_CONNECTED;
        this.time = System.currentTimeMillis();
        this.world = new WorldClient((WorldSettingsClient) packetJoinGame.worldSettings, packetJoinGame.biomeSettings);
        this.chunkManager = (ChunkManagerClient) this.world.getChunkManager();
        this.player.setFly((packetJoinGame.flags & 0x1) != 0);
        this.player.id = packetJoinGame.entId;
        this.world.addEntity(player);
        this.player.move(packetJoinGame.pos);
        Game.instance.setWorld(this.world);
        Game.instance.setPlayer(player);
    }

    /**
     * @param packetSTeleport
     */
    public void handleTeleport(PacketSTeleport packetSTeleport) {
        if (isValidWorld(packetSTeleport)) {
            this.player.setFly((packetSTeleport.flags & 0x1) != 0);
            this.player.move(packetSTeleport.pos);
            this.player.setYawPitch(packetSTeleport.yaw, packetSTeleport.pitch);
        }
        this.sendPacket(new PacketCTeleportAck(packetSTeleport.getWorldId(), packetSTeleport.sync));
    }

    public void sendPacket(Packet packet) {
        this.client.sendPacket(packet);
    }

    public static void byteToShortArray(byte[] blocks, short[] dst, int offset) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] = (short) ( (blocks[offset+i*2+0]&0xFF) | ((blocks[offset+i*2+1]&0xFF)<<8) );
        }
    }

    
    //TODO: PUT IN THREAD
    @Override
    public void handleChunkDataMulti(final PacketSChunkData packet, final int flags) {
        if ((flags&1)!=0) {
            AsyncTasks.submit(new AsyncTask() {
                byte[] decpressData;
                @Override
                public Void call() throws Exception {
                    if (isValidWorld(packet)) {
                        AsyncTaskThread context = ((AsyncTaskThread)Thread.currentThread());
                        this.decpressData = context.inflate(packet.blocks);
                    }
                    return null;
                }
                @Override
                public void post() {
                    if (isValidWorld(packet)) {
                        processChunkData(packet, this.decpressData, flags);
                    }
                }
                @Override
                public TaskType getType() {
                    return TaskType.CHUNK_DECOMPRESS;
                }
            });
        } else {
            processChunkData(packet, packet.blocks, flags);
        }
    }

    private void processChunkData(PacketSChunkData packet, byte[] decompressed, int flags) {
        int[][] coords = packet.coords;
        int len = coords.length;
        int offset = 0;
        for (int i = 0; i < len; i++) {
            int[] pos = coords[i];
            Chunk c = this.chunkManager.getOrMake(pos[0], pos[1]);
            if (c == null) {
                throw new GameError("Failed recv. getOrMake returned null for chunk position "+pos[0]+"/"+pos[1]);
            }
            short[] dst = c.getBlocks();
            byteToShortArray(decompressed, dst, offset);
            offset += dst.length*2;
            if ((flags&2)!=0) {
                byte[] light = c.getBlockLight();
                System.arraycopy(decompressed, offset, light, 0, light.length);
                offset += light.length;
            }
            byte[] biomes = c.biomes;
            System.arraycopy(decompressed, offset, biomes, 0, biomes.length);
            offset += biomes.length;
            byte[] waterMask = c.waterMask;
            System.arraycopy(decompressed, offset, waterMask, 0, waterMask.length);
            offset += waterMask.length;
            
            int heightSlices = 0;
            heightSlices |= decompressed[offset+0]&0xFF;
            heightSlices |= (decompressed[offset+1]&0xFF)<<8;
            offset+=2;
            for (int j = 0; j < ChunkDataSliced2.DATA_HEIGHT_SLICES; j++) {
                if ((heightSlices&(1<<j))!=0) {
                    short[] dataArray = c.blockMetadata.getArray(j, true);
                    byteToShortArray(decompressed, dataArray, offset);
                    offset+=dataArray.length*2;
                }
            }
            heightSlices = 0;
            heightSlices |= decompressed[offset+0]&0xFF;
            heightSlices |= (decompressed[offset+1]&0xFF)<<8;
            offset+=2;
            for (int j = 0; j < BlockDataSliced.DATA_HEIGHT_SLICES; j++) {
                if ((heightSlices&(1<<j))!=0) {
                    ByteArrIO.readInt(decompressed, offset);
                    offset+=4;
                    int sliceElements = ByteArrIO.readShort(decompressed, offset);
                    offset+=2;
                    BlockData[] sData = c.blockData.getArray(j, true);
                    for (int k = 0; k < sliceElements; k++) {
                        int idx = ByteArrIO.readShort(decompressed, offset);
                        offset+=2;
                        int type = ByteArrIO.readUnsignedByte(decompressed, offset);
                        offset+=1;
                        int lenElement = ByteArrIO.readUnsignedByte(decompressed, offset)*4;
                        offset+=1;
                        BlockData bdata = BlockData.fromType(type);
                        if (bdata == null) {
                            offset += len;
                            continue;
                        }
                        int dataRead = bdata.readData(decompressed, offset);
                        if (dataRead != lenElement) {
                            System.err.println("header/read missmatch");
                            break;
                        }
                        offset+=dataRead;
                        sData[idx] = bdata;
                    }
                }
            }
            Engine.regionRenderer.flagChunk(c.x, c.z);
        }
    }

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
        this.world.setType(p.x, p.y, p.z, p.type, Flags.MARK|Flags.LIGHT);
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
        short[] datas = p.data;
        BlockData[] bdatas = p.bdata;
        int len = positions.length;
        for (int i = 0; i < len; i++) {
            short pos = positions[i];
            int type = blocks[i]&Block.BLOCK_MASK;
            short data = datas[i];
            byte light = lights[i];
            BlockData bdata = bdatas == null ? null : bdatas[i]; 
            int x = TripletShortHash.getX(pos);
            int y = TripletShortHash.getY(pos);
            int z = TripletShortHash.getZ(pos);
            c.setType(x&Chunk.MASK, y, z&Chunk.MASK, type);
            c.setLight(x&Chunk.MASK, y, z&Chunk.MASK, -1, light&0xFF);
            c.setFullData(x&Chunk.MASK, y, z&Chunk.MASK, data);
            c.setBlockData(x&Chunk.MASK, y, z&Chunk.MASK, bdata);
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
        byte[] decompressed = inflate(packet.data);
        if (c.setLights(decompressed, box)) {
            Engine.regionRenderer.flagChunk(c.x, c.z); //TODO: do not flag whole y-slice
        } else {
//            System.out.println("not flagging empty light update "+packet.coordX+"/"+packet.coordZ+" - "+box);  
        }

    }
    public void handleTrackChunk(PacketSTrackChunk p) {
        if (!p.add) {
            this.chunkManager.remove(p.x, p.z);
            Engine.regionRenderer.flagChunk(p.x, p.z);
        }
    }

    public void handleChat(PacketChatMessage packetChatMessage) {
        ChatManager.getInstance().receiveMessage(packetChatMessage.channel, packetChatMessage.message);
    }

    public void handleChannels(PacketChatChannels packetChatChannels) {
        ChatManager.getInstance().syncChannels(packetChatChannels.list);

    }

    /**
     * @param p
     */
    public void handleWorldTime(PacketSWorldTime p) {
        IWorldSettings settings = this.world.getSettings();
        settings.setTime(p.time);
        settings.setDayLen(p.daylen);
        settings.setFixedTime(p.isFixed);
    }

    /**
     * @param packetSEntityUnTrack
     */
    public void handleEntityUntrack(PacketSEntityUnTrack p) {
        Entity e = this.world.getEntity(p.entId);
        if (e != null) {
            this.world.removeEntity(e);
        }
    }

    /**
     * @param packetSEntityTrack
     */
    public void handleEntityTrack(PacketSEntityTrack p) {
        Entity e = EntityType.newById(p.entType, false);
        e.id = p.entId;
        e.pitch = p.pitch;
        e.yaw = p.yaw;
        e.yawBodyOffset = p.yawbody;
        e.move(p.pos);
        Tag tag = p.data;
        if (tag != null) {
            e.readClientData(tag);
        }
        this.world.addEntity(e); 
    }

    /**
     * @param packetSEntityMove
     */
    public void handleEntityMove(PacketSEntityMove p) {
        Entity e = this.world.getEntity(p.entId);
        if (e == this.player) {
            System.err.println("wtf");
        }
        if (e != null) {
            //TODO interpolate
            if ((p.flags & 1) != 0) {
                e.setRemotePos(p.pos);
            }
            if ((p.flags & 2) != 0) {
                e.setRemoteRotation(p.pitch, p.yaw, p.yawBodyOffset);
            }
        }
    }
    
    @Override
    public void handleWorldBiomes(PacketSWorldBiomes packetSWorldBiomes) {
        IBiomeManager mgr = this.world.biomeManager;
        mgr.recvData(packetSWorldBiomes);
    }
    public void handleServerDigState(PacketSDigState packetSDigState) {
        Game.instance.dig.handleServerState(packetSDigState.stage);
    }
    public void handleDebugBBs(PacketSDebugBB packetSDebugBB) {
        Engine.worldRenderer.debugBBs.clear();
        for (int i = 0; i < packetSDebugBB.boxes.size(); i++) {
            Engine.worldRenderer.debugBBs.put(i, packetSDebugBB.boxes.get(i));    
        }
    }
    public void handleDebugPath(PacketSDebugPath packetSDebugPath) {
        Engine.worldRenderer.debugPaths.clear();
//        Engine.worldRenderer.debugPaths.put(0, packetSDebugPath.pts);
//        System.out.println(packetSDebugPath.pts);
    }

    public void handleInvSync(PacketSInvSync p) {
        PlayerSelf player = Game.instance.getPlayer();
        if (player != null) {
            BaseInventory inv = player.getInv(p.invId);
            if (inv != null) {
                inv.set(p.stacks);    
            }
        }
    }
    public void handleInvSyncIncr(PacketSInvSyncIncr p) {
        PlayerSelf player = Game.instance.getPlayer();
        if (player != null) {
            BaseInventory inv = player.getInv(p.invId);
            if (inv != null) {
                inv.setIncr(p.stacks);    
            }
        }
    }
    public void handleCraftingProgress(PacketSCraftingProgress p) {
        CraftingManagerClient mgr = player.getCrafting(p.id);
        if (mgr == null) {
            this.client.disconnect("Invalid packet");
            return;
        }
        if (p.action < 0 || p.action > 4) {
            this.client.disconnect("Invalid packet");
            return;
        }
        mgr.handleRequest(p.action, p);
    }

    public void handleInvCarried(PacketSInvCarried packetSInvCarried) {
        PlayerSelf player = Game.instance.getPlayer();
        if (player != null) {
            PlayerInventory inv = (PlayerInventory) player.getInv(0);
            inv.carried=packetSInvCarried.stack.stack;
        }
    }


    public void handleEntityProperties(PacketSEntityProperties p) {
        Entity e = this.world.getEntity(p.entId);
        if (e != null) {
            Tag tag = p.data;
            if (tag != null)
                e.readProperties(tag);
        }
    }



    public void handleEntityEquip(PacketSEntityEquip p) {
        Entity e = this.world.getEntity(p.entId);
        if (e != null) {
            e.setEquipment(p.stacks);
        }
    }

    public void handleList(PacketSList packetSList) {
        switch (packetSList.type) {
            case WORLDS:
                if (this.state == STATE_CONNECTED) {
                    this.state = STATE_LOBBY;
                }
                this.worldList.clear();
                this.worldList.addAll(packetSList.list);
                Game.instance.showGUI(new GuiSelectWorld());
                break;

        }
    }

    public void handleParticles(PacketSParticles p) {
        Engine.particleRenderer.spawnParticles(world, p.pos.x, p.pos.y, p.pos.z, p.type, p.arg);
    }
}
