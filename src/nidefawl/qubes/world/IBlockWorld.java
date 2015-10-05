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

    /** Sets the block data value (8 bits)
     * @param x
     * @param y
     * @param z
     * @param type
     * @param render
     * @return
     */
    boolean setData(int x, int y, int z, int type, int render);

    /** Gets the block data value (8 bits)
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    public int getData(int ix, int iy, int iz);

    /**
     * @param ix
     * @param iy
     * @param iz
     * @param offsetId
     * @return
     */
    public boolean isNormalBlock(int ix, int iy, int iz, int offsetId);

    /**
     * @param x
     * @param y
     * @param z
     * @param type
     * @param data
     * @param render
     * @return
     */
    boolean setTypeData(int x, int y, int z, int type, int data, int render);

}