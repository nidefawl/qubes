/**
 * 
 */
package nidefawl.qubes.block;

import java.util.Arrays;
import java.util.List;

import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class BlockSlab extends BlockSliced {

    /**
     * @param id
     * @param transparent
     */
    final Block baseBlock;
    private final int overrideTextureIdx;
    public BlockSlab(int id, Block baseBlock) {
        this(id, baseBlock, -1);
    }
    public BlockSlab(int id, Block baseBlock, int textureIdx) {
        super(id, baseBlock.isTransparent());
        setTextures(new String[0]);
        this.baseBlock = baseBlock;
        this.overrideTextureIdx = textureIdx;
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
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        return this.baseBlock.getFaceColor(w, x, y, z, faceDir, pass);
    }

    @Override
    public float getAlpha() {
        return this.baseBlock.getAlpha();
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
    public int getBBs(World w, int ix, int iy, int iz, AABBFloat[] tmp) {
        int data = w.getData(ix, iy, iz)&0x3;
        AABBFloat bb = tmp[0];
        bb.set(0f, 0f, 0f, 1f, 0.5f, 1f);
        if (data == 1) {
            bb.offset(0, 0.5f, 0);
        } else if (data == 2) {
            bb.set(0, 0, 0, 1, 1, 1);
        }
        bb.offset(ix, iy, iz);
        return 1;
    }

    @Override
    public String getName() {
        return super.getName();
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
    public boolean canPlaceAt(BlockPlacer blockPlacer, BlockPos against, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        return super.canPlaceAt(blockPlacer, against, pos, fpos, offset, type, data) || (type == this.id && data != 2);
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
        System.out.println(yOff);
        Block b = Block.get(idAt);
        if (b != Block.air && b.isReplaceable())
            return 0;
        return (yOff >= 0.5 ? 1 : 0);
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
            return true;// show face, the code below is for neighbours other than slab
        }
        if (isVisibleBounds(w, axis, side, bb)) {
            return true;
        }
     // hide if y axis (the visible case was handled above)
        return axis != 1;// show if xz axis (the fullslab case was handled above)
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
    public int getTexture(int faceDir, int dataVal, int pass) {
        int idx = overrideTextureIdx;
        if (idx >= 0) {
            return BlockTextureArray.getInstance().getTextureIdx(baseBlock.id, idx);
        }
        if (this.textures.length == 0)
            return baseBlock.getTexture(faceDir, 0, pass);
        return BlockTextureArray.getInstance().getTextureIdx(this.id, Dir.isTopBottom(faceDir) ? 1 : 0);
    }
    public boolean isFullBB() {
        return false;
    }
    public boolean isOccludingBlock(IBlockWorld w, int x, int y, int z) {
        return isNormalBlock(w, x, y, z);
    }
    

    public void getQuarters(IBlockWorld w, int x, int y, int z, int[] quarters) {
        int data = w.getData(x, y, z) & 0x3;
        int start = data * 4;
        int end = start + 4;
        if (data == 2) {
            start = 0;
            end = quarters.length;
        }
        for (int i = 0; i < 8; i++) {
            quarters[i] = i >= start && i < end ? this.id : 0;
        }
    }
    
    @Override
    public int getItems(List<BlockStack> l) {
        l.add(new BlockStack(this.id));
        return 1;
    }
}
