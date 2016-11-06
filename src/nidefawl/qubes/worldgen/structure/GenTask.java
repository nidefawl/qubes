package nidefawl.qubes.worldgen.structure;

import java.util.List;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.server.ChunkManagerServer;
import nidefawl.qubes.vec.ChunkPos;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;

public class GenTask {
    private StructureGen generator;
    final int chunkX;
    final int chunkZ;
    final WorldServer world;
    List<ChunkPos> neededChunks;
    private boolean prepared = false;
    public GenTask(World w, int x, int z, StructureGen gen) {
        this.generator = gen;
        this.chunkX = x;
        this.chunkZ = z;
        this.world = (WorldServer) w;
    }


    public boolean run() {
        if (!this.prepared) {
            this.prepared = true;
            neededChunks = this.generator.prepare(this.world, this.chunkX, this.chunkZ);
//            System.out.println("prepare "+neededChunks);
        }
        if (this.neededChunks != null) {
//            System.out.println("neededChunks "+neededChunks);
            ChunkManagerServer server = (ChunkManagerServer) this.world.getChunkManager();
            for (ChunkPos c : this.neededChunks) {
                
                
                Chunk chunk = server.get(c.x, c.z);
                if (chunk == null) {
//                    System.out.println("queueLoadChecked");
                    server.queueLoadChecked(c.x, c.z);
                    return false;
                }
            }
            int built = this.generator.generate(this.world);

            for (ChunkPos c : this.neededChunks) {
                this.world.flagChunk(c.x, c.z);
            }
        }
        return true;
    }

}
