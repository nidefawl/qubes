/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockGrassBush extends BlockPlantCrossedSquares {

    /**
     * @param id
     */
    public BlockGrassBush(int id) {
        super(id);
        setCategory(BlockCategory.PLANT);
    }

    @Override
    public boolean applyRandomOffset() {
        return true;
    }
    @Override
    public int getLODPass() {
        return WorldRenderer.PASS_LOD;
    }
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        return w.getBiomeFaceColor(x, y, z, Dir.DIR_POS_Y, pass, faceDir%2==0?BiomeColor.FOLIAGE:BiomeColor.GRASS);
    }
    
    public boolean isWaving() {
        return true;
    }
}
