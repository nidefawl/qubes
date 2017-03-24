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

public class EntityModelZombie extends EntityModel {

    boolean isMale = false;
    final ModelOption modelSel = new ModelOption(this, "Model");
    final ModelOption texSkin = new ModelOption(this, "Skin texture").setOptionCount("Skin #", 3);
    List<QModelObject> modelList = Lists.newArrayList();
    private QModelAction walk;
    private QModelAction walk2;
    private QModelAction idle1;
    private QModelAction idle2;
    public EntityModelZombie(String name) {
        super(name);
    }

    public String getDefault(String option) {
        return "Zombie 1";
    }
    @Override
    public void onLoad(ModelQModel model) {
        modelList.clear();
        List<QModelObject> list = model.getObjects();
        int idx = 0;
        List<String> listStr = Lists.newArrayList();
        List<String> listStr2 = Lists.newArrayList();
        for (QModelObject o : list) {
            listStr.add(o.name);
        }
        Collections.sort(listStr);
        for (String s : listStr) {
            for (QModelObject o : list) {
                if (o.name.equals(s)) {
                    modelList.add(o);
                    listStr2.add("Zombie "+(idx+++1));
                }
            }
        }
        modelSel.setOptions(listStr2.toArray(new String[listStr2.size()]));
        



        QModelAction action = model.loader.listActions.get(0);
        actions.add(this.walk=action.split("walk", 0, 40));
        actions.add(this.walk2=action.split("walk2", 372, 411));
        actions.add(this.idle1=action.split("idle1", 52, 92));
        actions.add(this.idle2=action.split("idle2", 92, 190));
        actions.add(action.split("attack1", 190, 230));
        actions.add(action.split("attack2", 240, 285));
        actions.add(action.split("damage1", 290, 305));
        actions.add(action.split("damage2", 310, 325));
        actions.add(action.split("death", 330, 360));
    }
    @Override
    public void setActions(QModelRender rend, QModelProperties properties, float fabs, float fTime) {
        Entity e = properties.entity;
        if (e != null) {
            properties.setAction(0, null);
            properties.setAction(1, null);
            if (e.pos.distanceSq(e.lastPos) > 1.0E-4) {
                properties.setAction(1, this.walk);
            } else {
                properties.setAction(0, idle1);
            }
        }
    }

    @Override
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
        QModelObject n = modelList.get(properties.getOption(this.modelSel.getId()));
        n.bindTextureIdx(0, skinSel);
        rend.addObject(n);
    }
}
