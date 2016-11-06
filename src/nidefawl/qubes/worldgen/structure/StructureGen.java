package nidefawl.qubes.worldgen.structure;

import java.util.List;

import nidefawl.qubes.vec.ChunkPos;
import nidefawl.qubes.world.WorldServer;

public abstract class StructureGen {

    public abstract List<ChunkPos> prepare(WorldServer world, int chunkX, int chunkZ);

    public abstract int generate(WorldServer world);

}
