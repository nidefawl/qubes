package nidefawl.qubes.models;

import java.util.HashMap;
import java.util.HashSet;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.util.GameError;

/**
 * Loads Entity models defined in {@link EntityModel}
 * @author Michael Hept 2016
 * Copyright: Michael Hept
 */
public class EntityModelManager {
    static final EntityModelManager instance = new EntityModelManager();
    public static EntityModelManager getInstance() {
        return instance;
    }
    HashMap<AssetBinary, ModelLoaderQModel> models = new HashMap<>();
    
    public void init() {
    }
    public void release() {
        for (int i = 0; i < EntityModel.HIGHEST_MODEL_ID+1; i++) {
            EntityModel model = EntityModel.models[i];
            if (model == null) {
                continue;
            }
            if (model.model != null) {
                model.model.release();
            }
            model.loader = null;
            model.model = null;
        }
        this.models.clear();
    }
    /**
     * 
     */
    public void reload() {
        AssetManager mgr = AssetManager.getInstance();
        
        HashSet<String> modelNames = new HashSet<>();
        for (int i = 0; i < EntityModel.HIGHEST_MODEL_ID+1; i++) {
            EntityModel model = EntityModel.models[i];
            if (model == null) {
                continue;
            }
            modelNames.add(model.getModelFile());
        }
        HashMap<String, AssetBinary> modelAssets = new HashMap<>();
        for (String s : modelNames) {
            AssetBinary tex = mgr.loadBin("models/"+s);
            if (tex == null) {
                throw new GameError("Missing Asset "+s);
            }
            modelAssets.put(s, tex);
        }
        for (AssetBinary t : modelAssets.values()) {
            ModelLoaderQModel qModelLoader = new ModelLoaderQModel();
            qModelLoader.loadModel(t);
            this.models.put(t, qModelLoader);
        }
        for (int i = 0; i < EntityModel.HIGHEST_MODEL_ID+1; i++) {
            EntityModel model = EntityModel.models[i];
            if (model == null) {
                continue;
            }
            String modelname = model.getModelFile();
            AssetBinary asset = modelAssets.get(modelname);
            model.setModel(this.models.get(asset));
            
        }
    }
    public void redraw() {
        for (int i = 0; i < EntityModel.HIGHEST_MODEL_ID+1; i++) {
            EntityModel model = EntityModel.models[i];
            if (model == null) {
                continue;
            }
            model.getLoader().loadTextures();
            model.getModel().draw();
        }
    }

}
