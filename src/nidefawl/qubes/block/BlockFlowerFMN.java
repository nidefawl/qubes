/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.texture.array.impl.gl.BlockTextureArrayGL;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockFlowerFMN extends BlockPlantCrossedSquares {

    /**
     * @param id
     * @param multipass
     */
    public BlockFlowerFMN(String id) {
        super(id, true);
        setCategory(BlockCategory.FLOWER);
    }
    /* (non-Javadoc)
     * @see nidefawl.qubes.block.Block#getColorFromSide(int)
     */
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        if (multipass && pass != 1) {
            return w.getBiomeFaceColor(x, y, z, faceDir, pass, (multipass && pass == 2)?BiomeColor.GRASS:BiomeColor.FOLIAGE);
        }
        return super.getFaceColor(w, x, y, z, faceDir, pass);
    }
    public int getTexturePasses() {
        return multipass?3:1;
    }
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        if (multipass) {
            return TextureArrays.blockTextureArray.getTextureIdx(this.id, pass);
        }
        return super.getTexture(faceDir, dataVal, pass);
    }


}
