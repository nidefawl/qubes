/**
 * 
 */
package nidefawl.qubes.block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockLongGrass extends BlockPlantCrossedSquares {

    /**
     * @param id
     */
    public BlockLongGrass(int id) {
        super(id);
    }

    public boolean applyRandomOffset() {
        return true;
    }
}
