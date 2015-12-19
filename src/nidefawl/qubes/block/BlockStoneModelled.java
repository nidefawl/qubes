package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;

public class BlockStoneModelled extends BlockModelled {

    private Block baseBlock;


    private final int overrideTextureIdx;
    public BlockStoneModelled(int id, Block baseBlock) {
        this(id, baseBlock, -1);
    }
    public BlockStoneModelled(int id, Block baseBlock, int overrideTextureIdx) {
        super(id);
        this.textures = NO_TEXTURES;
        this.baseBlock = baseBlock;
        this.overrideTextureIdx = overrideTextureIdx;
    }
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        int idx = overrideTextureIdx;
        if (idx >= 0) {
            return BlockTextureArray.getInstance().getTextureIdx(baseBlock.id, idx);
        }
        if (this.textures.length == 0)
            return baseBlock.getTexture(faceDir, dataVal, pass);
        return super.getTexture(faceDir, dataVal, pass);
    }

}
