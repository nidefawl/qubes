package nidefawl.qubes.entity;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.server.ChunkReader;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.network.server.ServerHandler;
import nidefawl.qubes.player.PlayerData;
import nidefawl.qubes.server.compress.CompressChunks;
import nidefawl.qubes.server.compress.ICompressTask;
import nidefawl.qubes.server.compress.CompressThread;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.WorldServer;

public class Player extends Entity {

    public String        name;
    public ServerHandler netHandler;
    public boolean flying;
    public int chunkX;
    public int chunkZ;
    public boolean chunkTracked;
     Set<Long> chunks = Sets.newHashSet();
     Set<Long> sendChunks = Sets.newHashSet();

    public Player() {
        super();
    }

    public void load(PlayerData data) {
        this.pos.set(data.pos);
        this.lastPos.set(data.pos);
        this.flying = data.flying;
    }

    public PlayerData save() {
        PlayerData data = new PlayerData();
        data.pos = new Vector3f(this.pos);
        data.flying = this.flying;
        data.world = this.world != null ? this.world.getUUID() : null; 
        return data;
    }

    public int getChunkLoadDistance() {
        return 6;
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
                    CompressThread.submit(new CompressChunks(this.world.getId(), list, this.netHandler));
                }
            }
        }
    }

    public void watchingChunk(long hash) {
        if (this.chunks.add(hash)) {
            this.sendChunks.add(hash);
        }
    }

    public void unwatchingChunk(long hash) {
        if (this.chunks.remove(hash)) {
            this.sendChunks.remove(hash);
        }
    }

}
