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
public class BlockSlab extends Block {

    /**
     * @param id
     * @param transparent
     */
    final Block baseBlock;

    public BlockSlab(int id, Block baseBlock) {
        super(id, baseBlock.isTransparent());
        this.baseBlock = baseBlock;
    }

    @Override
    public int getRenderType() {
        return 2;
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
        int idAt = blockPlacer.getWorld().getType(pos);
        
        if (idAt == this.id) {
            return 2;
        }
        if (offset == Dir.DIR_POS_Y) // placed against a top face
            return 0;
        if (offset == Dir.DIR_NEG_Y) // placed against a bottom face
            return 1;
        float yOff = (fpos.y%1.0f);
        Block b = Block.get(idAt);
        if (b != null && b.isReplaceable())
            return 0;
        return yOff >= 0.5 ? 1 : 0;
    }
    @Override
    public int placeOffset(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        int dataAt1 = blockPlacer.getWorld().getData(pos) & 0x3;
        if (offset == Dir.DIR_POS_Y && type == this.id && dataAt1 == 0) {
            return -1;
        }
        if (offset == Dir.DIR_NEG_Y && type == this.id && dataAt1 == 1) {
            return -1;
        }
        return offset;
    }

    @Override
    public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
        int offx = axis == 0 ? 1-side*2 : 0;
        int offy = axis == 1 ? 1-side*2 : 0;
        int offz = axis == 2 ? 1-side*2 : 0;
        int data = w.getData(ix, iy, iz);
        data &= 0x3;
        if (data == 2) {
            return super.isFaceVisible(w, ix, iy, iz, axis, side, block, bb);
        }
        //if axis == Y and face == offset (half top always shows bottom, bottom slab always shows top)
        if (axis == 1 && data != side) {
            return true; // render the face 
        }
        if (block.isSlab()) {
            // adjacent blocks position (0 = bottom, 1 = top, 2 = bottom+top (=full block))
            int dataAdj = w.getData(ix - offx, iy - offy, iz - offz) & 0x3;

            if (axis != 1) { // on x+z axis hide face if the adj slab is on the same level 
                if (dataAdj == data)
                    return false;
                //otherwise render
            } else { // only top and bottom faces here
                if (data == 2) { // if this is full
                    return dataAdj != 2 && dataAdj != side; // render if other is not full and directly attached to this
                }
                if (dataAdj == 2) { // if adj slab is full
                    return data != 2 && data != side;  // render if this is not full and directly attached to other
                }
                return data == dataAdj; // render face if other is not directly connected to this one (=not same level)
            }
            return true;
        }
        if (axis == 1) {
            return false;
        }
        return true;
//        if (axis != 1) {
//            System.out.println("axis "+axis+", data "+data);
//            return data != 2;
//        }
//        return data == side && super.isFaceVisible(w, ix, iy, iz, axis, side, block, bb);
//        return super.isFaceVisible(w, ix, iy, iz, axis, side, block, bb);
//        int type = w.getType(ix - offx, iy - offy, iz - offz);
//        boolean b = w.isNormalBlock(ix - offx, iy - offy, iz - offz, -1);
//        System.out.printf("%d %d %d = %d = normal="+b+"\n", offx, offy, offz, type);
//        if (b) {
//
//            System.out.printf("hide "+ix+","+iy+","+iz+" axis "+axis+", side "+side+"\n");
//        }
//        return !b;
    }
    @Override
    public boolean isNormalBlock(IBlockWorld w, int ix, int iy, int iz) {
      int data = w.getData(ix, iy, iz) & 0x3;
        return data == 2;
//        return false;
    }
    /**
     * @return
     */
    public boolean isSlab() {
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