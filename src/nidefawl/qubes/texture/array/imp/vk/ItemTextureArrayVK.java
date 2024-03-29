package nidefawl.qubes.texture.array.imp.vk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.TextureUtil;

public class ItemTextureArrayVK extends TextureArrayVK {

    public static final int        maxTextures = 128;
    public ItemTextureArrayVK() {
        super(maxTextures);
    }

    @Override
    protected void uploadTextures() {
        int totalBlocks = blockIDToAssetList.size();
        int nBlock = 0;
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
        int slot = 0;
        byte[] scratchpad = new byte[(512*512*4)*4];
        TextureBinMips[] binMips = new TextureBinMips[this.numTextures];
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            int blockId = entry.getKey();
            ArrayList<AssetTexture> blockTexture = entry.getValue();
            for (int i = 0; i < blockTexture.size(); i++) {
                AssetTexture tex = blockTexture.get(i);
                int reuseslot = tex.getSlot();
                if (reuseslot < 0) {
                    this.numUploaded++;
                    byte[] data = tex.getData();
                    TextureUtil.clampAlpha(data, this.tileSize, this.tileSize);
                    int avg = TextureUtil.getAverageColor(data, this.tileSize, this.tileSize);
                    int offset = 0;
                    TextureBinMips slotTex = new TextureBinMips();
                    slotTex.sizes = new int[numMipmaps];
                    slotTex.w = new int[numMipmaps];
                    slotTex.h = new int[numMipmaps];
                    int mipmapSize = this.tileSize;
                    for (int m = 0; m < numMipmaps; m++) {
                        System.arraycopy(data, 0, scratchpad, offset, data.length);
                        slotTex.sizes[m] = data.length;
                        slotTex.w[m] = mipmapSize;
                        slotTex.h[m] = mipmapSize;
                        offset += data.length;
                        mipmapSize /= 2;
                        if (mipmapSize > 0)
                            data = TextureUtil.makeMipMap(data, mipmapSize, mipmapSize, avg);
                    }
                    slotTex.totalSize = offset;
                    slotTex.mips = this.numMipmaps;
                    slotTex.data = new byte[offset];
                    System.arraycopy(scratchpad, 0, slotTex.data, 0, offset);
                    binMips[slot] = slotTex;
                    tex.setSlot(slot);
                    slotTextureMap.put(slot, tex);
                    setTexture(blockId, i, slot);
                    slot++;
                } else {
                    setTexture(blockId, i, reuseslot);
                }
            }
        }

        this.totalSlots = slot;
        this.texture.build(this.internalFormat, binMips);
    }

    @Override
    protected void collectTextures(AssetManager mgr) {
        collectItemTextures(mgr);
    }

}
