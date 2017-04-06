/**
 * 
 */
package nidefawl.qubes.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.models.qmodel.ModelQModel;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;

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
    public void release() {
        if (!this.models.isEmpty()) {
            for (ModelQModel model : this.models.values()) {
                model.release();
            }
            this.models.clear();
        }
    }
    /**
     * 
     */
    public void reload() {
        AssetManager mgr = AssetManager.getInstance();
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
            model.loadedModels = models;
        }
    }
    public void redraw() {
        for (int i = 0; i < ItemModel.HIGHEST_MODEL_ID+1; i++) {
            ItemModel model = ItemModel.model[i];
            if (model == null) {
                continue;
            }
            for (int j = 0;j < model.loadedModels.length; j++) {
                model.loadedModels[j].draw();    
            }
        }
    }

}
