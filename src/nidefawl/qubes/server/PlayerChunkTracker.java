package nidefawl.qubes.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.*;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.network.packet.PacketSSetBlocks;
import nidefawl.qubes.network.server.ServerHandlerPlay;
import nidefawl.qubes.server.compress.CompressChunks;
import nidefawl.qubes.server.compress.CompressThread;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.SnakeIterator;
import nidefawl.qubes.util.TripletShortHash;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.WorldServer;

public class PlayerChunkTracker {
    public static class Entry {
        final int          x;
        final int          z;
        final List<Player> players          = Lists.newArrayList();
        public long        hash;
        boolean            wholeChunkUpdate = false;
        final List<Short>  flaggedBlocks    = Lists.newArrayList();//TODO: this really needs to be thread safe!

        public Entry(int x, int z) {
            this.x = x;
            this.z = z;
            this.hash = GameMath.toLong(x, z);
        }

        public void addPlayer(Player player) {
            this.players.add(player);
        }

        public void removePlayer(Player player) {
            this.players.remove(player);
        }

        public boolean isEmpty() {
            return this.players.isEmpty();
        }

        public void flag(int x, int y, int z) {
            if (this.flaggedBlocks.size() > 20) {
                this.wholeChunkUpdate = true;
            } else {
                this.flaggedBlocks.add(TripletShortHash.toHash(x, y, z));
            }
        }
    }

    Map<Long, Entry> map              = new MapMaker().makeMap();
    Set<Entry>       flaggedInstances = Sets.newHashSet();

    private WorldServer worldServer;
    int                 ticksLastCheck = 0;

    public PlayerChunkTracker(WorldServer worldServer) {
        this.worldServer = worldServer;
    }

    public void update(Player player) {
        if (!player.chunkTracked) {
            throw new IllegalArgumentException("Player is not chunk tracked");
        }
        int dist = player.getChunkLoadDistance();
        BlockPos pos = player.pos.toBlock();
        int lastX = player.chunkX;
        int lastZ = player.chunkZ;
        int chunkX = pos.x >> Chunk.SIZE_BITS;
        int chunkZ = pos.z >> Chunk.SIZE_BITS;
        int dx = chunkX - lastX;
        int dz = chunkZ - lastZ;
        if (Math.abs(dx)>2||Math.abs(dz)>2) {
            for (int x = -dist; x <= dist; x++) {
                for (int z = -dist; z <= dist; z++) {
                    int rX = x + lastX;
                    int rZ = z + lastZ;
                    int aX = x + chunkX;
                    int aZ = z + chunkZ;
                    //if old position outside new boundaries: remove old 
                    if (rX < chunkX - dist || rZ < chunkZ - dist || rX > chunkX + dist || rZ > chunkZ + dist) {
                        untrackPlayerChunk(player, rX, rZ);

                        //if new position outside old boundaries: add new
                    }
                    if (aX < lastX - dist || aZ < lastZ - dist || aX > lastX + dist || aZ > lastZ + dist) {
                        trackPlayerChunk(player, aX, aZ);
                    }
                }
            }
            player.chunkX = chunkX;
            player.chunkZ = chunkZ;
        }
    }

    private void trackPlayerChunk(Player player, int x, int z) {
        Entry entry = getEntry(x, z, true);
        entry.addPlayer(player);
        player.watchingChunk(entry.hash);
    }

    private void untrackPlayerChunk(Player player, int x, int z) {
        Entry e = getEntry(x, z, false);
        long l;
        if (e != null) {
            e.removePlayer(player);
            l = e.hash;
            if (e.isEmpty()) {
                this.map.remove(e.hash);
            }
        } else {
            System.err.println("Expected PlayerChunkTracker.Entry at " + x + ", " + z + " to not be null");
            Thread.dumpStack();
            l = GameMath.toLong(x, z);
        }
        player.unwatchingChunk(l);
    }

    public void removePlayer(Player player) {
        if (!player.chunkTracked) {
            throw new IllegalArgumentException("Player is not chunk tracked");
        }
        int dist = player.getChunkLoadDistance();
        for (int x = -dist; x <= dist; x++) {
            for (int z = -dist; z <= dist; z++) {
                untrackPlayerChunk(player, player.chunkX + x, player.chunkZ + z);
            }
        }
        player.chunkTracked = false;
    }

    public void addPlayer(Player player) {
        if (player.chunkTracked) {
            throw new IllegalArgumentException("Player is already chunk tracked");
        }
        int dist = player.getChunkLoadDistance();
        BlockPos pos = player.pos.toBlock();
        player.chunkX = pos.x >> Chunk.SIZE_BITS;
        player.chunkZ = pos.z >> Chunk.SIZE_BITS;

        SnakeIterator snakeit = new SnakeIterator();
        while (true) {
            int dx = snakeit.getX();
            int dz = snakeit.getZ();
            if (dx < -dist || dx > dist || dz < -dist || dz > dist) {
                break;
            }
            trackPlayerChunk(player, player.chunkX + dx, player.chunkZ + dz);
            snakeit.next();
        }
        player.chunkTracked = true;
    }

    private Entry getEntry(int x, int z, boolean create) {
        long l = GameMath.toLong(x, z);
        Entry e = this.map.get(l);
        if (e == null && create) {
            e = new Entry(x, z);
            this.map.put(l, e);
            worldServer.getChunkManager().queueLoadChecked(l);
        }
        return e;
    }

    public void flagBlock(int x, int y, int z) {
        if (y < 0 || y >= this.worldServer.worldHeight) {
            return;
        }
        Entry e = getEntry(x >> Chunk.SIZE_BITS, z >> Chunk.SIZE_BITS, false);
        if (e != null) {
            e.flag(x & Chunk.MASK, y, z & Chunk.MASK);
            this.flaggedInstances.add(e);
        }
    }

    public void sendBlockChanges() {
        if (!this.flaggedInstances.isEmpty()) {
            for (Entry e : this.flaggedInstances) {
                Chunk c = this.worldServer.getChunk(e.x, e.z);
                if (c != null) {
                    if (e.wholeChunkUpdate) {
                        ServerHandlerPlay[] handlers = new ServerHandlerPlay[e.players.size()];
                        int i = 0;
                        for (Player p : e.players) {
                            handlers[i++] = p.netHandler;
                        }
                        CompressThread.submit(new CompressChunks(this.worldServer.getId(), ImmutableList.of(c), handlers));
                    } else {
                        PacketSSetBlocks packet = new PacketSSetBlocks(this.worldServer.getId(), e.x, e.z);
                        packet.positions = Lists.newArrayList(e.flaggedBlocks);
                        packet.types = Lists.newArrayList();
                        packet.len = packet.positions.size();
                        for (int i = 0; i < packet.positions.size(); i++) {
                            Short s = packet.positions.get(i);
                            int x = TripletShortHash.getX(s);
                            int y = TripletShortHash.getY(s);
                            int z = TripletShortHash.getZ(s);
                            int type = c.getTypeId(x, y, z);
                            ;
                            packet.types.add((short) type);

                        }
                        for (Player p : e.players) {
                            p.netHandler.sendPacket(packet);
                        }

                    }
                }

                e.flaggedBlocks.clear();
                e.wholeChunkUpdate = false;
            }

            this.flaggedInstances.clear();

        }
    }

    public int getSize() {
        return this.map.size();
    }

    public boolean isRequired(int cx, int cz) {
        int dist = 2;
        for (int x = -dist; x <= dist; x++) {
            for (int z = -dist; z <= dist; z++) {
                if (this.map.containsKey(GameMath.toLong(x + cx, z + cz))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void recheckIfRequiredChunksLoaded() {
        ticksLastCheck++;
        if (ticksLastCheck > 100) {
            ticksLastCheck = 0;
            ChunkManagerServer mgr = (ChunkManagerServer) worldServer.getChunkManager();
            for (Entry e : this.map.values()) {
                Chunk c = mgr.get(e.x, e.z);
                if (c == null) {
                    mgr.queueLoadChecked(e.hash);
                }
            }
        }
    }

}
