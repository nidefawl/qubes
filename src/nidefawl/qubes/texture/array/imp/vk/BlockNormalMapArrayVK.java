package nidefawl.qubes.texture.array.imp.vk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.util.GameError;

public class BlockNormalMapArrayVK extends TextureArrayVK {
    
    public BlockNormalMapArrayVK() {
        super(256);
    }
    
    @Override
    public void load() {
        super.load();
//        this.numMipmaps = 1;
    }

    @Override
    protected void findMaxTileWidth() {
        super.findMaxTileWidth();
        if (this.tileSize == 0) {
            this.tileSize = 1;
        }
        this.numTextures++;
    }


    @Override
    protected void collectTextures(AssetManager mgr) {
        Block[] blocks = Block.block;
        int len = blocks.length;
        for (int i = 0; i < len; i++) {
            Block b = blocks[i];
            if (b != null) {
                ArrayList<AssetTexture> list = Lists.newArrayList();
                for (String s : b.getNormalMaps()) {
                    AssetTexture tex = texNameToAssetMap.get(s);
                    if (tex == null) {
                        tex = mgr.loadPNGAsset(s, false);
                        if (tex.getWidth() != tex.getHeight()) {
                            if (tex.getHeight()>tex.getWidth()) {
                                tex.cutH();
                            }else 
                            throw new GameError("Block tiles must be width == height");
                        }
                        texNameToAssetMap.put(s, tex);
                    }
                    list.add(tex);
                }
                blockIDToAssetList.put(b.id, list);
            }
            loadprogress = (i / (float) len);
        }
    
    }
    @Override
    protected void uploadTextures() {
        int totalBlocks = blockIDToAssetList.size();
        int nBlock = 0;
        TextureBinMips[] binMips = new TextureBinMips[this.numTextures];
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
        int slot = 0;
          byte[] defNormal = new byte[4*this.tileSize*this.tileSize];
          for (int tx = 0; tx < this.tileSize; tx++) {
              for (int ty = 0; ty < this.tileSize; ty++) {
                  int idx = ty*this.tileSize+tx;
                  defNormal[idx*4+0] = (byte)0x7f;
                  defNormal[idx*4+1] = (byte)0x7f;
                  defNormal[idx*4+2] = (byte)0xFF;
                  defNormal[idx*4+3] = (byte)0xFF;
              }
          }
          {

              binMips[0] = new TextureBinMips(defNormal, this.tileSize, this.tileSize);
          }
        slot++;
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            int blockId = entry.getKey();
            ArrayList<AssetTexture> blockTexture = entry.getValue();
            for (int i = 0; i < blockTexture.size(); i++) {
                AssetTexture tex = blockTexture.get(i);
                int reuseslot = tex.getSlot();
                if (reuseslot < 0) {
                    byte[] data = tex.getData();
                    binMips[slot] = new TextureBinMips(data, this.tileSize, this.tileSize);
                    
                    tex.setSlot(slot);
                    slotTextureMap.put(slot, tex);
                    setTexture(blockId, i, slot);
                    slot++;
                } else {
                    setTexture(blockId, i, reuseslot);
                }
            }
            uploadprogress = ++nBlock/(float)totalBlocks;
        }

        this.totalSlots = slot;
        this.texture.build(this.internalFormat, binMips);
    }



}
