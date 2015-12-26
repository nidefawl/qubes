/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockPlantFlat extends Block {

    /**
     * @param id
     */
    public BlockPlantFlat(String id) {
        super(id, true);
        this.blockBounds.set(0, 0, 0, 1, 2/16f, 1);
        setCategory(BlockCategory.PLANT);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        return w.getBiomeFaceColor(x, y, z, faceDir, pass, BiomeColor.FOLIAGE);
    }

    public int getRenderType() {
        return 11;
    }

    @Override
    public boolean isOccluding() {
        return false;
    }
    
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
        bb.set(this.blockBounds);
        float h = 0.15f;
        bb.minX = h;
        bb.minY=bb.maxY=0.02f;
        bb.maxX=1-h;
        bb.minZ = h;
        bb.maxZ=1-h;
        return bb;
    }

    
    public int getRenderShadow() {
        return 0;
    }

    
    @Override
    public int getLODPass() {
        return WorldRenderer.PASS_LOD;
    }
}
