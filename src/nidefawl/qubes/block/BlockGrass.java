package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.Dir;

public class BlockGrass extends Block {


    BlockGrass(int id) {
        super(id);
    }
    
    @Override
    public int getColorFromSide(int side) {
        return side == Dir.DIR_POS_Y ? 0x74800E : super.getColorFromSide(side);
    }

    public int getTextureFromSide(int faceDir) {
        
        int idx = 0;
        if (faceDir == Dir.DIR_NEG_Y) {
            return BlockTextureArray.getInstance().getTextureIdx(Block.dirt.id, 0);
        } else if (faceDir != Dir.DIR_POS_Y) {
            idx = 1;
        }
        return BlockTextureArray.getInstance().getTextureIdx(this.id, idx);
    }
}
