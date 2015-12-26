/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockSnowLayer extends Block {

	/**
	 * @param id
	 */
	public BlockSnowLayer(String id) {
		super(id, true);
	}
	
	@Override
	public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
		int data = w.getData(ix, iy, iz)&7;
		float height = (1/8f)*(data+1);
        bb.set(0, 0, 0, 1, height, 1);
        return bb;
	}
	@Override
	public int getBBs(World world, int x, int y, int z, AABBFloat[] tmp) {
		return 0;
	}
	
	@Override
	public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
		if (isVisibleBounds(w, axis, side, bb))
			return true;
		int data = w.getData(ix, iy, iz)&7;
		return data != 7;
	}
	
	@Override
	public int getLODPass() {
		return WorldRenderer.PASS_LOD;
	}
	
	@Override
	public boolean isOccluding() {
		return false;// super.isOccluding();
	}
	
	@Override
	public int getRenderType() {
		return 2;
	}
	@Override
	public boolean isNormalBlock(IBlockWorld w, int ix, int iy, int iz) {
		int data = w.getData(ix, iy, iz)&7;
		return data == 7;
	}
	
	@Override
	public boolean isReplaceable() {
		return true;
	}

}
