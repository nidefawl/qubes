/**
 * 
 */
package nidefawl.qubes.world;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface IBlockWorld {

    public int getType(int x, int y, int z);

    public boolean setType(int x, int y, int z, int type, int flags);

    public int getHeight(int x, int z);

}