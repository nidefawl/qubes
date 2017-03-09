/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.texture.array.impl.gl.BlockTextureArrayGL;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class BlockPlantCrossedSquares extends Block {

    protected boolean multipass;
    /**
     * @param id
     */
    public BlockPlantCrossedSquares(String id) {
        this(id, false);
    }
    public BlockPlantCrossedSquares(String id, boolean multipass) {
        super(id, true);
        this.multipass=multipass;
        setCategory(BlockCategory.FLOWER);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        if (multipass && pass == 0) {
            return w.getBiomeFaceColor(x, y, z, Dir.DIR_POS_Y, pass, faceDir%2==0?BiomeColor.FOLIAGE:BiomeColor.GRASS);
        }
        return super.getFaceColor(w, x, y, z, Dir.DIR_POS_Y, pass);
    }

    public int getRenderType() {
        return 1;
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

    
    public int getRenderShadow() {
        return 0;
    }
    public int getTexturePasses() {
        return multipass?2:1;
    }
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        if (multipass) {
            return TextureArrays.blockTextureArray.getTextureIdx(this.id, pass);
        }
        return super.getTexture(faceDir, dataVal, pass);
    }

    
    @Override
    public int getLODPass() {
        return WorldRenderer.PASS_LOD;
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
        Block b = w.getBlock(x, y, z);
        return b.isFullBB() && b.isOccluding();
    }
}
