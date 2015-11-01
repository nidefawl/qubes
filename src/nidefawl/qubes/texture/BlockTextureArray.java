package nidefawl.qubes.texture;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.Map.Entry;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

import org.lwjgl.opengl.*;

public class BlockTextureArray {
    public static final int BLOCK_TEXTURE_BITS = 4;
    static final BlockTextureArray instance = new BlockTextureArray();
    HashMap<Integer, ArrayList<AssetTexture>> lastLoaded = new HashMap<>();
    public static BlockTextureArray getInstance() {
        return instance;
    }

    
    int[]      textures;
    public int glid;
    public int tileSize = 0;
    private int maxTextures;
    private int numMipmaps;

    BlockTextureArray() {
    }

    public void init() {
        glid = GL11.glGenTextures();
    }

    public void reloadTexture(String path) {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindTexture(GL30.GL_TEXTURE_2D_ARRAY)");
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = lastLoaded.entrySet().iterator();
        ByteBuffer directBuf = null;
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            ArrayList<AssetTexture> blockTexture = entry.getValue();
            for (int i = 0; i < blockTexture.size(); i++) {
                AssetTexture tex = blockTexture.get(i);
                if (tex.getName().endsWith(path)) {
                    System.out.println("need reload slot "+tex.getSlot());
                    tex.reload();
                    if (tex.getWidth() != tex.getHeight()) {
                        if (tex.getHeight()>tex.getWidth()) {
                            tex.cutH();
                        }else 
                        throw new GameError("Block tiles must be width == height");
                    }
                    if (tex.getWidth() < this.tileSize) {
                        tex.rescale(this.tileSize);
                    }
                    byte[] data = tex.getData();
                    TextureUtil.clampAlpha(data, this.tileSize, this.tileSize);
                    directBuf = put(directBuf, data);
                    int avg = TextureUtil.getAverageColor(data, this.tileSize, this.tileSize);
                    int mipmapSize = this.tileSize;
                    for (int m = 0; m < numMipmaps; m++) {
                        directBuf = put(directBuf, data);
                      System.out.println(m+"/"+mipmapSize+"/"+directBuf.position()+"/"+directBuf.capacity()+"/"+directBuf.remaining());
                        GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m,                     //Mipmap number
                              0, 0, tex.getSlot(),                 //xoffset, yoffset, zoffset
                              mipmapSize, mipmapSize, 1,                 //width, height, depth
                              GL_RGBA,                //format
                              GL_UNSIGNED_BYTE,      //type
                              directBuf);                //pointer to data
                        Engine.checkGLError("GL12.glTexSubImage3D");
                        if (mipmapSize > 1) {
                            mipmapSize /= 2;
                            data = TextureUtil.makeMipMap(data, mipmapSize, mipmapSize, avg);
                        }
                    }
//                    AssetTexture tex = AssetManager.getInstance().loadPNGAsset(s);
                }
            }
        }
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindTexture(GL30.GL_TEXTURE_2D_ARRAY)");
        
    }
    public void reload() {
        ByteBuffer directBuf = null;
        boolean firstInit = textures == null;
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindTexture(GL30.GL_TEXTURE_2D_ARRAY)");

        int maxSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        float maxAnisotropy = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        int maxMipMap = GameMath.log2(maxSize);
        System.out.println("GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = "+maxAnisotropy);
        System.out.println("GL_MAX_TEXTURE_SIZE = "+maxSize);
        System.out.println("log2(GL_MAX_TEXTURE_SIZE) = "+maxMipMap);
        int maxTileW = 0;

        int maxTextures = 0;
        String maxTexture = null;
        HashMap<Integer, ArrayList<AssetTexture>> text = new HashMap<>();
        Block[] blocks = Block.block;
        int len = blocks.length;
        float progress = 0f;
        int totalBlocks = Block.getRegisteredIDs().length;
        int nBlock = 0;
        for (int i = 0; i < len; i++) {
            Block b = blocks[i];
            if (b != null) {
                String[] textures = b.getTextures();
                if (textures != null) {
                    ArrayList<AssetTexture> blockTextures = new ArrayList<>();
                    for (String s : textures) {
                        AssetTexture tex = AssetManager.getInstance().loadPNGAsset(s);
                        if (tex == null) {
                            throw new GameError("Failed loading block texture " + s);
                        }
                        int texW = tex.getWidth();//Math.max(tex.getWidth(), tex.getHeight());
                        if (texW > maxTileW) {
                            maxTileW = texW;
                            maxTexture = s;
                        }
                        blockTextures.add(tex);
                        maxTextures++;
                        if (maxTileW > 512) {
                            throw new GameError("Maximum resolution must not exceed 512! (texture '"+s+"')");
                        }
                    }
                    text.put(b.id, blockTextures);
                }
                if (firstInit) {
                    progress = ++nBlock/(float)totalBlocks;
                    Game.instance.loadRender(1, progress, b.getName());
                }
            }
        }
        if (firstInit) {
            Game.instance.loadRender(1, 1);
        }
        System.out.println("maxTileW = "+maxTileW);
        System.out.println("maxTexture = "+maxTexture);
        this.tileSize = maxTileW;
        this.maxTextures = maxTextures;
        this.textures = new int[Block.NUM_BLOCKS<<BLOCK_TEXTURE_BITS];
        this.numMipmaps = 1+GameMath.log2(this.tileSize);

        if (firstInit)
        nidefawl.qubes.gl.GL.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, numMipmaps, GL_RGBA8,              //Internal format
                this.tileSize, this.tileSize,   //width,height
                this.maxTextures       //Number of layers
        );
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("GL42.glTexStorage3D");
        totalBlocks = text.size();
        nBlock = 0;
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = text.entrySet().iterator();
        int slot = 0;
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            ArrayList<AssetTexture> blockTexture = entry.getValue();
            for (int i = 0; i < blockTexture.size(); i++) {
                AssetTexture tex = blockTexture.get(i);
                if (tex.getWidth() != tex.getHeight()) {
                    if (tex.getHeight()>tex.getWidth()) {
                        tex.cutH();
                    }else 
                    throw new GameError("Block tiles must be width == height");
                }
                if (tex.getWidth() < this.tileSize) {
                    tex.rescale(this.tileSize);
                }
            }
        }
        it = text.entrySet().iterator();
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
                    directBuf = put(directBuf, data);
                    int avg = TextureUtil.getAverageColor(data, this.tileSize, this.tileSize);
                    int mipmapSize = this.tileSize;
                    for (int m = 0; m < numMipmaps; m++) {
                        directBuf = put(directBuf, data);
//                      System.out.println(m+"/"+mipmapSize+"/"+directBuf.position()+"/"+directBuf.capacity()+"/"+directBuf.remaining());
                        GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m,                     //Mipmap number
                              0, 0, slot,                 //xoffset, yoffset, zoffset
                              mipmapSize, mipmapSize, 1,                 //width, height, depth
                              GL_RGBA,                //format
                              GL_UNSIGNED_BYTE,      //type
                              directBuf);                //pointer to data
                        Engine.checkGLError("GL12.glTexSubImage3D");
                        mipmapSize /= 2;
                        data = TextureUtil.makeMipMap(data, mipmapSize, mipmapSize, avg);
                    }
                    tex.setSlot(slot);
                    textures[blockId << 4 | i] = slot;
                    slot++;
                } else {

                    textures[blockId << 4 | i] = reuseslot;
                }
            }
            if (firstInit) {
                progress = ++nBlock/(float)totalBlocks;
                Game.instance.loadRender(2, progress);
            }
        }
        Game.instance.loadRender(2, 1);
        lastLoaded = text;
        boolean useAnisotrophic = true;
        if (useAnisotrophic) {

            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        }
        else { // does not work with alpha testing

            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, 4.0f);
//          GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
        }
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    /**
     * @param directBuf
     * @param data
     * @return
     */
    private ByteBuffer put(ByteBuffer directBuf, byte[] data) {
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

}
