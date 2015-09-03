package nidefawl.qubes.chunk;

import nidefawl.qubes.BootClient;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.world.World;

public class RegionLoadTask {

    public int worldInstance;
    Region     region;

    public boolean prepare(Region region) {
        this.region = region;
        region.isEmpty = true;
        region.state = Region.STATE_LOADING;
        return true;
    }

    public boolean finish(int id) {
        this.region.state = Region.STATE_LOAD_COMPLETE;
        return true;
    }

    public boolean isValid(int id) {
        return this.worldInstance == id;
    }

    public boolean updateFromThread() {
        World w = BootClient.instance.getWorld();
        if (w != null) {
            for (int x = 0; x < Region.REGION_SIZE; x++) {
                for (int z = 0; z < Region.REGION_SIZE; z++) {
                    long l = System.nanoTime();
                    Chunk c = w.generateChunk(region.rX << Region.REGION_SIZE_BITS | x, region.rZ << Region.REGION_SIZE_BITS | z);
                    c.checkIsEmtpy();
                    Stats.timeWorldGen += (System.nanoTime()-l) / 1000000.0D;
                    region.setChunk(x, z, c);
                    if (!c.isEmpty()) {
                        region.isEmpty = false;
                    }
                }
            }
            return true;
        }
        return false;
    }

}
