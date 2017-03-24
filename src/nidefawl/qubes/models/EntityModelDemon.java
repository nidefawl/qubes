package nidefawl.qubes.models;

import java.util.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.models.render.QModelRender;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vector3f;

public class EntityModelDemon extends EntityModel {

    boolean isMale = false;
    final ModelOption modelArmor = new ModelOption(this, "Armor");
    final ModelOption texSkin = new ModelOption(this, "Skin").setOptionCount("Skin #", 5);
    ArrayList<QModelObject> armors = Lists.newArrayList();
    private QModelObject main;
    private QModelAction walk;
    private QModelAction run;
    private QModelAction idle1;
    public EntityModelDemon(String name) {
        super(name);
    }
    

    @Override
    public void onLoad(ModelQModel model) {
        armors.clear();
        actions.clear();
        List<QModelObject> list = model.getObjects();
        ArrayList<String> list2 = Lists.newArrayList();
        for (QModelObject o : list) {
            if (o.name.contains("armor")) {
                list2.add(o.name);
            }
            if (o.name.contains("main")) {
                this.main = o;
            }
        }
        Collections.sort(list2);
        armors.add(null);
        for (String s : list2) {
            for (QModelObject o : list) {
                if (o.name.equals(s)) {
                    armors.add(o);
                }
            }
        }
        ArrayList<String> list3 = Lists.newArrayList();
        list3.add("No armor");
        for (int i = 1; i < armors.size(); i++) {
            list3.add("Armor "+(i+1));
        }
        modelArmor.setOptions(list3.toArray(new String[list3.size()]));

        QModelAction action = model.loader.listActions.get(0);
        actions.add(this.run=action.split("run", 0, 25));
        actions.add(action.split("strafe_left", 35, 60));
        actions.add(action.split("strafe_right", 105, 130));
        actions.add(action.split("run_back", 150, 175));
        actions.add(this.walk=action.split("walk", 200, 240));
        actions.add(action.split("walk_right", 280, 320));
        actions.add(action.split("walk_left", 360, 400));
        actions.add(action.split("walk_back", 438, 478));
        actions.add(this.idle1=action.split("idle1", 500, 560));
        actions.add(action.split("attack1", 580, 640));
        actions.add(action.split("attack2", 650, 670));
        actions.add(action.split("attack3", 690, 710));
        actions.add(action.split("attack4", 730, 760));
        actions.add(action.split("damage1", 770, 785));
        actions.add(action.split("death", 790, 840));
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
        main.bindTextureIdx(0, skinSel);
        rend.addObject(main);
        int modelWeaponSel = properties.getOption(modelArmor.getId());
        QModelObject n = armors.get(modelWeaponSel);
        if (n != null)
            rend.addObject(n);
    }
}
