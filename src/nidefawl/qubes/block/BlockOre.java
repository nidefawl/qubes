package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.world.IBlockWorld;

public class BlockOre extends Block {

    private Block baseBlock;

    public BlockOre(int id, Block b) {
        super(id);
        this.textures = NO_TEXTURES;
        this.baseBlock = b;
    }
    
    @Override
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        return super.getRenderBlockBounds(w, ix, iy, iz, bb);
    }
    @Override
    public int getRenderType() {
        return 13;
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
    public int getTexture(int faceDir, int dataVal, int pass) {
        return baseBlock.getTexture(faceDir, dataVal, pass);
    }
}
