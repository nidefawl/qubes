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
        
        HashSet<String> modelNames = new HashSet<>();
        for (int i = 0; i < ItemModel.HIGHEST_MODEL_ID+1; i++) {
            ItemModel model = ItemModel.model[i];
            if (model == null) {
                continue;
            }
            modelNames.addAll(Arrays.asList(model.getModels()));
        }
        HashMap<String, AssetBinary> modelAssets = new HashMap<>();
        for (String s : modelNames) {
            AssetBinary tex = mgr.loadBin(s);
            modelAssets.put(s, tex);
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
            String[] modelnames = model.getModels();
            ModelQModel[] models = new ModelQModel[modelnames.length];
            for (int j = 0; j < modelnames.length; j++) {
                AssetBinary asset = modelAssets.get(modelnames[j]);
                models[j] = this.models.get(asset);
            }
            System.out.println("loaded model "+model.name+" has id "+model.id);
            model.loadedModels = models;
        }
    }

}