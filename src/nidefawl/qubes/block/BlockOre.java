package nidefawl.qubes.block;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.texture.array.BlockTextureArray;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

public class BlockOre extends Block {

    private Block baseBlock;

    public BlockOre(String id, Block b) {
        super(id, false);
        this.textures = NO_TEXTURES;
        ArrayList<String> list = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            list.add("destroy/destroy_stage_"+i);
        }
        setTextures(list.toArray(new String[list.size()]));
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
      if (pass == 1) {
          int v = dataVal < 0 ? 0 : dataVal > 9 ? 9 : dataVal;
          return BlockTextureArray.getInstance().getTextureIdx(this.id, v);
      }
        return baseBlock.getTexture(faceDir, dataVal, pass);
    }
    
    @Override
    public int getTexturePasses() {
        return 2;
    }

    @Override
    public void onBlockMine(BlockPlacer placer, World w, BlockPos pos, PlayerServer player, ItemStack itemstack) {
        int data = w.getData(pos);
        data++;
        if (data >= 10) {
            w.setType(pos.x, pos.y, pos.z, 0, Flags.MARK|Flags.LIGHT);
        } else {
            w.setData(pos.x, pos.y, pos.z, data, Flags.MARK|Flags.LIGHT);
        }
    }
    
    @Override
    public boolean skipTexturePassSide(IBlockWorld w, int x, int y, int z, int axis, int side, int texPass) {
        if (texPass > 0) {
            int data = w.getData(x, y, z);
            return data == 0;
        }
        return super.skipTexturePassSide(w, x, y, z, axis, side, texPass);
    }
    @Override
    public int getNormalMap(int texture) {
        return this.baseBlock.getNormalMap(texture);
    }
    @Override
    public int getRenderType() {
        return 14;
    }
}
