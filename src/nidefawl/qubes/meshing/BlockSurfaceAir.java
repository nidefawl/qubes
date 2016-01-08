package nidefawl.qubes.meshing;

public class BlockSurfaceAir extends BlockSurface {
    
    public BlockSurfaceAir() {
        this.pass = -1;
        this.resolved = true;
    }

    public BlockSurface copy() {
        throw new UnsupportedOperationException("Cannot copy BlockSurfaceAir");
    }
}
