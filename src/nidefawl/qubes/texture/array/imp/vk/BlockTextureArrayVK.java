package nidefawl.qubes.texture.array.imp.vk;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_BC3_UNORM_BLOCK;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.lwjgl.opengl.EXTTextureCompressionS3TC;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.texture.DXTCompressor;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.vulkan.VkMemoryManager;

public class BlockTextureArrayVK extends TextureArrayVK {
    public static final int        BLOCK_TEXTURE_BITS = 4;




    public BlockTextureArrayVK() {
        super(Block.NUM_BLOCKS << BLOCK_TEXTURE_BITS);
        this.internalFormat = VK_FORMAT_BC3_UNORM_BLOCK;
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
        ByteBuffer directBuf = null;
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
                    TextureUtil.setTransparentPixelsColor(data, this.tileSize, this.tileSize, avg);
                    int mipmapSize = this.tileSize;

                    int offset = 0;
                    TextureBinMips slotTex = new TextureBinMips();
                    slotTex.sizes = new int[numMipmaps];
                    slotTex.w = new int[numMipmaps];
                    slotTex.h = new int[numMipmaps];
                    for (int m = 0; m < numMipmaps; m++) {
                        
                        ByteBuffer rgba = ByteBuffer.wrap(data);
                        int dlen = (int)VkMemoryManager.align(data.length, 16);
                        if (directBuf == null || directBuf.capacity() < dlen) {
                            directBuf = ByteBuffer.allocateDirect(dlen).order(ByteOrder.nativeOrder());
                        }
                        directBuf.clear();
                        DXTCompressor.stbgl__compress(directBuf, rgba, mipmapSize, mipmapSize, EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, directBuf.capacity());
                        int rem = directBuf.remaining();
                        int len = (int)VkMemoryManager.align(rem, 16);
                        directBuf.get(scratchpad, offset, rem);
                        for (int o = rem; o < len; o++) {
                            scratchpad[o] = 0;
                        }
                        
//                        System.arraycopy(data, 0, scratchpad, offset, data.length);
                        slotTex.sizes[m] = len;
                        slotTex.w[m] = mipmapSize;
                        slotTex.h[m] = mipmapSize;
                        offset += len;
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

    public void setAnisotropicFiltering(int anisotropicFiltering) {
        this.anisotropicFiltering = anisotropicFiltering;
    }
    protected boolean isFilterNearest() {
        return true;
    }

    @Override
    protected void collectTextures(AssetManager mgr) {
        collectBlockTextures(mgr);
    }
}
