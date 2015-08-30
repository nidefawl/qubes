package nidefawl.qubes.meshing;

public class BlockSurfaceAir extends BlockSurface {
    
    public BlockSurfaceAir() {
        this.pass = -1;
    }
    
    
    
    public boolean mergeWith(BlockSurfaceAir c) {
        return false;
    }

}
