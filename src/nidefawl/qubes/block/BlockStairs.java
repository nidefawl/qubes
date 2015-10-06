/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.util.RayTrace;
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
    public int getBBs(World w, int ix, int iy, int iz, AABBFloat[] bb) {
        int data = w.getData(ix, iy, iz);
        int rot = data & 0x3;
        int bottomTop = (data>>2) & 0x1;
        AABBFloat bb1 = bb[0];
        AABBFloat bb2 = bb[1];
        bb1.set(0f, 0f, 0f, 1f, 0.5f, 1f);
        if (bottomTop == 1) {
            bb1.offset(0, 0.5f, 0);
        }
        setStairBB(bb2, rot, bottomTop, 0);
        bb1.offset(ix, iy, iz);
        bb2.offset(ix, iy, iz);
        return 2;
    }
    @Override
    public boolean raytrace(RayTrace rayTrace, World world, int x, int y, int z, Vector3f origin, Vector3f direction, Vector3f dirFrac) {
        int data = world.getData(x, y, z);
        int rot = data & 0x3;
        int bottomTop = (data>>2) & 0x1;
        AABBFloat bb1 = rayTrace.getTempBB();
        bb1.set(0f, 0f, 0f, 1f, 0.5f, 1f);
        if (bottomTop == 1) {
            bb1.offset(0, 0.5f, 0);
        }
        bb1.offset(x, y, z);
        boolean b = bb1.raytrace(rayTrace, origin, direction, dirFrac);
        setStairBB(bb1, rot, bottomTop, 0);
        bb1.offset(x, y, z);
        b |= bb1.raytrace(rayTrace, origin, direction, dirFrac);
        return b;
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
        if (isVisibleBounds(w, axis, side, bb)) {
            return true;
        }
        int data = w.getData(ix, iy, iz);
        int rot = data & 0x3;
        int bottomTop = (data>>2) & 0x1;
        if (axis == 1) {
            return bottomTop != side;
        } else {
            if (rot == 3 && axis == 2)
                return side == 1;
            if (rot == 1 && axis == 2)
                return side == 0;
            if (rot == 0 && axis == 0)
                return side == 0;
            if (rot == 2 && axis == 0)
                return side == 1;
        }
        return true;
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

    /**
     * @param bb
     * @param rot
     * @param topBottom
     * @param i
     */
    public static void setStairBB(AABBFloat bb, int rot, int topBottom, int i) {
        switch (rot) {
            case 2:
                bb.set(0,0,0,0.5f,0.5f,1);
                break;
            case 3:
                bb.set(0,0,0,1,0.5f,0.5f);
                break;
            case 0:
                bb.set(0.5f,0,0,1,0.5f,1);
                break;
            default:
            case 1:
                bb.set(0,0,0.5f,1,0.5f,1);
                break;
        }
        if (topBottom == 0) {
            bb.minY+=0.5f;
            bb.maxY+=0.5f;   
        }
    }
}
