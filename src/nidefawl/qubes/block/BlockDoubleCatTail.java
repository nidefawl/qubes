package nidefawl.qubes.block;

import nidefawl.qubes.texture.array.BlockTextureArray;

public class BlockDoubleCatTail extends BlockDoublePlant {

    public BlockDoubleCatTail(String id) {
        super(id);
    }

    @Override
    public int getTexturePasses() {
        return 2;
    }
    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
        if (pass == 1 && (dataVal&0x8)!=0) {
            return BlockTextureArray.getInstance().getTextureIdx(this.id, 2);
        }
        return super.getTexture(faceDir, dataVal, pass);
    }
}
