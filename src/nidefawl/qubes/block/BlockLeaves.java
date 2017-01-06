/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.blocklight.LightChunkCache;
import nidefawl.qubes.texture.array.BlockTextureArray;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockLeaves extends Block {
    
    final int leavesColor;
    /**
     * @param id
     */
    public BlockLeaves(String id) {
        this(id, -1);
        setCategory(BlockCategory.LEAVES);
    }

    public BlockLeaves(String id, int rgb) {
        super(id, true);
        this.leavesColor = rgb;
    }
    
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
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
        return BlockTextureArray.getInstance().getTextureIdx(this.id, idxTexture);
    }
}
