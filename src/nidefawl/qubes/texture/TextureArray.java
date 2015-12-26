package nidefawl.qubes.texture;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public abstract class TextureArray {
    HashMap<Integer, ArrayList<AssetTexture>> lastLoaded         = new HashMap<>();
    HashMap<String, AssetTexture>             texNameToAssetMap  = new HashMap<>();
    HashMap<Integer, ArrayList<AssetTexture>> blockIDToAssetList = new HashMap<>();
    HashMap<Integer, AssetTexture> slotTextureMap = new HashMap<>();

    protected boolean firstInit = true;

    int[]         textures;
    public int    glid;
    public int    tileSize = 0;
    protected int numTextures;
    protected int numMipmaps;

    public TextureArray(int maxTextures) {
        this.textures = new int[maxTextures];
    }

    protected void unload() {
        if (!firstInit) {
            TextureManager.getInstance().releaseTexture(this.glid);
            this.texNameToAssetMap.clear();
            this.blockIDToAssetList.clear();
            this.lastLoaded.clear();
            this.slotTextureMap.clear();
            Arrays.fill(this.textures, 0);
        }
    }
    public void init() {
        glid = GL11.glGenTextures();
    }


    public void reload() {
        unload();
        init();
        int maxSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        float maxAnisotropy = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        int maxMipMap = GameMath.log2(maxSize);
        System.out.println("GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = "+maxAnisotropy);
        System.out.println("GL_MAX_TEXTURE_SIZE = "+maxSize);
        System.out.println("log2(GL_MAX_TEXTURE_SIZE) = "+maxMipMap);
        
        
        AssetManager mgr = AssetManager.getInstance();
        collectTextures(mgr);
        findMaxTileWidth();
        upscaleTextures();
        this.numMipmaps = 1+GameMath.log2(this.tileSize);
        
        System.out.println("tileSize = "+this.tileSize);
        System.out.println("numTextures = "+this.numTextures);
        

        initGLStorage();
        uploadTextures();
        postUpload();



        lastLoaded = blockIDToAssetList;
        this.firstInit = false;

    }

    /**
     * @param directBuf
     * @param data
     * @return
     */
    protected ByteBuffer put(ByteBuffer directBuf, byte[] data) {
        if (directBuf == null || directBuf.capacity() < data.length) {
            directBuf = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
        }
        directBuf.clear();
        directBuf.put(data, 0, data.length);
        directBuf.position(0).limit(data.length);
        return directBuf;
    }

    public int getTextureIdx(int block, int texId) {
        return this.textures[block << 4 | texId];
    }


    protected void findMaxTileWidth() {
        int maxTileW = 0;
        for (AssetTexture tex : texNameToAssetMap.values()) {
            int texW = tex.getWidth();//Math.max(tex.getWidth(), tex.getHeight());
            if (texW > maxTileW) {
                maxTileW = texW;
            }
            numTextures++;
            if (maxTileW > 512) {
                throw new GameError("Maximum resolution must not exceed 512! (texture '"+tex.getName()+"')");
            }
        }
        this.numTextures = texNameToAssetMap.size();
        this.tileSize = maxTileW;
    }

    protected void upscaleTextures() {
        for (AssetTexture tex : texNameToAssetMap.values()) {
            if (tex.getWidth() < this.tileSize) {
                tex.rescale(this.tileSize);
            }
        }
    }


    protected void initGLStorage() {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);
        Engine.checkGLError("pre glTexStorage3D");
        System.out.println("make storage for "+this.tileSize+"x"+this.tileSize+"x"+this.numTextures+"x"+this.numMipmaps);
        nidefawl.qubes.gl.GL.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, numMipmaps, 
                GL_RGBA8,              //Internal format
                this.tileSize, this.tileSize,   //width,height
                this.numTextures       //Number of layers
        );
        Engine.checkGLError("glTexStorage3D");
    }

    protected abstract void uploadTextures();
    protected abstract void collectTextures(AssetManager mgr);
    protected abstract void postUpload();

    


//  public void reloadTexture(String path) {
//      GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid_color);
//      if (Game.GL_ERROR_CHECKS)
//          Engine.checkGLError("glBindTexture(GL30.GL_TEXTURE_2D_ARRAY)");
//      Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = lastLoaded.entrySet().iterator();
//      ByteBuffer directBuf = null;
//      while (it.hasNext()) {
//          Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
//          ArrayList<AssetTexture> blockTexture = entry.getValue();
//          for (int i = 0; i < blockTexture.size(); i++) {
//              AssetTexture tex = blockTexture.get(i);
//              if (tex.getName().endsWith(path)) {
//                  System.out.println("need reload slot "+tex.getSlot());
//                  tex.reload();
//                  if (tex.getWidth() != tex.getHeight()) {
//                      if (tex.getHeight()>tex.getWidth()) {
//                          tex.cutH();
//                      }else 
//                      throw new GameError("Block tiles must be width == height");
//                  }
//                  if (tex.getWidth() < this.tileSize) {
//                      tex.rescale(this.tileSize);
//                  }
//                  byte[] data = tex.getData();
//                  TextureUtil.clampAlpha(data, this.tileSize, this.tileSize);
//                  directBuf = put(directBuf, data);
//                  int avg = TextureUtil.getAverageColor(data, this.tileSize, this.tileSize);
//                  int mipmapSize = this.tileSize;
//                  for (int m = 0; m < numMipmaps; m++) {
//                      directBuf = put(directBuf, data);
//                    System.out.println(m+"/"+mipmapSize+"/"+directBuf.position()+"/"+directBuf.capacity()+"/"+directBuf.remaining());
//                      GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m,                     //Mipmap number
//                            0, 0, tex.getSlot(),                 //xoffset, yoffset, zoffset
//                            mipmapSize, mipmapSize, 1,                 //width, height, depth
//                            GL_RGBA,                //format
//                            GL_UNSIGNED_BYTE,      //type
//                            directBuf);                //pointer to data
//                      Engine.checkGLError("GL12.glTexSubImage3D");
//                      if (mipmapSize > 1) {
//                          mipmapSize /= 2;
//                          data = TextureUtil.makeMipMap(data, mipmapSize, mipmapSize, avg);
//                      }
//                  }
////                  AssetTexture tex = AssetManager.getInstance().loadPNGAsset(s);
//              }
//          }
//      }
//      GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
//      if (Game.GL_ERROR_CHECKS)
//          Engine.checkGLError("glBindTexture(GL30.GL_TEXTURE_2D_ARRAY)");
//      
//  }
}
