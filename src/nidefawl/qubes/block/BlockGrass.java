package nidefawl.qubes.block;

import nidefawl.qubes.meshing.BlockSurface;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.texture.ColorMap;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.IBlockWorld;

public class BlockGrass extends Block {


    BlockGrass(int id) {
        super(id);
    }
    
    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        return ColorMap.grass.get(0.8, 0.4);
    }

    public int getTexture(int faceDir, int dataVal, int pass) {
        
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
}
