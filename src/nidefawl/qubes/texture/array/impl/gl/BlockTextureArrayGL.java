package nidefawl.qubes.texture.array.impl.gl;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.texture.DXTCompressor;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.GameError;

public class BlockTextureArrayGL extends TextureArrayGL {
    public static final int        BLOCK_TEXTURE_BITS = 4;




    public BlockTextureArrayGL() {
        super(Block.NUM_BLOCKS << BLOCK_TEXTURE_BITS);
        this.internalFormat=EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
        this.internalFormat=GL21.GL_SRGB8_ALPHA8;
    }

    protected void postUpload() {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);

        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        if (anisotropicFiltering > 0) {// does not work well with alpha testing
            float f = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            if (anisotropicFiltering < f) {
                f = anisotropicFiltering;
            }
            if (f > 0) {
                glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, f);
            }
        }
        if (this.report && GameBase.loadingScreen != null) {
            GameBase.loadingScreen.render(2, 1);
        }
    }

    protected void uploadTextures() {
        int totalBlocks = blockIDToAssetList.size();
        int nBlock = 0;
        float progress = 0;
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
        int slot = 0;
        ByteBuffer directBuf = null;
        boolean compress = false;
        switch (this.internalFormat) {
            case EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
                compress = true;
                break;
            case EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
                compress = true;
                break;
            case EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
                compress = true;
                break;
            case EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
                compress = true;
                break;
            default:
                compress = false;
                break;
        }
        
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

                    
                    for (int m = 0; m < numMipmaps; m++) {
                        if (compress) {
                            ByteBuffer rgba = ByteBuffer.wrap(data);
                            if (directBuf == null || directBuf.capacity() < data.length) {
                                directBuf = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
                            }
                            directBuf.clear();
                            DXTCompressor.stbgl__compress(directBuf, rgba, mipmapSize, mipmapSize, this.internalFormat, directBuf.capacity());
                            GL13.glCompressedTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m, //Mipmap number
                                    0, 0, slot, //xoffset, yoffset, zoffset
                                    mipmapSize, mipmapSize, 1, //width, height, depth
                                    this.internalFormat, //format
                                    directBuf);//pointer to data
                        } else {
                            directBuf = put(directBuf, data);
                            GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m, //Mipmap number
                                    0, 0, slot, //xoffset, yoffset, zoffset
                                    mipmapSize, mipmapSize, 1, //width, height, depth
                                    GL_RGBA, //format
                                    GL_UNSIGNED_BYTE, //type
                                    directBuf);//pointer to data
                        }

                        Engine.checkGLError("GL12.glTexSubImage3D");
                        mipmapSize /= 2;
                        if (mipmapSize > 0)
                            data = TextureUtil.makeMipMap(data, mipmapSize, mipmapSize, avg);
                    }
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

}
