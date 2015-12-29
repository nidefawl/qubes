package nidefawl.qubes.server;

import java.util.*;

import com.google.common.collect.*;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.network.packet.PacketSSetBlocks;
import nidefawl.qubes.network.server.ServerHandlerPlay;
import nidefawl.qubes.server.compress.CompressChunks;
import nidefawl.qubes.server.compress.CompressLight;
import nidefawl.qubes.server.compress.CompressThread;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.SnakeIterator;
import nidefawl.qubes.util.TripletShortHash;
import nidefawl.qubes.vec.BlockBoundingBox;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.WorldServer;

// TODO: Auto-generated Javadoc
/**
 * The Class PlayerChunkTracker.
 */
public class PlayerChunkTracker {
    
    /**
     * The Class Entry.
     */
    public static class Entry {
        
        /** The x. */
        final int          x;
        
        /** The z. */
        final int          z;
        
        /** The players. */
        final List<PlayerServer> players          = Lists.newArrayList();
        
        /** The hash. */
        public long        hash;
        
        /** The whole chunk update. */
        boolean            wholeChunkUpdate = false;
        
        /** The flagged blocks. */
        final Set<Short>  flaggedBlocks    = Sets.newConcurrentHashSet();//TODO: this really needs to be thread safe!
        
        /** The flagged lights. */
        final BlockBoundingBox flaggedLights = new BlockBoundingBox();
        
        /** The has flagged lights. */
        public boolean hasFlaggedLights;

        /**
         * Instantiates a new entry.
         *
         * @param x the x
         * @param z the z
         */
        public Entry(int x, int z) {
            this.x = x;
            this.z = z;
            this.hash = GameMath.toLong(x, z);
        }

        /**
         * Adds the PlayerServer.
         *
         * @param PlayerServer the PlayerServer
         */
        public void addPlayer(PlayerServer PlayerServer) {
            this.players.add(PlayerServer);
        }

        /**
         * Removes the PlayerServer.
         *
         * @param PlayerServer the PlayerServer
         */
        public void removePlayer(PlayerServer PlayerServer) {
            this.players.remove(PlayerServer);
        }

        /**
         * Checks if is empty.
         *
         * @return true, if is empty
         */
        public boolean isEmpty() {
            return this.players.isEmpty();
        }
        
        /**
         * Flag light. Avoid calling this
         *
         * @param x the x
         * @param y the y
         * @param z the z
         */
        public void flagLight(int x, int y, int z) {
            synchronized (flaggedLights) {
                this.flaggedLights.flag(x, y, z);
                this.hasFlaggedLights=true;   
            }
        }

        /**
         * Flag.
         *
         * @param x the x
         * @param y the y
         * @param z the z
         */
        public void flag(int x, int y, int z) {
            if (this.flaggedBlocks.size() > 20) {
                this.wholeChunkUpdate = true;
            } else {
                this.flaggedBlocks.add(TripletShortHash.toHash(x, y, z));
            }
        }
    }

    /** The map. */
    Map<Long, Entry> map              = new MapMaker().makeMap();
    
    /** The flagged instances. */
    Set<Entry>       flaggedInstances = Sets.newConcurrentHashSet();

    /** The world server. */
    private WorldServer worldServer;
    
    /** The ticks last check. */
    int                 ticksLastCheck = 0;

    /**
     * Instantiates a new PlayerServer chunk tracker.
     *
     * @param worldServer the world server
     */
    public PlayerChunkTracker(WorldServer worldServer) {
        this.worldServer = worldServer;
    }

    /**
     * Update.
     *
     * @param PlayerServer the PlayerServer
     */
    public void update(PlayerServer PlayerServer) {
        if (!PlayerServer.chunkTracked) {
            throw new IllegalArgumentException("PlayerServer is not chunk tracked");
        }
        int dist = PlayerServer.getChunkLoadDistance();
        BlockPos pos = PlayerServer.pos.toBlock();
        int lastX = PlayerServer.chunkX;
        int lastZ = PlayerServer.chunkZ;
        int chunkX = pos.x >> Chunk.SIZE_BITS;
        int chunkZ = pos.z >> Chunk.SIZE_BITS;
        int dx = chunkX - lastX;
        int dz = chunkZ - lastZ;
        int moveDist = 0;
        if (dist > 6)
            moveDist++;
        if (dist > 12)
            moveDist++;
        if (Math.abs(dx)>moveDist||Math.abs(dz)>moveDist) {
            for (int x = -dist; x <= dist; x++) {
                for (int z = -dist; z <= dist; z++) {
                    int rX = x + lastX;
                    int rZ = z + lastZ;
                    int aX = x + chunkX;
                    int aZ = z + chunkZ;
                    //if old position outside new boundaries: remove old 
                    if (rX < chunkX - dist || rZ < chunkZ - dist || rX > chunkX + dist || rZ > chunkZ + dist) {
                        untrackPlayerChunk(PlayerServer, rX, rZ);

                        //if new position outside old boundaries: add new
                    }
                    if (aX < lastX - dist || aZ < lastZ - dist || aX > lastX + dist || aZ > lastZ + dist) {
                        trackPlayerChunk(PlayerServer, aX, aZ);
                    }
                }
            }
            PlayerServer.chunkX = chunkX;
            PlayerServer.chunkZ = chunkZ;
        }
    }

    /**
     * Track PlayerServer chunk.
     *
     * @param PlayerServer the PlayerServer
     * @param x the x
     * @param z the z
     */
    private void trackPlayerChunk(PlayerServer PlayerServer, int x, int z) {
        Entry entry = getEntry(x, z, true);
        entry.addPlayer(PlayerServer);
        PlayerServer.watchingChunk(entry.hash, x, z);
    }

    /**
     * Untrack PlayerServer chunk.
     *
     * @param PlayerServer the PlayerServer
     * @param x the x
     * @param z the z
     */
    private void untrackPlayerChunk(PlayerServer PlayerServer, int x, int z) {
        Entry e = getEntry(x, z, false);
        long l;
        if (e != null) {
            e.removePlayer(PlayerServer);
            l = e.hash;
            if (e.isEmpty()) {
                this.map.remove(e.hash);
            }
        } else {
            System.err.println("Expected PlayerChunkTracker.Entry at " + x + ", " + z + " to not be null");
            Thread.dumpStack();
            l = GameMath.toLong(x, z);
        }
        PlayerServer.unwatchingChunk(l, x, z);
    }

    /**
     * Removes the PlayerServer.
     *
     * @param PlayerServer the PlayerServer
     */
    public void removePlayer(PlayerServer PlayerServer) {
        if (!PlayerServer.chunkTracked) {
            throw new IllegalArgumentException("PlayerServer is not chunk tracked");
        }
        int dist = PlayerServer.getChunkLoadDistance();
        for (int x = -dist; x <= dist; x++) {
            for (int z = -dist; z <= dist; z++) {
                untrackPlayerChunk(PlayerServer, PlayerServer.chunkX + x, PlayerServer.chunkZ + z);
            }
        }
        PlayerServer.chunkTracked = false;
    }

    /**
     * Adds the PlayerServer.
     *
     * @param PlayerServer the PlayerServer
     */
    public void addPlayer(PlayerServer PlayerServer) {
        if (PlayerServer.chunkTracked) {
            throw new IllegalArgumentException("PlayerServer is already chunk tracked");
        }
        int dist = PlayerServer.getChunkLoadDistance();
        BlockPos pos = PlayerServer.pos.toBlock();
        PlayerServer.chunkX = pos.x >> Chunk.SIZE_BITS;
        PlayerServer.chunkZ = pos.z >> Chunk.SIZE_BITS;

        SnakeIterator snakeit = new SnakeIterator();
        while (true) {
            int dx = snakeit.getX();
            int dz = snakeit.getZ();
            if (dx < -dist || dx > dist || dz < -dist || dz > dist) {
                break;
            }
            trackPlayerChunk(PlayerServer, PlayerServer.chunkX + dx, PlayerServer.chunkZ + dz);
            snakeit.next();
        }
        PlayerServer.chunkTracked = true;
    }

    /**
     * Gets the entry.
     *
     * @param x the x
     * @param z the z
     * @param create the create
     * @return the entry
     */
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

    /**
     * Flag block.
     *
     * @param x the x
     * @param y the y
     * @param z the z
     */
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

    /** The blocks to send. */
    final List<Short>  blocksToSend = Lists.newArrayList();
    
    /** The box2. */
    final BlockBoundingBox box2 = new BlockBoundingBox();
    
    /**
     * Send block changes.
     */
    public void sendBlockChanges() {
        if (!this.flaggedInstances.isEmpty()) {
            //TODO: drain atomic!
            for (Entry e : this.flaggedInstances) {
                Chunk c = this.worldServer.getChunk(e.x, e.z);
                ServerHandlerPlay[] handlers = null;
                if (c != null) {
                    if (e.wholeChunkUpdate) {
                        e.wholeChunkUpdate = false;
                        handlers = getHandlerArr(e);
                        CompressThread.submit(new CompressChunks(this.worldServer.getId(), ImmutableList.of(c), handlers, false));
                    } else if (!e.flaggedBlocks.isEmpty()) {
                        blocksToSend.clear();
                        Iterator<Short> it = e.flaggedBlocks.iterator();
                        while (it.hasNext()) {
                            blocksToSend.add(it.next());
                            it.remove();
                        } 
                        short[] pos = new short[blocksToSend.size()];
                        short[] blocks = new short[pos.length];
                        short[] data = new short[pos.length];
                        byte[] lights = new byte[pos.length];
//                        ArrayList<BlockData> bdata = new ArrayList<>();
                        BlockData[] bdata = new BlockData[pos.length];
                        for (int i = 0; i < pos.length; i++) {
                            short s = pos[i] = blocksToSend.get(i);
                            int x = TripletShortHash.getX(s);
                            int y = TripletShortHash.getY(s);
                            int z = TripletShortHash.getZ(s);
                            blocks[i] = (short) c.getTypeId(x, y, z);
                            lights[i] = (byte) c.getLight(x, y, z);
                            data[i] = (short) c.getFullData(x, y, z);
                            bdata[i] = c.getBlockData(x, y, z);
                        }
                        PacketSSetBlocks packet = new PacketSSetBlocks(this.worldServer.getId(), e.x, e.z, pos, blocks, lights, data, bdata);
                        for (PlayerServer p : e.players) {
                            p.netHandler.sendPacket(packet);
                        }

                    }
                }
                int vol = e.flaggedLights.getVolume();
                if (vol > 0) {
                    BlockBoundingBox bb = null;
                    synchronized (e.flaggedLights) {
                        bb = e.flaggedLights.copyTo(new BlockBoundingBox());
                        e.flaggedLights.reset();
                    }
                    if (c != null) {
                        if (handlers == null)
                            handlers = getHandlerArr(e);
                        if (bb.getVolume() <= 0) {
                            System.err.println("volumue <= 0");
                            continue;
                        }
                        CompressThread.submit(new CompressLight(this.worldServer.getId(), c, bb, handlers));
//                        System.out.println("send "+len.length+" bytes of light data to PlayerServer");
                    }
                }
            
            }

            this.flaggedInstances.clear();

        }
    }


    /**
     * @return
     */
    private ServerHandlerPlay[] getHandlerArr(Entry e) {
        ServerHandlerPlay[] arr = new ServerHandlerPlay[e.players.size()];
        int i = 0;
        for (PlayerServer p : e.players) {
            arr[i++] = p.netHandler;
        }
        return arr;
    }

    /**
     * Gets the size.
     *
     * @return the size
     */
    public int getSize() {
        return this.map.size();
    }

    /**
     * Checks if is required.
     *
     * @param cx the cx
     * @param cz the cz
     * @return true, if is required
     */
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

    /**
     * Recheck if required chunks loaded.
     */
    public void recheckIfRequiredChunksLoaded(boolean force) {
        ticksLastCheck++;
        if (ticksLastCheck > 100 || force) {
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
    
    /**
     * Flag lights.
     *
     * @param i the i
     * @param j the j
     * @param blockBoundingBox the block bounding box
     */
    public void flagLights(int i, int j, BlockBoundingBox blockBoundingBox) {
        Entry e = getEntry(i, j, false);
        if (e != null) {
            synchronized (e.flaggedLights) {
                e.flaggedLights.extend(blockBoundingBox);
                e.hasFlaggedLights = true;
            }
            this.flaggedInstances.add(e);
        } else {
//            System.err.println("missing PlayerServer instance while flagging lights");
        }
    }


    /**
     * Flag lights in given boundaries
     *
     * @param chunkX the chunk x
     * @param chunkZ the chunk z
     * @param minBlockX the min block x
     * @param minBlockY the min block y
     * @param minBlockZ the min block z
     * @param maxBlockX the max block x (must be >= min z)
     * @param maxBlockY the max block y (must be >= min z)
     * @param maxBlockz the max block z (must be >= min z)
     */
    public void flagLights(int chunkX, int chunkZ, int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {

        Entry e = getEntry(chunkX, chunkZ, false);
        if (e != null) {
            synchronized (e.flaggedLights) {
                e.flaggedLights.expandTo(minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
                e.hasFlaggedLights = true;
            }
            this.flaggedInstances.add(e);
        }
    }
}
