/**
 * 
 */
package nidefawl.qubes.block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockGlass extends Block {
    
    public BlockGlass(String id) {
        super(id, true);
    }
    
    
    @Override
    public int getRenderShadow() {
        return 0;
    }
    
    @Override
    public boolean isOccluding() {
        return false;
    }

}
