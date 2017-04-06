package nidefawl.qubes.texture.array.impl.gl;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.GameError;

public class ItemTextureArrayGL extends TextureArrayGL {

    public static final int        maxTextures = 128;
    public ItemTextureArrayGL() {
        super(maxTextures);
    }

    @Override
    protected void uploadTextures() {
        int totalBlocks = blockIDToAssetList.size();
        int nBlock = 0;
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
        int slot = 0;
        ByteBuffer directBuf = null;
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
                    int mipmapSize = this.tileSize;
                    for (int m = 0; m < numMipmaps; m++) {
                        directBuf = put(directBuf, data);
                        //                      System.out.println(m+"/"+mipmapSize+"/"+directBuf.position()+"/"+directBuf.capacity()+"/"+directBuf.remaining());
                        GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m, //Mipmap number
                                0, 0, slot, //xoffset, yoffset, zoffset
                                mipmapSize, mipmapSize, 1, //width, height, depth
                                GL_RGBA, //format
                                GL_UNSIGNED_BYTE, //type
                                directBuf);//pointer to data
                        Engine.checkGLError("GL12.glTexSubImage3D");
                        mipmapSize /= 2;
                        if (mipmapSize > 0)
                            data = TextureUtil.makeMipMap(data, mipmapSize, mipmapSize, avg);
                    }
//                    System.out.println(mipmapSize);
                    tex.setSlot(slot);
                    slotTextureMap.put(slot, tex);
                    setTexture(blockId, i, slot);
                    slot++;
                    this.numUploaded++;
                } else {
                    setTexture(blockId, i, reuseslot);
                }
            }
        }

        this.totalSlots = slot;
    }


    protected void collectTextures(AssetManager mgr) {
        Item[] items = Item.item;
        int len = items.length;
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < len; i++) {
            Item itzem = items[i];
            if (itzem != null) {
                set.addAll(Arrays.asList(itzem.getTextures()));
            }
        }
        this.numTotalTextures = set.size();
        for (int i = 0; i < len; i++) {
            Item itzem = items[i];
            if (itzem != null) {
                ArrayList<AssetTexture> list = Lists.newArrayList();
                for (String s : itzem.getTextures()) {
                    AssetTexture tex = texNameToAssetMap.get(s);
                    if (tex == null) {
                        tex = mgr.loadPNGAsset(s, false);
                        this.numLoaded++;
                        if (tex.getWidth() != tex.getHeight()) {
                            if (tex.getHeight() > tex.getWidth()) {
                                tex.cutH();
                            } else
                                throw new GameError("Item textures must be width == height");
                        }
                        texNameToAssetMap.put(s, tex);
                    }
                    list.add(tex);
                }
                blockIDToAssetList.put(itzem.id, list);
            }
        }
    }

    @Override
    protected void postUpload() {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);
        boolean useDefault = false;
        if (useDefault) {

            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        }
        else { // does not work with alpha testing

            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, 16.0f);
//          GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
        }
    }

}
