/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class BlockStairs extends Block {

    /**
     * @param id
     * @param transparent
     */
    final Block baseBlock;

    public BlockStairs(int id, Block baseBlock) {
        super(id, baseBlock.isTransparent());
        this.baseBlock = baseBlock;
    }

    @Override
    public int getRenderType() {
        return 3;
    }

    @Override
    public boolean isOccluding() {
        return this.baseBlock.isOccluding();
    }

    @Override
    public int getLightValue() {

        return this.baseBlock.getLightValue();
    }

    @Override
    public boolean applyAO() {
        return this.baseBlock.applyAO();
    }

    @Override
    public boolean isTransparent() {
        return this.baseBlock.isTransparent();
    }

    @Override
    public AABB getCollisionBB(World w, int ix, int iy, int iz, AABB bb) {
        int data = w.getData(ix, iy, iz)&0x3;
        bb.set(0f, 0f, 0f, 1f, 0.5f, 1f);
        if (data == 1) {
            bb.offset(0, 0.5f, 0);
        } else if (data == 2) {
            bb.set(0, 0, 0, 1, 1, 1);
        }
        bb.offset(ix, iy, iz);
        return bb;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public int getColorFromSide(int side) {
        return this.baseBlock.getColorFromSide(side);
    }

    @Override
    public float getAlpha() {
        return this.baseBlock.getAlpha();
    }

    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        int data = w.getData(ix, iy, iz)&0x3;
        bb.set(0f, 0f, 0f, 1f, 0.5f, 1f);
        if (data == 1) {
            bb.offset(0, 0.5f, 0);
        } else if (data == 2) {
            bb.set(0, 0, 0, 1, 1, 1);
        }
        return bb;
    }

    @Override
    public boolean canPlaceAt(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        return super.canPlaceAt(blockPlacer, pos, fpos, offset, type, data) || (type == this.id && data != 2);
    }
    @Override
    public int prePlace(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        int sData = 0;
        if (offset != Dir.DIR_POS_Y && (offset == Dir.DIR_NEG_Y || (fpos.y%1.0f) >= 0.5f)) // placed against a bottom face
            sData = 4;
        int rot = blockPlacer.getPlayer().getLookDir();
        int rotdata = 0;
        switch (rot) {
            case Dir.DIR_NEG_X:
                rotdata = 1;
                break;
            case Dir.DIR_POS_X:
                rotdata = 3;
                break;
            case Dir.DIR_NEG_Z:
                rotdata = 2;
                break;
            case Dir.DIR_POS_Z:
                rotdata = 0;
                break;
        }
        return sData|rotdata;
    }

    @Override
    public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
        return super.isFaceVisible(w, ix, iy, iz, axis, side, block, bb);
    }
    @Override
    public boolean isNormalBlock(IBlockWorld w, int ix, int iy, int iz) {
//      int data = w.getData(ix, iy, iz) & 0x3;
        //return data == 2;
        return false;
    }
    /**
     * @return
     */
    public boolean isStairs() {
        return true;
    }
    
    @Override
    public int getTextureFromSide(int faceDir) {
        if (this.textures.length == 0)
            return baseBlock.getTextureFromSide(faceDir);
        return BlockTextureArray.getInstance().getTextureIdx(this.id, Dir.isTopBottom(faceDir) ? 1 : 0);
    }
    public boolean isFullBB() {
        return false;
    }
}
