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
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.PlayerChunkTracker;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TripletLongHash;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.worldgen.AbstractGen;
import nidefawl.qubes.worldgen.TerrainGenerator2;
import nidefawl.qubes.worldgen.TestTerrain2;

public class WorldServer extends World {

    private final GameServer         server;
    private final PlayerChunkTracker chunkTracker     = new PlayerChunkTracker(this);
    private final List<Player>       players          = Lists.newArrayList();
    private final ChunkManagerServer chunkServer;
    private AbstractGen              generator;
//    private Queue<Long> lightUpdateQueue = Queues.newConcurrentLinkedQueue();
    private Set<Long> lightUpdateQueue = Sets.newConcurrentHashSet();
    private final BlockLightThread lightUpdater;

    public WorldServer(WorldSettings settings, GameServer server) {
        super(settings);
        System.out.println("World directory at "+settings.getWorldDirectory().getAbsolutePath());
        this.server = server;
        this.chunkServer = (ChunkManagerServer) getChunkManager();
        this.generator = new TestTerrain2(this, this.getSeed());
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

    
    
    
    public AbstractGen getGenerator() {
        return generator;
    }

    public void tickUpdate() {
        super.tickUpdate();
        updateChunks();
    }

    @Override
    public ChunkManager makeChunkManager() {
        return new ChunkManagerServer(this, ((WorldSettings) this.settings).getWorldDirectory());
    }

    public GameServer getServer() {
        return this.server;
    }

    public void updateChunks() {
        if (!this.lightUpdateQueue.isEmpty()) {
            Iterator<Long> it = lightUpdateQueue.iterator();
            while (it.hasNext()) {
                long l = it.next();
                int x = GameMath.lhToX(l);
                int z = GameMath.lhToZ(l);
                Chunk c = getChunkIfNeightboursLoaded(x, z);
                if (c != null) {
                    it.remove();
                    System.out.println("calc sun "+x+"/"+z);
                    this.calcSunLight(c);
                }
            }
        }
        for (int i = 0; i < players.size(); i++) {
            this.chunkTracker.update(players.get(i));
        }
        this.chunkTracker.recheckIfRequiredChunksLoaded();
        this.chunkTracker.sendBlockChanges();
        this.chunkServer.saveAndUnloadChunks(10);
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
        int blockX = chunk.x<<SIZE_BITS | x;
        int blockZ = chunk.z<<SIZE_BITS | z;
        this.chunkTracker.flagLights(chunk.x, chunk.z, x, min, z, x, max, z);
        
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
}
