package nidefawl.qubes.meshing;

import nidefawl.qubes.chunk.RegionCache;

public class BlockSurfaceAir extends BlockSurface {
    
    public BlockSurfaceAir() {
        this.pass = -1;
        this.resolved = true;
    }
    
    @Override
    public boolean mergeWith(RegionCache cache, BlockSurface c) {
        return super.mergeWith(cache, c);
    }
    

}
