/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.blocklight.LightChunkCache;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.texture.array.impl.gl.BlockTextureArrayGL;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockLeaves extends Block {
    
    final boolean hasOverrideColor;
    int leavesColor;
    /**
     * @param id
     */
    public BlockLeaves(String id) {
        this(id, false, -1);
        setCategory(BlockCategory.LEAVES);
    }

    public BlockLeaves(String id, boolean hasOverrideColor, int rgb) {
        super(id, true);
        this.hasOverrideColor = hasOverrideColor;
        this.leavesColor = rgb;
    }

    public void setOverrideColor(int rgb) {
        this.leavesColor = rgb;
    }
    
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
      if (hasOverrideColor) {
          return leavesColor;
      }
      return w.getBiomeFaceColor(x, y, z, faceDir, pass, BiomeColor.LEAVES);
    }

    @Override
    public boolean applyAO() {
        return true;
    }
    
    @Override
    public boolean isOccluding() {
        return true;
    }
    
    public int getLightLoss(LightChunkCache c, int i, int j, int k, int type) {
        return 3;
    }
    
    @Override
    public boolean renderMeshedAndNormal() {
        return true;
    }
    @Override
    public int getRenderType() {
        return 15;
    }
    public int getTexture(int faceDir, int dataVal, int pass) {
        int idxTexture = pass > 0 ? Math.min(this.textures.length-1, pass) : 0;
//        System.out.println(this.textures.length);
        return TextureArrays.blockTextureArray.getTextureIdx(this.id, idxTexture);
    }
}
