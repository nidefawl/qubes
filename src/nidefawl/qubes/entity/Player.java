package nidefawl.qubes.entity;

import java.util.*;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import nidefawl.qubes.chat.ChatUser;
import nidefawl.qubes.chat.channel.GlobalChannel;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketChatMessage;
import nidefawl.qubes.network.packet.PacketSTrackChunk;
import nidefawl.qubes.network.server.ServerHandlerPlay;
import nidefawl.qubes.player.PlayerData;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.commands.Command;
import nidefawl.qubes.server.commands.CommandException;
import nidefawl.qubes.server.commands.ICommandSource;
import nidefawl.qubes.server.compress.CompressChunks;
import nidefawl.qubes.server.compress.CompressThread;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;

public class Player extends Entity implements ChatUser, ICommandSource {

    public String        name;
    public ServerHandlerPlay netHandler;
    public boolean flying;
    public int chunkX;
    public int chunkZ;
    public boolean chunkTracked;
     Set<Long> chunks = Sets.newLinkedHashSet();
     Set<Long> sendChunks = Sets.newLinkedHashSet();
     int lastLight = 0;
    public UUID spawnWorld;
    private int chunkLoadDistance;
    private Set<String> joinedChannels = Sets.newConcurrentHashSet();
     
    public Player() {
        super();
    }

    public void load(PlayerData data) {
        this.pos.set(data.pos);
        this.lastPos.set(data.pos);
        this.flying = data.flying;
        this.spawnWorld = data.world;
        this.chunkLoadDistance = data.chunkLoadDistance;
        this.joinedChannels.clear();
        this.joinedChannels.addAll(data.joinedChannels);
    }

    public PlayerData save() {
        PlayerData data = new PlayerData();
        data.pos = new Vector3f(this.pos);
        data.flying = this.flying;
        data.world = this.world != null ? this.world.getUUID() : null;
        data.joinedChannels = new HashSet<String>(this.joinedChannels);
        return data;
    }

    public int getChunkLoadDistance() {
        return this.chunkLoadDistance;
    }
    @Override
    public void tickUpdate() {
        if (!this.sendChunks.isEmpty()) {
            Iterator<Long> it = this.sendChunks.iterator();
            Set<Chunk> chunks = null;
            while (it.hasNext()) {
                long l = it.next();
                int x = GameMath.lhToX(l);
                int z = GameMath.lhToZ(l);
                Chunk c = this.world.getChunkManager().get(x, z);
                if (c != null) {
                    if (chunks == null) {
                        chunks = Sets.newHashSet();
                    }
//                    System.out.println("send chunk "+c);
                    chunks.add(c);
                    it.remove();
                }
            }
            if (chunks != null) {
//                System.out.println("got list with "+chunks.size()+" chunks for client ");
                Iterable<List<Chunk>> iter = Iterables.partition(chunks, 16);
                for (List<Chunk> list : iter) {
                    CompressThread.submit(new CompressChunks(this.world.getId(), list, new ServerHandlerPlay[] {this.netHandler}, true));
                }
            }
        }
//        if (lastLight++>444) {
//            lastLight = 0;
//            Iterator<Long> it  = this.chunks.iterator();
//            Set<Chunk> chunks = null;
//            while (it.hasNext()) {
//                long l = it.next();
//                int x = GameMath.lhToX(l);
//                int z = GameMath.lhToZ(l);
//                Chunk c = this.world.getChunkManager().get(x, z);
//                if (c != null) {
//                    if (chunks == null) {
//                        chunks = Sets.newHashSet();
//                    }
////                    System.out.println("send chunk "+c);
//                    chunks.add(c);
//                    if (chunks.size() > 31)
//                        break;
//                }
//            }
//            if (chunks != null) {
////              System.out.println("got list with "+chunks.size()+" chunks for client ");
//              Iterable<List<Chunk>> iter = Iterables.partition(chunks, 16);
//              for (List<Chunk> list : iter) {
//              }
//          }
//        }
    }

    public void watchingChunk(long hash, int x, int z) {
        if (this.chunks.add(hash)) {
            this.sendChunks.add(hash);
            this.netHandler.sendPacket(new PacketSTrackChunk(x, z, true));
        }
    }

    public void unwatchingChunk(long hash, int x, int z) {
        if (this.chunks.remove(hash)) {
            this.sendChunks.remove(hash);
            this.netHandler.sendPacket(new PacketSTrackChunk(x, z, false));
        }
    }

    public String getName() {
        return this.name;
    }

    public void kick(String string) {
        this.netHandler.kick(string);
    }

    /**
     * @param chunkLoadDistance2
     */
    public void setChunkLoadDistance(int distance) {
        if (distance < 2 || distance > 32) {
            throw new IllegalArgumentException("Invalid chunk load distance");
        }
        this.chunkLoadDistance = distance;
    }


    @Override
    public Collection<String> getJoinedChannels() {
        return this.joinedChannels;
    }


    @Override
    public void preExecuteCommand(Command c) {
    }

    @Override
    public void onError(Command c, CommandException e) {
        
        if (e.getCause() != null) {
            sendMessage("An exception occured while executing '"+c.getName()+"': "+e.getMessage());
            e.getCause().printStackTrace();
        } else {
            sendMessage(e.getMessage());
        }
    }

    @Override
    public void onUnknownCommand(String cmd, String line) {
        sendMessage("Unknown command '"+cmd+"'");
    }

    @Override
    public GameServer getServer() {
        return ((WorldServer) this.world).getServer();
    }

    @Override
    public void sendMessage(String string) {
        this.netHandler.sendPacket(new PacketChatMessage(GlobalChannel.TAG, string));
    }
    
    @Override
    public String getChatName() {
        return getName();
    }

    @Override
    public void sendMessage(String channel, String string) {
        this.netHandler.sendPacket(new PacketChatMessage(channel, string));
    }

    public void sendPacket(Packet packet) {
        this.netHandler.sendPacket(packet);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.server.commands.ICommandSource#getWorld()
     */
    @Override
    public World getWorld() {
        return this.world;
    }
}
