package nidefawl.qubes.texture.array;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public abstract class TextureArray {
    public final static boolean SKIP_LOAD_TEXTURES = false;
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
            GL.deleteTexture(this.glid);
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
        if (!SKIP_LOAD_TEXTURES)
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
        if (this.numTextures == 0) {
            this.numTextures++;
        }
        if (maxTileW == 0) {
            maxTileW++;
        }
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
    

}
