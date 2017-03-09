package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.texture.array.impl.gl.BlockTextureArrayGL;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
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
    public BlockDoublePlant(String id) {
        super(id, true);
        setCategory(BlockCategory.PLANT);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        return w.getBiomeFaceColor(x, y, z, faceDir, pass, BiomeColor.FOLIAGE);
    }
    @Override
    public void postPlace(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        pos = pos.copy();
        pos.offset(Dir.DIR_POS_Y);
        blockPlacer.placeDefault(pos, offset, type, data|8);
    }

    public int getRenderType() {
        return 7;
    }

    @Override
    public boolean applyAO() {
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
    public int getTexture(int faceDir, int dataVal, int pass) {
        boolean upper=(dataVal&8)!=0;
        int idx = dataVal&7;
        if (upper) {
            idx+=1;
        }
        return TextureArrays.blockTextureArray.getTextureIdx(this.id, idx);
    }

    
    public int getRenderShadow() {
        return 0;
    }
    
    public boolean isWaving() {
        return true;
    }
    public void onUpdate(World w, int ix, int iy, int iz, int from) {
        if (!canStayOn(w, ix, iy-1, iz)) {
            w.setType(ix, iy, iz, 0, Flags.MARK|Flags.LIGHT);
        }
    }
    public boolean canStayOn(World w, int x, int y, int z) {
        int dataVal = w.getData(x, y, z);
        boolean upper=(dataVal&8)!=0;
        Block b = w.getBlock(x, y, z);
        if (upper)
            return b == this;
        return b.isFullBB() && b.isOccluding();
    }
}
