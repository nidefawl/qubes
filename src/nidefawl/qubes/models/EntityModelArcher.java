package nidefawl.qubes.models;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.models.render.QModelRender;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vector3f;

public class EntityModelArcher extends EntityModel {
    static enum ModelOptionType {
        STRING, FLOAT 
    }
    final ModelOption texSkin = new ModelOption(this, "Skin texture").setOptionCount("Skin #", 3);
    final ModelOption modelHelmet = new ModelOption(this, "Helmet").setOptionCount("\0Helmet #", 3);
    final ModelOption texHelmet = new ModelOption(this, "Helmet texture").setOptionCount("Texture #", 3);
    final ModelOption modelShoulders = new ModelOption(this, "Shoulders").setOptionCount("\0Shoulders #", 3);
    final ModelOption texShoulders = new ModelOption(this, "Shoulders texture").setOptionCount("Texture #", 3);
    final ModelOption modelBracers = new ModelOption(this, "Bracers").setOptionCount("\0Bracers #", 3);
    final ModelOption texBracers = new ModelOption(this, "Bracers texture").setOptionCount("Texture #", 3);
    final ModelOption modelShield = new ModelOption(this, "Shield").setOptionCount("\0Shield #", 3);
    final ModelOption texShield = new ModelOption(this, "Shield texture").setOptionCount("Texture #", 3);
    final ModelOption modelWeapon = new ModelOption(this, "Weapon").setOptionCount("\0Weapon #", 2);
    boolean isMale = false;
    List<QModelObject[]> listBracers = Lists.newArrayList();
    List<QModelObject[]> listShoulders = Lists.newArrayList();
    List<QModelObject> listShields = Lists.newArrayList();
    List<QModelObject> listHelmets = Lists.newArrayList();
    List<QModelObject> listWeapon = Lists.newArrayList();
    Map<String, QModelMaterial> optNameToMat = Maps.newLinkedHashMap();
    Map<String, Map<String, QModelTexture>> optNameToOptTexMap = Maps.newLinkedHashMap();
    Map<String, String> defaultSettings = Maps.newLinkedHashMap();
    private QModelObject main_0;

    private boolean isArcher;
    private QModelAction idle1;
    private QModelAction walk;
    private QModelAction idle2;
    
    public EntityModelArcher(String name, boolean isArcher) {
        super(name);
        this.isArcher = isArcher;
        if (isArcher) {
            this.modelWeapon.setDefaultVal(1);
        }
    }

    @Override
    public void onLoad(ModelQModel model) {
        actions.clear();
        listBracers.clear();
        listShoulders.clear();
        listShields.clear();
        listHelmets.clear();
        listWeapon.clear();
        listBracers.add(null);
        listShoulders.add(null);
        listShields.add(null);
        listHelmets.add(null);
        listWeapon.add(null);
        List<QModelObject> list = model.getObjects();
        for (int i = 0; i < 5; i++) {
            QModelObject bracersL = getObject(list, "attach_brace_"+i+".0");
            QModelObject bracersR = getObject(list, "attach_brace_"+i+".1");
            if (bracersL != null && bracersR != null) {
                listBracers.add(new QModelObject[] { bracersL, bracersR });
            }
            QModelObject shouldersL = getObject(list, "attach_shoulder_"+i+".0");
            QModelObject shouldersR = getObject(list, "attach_shoulder_"+i+".1");
            if (shouldersL != null && shouldersR != null) {
                listShoulders.add(new QModelObject[] { shouldersL, shouldersR });
            }
            QModelObject shield = getObject(list, "attach_shield_"+i);
            if (shield != null)
                listShields.add(shield);
            QModelObject helmet = getObject(list, "attach_helmet_"+i);
            if (helmet != null)
                listHelmets.add(helmet);
            QModelObject weapon = getObject(list, "attach_weapon_"+i);
            if (weapon != null)
                listWeapon.add(weapon);
        }

        main_0 = getObject(list, "main");
        


        QModelAction action = model.loader.listActions.get(0);
        actions.add(this.walk=action.split("walk", 0, 30));
        actions.add(action.split("walk_back", 60, 90));
        actions.add(action.split("strafe_left", 110, 140));
        actions.add(action.split("strafe_right", 190, 220));
        actions.add(this.idle1=action.split("idle1", 252, 282));
        actions.add(this.idle2=action.split("idle2", 285, 345));
        actions.add(action.split("attack1", 355, 385));
        actions.add(action.split("attack2", 385, 415));
        actions.add(action.split("attack3", 420, 450));
        actions.add(action.split("attack4", 450, 485));
        actions.add(action.split("damage1", 485, 505));
        actions.add(action.split("death", 505, 535));
        actions.add(action.split("jump", 540, 570));
        actions.add(action.split("idleblock", 580, 600));
    }

    @Override
    public void setActions(QModelRender rend, QModelProperties properties, float fabs, float fTime) {
        Entity e = properties.entity;
        if (e != null) {
            properties.setAction(0, null);
            properties.setAction(1, null);
//            if (e.timePunch > 0 && e.timePunch + this.hit1.lenTime > Game.absTime) {
//                properties.setAction(0, hit1);
//                properties.setActionOffset(0, e.timePunch);
//            } else if (e.pos.distanceSq(e.lastPos) > 1.0E-4) {
//            } else {
//                properties.setAction(0, idle);
//            }
//            if (e.timeJump > 0 && e.timeJump + this.jump.lenTime > Game.absTime) {
//                properties.setAction(0, this.jump);
//                properties.setActionOffset(0, e.timeJump);
//            }
            if (e.pos.distanceSq(e.lastPos) > 1.0E-4) {
                properties.setAction(1, this.walk);
            }else {
              properties.setAction(0, this.idle1);
          }
        }
    }

    public void setPoseAndSubmit(QModelRender rend, QModelProperties properties, float fabs, float fTime) {
        Vector3f rot = properties.rot;
        Vector3f pos = properties.pos;
        float headYaw = rot.x;
        float yaw = rot.y;
        float pitch = rot.z;
        yaw -= headYaw;
        float rotYBase = -1 * yaw - 90;
        float scale = 1.0f / 50.0f;
        rend.modelMat.setIdentity();
        rend.modelMat.translate(pos.x, pos.y, pos.z);
        rend.modelMat.translate(-Engine.GLOBAL_OFFSET.x, -Engine.GLOBAL_OFFSET.y, -Engine.GLOBAL_OFFSET.z);
        rend.modelMat.rotate(rotYBase * GameMath.PI_OVER_180, 0, 1, 0);
        rend.modelMat.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
        rend.modelMat.rotate(-90 * GameMath.PI_OVER_180, 0, 0, 1);
        rend.modelMat.scale(scale);
        rend.modelMat.update();
        rend.normalMat.load(rend.modelMat);
        rend.normalMat.clearTranslation();
        rend.normalMat.invert();
        rend.normalMat.transpose();
        rend.normalMat.update();
        this.model.animate(properties, fabs, fTime);
        this.model.setHeadOrientation(270 + headYaw, pitch);
        this.model.animateNodes(properties, fabs, fTime);
        int skinSel = properties.getOption(this.texSkin.getId());
        main_0.bindTextureIdx(0, skinSel);
        rend.addObject(this.model, main_0);
        int modelWeaponSel = properties.getOption(this.modelWeapon.getId());
        boolean hasBow = modelWeaponSel == 2;
        QModelObject n = listWeapon.get(modelWeaponSel);
        if (n != null)
            rend.addObject(this.model, n);
        attach(rend, properties, this.listHelmets, this.modelHelmet, this.texHelmet);
        if (!hasBow)
            attach(rend, properties, this.listShields, this.modelShield, this.texShield);
        attach2(rend, properties, this.listBracers, this.modelBracers, this.texBracers);
            attach2(rend, properties, this.listShoulders, this.modelShoulders, this.texShoulders);
    }
    private void attach(QModelRender rend, QModelProperties config, List<QModelObject> list, ModelOption modelOption, ModelOption texOption) {
        int modelSel = config.getOption(modelOption.getId());
        int texSel = config.getOption(texOption.getId());
        QModelObject model = list.get(modelSel);
        if (model != null) {
            model.bindTextureIdx(0, texSel);
            rend.addObject(this.model, model);
        }
    }
    private void attach2(QModelRender rend, QModelProperties config, List<QModelObject[]> list, ModelOption modelOption, ModelOption texOption) {
        int modelSel = config.getOption(modelOption.getId());
        int texSel = config.getOption(texOption.getId());
        QModelObject[] model = list.get(modelSel);
        if (model != null) {
            model[0].bindTextureIdx(0, texSel);
            model[1].bindTextureIdx(0, texSel);
            rend.addObject(this.model, model[0]);
            rend.addObject(this.model, model[1]);
        }
    }
}
