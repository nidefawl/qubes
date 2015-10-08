/**
 * 
 */
package nidefawl.qubes.block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockSand extends Block {

    /**
     * @param i
     */
    public BlockSand(int i) {
        super(i, false);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getColorFromSide(int side) {
        return super.getColorFromSide(side);//0x989898
    }
}
