/**
 * 
 */
package nidefawl.qubes.world;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class BlockPlacer {

    private Player player;

    /**
     * @param serverHandlerPlay
     */
    public BlockPlacer(Player player) {
        this.player = player;
    }
    /**
     * @return the player
     */
    public Player getPlayer() {
        return this.player;
    }
    public World getWorld() {
        return this.player.world;
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param fx 
     * @param fy 
     * @param fz 
     * @param type
     * @param data
     * @param face 
     */
    public void tryPlace(BlockPos pos, Vector3f fpos, int type, int data, int face) {
        World w = this.player.world;
        if (pos.y < 0 || pos.y >= w.worldHeight) {
            return;
        }
//        int x, int y, int z, float fx, float fy, float fz
        Block b = Block.get(type);
        if (b == null) {
            this.player.kick("Invalid block received");
            return;
        }
        int typeAt = getWorld().getType(pos);
        Block bAgainst = Block.get(typeAt);
        if (b == Block.air) {
            tryHarvest(pos.x, pos.y, pos.z);
        } else {
            int offset = bAgainst == null ? -1 : bAgainst.placeOffset(this, pos, fpos, face, type, data);
            pos.offset(offset);
            if (b.canPlaceAt(this, pos, fpos, face, type, data)) {
                data = b.prePlace(this, pos, fpos, face, type, data);
                b.place(this, pos, fpos, face, type, data);
                b.postPlace(this, pos, fpos, face, type, data);
            }

        }
        
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param type
     * @param data
     */
    private void tryHarvest(int x, int y, int z) {
        World w = this.player.world;
        if (y < 0 || y >= w.worldHeight) {
            return;
        }
        int type = getWorld().getType(x, y, z);
        Block block = Block.get(type);
        if (block != null) {
            getWorld().setType(x, y, z, 0, Flags.MARK|Flags.LIGHT);
        }
    }
    /**
     * @param x
     * @param y
     * @param z
     * @param type 
     * @param data 
     * @param data2 
     * @return
     */
    public boolean canPlaceDefault(BlockPos pos, int offset, int type, int data) {
        int wtype = getWorld().getType(pos);
        Block block = Block.get(wtype);
        return block == null || block.isReplaceable();
    }
    /**
     * @param x
     * @param y
     * @param z
     * @param type
     * @param data
     */
    public void placeDefault(BlockPos pos, int offset, int type, int data) {
        Block block = Block.get(type);
        if (block != null) {
            getWorld().setTypeData(pos.x, pos.y, pos.z, type, data, Flags.MARK);
        }
        
    }

}
