package nidefawl.qubes.models;

import java.util.HashMap;
import java.util.HashSet;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.models.qmodel.ModelLoaderQModel;
import nidefawl.qubes.models.qmodel.ModelQModel;
import nidefawl.qubes.models.qmodel.ModelRigged;
import nidefawl.qubes.render.BatchedRiggedModelRenderer;
import nidefawl.qubes.util.GameError;
/**
 * Loads Entity models defined in {@link EntityModel}
 * @author Michael Hept 2016
 * Copyright: Michael Hept
 */
public class EntityModelmanager {
    static final EntityModelmanager instance = new EntityModelmanager();
    public static EntityModelmanager getInstance() {
        return instance;
    }
    HashMap<AssetBinary, ModelLoaderQModel> models = new HashMap<>();
    
    public void init() {
    }
    /**
     * 
     */
    public void reload() {
        AssetManager mgr = AssetManager.getInstance();
        this.models.clear();
        
        HashSet<String> modelNames = new HashSet<>();
        for (int i = 0; i < EntityModel.HIGHEST_MODEL_ID+1; i++) {
            EntityModel model = EntityModel.models[i];
            if (model == null) {
                continue;
            }
            model.loader = null;
            model.model = null;
            if (model.model != null) {
                model.model.release();
            }
            modelNames.add(model.getModelPath());
        }
        HashMap<String, AssetBinary> modelAssets = new HashMap<>();
        for (String s : modelNames) {
            AssetBinary tex = mgr.loadBin(s);
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
            String modelname = model.getModelPath();
            AssetBinary asset = modelAssets.get(modelname);
            model.loader = this.models.get(asset);
            model.model = model.loader.buildModel();
            System.out.println("loaded model "+model.name+" has id "+model.id);
        }
    }

}
