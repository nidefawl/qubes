/**
 * 
 */
package nidefawl.qubes.models;

import java.util.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.models.render.QModelRender;


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
    
    public final static EntityModel modelPlayerMale = new EntityModelPlayer("player_male", true).setModelPath("male.qmodel");
  public final static EntityModel modelPlayerFemale = new EntityModelPlayer("player_female", false).setModelPath("female.qmodel");
//
//
  
    public final static EntityModel modelArcher = new EntityModelArcher("archer", true).setModelPath("archer.qmodel");
    public final static EntityModel modelWarrior = new EntityModelArcher("warrior", false).setModelPath("warrior.qmodel");
    public final static EntityModel modelSkeleton = new EntityModelSkeleton("skeleton").setModelPath("skeleton.qmodel");
    public final static EntityModel modelZombie = new EntityModelZombie("zombie").setModelPath("zombie.qmodel");
    public final static EntityModel modelDemon = new EntityModelDemon("demon").setModelPath("demon.qmodel");
    
    public final static EntityModel modelCat = new EntityModelAnimal("cat").setModelPath("cat.qmodel");
    public final static EntityModel modelCow = new EntityModelAnimal("cow").setModelPath("cow.qmodel");
    public final static EntityModel modelChicken = new EntityModelAnimal("chicken").setModelPath("chicken.qmodel");
    public final static EntityModel modelDog = new EntityModelAnimal("dog").setModelPath("dog.qmodel");
    public final static EntityModel modelDuck = new EntityModelAnimal("duck").setModelPath("duck.qmodel");
    public final static EntityModel modelGoat = new EntityModelAnimal("goat").setModelPath("goat.qmodel");
    public final static EntityModel modelPig = new EntityModelAnimal("pig").setModelPath("pig.qmodel");
    public final static EntityModel modelPony = new EntityModelAnimal("pony").setModelPath("pony.qmodel");
    public final static EntityModel modelPuppy = new EntityModelAnimal("puppy").setModelPath("puppy.qmodel");
    public final static EntityModel modelSheep = new EntityModelAnimal("sheep").setModelPath("sheep.qmodel");
    
    public final static EntityModel modelTest = new EntityModelTest("test").setModelPath("test.qmodel");

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
    List<ModelOption> modelOptions = Lists.newArrayList();
    ArrayList<QModelAction> actions = Lists.newArrayList();
    

    public EntityModel(String name) {
        this.name = name;
        this.id = (HIGHEST_MODEL_ID++);
        models[this.id] = this;
    }
    public int addOption(ModelOption modelOption) {
        int id = this.modelOptions.size();
        this.modelOptions.add(modelOption);
        return id;
    }

    protected EntityModel setModelPath(String modelPath) {
        this.modelPath = modelPath;
        return this;
    }

    public String getModelFile() {
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


    public abstract void setPose(QModelRender rend, QModelProperties properties, float fabs, float fTime);
    public void setModel(ModelLoaderQModel modelLoaderQModel) {
        this.loader = modelLoaderQModel;
        this.model = modelLoaderQModel.buildModel();
        onLoad(this.model);
    }

    public void onLoad(ModelQModel model) {
    }

    public String getName() {
        return this.name;
    }


    public QModelObject getObject(List<QModelObject> list, String name) {
        for (QModelObject g : list) {
            if (g.name.startsWith(name)) {
                return g;
            }
        }
        return null;
    }
    public List<ModelOption> getModelOptions() {
        return this.modelOptions;
    }

    public List<QModelAction> getActions() {
        return this.actions;
    }

    public void setActions(QModelRender rend, QModelProperties properties, float fabs, float fTime) {
    }
    public QModelAction getIdle() {
        return null;
    }
}
