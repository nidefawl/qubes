/**
 * 
 */
package nidefawl.qubes.meshing;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockSurfaceHidden extends BlockSurfaceAir {
    public BlockSurfaceHidden() {
        this.pass = -2;
        this.resolved = true;
    }
}
