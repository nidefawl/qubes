package nidefawl.qubes.world;

import static nidefawl.qubes.chunk.Chunk.SIZE_BITS;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import nidefawl.qubes.GameRegistry;
import nidefawl.qubes.blocklight.BlockLightThread;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketSWorldTime;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.PlayerChunkTracker;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.biomes.IBiomeManager;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.structure.GenTask;
import nidefawl.qubes.worldgen.structure.MineGen;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;

@SideOnly(value = Side.SERVER)
public class WorldServer extends World {

    private final GameServer         server;
    private final PlayerChunkTracker chunkTracker     = new PlayerChunkTracker(this);
    private final List<PlayerServer>       players          = Lists.newArrayList();
    private final ChunkManagerServer chunkServer;
    private final ITerrainGen              generator;
    private final IChunkPopulator populator;

    public Set<GenTask> generatorQueue = Sets.newConcurrentHashSet();
    
//    private Queue<Long> lightUpdateQueue = Queues.newConcurrentLinkedQueue();
    private Set<Long> lightUpdateQueue = Sets.newConcurrentHashSet();
    private final BlockLightThread lightUpdater;
    public HashMap<Integer, Entity>  entities   = new HashMap<>();                                             // use trove or something
    public ArrayList<Entity>         entityList = new ArrayList<>();                                           // use fast array list
    ArrayList<Entity>         entityRemove = new ArrayList<>();                                           // use fast array list


    public WorldServer(WorldSettings settings, GameServer server) {
        super(settings);
        this.server = server;
        this.chunkServer = (ChunkManagerServer) getChunkManager();
        this.generator = GameRegistry.newGenerator(this, settings);
        this.biomeManager = GameRegistry.newBiomeManager(this, this.generator, settings);
        this.populator = GameRegistry.newPopulator(this, this.generator, settings);
        this.generator.init();
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
        if (!this.settings.isFixedTime()) {
            this.settings.setTime(this.settings.getTime() + 1L);
        }
        this.entityRemove.clear();
        int size = this.entityList.size();
        for (int i = 0; i < size; i++) {
            Entity e = this.entityList.get(i);
            e.tickUpdate();
            if (e.flagRemove) {
                this.entityRemove.add(e);
            }
        }
        size = this.entityRemove.size();
        for (int i = 0; i < size; i++) {
            Entity e = this.entityRemove.get(i);
            this.removeEntity(e);
        }
        updateChunks();
        genStructures();
    }
    
    public void resyncTime() {
        PacketSWorldTime wTime = new PacketSWorldTime(this.getId(), this.getTime(), this.getDayLength(), this.settings.isFixedTime());
        this.broadcastPacket(wTime);
    }
    
    public void broadcastPacket(Packet p) {
        int l = this.players.size();
        for (int i = 0; i < l; i++) {
            PlayerServer PlayerServer = this.players.get(i);
            PlayerServer.sendPacket(p);
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
//                    System.out.println("remove missing chunk from queue");
                    it.remove();
                    continue;
                }
                Chunk c = getChunkIfNeightboursLoaded(x, z);
                if (c != null) {
                    it.remove();
                    long l1 = System.nanoTime();
                    this.calcSunLight(c);
                    long l2 = System.nanoTime();
                    long took = l2-l1;
                    ServerStats.add("calcSunlight.pre", took);
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
                        long l = System.nanoTime();
                        getChunkPopulator().populate(cPopulate);
                        long l2 = System.nanoTime();
                        long took = l2-l;
                        ServerStats.add("populate", took);
                        this.calcSunLight(cPopulate); 
                        long took2 = System.nanoTime()-l2;
                        ServerStats.add("calcSunlight.post", took2);
                    }
                }
            }
            maxChunks--;
            if (maxChunks <= 0) {
                break;
            }
        }

        long start1 = System.currentTimeMillis();
        long passed2 = System.currentTimeMillis()-start1;
        this.chunkTracker.sendBlockChanges();
        this.biomeManager.saveChanges();
        this.biomeManager.sendChanges();
        if (passed2 > 80)
            System.err.println("SLOW saveChanges "+passed2+"ms");
    }
    public void updateGeneratedChunks() {
        
    }
    public void unloadUnused() {

        this.chunkServer.saveAndUnloadChunks(200);
    }

    public void addPlayer(PlayerServer PlayerServer) {
        Vector3f position = PlayerServer.worldPositions.get(this.getUUID());
        if (position == null) {
            position = new Vector3f(this.getSpawnPosition());
            PlayerServer.worldPositions.put(this.getUUID(), position);
        }
        PlayerServer.world = this;
        PlayerServer.move(position);
        addEntity(PlayerServer);
        this.players.add(PlayerServer);
        this.chunkTracker.addPlayer(PlayerServer);
        PlayerServer.entTracker.joinWorld(this);
    }
    
    public void removePlayer(PlayerServer PlayerServer) {
        Vector3f position = new Vector3f(PlayerServer.pos);
        PlayerServer.worldPositions.put(this.getUUID(), position);
        removeEntity(PlayerServer);
        this.players.remove(PlayerServer);
        this.chunkTracker.removePlayer(PlayerServer);
        PlayerServer.entTracker.leaveWorld();
    }


    public boolean addEntity(Entity ent) {
        Entity e = this.entities.put(ent.id, ent);
        if (e != null) {
            throw new GameError("Entity with id " + ent.id + " already exists");
        }
        this.entityList.add(ent);
        ent.world = this;
        int size = this.players.size();
        for (int i = 0; i < size; i++) {
            PlayerServer p = this.players.get(i);
            p.entTracker.track(ent);
        }
        return true;
    }

    public boolean removeEntity(Entity ent) {
        Entity e = this.entities.remove(ent.id);
        if (e != null) {
            this.entityList.remove(e);
            ent.world = null;
            int size = this.players.size();
            for (int i = 0; i < size; i++) {
                PlayerServer p = this.players.get(i);
                p.entTracker.untrack(ent);
            }
            return true;
        }
        return false;
    }
    private Vector3f getSpawnPosition() {
        return new Vector3f(0, 200, 0);
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
    public void flagChunk(int x, int z) {
        this.chunkTracker.flagChunk(x, z);
    }

    public PlayerChunkTracker getPlayerChunkTracker() {
        return this.chunkTracker;
    }

    public void calcSunLight(Chunk c) {
        lightUpdater.queueChunk(c.x, c.z, 0);
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
        this.chunkTracker.flagLights(chunk.x, chunk.z, x, Math.max(min-1, 0), z, x, Math.min(max+1, worldHeightMinusOne), z);
        
        //run from max y to min y, check if a neighbour can receive light and doesn't see the sky
        // if so, trigger a sun light update on that block
        for (int y3 = max; y3 >= min; y3--) {
            for (int i = 0; i < 5; i++) { // < 5 because we also want center update (offx == 0 and offz == 0)
                int offx = i==0?-1:i==2?1:0;
                int offz = i==1?-1:i==3?1:0;
                int bX = offx + blockX;
                int bZ = offz + blockZ;
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
            PlayerServer PlayerServer = this.players.get(i);
            tr.removePlayer(PlayerServer);
        }
        this.lightUpdateQueue.clear();
        this.lightUpdater.ensureEmpty();
        int n = getChunkManager().deleteAllChunks();
        for (int i = 0; i < l; i++) {
            PlayerServer PlayerServer = this.players.get(i);
            tr.addPlayer(PlayerServer);
        }
        this.generatorQueue.clear();
        this.biomeManager.deleteAll();
        return n;
    }
    public int regenChunks(Collection<Long> chunks) {
        PlayerChunkTracker tr = getPlayerChunkTracker();
        int l = this.players.size();
        for (int i = 0; i < l; i++) {
            PlayerServer PlayerServer = this.players.get(i);
            tr.removePlayer(PlayerServer);
        }
        this.lightUpdateQueue.clear();
        this.lightUpdater.ensureEmpty();
        int n = getChunkManager().regenChunks(chunks);
        for (int i = 0; i < l; i++) {
            PlayerServer PlayerServer = this.players.get(i);
            tr.addPlayer(PlayerServer);
        }
//        this.biomeManager.deleteAll();
        return n;
    }

    public Entity getEntity(int entId) {
        return this.entities.get(entId);
    }
    
    public List<Entity> getEntityList() {
        return this.entityList;
    }
    /**
     * @return
     */
    public int getWorldType() {
        return this.biomeManager.getWorldType();
    }
    /**
     * @return
     */
    public IBiomeManager getBiomeManager() {
        return this.biomeManager;
    }
    public void queueGenTask(GenTask mineGen) {
        generatorQueue.add(mineGen);
    }
    public void genStructures() {
        if (this.generatorQueue.isEmpty()) return;
        Iterator<GenTask> it = this.generatorQueue.iterator();
        GenTask task;
        int limit = 3;
        while (it.hasNext()) {
            task = it.next();
            try {
                if (task.run()) {
                    it.remove();
                    if (limit--<0) {
                        return;
                    }
                }
            } catch(Exception e) {
                if (task != null) {
                    System.err.println("exception while running task "+task);
                    it.remove();
                }
                e.printStackTrace();
            }
        }
    }
}
