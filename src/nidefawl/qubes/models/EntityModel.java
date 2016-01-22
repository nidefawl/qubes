/**
 * 
 */
package nidefawl.qubes.models;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.models.qmodel.ModelLoaderQModel;
import nidefawl.qubes.models.qmodel.ModelQModel;
import nidefawl.qubes.render.BatchedRiggedModelRenderer;
import nidefawl.qubes.vec.Vector3f;


/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class EntityModel {

    public static final int MODEL_BITS = 8;
    public static final int NUM_MODELS = (1<<MODEL_BITS);
    public static final int MODEL_MASK = NUM_MODELS-1;
    public static int HIGHEST_MODEL_ID = 0;
    private static EntityModel[] registeredmodels;
    private static short[] registeredmodelIds;
    public static final EntityModel[] models = new EntityModel[NUM_MODELS];
    

    public final static EntityModel modelPlayer = new EntityModelPlayer("player").setModelPath("models/test.qmodel");

    public static void preInit() {
        
        ArrayList<EntityModel> list = Lists.newArrayList();
        for (int i = 0; i < models.length; i++) {
            if (models[i] != null) {
                list.add(models[i]);
            }
        }
        
        registeredmodels = list.toArray(new EntityModel[list.size()]);
        registeredmodelIds = new short[registeredmodels.length];
        for (int i = 0; i < registeredmodels.length; i++) {
            registeredmodelIds[i] = (short) registeredmodels[i].id;
        }
    
    }

    public static void postInit() {
    }

    public final int   id;

    protected String modelPath;
    public final String name;
    public ModelLoaderQModel loader;
    public ModelQModel model;
    

    public EntityModel(String name) {
        this.name = name;
        this.id = (HIGHEST_MODEL_ID++) + 1;
        models[this.id] = this;
        System.out.println("model "+this.name+" has id "+this.id);
    }

    protected EntityModel setModelPath(String modelPath) {
        this.modelPath = modelPath;
        return this;
    }

    public String getModelPath() {
        return this.modelPath;
    }

    /**
     * 
     */
    public void release() {
    }
    
    public ModelQModel getModel() {
        return this.model;
    }

    public abstract void setPose(BatchedRiggedModelRenderer rend, Entity e, float fabs, float fTime, Vector3f rot, Vector3f pos);

}
