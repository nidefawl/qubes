package nidefawl.qubes.models;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.models.render.QModelRender;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

public class EntityModelAnimal extends EntityModel {

    private QModelObject main;
    final ModelOption texSkin = new ModelOption(this, "Skin");
    private final Matrix4f matDeform = new Matrix4f();
    Matrix4f rotation = new Matrix4f();
    public EntityModelAnimal(String name) {
        super(name);
    }
    
    @Override
    public void setActions(QModelRender rend, QModelProperties properties, float fabs, float fTime) {
        Entity e = properties.entity;
        QModelAction act = this.actions.get(1);
        if (e != null && e.pos.distanceSq(e.lastPos) > 1.0E-4) {
            act = this.actions.get(17);
        }
        properties.setAction(0, act);
    }
    @Override
    public void setPose(QModelRender rend, QModelProperties properties, float fabs, float fTime) {

        Vector3f rot = properties.rot;
        Vector3f pos = properties.pos;
        float headYaw = rot.x;
        float yaw = rot.y;
        float pitch = rot.z;
        yaw -= headYaw;
        float rotYBase = -1 * yaw - 90;
        rend.modelMat.setIdentity();
        rend.modelMat.translate(pos.x, pos.y, pos.z);
        rend.modelMat.translate(-Engine.GLOBAL_OFFSET.x, -Engine.GLOBAL_OFFSET.y, -Engine.GLOBAL_OFFSET.z);
        rend.modelMat.rotate(rotYBase * GameMath.PI_OVER_180, 0, 1, 0);
//        System.out.println(this.actions.get(0).map.keySet());
        QModelAction anim = properties.getAction(0);
        float offset = properties.getActionOffset(0);
//        System.out.println(anim);
        if (anim != null && anim.armatureAnim != null && anim.armatureAnim.setDeform(0, fabs-offset, this.matDeform)) {
            this.matDeform.mulMat(rotation);
            this.matDeform.mulMat(rend.modelMat);
            rend.modelMat.load(this.matDeform); 
        }
        rend.modelMat.update();
        rend.normalMat.load(rend.modelMat);
        rend.normalMat.clearTranslation();
        rend.normalMat.invert();
        rend.normalMat.transpose();
        rend.normalMat.update();
        this.model.animate(properties, fabs, fTime);
        this.model.setHeadOrientation(270 + headYaw, pitch);
        this.model.animateNodes(properties, fabs, fTime);
        main.bindTextureIdx(0, properties.getOption(this.texSkin.getId()));
        rend.addObject(main);
    }
    @Override
    public void onLoad(ModelQModel model) {
        rotation.setIdentity();
        rotation.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
        rotation.rotate(-90 * GameMath.PI_OVER_180, 0, 0, 1);
        actions.clear();
        super.onLoad(model);
        this.main = model.getObjects().get(0);
        QModelGroup grp = this.main.listGroups.get(0);
        QModelMaterial mat = grp.material;
        texSkin.setOptionCount("Skin #", mat.qTextures.length);
        QModelAction action = model.loader.listActions.get(0);
        actions.add(action.split("T-pose", 0, 5));
        actions.add(action.split("idle_01", 10, 180));
        actions.add(action.split("idle_02", 180, 280));
        actions.add(action.split("idle_03", 280, 380));
        actions.add(action.split("sitting", 390, 490));
        actions.add(action.split("sleeping", 500, 600));
        actions.add(action.split("hit", 610, 625));
        actions.add(action.split("attack", 625, 660));
        actions.add(action.split("death", 660, 700));
        actions.add(action.split("wash", 710, 830));
        actions.add(action.split("eat", 830, 900));
        actions.add(action.split("happy", 900, 980));
        actions.add(action.split("glum", 980, 1060));
        actions.add(action.split("voice", 1060, 1130));
        actions.add(action.split("licking", 1130, 1190));
        actions.add(action.split("shaking", 1190, 1240));
        actions.add(action.split("digging", 1240, 1300));
        actions.add(action.split("walk", 1310, 1350));
        actions.add(action.split("walk_back", 1355, 1395));
        actions.add(action.split("walk_left", 1400, 1440));
        actions.add(action.split("walk_right", 1440, 1480));
        actions.add(action.split("run", 1485, 1505));
        actions.add(action.split("jump", 1510, 1540));
        actions.add(action.split("swim", 1545, 1585));
        actions.add(action.split("jumpy", 1590, 1630));
    }

}
