package nidefawl.qubes.chunk;

import static nidefawl.qubes.render.region.RegionRenderer.*;

import java.util.Arrays;

import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.world.World;

/**
 * Cache neighbour regions for rendering
 * @author Michael
 *
 */
public class RegionCache {
    final public Region[] regions = new Region[9]; //TODO: are corners required?

    public void set(int x, int z, Region region) {
        this.regions[(x+1)*3+(z+1)] = region;
    }
    
    public Region get(int x, int z) {
        return this.regions[(x+1)*3+(z+1)];
    }

    public void flush() {
        Arrays.fill(this.regions, null);
    }

    
    public int getTypeId(int i, int j, int k) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0;
        }
        int regionX = 0;
        int regionZ = 0;
        if (i < 0) {
            i += RegionRenderer.REGION_SIZE_BLOCKS;
            regionX--;
        } else if (i >= RegionRenderer.REGION_SIZE_BLOCKS) {
            i -= RegionRenderer.REGION_SIZE_BLOCKS;
            regionX++;
        }
        if (k < 0) {
            k += RegionRenderer.REGION_SIZE_BLOCKS;
            regionZ--;
        } else if (k >= RegionRenderer.REGION_SIZE_BLOCKS) {
            k -= RegionRenderer.REGION_SIZE_BLOCKS;
            regionZ++;
        }
        Region region = get(regionX, regionZ);
        return region != null ? region.getTypeId(i, j, k) : 0;
    }

    
}
