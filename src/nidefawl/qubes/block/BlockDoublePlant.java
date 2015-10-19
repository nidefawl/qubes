package nidefawl.qubes.block;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.texture.ColorMap;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockDoublePlant extends Block {

    /**
     * 
     */
    public BlockDoublePlant(int id) {
        super(id, true);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir) {
        int d = w.getData(x, y, z)&7;
        if (d>1&&d<4) {
            return ColorMap.grass.get(0.8, 0.4);
        }
        return super.getFaceColor(w, x, y, z, faceDir);
    }

    public int getRenderType() {
        return 7;
    }

    @Override
    public boolean applyAO() {
        // TODO Auto-generated method stub
        return super.applyAO();
    }

    @Override
    public boolean isOccluding() {
        return false;
    }
    
    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getCollisionBB(nidefawl.qubes.world.World, int, int, int, nidefawl.qubes.vec.AABB)
     */
    @Override
    public int getBBs(World world, int x, int y, int z, AABBFloat[] aabb) {
        return 0;
    }

    public boolean isReplaceable() {
        return true;
    }
    @Override
    public boolean isFullBB() {
        return false;
    }
    public boolean applyRandomOffset() {
        return false;
    }
    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        float size = 8/16f;
        size/=2.0f;
        bb.set(size, 0, size, 1-size, 1-1*size, 1-size);
        return bb;
    }
    @Override
    public int getTexture(int faceDir, int dataVal) {
        boolean upper=(dataVal&8)!=0;
        int idx = dataVal&7;
        if (upper) {
            idx+=6;
        }
        return BlockTextureArray.getInstance().getTextureIdx(this.id, idx);
    }

    
    public int getRenderShadow() {
        return 0;
    }
}
