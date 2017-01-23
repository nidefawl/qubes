package nidefawl.qubes.texture.array;

import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;

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
    HashMap<String, AssetTexture>             texNameToAssetMap  = new HashMap<>();
    HashMap<Integer, ArrayList<AssetTexture>> blockIDToAssetList = new HashMap<>();
    HashMap<Integer, AssetTexture> slotTextureMap = new HashMap<>();

    protected boolean firstInit = true;

    private final int[]         textures;
    public int    glid;
    public int    tileSize = 0;
    protected int numTextures;
    protected int numMipmaps;
    private int   subtypeBits = 0;
    protected boolean report;
    public float loadprogress;
    public float uploadprogress;

    public TextureArray(int maxTextures) {
        this.textures = new int[maxTextures];
    }

    protected void unload() {
        if (!firstInit) {
            GL.deleteTexture(this.glid);
            this.texNameToAssetMap.clear();
            this.blockIDToAssetList.clear();
            this.slotTextureMap.clear();
            Arrays.fill(this.textures, 0);
        }
    }
    public void init() {
        glid = GL11.glGenTextures();
    }

    public void preUpdate() {
        unload();
        init();
    }
    private void _load() {
        AssetManager mgr = AssetManager.getInstance();
        if (!SKIP_LOAD_TEXTURES) {
            collectTextures(mgr);
        }
        this.loadprogress = 1;
        findMaxTileWidth();
        upscaleTextures();
        calculateSubtypeBits();
        this.numMipmaps = 1+GameMath.log2(this.tileSize);
    }
    public void load() {
        this.report=false;
        _load();
    }

    public void reload() {
        preUpdate();
        this.report=true;
        _load();
        postUpdate();

    }


    public void postUpdate() {
        initGLStorage();
        uploadTextures();
        this.uploadprogress = 1;
        postUpload();
        free();
        this.firstInit = false;
    }

    private void free() {
        this.blockIDToAssetList.clear();
        this.slotTextureMap.clear();
        this.texNameToAssetMap.clear();
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
        return this.textures[block << subtypeBits | texId];
    }
    protected void setTexture(int i, int j, int s) {
        this.textures[i << subtypeBits | j] = s;
    }



    protected void calculateSubtypeBits() {
        int nSubTypes = 0;
        for (ArrayList<AssetTexture> tex : blockIDToAssetList.values()) {
            nSubTypes = Math.max(nSubTypes, tex.size());
        }
        this.subtypeBits = nSubTypes == 0 ? 0 : 1+GameMath.log2(nSubTypes);
//        System.err.println("nSubTypes "+nSubTypes+" requries "+subtypeBits+", max idx = "+(1<<this.subtypeBits));
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
//        System.err.println(glid+"/"+numMipmaps+"/"+this.tileSize+"/"+this.numTextures);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);
        Engine.checkGLError("pre glTexStorage3D");
        nidefawl.qubes.gl.GL.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, numMipmaps, 
                GL_RGBA8,              //Internal format
                this.tileSize, this.tileSize,   //width,height
                this.numTextures       //Number of layers
        );
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, numMipmaps-1);
        Engine.checkGLError("glTexStorage3D");
    }

    protected abstract void uploadTextures();
    protected abstract void collectTextures(AssetManager mgr);
    protected abstract void postUpload();

    public float getProgress() {
        return (loadprogress+uploadprogress)/2.0f;
    }

}
