/**
 * 
 */
package nidefawl.qubes.block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockWaterLily extends BlockPlantCrossedSquares {

    /**
     * @param id
     */
    public BlockWaterLily(int id) {
        super(id, false);
        setCategory(BlockCategory.FLOWER);
    }

    public int getRenderType() {
        return 12;
    }

}
