package nidefawl.qubes.texture.array.imp.vk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.GameError;

public class BlockTextureArrayVK extends TextureArrayVK {
    public static final int        BLOCK_TEXTURE_BITS = 4;




    public BlockTextureArrayVK() {
        super(Block.NUM_BLOCKS << BLOCK_TEXTURE_BITS);
    }

    protected void postUpload() {
        super.postUpload();
        if (this.report && GameBase.loadingScreen != null) {
            GameBase.loadingScreen.render(2, 1);
        }
    }

    protected void uploadTextures() {
        int totalBlocks = blockIDToAssetList.size();
        int nBlock = 0;
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
        int slot = 0;
        TextureBinMips[] binMips = new TextureBinMips[this.numTextures];
        byte[] scratchpad = new byte[(512*512*4)*4];
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            int blockId = entry.getKey();
            ArrayList<AssetTexture> blockTexture = entry.getValue();
            for (int i = 0; i < blockTexture.size(); i++) {
                AssetTexture tex = blockTexture.get(i);
                int reuseslot = tex.getSlot();
                if (reuseslot < 0) {
                    byte[] data = tex.getData();
                    TextureUtil.clampAlpha(data, this.tileSize, this.tileSize);
                    int avg = TextureUtil.getAverageColor(data, this.tileSize, this.tileSize);
                    TextureUtil.setTransparentPixelsColor(data, this.tileSize, this.tileSize, avg);
                    int mipmapSize = this.tileSize;

                    int offset = 0;
                    TextureBinMips slotTex = new TextureBinMips();
                    slotTex.sizes = new int[numMipmaps];
                    slotTex.w = new int[numMipmaps];
                    slotTex.h = new int[numMipmaps];
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
            uploadprogress = ++nBlock / (float) totalBlocks;
        }
        this.totalSlots = slot;
        this.texture.build(this.internalFormat, binMips);
    }


    protected void collectTextures(AssetManager mgr) {
        Block[] blocks = Block.block;
        int len = blocks.length;
        for (int i = 0; i < len; i++) {
            Block b = blocks[i];
            if (b != null) {
                ArrayList<AssetTexture> list = Lists.newArrayList();
                for (String s : b.getTextures()) {
                    AssetTexture tex = texNameToAssetMap.get(s);
                    if (tex == null) {
                        tex = mgr.loadPNGAsset(s, false);
                        if (tex.getWidth() != tex.getHeight()) {
                            if (tex.getHeight() > tex.getWidth()) {
                                tex.cutH();
                            } else
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

    public void setAnisotropicFiltering(int anisotropicFiltering) {
        this.anisotropicFiltering = anisotropicFiltering;
    }
    protected boolean isFilterNearest() {
        return true;
    }
}
