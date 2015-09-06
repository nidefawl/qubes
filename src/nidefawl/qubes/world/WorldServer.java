package nidefawl.qubes.world;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;

import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.PlayerChunkTracker;
import nidefawl.qubes.worldgen.AbstractGen;
import nidefawl.qubes.worldgen.TerrainGenerator2;
import nidefawl.qubes.worldgen.TestTerrain2;

public class WorldServer extends World {

    private File worldDirectory;
    private final GameServer server;
    private WorldSettings serverWorldSettings;
    private final PlayerChunkTracker chunkTracker = new PlayerChunkTracker(this);
    private final List<Player> players = Lists.newArrayList();
    private final ChunkManagerServer chunkServer;
    //    private AbstractGen generator;
    private AbstractGen generator;
    public WorldServer(WorldSettings settings, GameServer server) {
        super(settings);
        this.serverWorldSettings = settings;
        this.worldDirectory = settings.getWorldDirectory();
        this.server = server;
        this.chunkServer = (ChunkManagerServer) getChunkManager();
        //        this.generator = new TestTerrain2(this, this.seed);
        this.generator = new TerrainGenerator2(this, this.getSeed());
    }
    public AbstractGen getGenerator() {
        return generator;
    }
//  long lastCheck = System.currentTimeMillis();
//  int lastTime = this.time;
  public void tickUpdate() {
      super.tickUpdate();
      updateChunks();
  }
    @Override
    public ChunkManager makeChunkManager() {
        return new ChunkManagerServer(this);
    }
    //    
    //    public AbstractGen getGenerator() {
    //        return generator;
    //    }

    public File getWorldDirectory() {
        return worldDirectory;
    }

    public GameServer getServer() {
        return this.server;
    }
    public void updateChunks() {
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
}
