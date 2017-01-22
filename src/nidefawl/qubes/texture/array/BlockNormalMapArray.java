package nidefawl.qubes.texture.array;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public class BlockNormalMapArray extends TextureArray {
    static final BlockNormalMapArray instance = new BlockNormalMapArray();
    public static BlockNormalMapArray getInstance() {
        return instance;
    }




    public BlockNormalMapArray() {
        super(256);
    }
    @Override
    public void load() {
        super.load();
        this.numMipmaps = 1;
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
        float progress = 0;
        ByteBuffer directBuf = null;
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
        directBuf = put(directBuf, defNormal);
        GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,                     //Mipmap number
              0, 0, slot,                 //xoffset, yoffset, zoffset
              this.tileSize, this.tileSize, 1,                 //width, height, depth
              GL_RGBA,                //format
              GL_UNSIGNED_BYTE,      //type
              directBuf);                //pointer to data
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
//                    System.out.println("put data with dim "+tex.getWidth()+"x"+tex.getHeight()+" in tex slot "+slot+" with size "+this.tileSize+"x"+this.tileSize);
                    directBuf = put(directBuf, data);
                    GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,                     //Mipmap number
                          0, 0, slot,                 //xoffset, yoffset, zoffset
                          this.tileSize, this.tileSize, 1,                 //width, height, depth
                          GL_RGBA,                //format
                          GL_UNSIGNED_BYTE,      //type
                          directBuf);                //pointer to data
                    Engine.checkGLError("GL12.glTexSubImage3D");
                    
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

    }


    @Override
    protected void postUpload() {
        boolean useDefault = false;
        if (useDefault) {

            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL12.GL_TEXTURE_MAX_LEVEL, 0);
//            GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
        } else {// does not work with alpha testing

            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, 16.0f);
//            GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
            //        GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
        }
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }
    



}
