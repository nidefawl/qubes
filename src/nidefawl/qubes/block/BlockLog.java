/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.texture.array.impl.gl.BlockTextureArrayGL;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.structure.tree.Tree;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockLog extends Block {

    private int index;

    /**
     * @param id
     * @param i 
     */
    public BlockLog(String id, int i) {
        super(id, false);
        setCategory(BlockCategory.LOG);
        this.index = i;
    }
    
    public int getIndex() {
        return this.index;
    }

    @Override
    public int getTexture(int faceDir, int dataVal, int pass) {
      if (pass == 1) {
          dataVal = dataVal>>2;
          int v = dataVal < 0 ? 0 : dataVal > 9 ? 9 : dataVal;
          return TextureArrays.blockTextureArray.getTextureIdx(this.id, 2+v);
      }
      int rot = dataVal & 3;
      int topFace = Dir.DIR_POS_Y;
      int bottomFace = Dir.DIR_NEG_Y;
      int idx = 0;
      switch (rot) {
          case 1:
              topFace = Dir.DIR_POS_X;
              bottomFace = Dir.DIR_NEG_X;
              break;
          case 2:
              topFace = Dir.DIR_POS_Z;
              bottomFace = Dir.DIR_NEG_Z;
              break;
          case 3:
              topFace = -1;
              bottomFace = -1;
              break;
      }
      if (faceDir == topFace || faceDir == bottomFace) {
          idx = 1;
      }
      return TextureArrays.blockTextureArray.getTextureIdx(this.id, idx);
    }
    
    @Override
    public int getTexturePasses() {
        return 2;
    }

    @Override
    public void onBlockMine(BlockPlacer placer, World w, BlockPos pos, PlayerServer player, ItemStack itemstack) {
        Tree tree = placer.getTree();
        if (tree == null) {
            return;
        }
        tree.onMine(placer, this, w, pos, player, itemstack);
    }
    
    @Override
    public boolean skipTexturePassSide(IBlockWorld w, int x, int y, int z, int axis, int side, int texPass) {
        if (texPass > 0) {
            int data = w.getData(x, y, z)>>2;
            return data == 0;
        }
        return super.skipTexturePassSide(w, x, y, z, axis, side, texPass);
    }
    
    @Override
    public int getRenderType() {
        return 14;
    }
}
