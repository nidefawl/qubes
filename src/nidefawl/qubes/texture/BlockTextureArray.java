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
    public int glid_color;
    public int glid_normalmaps;
    public int tileSize = 0;
    private int maxTextures;
    private int numMipmaps;

    BlockTextureArray() {
    }

    public void init() {
        glid_color = GL11.glGenTextures();
        glid_normalmaps = GL11.glGenTextures();
    }

    public void reloadTexture(String path) {
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid_color);
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
        AssetManager mgr = AssetManager.getInstance();
        ByteBuffer directBuf = null;
        boolean firstInit = textures == null;
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid_color);
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
        HashMap<Integer, AssetTexture> slotTextureMap = new HashMap<>();
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
                        AssetTexture tex = mgr.loadPNGAsset(s);
                        if (tex == null) {
                            throw new GameError("Failed loading block texture " + s);
                        }
//                        System.out.println(tex.getName()+" - "+tex.getWidth()+","+tex.getHeight()+ " from "+tex.getPack());
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
                    GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid_color);
                    Engine.checkGLError("Game.instance.loadRender");
                }
            }
        }
        if (firstInit) {
            Game.instance.loadRender(1, 1);
            Engine.checkGLError("Game.instance.loadRender");
        }
        System.out.println("maxTileW = "+maxTileW);
        System.out.println("maxTexture = "+maxTexture);
        this.tileSize = maxTileW;
        this.maxTextures = maxTextures;
        this.textures = new int[Block.NUM_BLOCKS<<BLOCK_TEXTURE_BITS];
        this.numMipmaps = 1+GameMath.log2(this.tileSize);

        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid_color);
        if (firstInit) {
            Engine.checkGLError("pre glTexStorage3D");
            nidefawl.qubes.gl.GL.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, numMipmaps, 
                    GL_RGBA8,              //Internal format
                    this.tileSize, this.tileSize,   //width,height
                    this.maxTextures       //Number of layers
            );
            Engine.checkGLError("glTexStorage3D");
        }
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
                    slotTextureMap.put(slot, tex);
                    textures[blockId << 4 | i] = slot;
                    slot++;
                } else {

                    textures[blockId << 4 | i] = reuseslot;
                }
            }
            if (firstInit) {
                progress = ++nBlock/(float)totalBlocks;
                Game.instance.loadRender(2, progress);
                GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid_color);
            }
        }
        Game.instance.loadRender(2, 1);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid_color);
        lastLoaded = text;
        boolean useDefault = true;
        if (useDefault) {

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
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid_normalmaps);
        if (firstInit) {
            Engine.checkGLError("pre glTexStorage3D normalmaps");
            nidefawl.qubes.gl.GL.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, numMipmaps, 
                    GL_RGBA8,              //Internal format
                    this.tileSize, this.tileSize,   //width,height
                    this.maxTextures       //Number of layers
            );
            Engine.checkGLError("glTexStorage3D normalmaps");
        }
        Iterator<Entry<Integer, AssetTexture>> it2 = slotTextureMap.entrySet().iterator();
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
        while (it2.hasNext()) {
            Entry<Integer, AssetTexture> entry = it2.next();
            int texSlot = entry.getKey();
            AssetTexture texColor = entry.getValue();
            

            byte[] data = findNormalMapForColorTexture(mgr, texColor, defNormal);
//          System.out.println(texColor.getName());
            TextureUtil.clampAlpha(data, this.tileSize, this.tileSize);
            directBuf = put(directBuf, data);
            int avg = 0xff7f7fff;
            int mipmapSize = this.tileSize;
            for (int m = 0; m < 1; m++) {
                directBuf = put(directBuf, data);
//              System.out.println(m+"/"+mipmapSize+"/"+directBuf.position()+"/"+directBuf.capacity()+"/"+directBuf.remaining());
                GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m,                     //Mipmap number
                      0, 0, texSlot,                 //xoffset, yoffset, zoffset
                      mipmapSize, mipmapSize, 1,                 //width, height, depth
                      GL_RGBA,                //format
                      GL_UNSIGNED_BYTE,      //type
                      directBuf);                //pointer to data
                Engine.checkGLError("GL12.glTexSubImage3D");
                mipmapSize /= 2;
                data = TextureUtil.makeMipMap(data, mipmapSize, mipmapSize, avg); //TODO: change mip map mode for normal maps
            }
            
        }
        useDefault = false;
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
        GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    private byte[] findNormalMapForColorTexture(AssetManager mgr, AssetTexture texColor, byte[] defNormal) {
        String name = texColor.getName();
        if (!name.endsWith(".png")) {
            System.err.println("Expected texture tile name to end with .png: "+name);
            return defNormal;
        }
        name = name.substring(0, name.length()-4);
        name = name + "_normalmap.png";
        AssetTexture tex = mgr.loadPNGAsset(name, true);
        if (tex == null) {
            return defNormal;
        }
        if (tex.getWidth() != tex.getHeight()) {
            System.err.println("Expected normal map width == height: "+name);
            return defNormal;
        }
        System.out.println("using custom normal map "+name);
        return tex.getData();
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
