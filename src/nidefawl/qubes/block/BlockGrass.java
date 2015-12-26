package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.meshing.BlockSurface;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;

public class BlockGrass extends Block {


    BlockGrass(int id) {
        super(id);
        setCategory(BlockCategory.GROUND);
    }
    
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        if (pass < 0 || pass == 1 || faceDir == Dir.DIR_POS_Y) 
            return w.getBiomeFaceColor(x, y, z, faceDir, pass, BiomeColor.GRASS);
        return -1;
    }

    public int getTexture(int faceDir, int dataVal, int pass) {
        if (pass == 1) {
            return BlockTextureArray.getInstance().getTextureIdx(this.id, 2);
        }
        int idx = 0;
        if (faceDir == Dir.DIR_NEG_Y) {
            return BlockTextureArray.getInstance().getTextureIdx(Block.dirt.id, 0);
        } else if (faceDir != Dir.DIR_POS_Y) {
            idx = 1;
        }
        return BlockTextureArray.getInstance().getTextureIdx(this.id, idx);
    }
    public int getMeshedColor(BlockSurface bs) {
        return bs.axis==1&&bs.face==0?bs.faceColor:-1;
    }

    public int getTexturePasses() {
        return 2;
    }
    
    public boolean skipTexturePassSide(IBlockWorld w, int x, int y, int z, int axis, int side, int texPass) {
        return texPass == 1 && axis == 1;
    }
    @Override
    public int getNormalMap(int texture) {
        if (texture == BlockTextureArray.getInstance().getTextureIdx(this.id, 1))
            return 1;
        if (texture == BlockTextureArray.getInstance().getTextureIdx(Block.dirt.id, 0)) {
            return Block.dirt.getNormalMap(texture);
        }
        return 0;
    }
}
