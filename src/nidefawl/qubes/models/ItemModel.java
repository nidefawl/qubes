/**
 * 
 */
package nidefawl.qubes.models;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.models.qmodel.ModelQModel;


/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ItemModel {

    public static final int MODEL_BITS = 8;
    public static final int NUM_MODELS = (1<<MODEL_BITS);
    public static final int MODEL_MASK = NUM_MODELS-1;
    public static int HIGHEST_MODEL_ID = 0;
    private static ItemModel[] registeredmodels;
    private static short[] registeredmodelIds;
    public static final ItemModel[] model = new ItemModel[NUM_MODELS];
    

    public final static ItemModel modelPickaxe = new ItemModel("pickaxe").setModels("models/pick.qmodel");
    public final static ItemModel modelAxe = new ItemModel("axe").setModels("models/axe.qmodel");

    public static void preInit() {
        
        for (int i = 0; i < model.length; i++) {
            ItemModel b = model[i];
            if (b != null) {
                if (b.textures == null) {
                }
            }
        }
        
        ArrayList<ItemModel> list = Lists.newArrayList();
        for (int i = 0; i < model.length; i++) {
            if (model[i] != null) {
                list.add(model[i]);
            }
        }
        
        registeredmodels = list.toArray(new ItemModel[list.size()]);
        registeredmodelIds = new short[registeredmodels.length];
        for (int i = 0; i < registeredmodels.length; i++) {
            registeredmodelIds[i] = (short) registeredmodels[i].id;
        }
    
    }

    public static void postInit() {
    }

    public final int   id;

    protected String[] models;
    protected String[] textures;
    public ModelQModel[] loadedModels;
    public final String name;
    

    public ItemModel(String name) {
        this.name = name;
        this.id = (HIGHEST_MODEL_ID++) + 1;
        model[this.id] = this;
        System.out.println("model "+this.name+" has id "+this.id);
    }

    protected ItemModel setModels(String... models) {
        this.models = models;
        return this;
    }

    public String[] getModels() {
        return this.models;
    }

    /**
     * 
     */
    public void release() {
    }


}
