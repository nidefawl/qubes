package nidefawl.qubes.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.IDMappingBlocks;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.models.qmodel.ModelBlock;
import nidefawl.qubes.models.qmodel.ModelLoaderQModel;
import nidefawl.qubes.models.qmodel.ModelQModel;
import nidefawl.qubes.texture.TextureManager;

public class BlockModelManager {
    static final BlockModelManager instance = new BlockModelManager();
    public static BlockModelManager getInstance() {
        return instance;
    }
    HashMap<AssetBinary, ModelBlock> models = new HashMap<>();
    HashMap<AssetTexture, Integer> textures = new HashMap<>();
    /**
     * 
     */
    public void init() {
    }
    /**
     * 
     */
    public void reload() {
        AssetManager mgr = AssetManager.getInstance();
        if (!this.models.isEmpty()) {
            for (ModelQModel model : this.models.values()) {
                model.release();
            }
            this.models.clear();
        }
        if (!this.textures.isEmpty()) {
            for (Integer texture : this.textures.values()) {
                GL.deleteTexture(texture);
            }
            this.textures.clear();
        }
        
        //TODO: load texture dynamically (once we have lots of models/textures)
        HashSet<String> modelNames = new HashSet<>();
        for (int i = 0; i < IDMappingBlocks.HIGHEST_BLOCK_ID+1; i++) {
            Block block = Block.block[i];
            if (block == null) {
                continue;
            }
            String[] models = block.getModels();
            if (models != null)
                modelNames.addAll(Arrays.asList(models));
        }
        HashMap<String, AssetBinary> modelAssets = new HashMap<>();
        for (String s : modelNames) {
            AssetBinary tex = mgr.loadBin(s);
            modelAssets.put(s, tex);
        }
        for (AssetBinary t : modelAssets.values()) {
            ModelLoaderQModel loader = new ModelLoaderQModel();
            loader.loadModel(t);
            ModelBlock model = loader.buildBlockModel();
            this.models.put(t, model);
            System.out.println("reloaded block model "+model.loader.getModelName());
        }
        for (int i = 0; i < IDMappingBlocks.HIGHEST_BLOCK_ID+1; i++) {
            Block block = Block.block[i];
            if (block == null) {
                continue;
            }
            String[] modelnames = block.getModels();
            if (modelnames == null) {
                continue;
            }
            ModelBlock[] models = new ModelBlock[modelnames.length];
            for (int j = 0; j < modelnames.length; j++) {
                AssetBinary asset = modelAssets.get(modelnames[j]);
                models[j] = this.models.get(asset);
            }
            
            block.loadedModels = models;
        }
    }

}
