package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.blocklight.LightChunkCache;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.texture.array.impl.gl.BlockTextureArrayGL;
import nidefawl.qubes.world.IBlockWorld;

public class BlockHedgeModelled extends BlockModelled {

    private Block baseBlock;


    private final int overrideTextureIdx;
    public BlockHedgeModelled(String id, Block baseBlock) {
        this(id, baseBlock, -1);
    }
    public BlockHedgeModelled(String id, Block baseBlock, int overrideTextureIdx) {
        super(id, true);
        this.textures = NO_TEXTURES;
        this.baseBlock = baseBlock;
        this.overrideTextureIdx = overrideTextureIdx;
    }
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        int idx = overrideTextureIdx;
        if (idx >= 0) {
            return TextureArrays.blockTextureArray.getTextureIdx(baseBlock.id, idx);
        }
        if (this.textures.length == 0)
            return baseBlock.getTexture(faceDir, dataVal, pass);
        return super.getTexture(faceDir, dataVal, pass);
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
        return false;
    }

    @Override
    public boolean isFullBB() {
        return false;
    }
    
    public int getLightLoss(LightChunkCache c, int i, int j, int k, int type) {
        return 3;
    }

}
