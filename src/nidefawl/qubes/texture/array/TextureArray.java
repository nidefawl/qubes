package nidefawl.qubes.texture.array;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

public abstract class TextureArray {
    public final static boolean SKIP_LOAD_TEXTURES = false;
    protected HashMap<String, AssetTexture>             texNameToAssetMap  = new HashMap<>();
    protected HashMap<Integer, ArrayList<AssetTexture>> blockIDToAssetList = new HashMap<>();
    protected HashMap<Integer, AssetTexture> slotTextureMap = new HashMap<>();

    public boolean firstInit = true;

    protected final int[]         textures;
    public int    tileSize = 0;
    protected int numTextures;
    protected int numMipmaps;
    private int   subtypeBits = 0;
    protected boolean report;
    public int totalSlots;
    public float anisotropicFiltering = 0;
    public int internalFormat;
    public int numTotalTextures = 1;
    public int numUploaded;
    public int numLoaded;

    public TextureArray(int maxTextures) {
        this.textures = new int[maxTextures];
    }


    public void preUpdate() {
        this.numTotalTextures = 1;
        this.numLoaded = 0;
        this.numUploaded = 0;
        unload();
        init();
    }
    
    protected abstract void init();

    protected abstract void unload();

    private void _load() {
        AssetManager mgr = AssetManager.getInstance();
        if (!SKIP_LOAD_TEXTURES) {
            collectTextures(mgr);
        }
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
        initStorage();
        uploadTextures();
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
    protected ByteBuffer put(ByteBuffer directBuf, ByteBuffer bufdata) {
        int size = bufdata.remaining();
        if (directBuf == null || directBuf.capacity() < size) {
            directBuf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        }
        directBuf.clear();
        directBuf.put(bufdata);
        directBuf.position(0).limit(size);
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


    protected abstract void initStorage();

    protected abstract void uploadTextures();
    protected abstract void collectTextures(AssetManager mgr);
    protected abstract void postUpload();

    public int getNumTotaltextures() {
        return this.numTotalTextures;
    }
    public int getNumUploaded() {
        return this.numUploaded;
    }
    public int getNumLoaded() {
        return this.numLoaded;
    }
    
    public int getNumMipmaps() {
        return this.numMipmaps;
    }
    
    public int getNumTextures() {
        return this.numTextures;
    }

    public void setAnisotropicFiltering(int anisotropicFiltering) {
        this.anisotropicFiltering = anisotropicFiltering;
    }

    public void destroy() {
    }
    
    protected void collectNormalMapTextures(AssetManager mgr) {
        Block[] blocks = Block.block;
        int len = blocks.length;
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < len; i++) {
            Block b = blocks[i];
            if (b != null) {
                set.addAll(Arrays.asList(b.getNormalMaps()));
            }
        }
        this.numTotalTextures = set.size();
        for (int i = 0; i < len; i++) {
            Block b = blocks[i];
            if (b != null) {
                ArrayList<AssetTexture> list = Lists.newArrayList();
                for (String s : b.getNormalMaps()) {
                    AssetTexture tex = texNameToAssetMap.get(s);
                    if (tex == null) {
                        String path = "textures/blocks_512/"+s+".png";
                        tex = mgr.loadPNGAsset(path, false);
                        this.numLoaded++;
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
        }
    
    }
    
    protected void collectNoiseTextures(AssetManager mgr) {
        int len = 64;
        this.numTotalTextures = len;
        for (int i = 0; i < len; i++) {
            String path = "textures/noise/LDR_RGBA_" + i + ".png";
            AssetTexture tex = mgr.loadPNGAsset(path, false);
            blockIDToAssetList.put(i, Lists.newArrayList(tex));
            texNameToAssetMap.put(path, tex);
            this.numLoaded++;
        }
    }


    protected void collectItemTextures(AssetManager mgr) {
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
                        String path = "textures/items/"+s+".png";
                        tex = mgr.loadPNGAsset(path, false);
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

    protected void collectBlockTextures(AssetManager mgr) {
        Block[] blocks = Block.block;
        int len = blocks.length;
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < len; i++) {
            Block b = blocks[i];
            if (b != null) {
                set.addAll(Arrays.asList(b.getTextures()));
            }
        }
        this.numTotalTextures = set.size();
        for (int i = 0; i < len; i++) {
            Block b = blocks[i];
            if (b != null) {
                ArrayList<AssetTexture> list = Lists.newArrayList();
                for (String s : b.getTextures()) {
                    AssetTexture tex = texNameToAssetMap.get(s);
                    if (tex == null) {
                        String path = "textures/blocks_512/"+s+".png";
                        try {
                            tex = mgr.loadPNGAsset(path, false);
                        } catch (Exception e) {
                            throw new GameError("Missing texture "+path+" for block "+b, e);
                        }
                        this.numLoaded++;
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
        }
    }
}
