package nidefawl.qubes.block;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

public class BlockOre extends BlockModelled {

    private Block baseBlock;

    public BlockOre(int id, Block b) {
        super(id, true);
        this.textures = NO_TEXTURES;
        setModels("models/block.qmodel");
        this.baseBlock = b;
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
//        if (pass == 1) {
//            float fdataVal=dataVal/25.0f;
//            dataVal = (int) (10*fdataVal);
//            int v = dataVal < 0 ? 0 : dataVal > 9 ? 9 : dataVal;
//            return BlockTextureArray.getInstance().getTextureIdx(this.id, v);
//        }
        return baseBlock.getTexture(faceDir, dataVal, pass);
    }
//    @Override
//    public int getTexturePasses() {
//        return 2;
//    }
    public void onBlockMine(World w, BlockPos pos, PlayerServer player, ItemStack itemstack) {
//        int data = w.getData(pos);
//        data++;
//        if (data >= 25) {
//            w.setType(pos.x, pos.y, pos.z, 0, Flags.MARK|Flags.LIGHT);
//        } else {
//            w.setData(pos.x, pos.y, pos.z, data, Flags.MARK|Flags.LIGHT);
//        }
    }
}
