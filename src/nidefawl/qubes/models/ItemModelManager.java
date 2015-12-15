/**
 * 
 */
package nidefawl.qubes.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.models.qmodel.ModelLoaderQModel;
import nidefawl.qubes.models.qmodel.ModelQModel;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ItemModelManager {
    static final ItemModelManager instance = new ItemModelManager();
    public static ItemModelManager getInstance() {
        return instance;
    }
    HashMap<AssetBinary, ModelQModel> models = new HashMap<>();
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
        TextureManager textureManager = TextureManager.getInstance();
        if (!this.models.isEmpty()) {
            for (ModelQModel model : this.models.values()) {
                model.release();
            }
            this.models.clear();
        }
        if (!this.textures.isEmpty()) {
            for (Integer texture : this.textures.values()) {
                textureManager.releaseTexture(texture);
            }
            this.textures.clear();
        }
        
        //TODO: load texture dynamically (once we have lots of models/textures)
        HashSet<String> textureNames = new HashSet<>();
        HashSet<String> modelNames = new HashSet<>();
        for (int i = 0; i < ItemModel.HIGHEST_MODEL_ID+1; i++) {
            ItemModel model = ItemModel.model[i];
            if (model == null) {
                continue;
            }
            textureNames.addAll(Arrays.asList(model.getTextures()));
            modelNames.addAll(Arrays.asList(model.getModels()));
        }
        HashMap<String, AssetTexture> textureAssets = new HashMap<>();
        HashMap<String, AssetBinary> modelAssets = new HashMap<>();
        for (String s : textureNames) {
            AssetTexture tex = mgr.loadPNGAsset(s);
            textureAssets.put(s, tex);
        }
        for (String s : modelNames) {
            AssetBinary tex = mgr.loadBin(s);
            modelAssets.put(s, tex);
        }
        for (AssetTexture t : textureAssets.values()) {
            int maxDim = Math.max(t.getWidth(), t.getHeight());
            int mipmapLevel = 1+GameMath.log2(maxDim);
            int tex = textureManager.makeNewTexture(t, false, true, mipmapLevel);
            this.textures.put(t, tex);
        }
        for (AssetBinary t : modelAssets.values()) {
            ModelLoaderQModel loader = new ModelLoaderQModel();
            loader.loadModel(t);
            ModelQModel model = loader.buildModel();
            this.models.put(t, model);
        }
        for (int i = 0; i < ItemModel.HIGHEST_MODEL_ID+1; i++) {
            ItemModel model = ItemModel.model[i];
            if (model == null) {
                continue;
            }
            String[] modeltextures = model.getTextures();
            int[] modelgltextures = new int[modeltextures.length];
            for (int j = 0; j < modeltextures.length; j++) {
                AssetTexture asset = textureAssets.get(modeltextures[j]);
                modelgltextures[j] = this.textures.get(asset);
            }
            String[] modelnames = model.getModels();
            ModelQModel[] models = new ModelQModel[modelnames.length];
            for (int j = 0; j < modelnames.length; j++) {
                AssetBinary asset = modelAssets.get(modelnames[j]);
                models[j] = this.models.get(asset);
            }
            System.out.println("loaded model "+model.name+" has id "+model.id);
            model.loadedModels = models;
            model.loadedTextures = modelgltextures;
        }
        System.out.println(Item.pickaxe.getItemModel().loadedTextures[0]);;
        System.out.println(Item.axe.getItemModel().loadedTextures[0]);;
    }

}
