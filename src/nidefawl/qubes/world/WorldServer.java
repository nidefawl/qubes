package nidefawl.qubes.world;

import static nidefawl.qubes.chunk.Chunk.*;

import java.io.File;
import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.blocklight.BlockLightThread;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketSWorldTime;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.PlayerChunkTracker;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TripletLongHash;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;
import nidefawl.qubes.worldgen.terrain.TerrainGenerator2;
import nidefawl.qubes.worldgen.terrain.TestTerrain2;

public class WorldServer extends World {

    private final GameServer         server;
    private final PlayerChunkTracker chunkTracker     = new PlayerChunkTracker(this);
    private final List<Player>       players          = Lists.newArrayList();
    private final ChunkManagerServer chunkServer;
    private final ITerrainGen              generator;
    private final IChunkPopulator populator;
//    private Queue<Long> lightUpdateQueue = Queues.newConcurrentLinkedQueue();
    private Set<Long> lightUpdateQueue = Sets.newConcurrentHashSet();
    private final BlockLightThread lightUpdater;

    public WorldServer(WorldSettings settings, GameServer server) {
        super(settings);
        this.server = server;
        this.chunkServer = (ChunkManagerServer) getChunkManager();
        this.generator = settings.getGenerator(this);
        this.populator = settings.getPopulator(this);
        this.lightUpdater = new BlockLightThread(this);
    }
    public void onLeave() {
        this.entities.clear();
        this.entityList.clear();
        this.lightUpdater.halt();
        this.chunkServer.onWorldUnload();
    }


    public void onLoad() {
        this.chunkServer.startThreads();
        this.lightUpdater.start();
    }

    
    
    
    public ITerrainGen getGenerator() {
        return generator;
    }

    public void tickUpdate() {
        super.tickUpdate();
        updateChunks();
    }
    public void resyncTime() {
        PacketSWorldTime wTime = new PacketSWorldTime(this.getId(), this.getTime(), this.getDayLength(), this.settings.isFixedTime());
        this.broadcastPacket(wTime);
        
    }
    public void broadcastPacket(Packet p) {
        int l = this.players.size();
        for (int i = 0; i < l; i++) {
            Player player = this.players.get(i);
            player.sendPacket(p);
        }
    }
    @Override
    public ChunkManager makeChunkManager() {
        return new ChunkManagerServer(this, ((WorldSettings) this.settings).getWorldDirectory());
    }

    public GameServer getServer() {
        return this.server;
    }

    Iterator<Chunk> updateIt = null;
    public void updateChunks() {
        if (!this.lightUpdateQueue.isEmpty()) {
            Iterator<Long> it = lightUpdateQueue.iterator();
            while (it.hasNext()) {
                long l = it.next();
                int x = GameMath.lhToX(l);
                int z = GameMath.lhToZ(l);
                Chunk self = getChunk(x, z);
                if (self == null || !self.isValid) {
                    System.out.println("remove missing chunk from queue");
                    it.remove();
                    continue;
                }
                Chunk c = getChunkIfNeightboursLoaded(x, z);
                if (c != null) {
                    it.remove();
                    this.calcSunLight(c);
                }
            }
        }
        for (int i = 0; i < players.size(); i++) {
            this.chunkTracker.update(players.get(i));
        }
        this.chunkTracker.recheckIfRequiredChunksLoaded(false);
        if (updateIt == null || !updateIt.hasNext()) {
            updateIt = this.chunkServer.newUpdateIterator();
        }
        int maxChunks = 10;
        Iterator<Chunk> it = this.updateIt;
        while (it.hasNext()) {
            Chunk c = it.next();
            if (!c.isUnloading && c.isValid) {
                if (c.isLit && !c.isPopulated) {
                    Chunk cPopulate = getChunkIfNeightboursLoaded(c.x, c.z);
                    if (cPopulate == c) {
                        c.isPopulated = true;
                        getChunkPopulator().populate(this, cPopulate);   
                        this.calcSunLight(cPopulate); 
                    }
                }
            }
            maxChunks--;
            if (maxChunks <= 0) {
                break;
            }
        }
        
        this.chunkTracker.sendBlockChanges();
    }
    public void updateGeneratedChunks() {
        // TODO Auto-generated method stub
        
    }
    public void unloadUnused() {

        this.chunkServer.saveAndUnloadChunks(200);
    }

    public void addPlayer(Player player) {
        addEntity(player);
        this.players.add(player);
        this.chunkTracker.addPlayer(player);
    }

    public void removePlayer(Player player) {
        removeEntity(player);
        this.players.remove(player);
        this.chunkTracker.removePlayer(player);
    }

    public void save(boolean b) {
        getChunkManager().saveAll();
        if (b) {
            ((WorldSettings)this.settings).saveFile();
        }
    }

    public void flagBlock(int x, int y, int z) {
        this.chunkTracker.flagBlock(x, y, z);
    }

    public PlayerChunkTracker getPlayerChunkTracker() {
        return this.chunkTracker;
    }

    public void calcSunLight(Chunk c) {
        lightUpdater.queueChunk(c.x, c.z, 1);
    }

    public void updateLightHeightMap(Chunk chunk, int x, int z, int min, int max, boolean add) {
        
        for (int y = min; y < max; y++) {
            chunk.setLight(x, y, z, 1, add ? 0xF: 0);
        }
        while (!add && min > 0) {
            min--;
            int lvl = chunk.getLight(x, min, z, 1);
            if (lvl > 0) {
                chunk.setLight(x, min, z, 1, 0);
            } else break;
        }
        int blockX = chunk.x<<SIZE_BITS | x;
        int blockZ = chunk.z<<SIZE_BITS | z;
        this.chunkTracker.flagLights(chunk.x, chunk.z, x, Math.max(min-1, 0), z, x, max, z);
        
        //run from max y to min y, check if a neighbour can receive light and doesn't see the sky
        // if so, trigger a sun light update on that block
        for (int y3 = max; y3 >= min; y3--) {
            for (int i = 0; i < 4; i++) {
                int bX = Dir.getDirX(i) + blockX;
                int bZ = Dir.getDirZ(i) + blockZ;
                if (isTransparent(bX, y3, bZ) && !canSeeSky(bX, y3, bZ)) {
                    this.lightUpdater.queueBlock(bX, y3, bZ, 1);
                }
            }
        }
    }
    @Override
    public void updateLight(int x, int y, int z) {
        this.lightUpdater.queueBlock(x, y, z, 1);
        this.lightUpdater.queueBlock(x, y, z, 0);
    }

    public void flagChunkLightUpdate(int x, int z) {
        this.lightUpdateQueue.add(GameMath.toLong(x, z));
    }
    /**
     * @return
     */
    public IChunkPopulator getChunkPopulator() {
        return this.populator;
    }
    /**
     * @return
     */
    public int deleteAllChunks() {
        PlayerChunkTracker tr = getPlayerChunkTracker();
        int l = this.players.size();
        for (int i = 0; i < l; i++) {
            Player player = this.players.get(i);
            tr.removePlayer(player);
        }
        this.lightUpdateQueue.clear();
        this.lightUpdater.ensureEmpty();
        int n = getChunkManager().deleteAllChunks();
        for (int i = 0; i < l; i++) {
            Player player = this.players.get(i);
            tr.addPlayer(player);
        }
        return n;
    }
}
