package nidefawl.qubes.models;

import java.util.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.input.DigController;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.qmodel.animation.QAnimationChannel;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.models.render.QModelRender;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

public class EntityModelPlayer extends EntityModel {

    final Matrix4f modelMat = new Matrix4f();
    final Matrix4f normalMat = new Matrix4f();
    public boolean isMale = false;
    ArrayList<QModelObject> hats = Lists.newArrayList();
    ArrayList<QModelObject> beards = Lists.newArrayList();
    ArrayList<QModelObject> hairs = Lists.newArrayList();
    ArrayList<QModelObject> bags = Lists.newArrayList();
    public final ModelOption modelSize = new ModelOption(this, "Model").setOptions("Normal", "Slim", "Strong");
    public final ModelOption texSkin = new ModelOption(this, "Skin texture").setOptionCount("Skin #", 13);
    public final ModelOption texFace = new ModelOption(this, "Face texture").setOptionCount("Face #", 13);
    public final ModelOption modelHair = new ModelOption(this, "Hair");
    public final ModelOption texHair = new ModelOption(this, "Hair texture");
    public final ModelOption modelBeard = new ModelOption(this, "Beard");
    public final ModelOption texBeard = new ModelOption(this, "Beard texture");
    public final ModelOption modelHat = new ModelOption(this, "Hat");
    public final ModelOption texHat = new ModelOption(this, "Hat texture").setOptionCount("Texture #", 7);
    public final ModelOption modelBag= new ModelOption(this, "Bag");
    public final ModelOption texBag = new ModelOption(this, "Bag texture").setOptionCount("Texture #", 2);
//    final ModelOption curAction = new ModelOption(this, "Action");
    private QModelObject main_0;
    private QModelObject main_1;
    private QModelObject main_2;
    private QModelObject[] body;
    private QModelAction run;
    private QModelAction jump;
    private QModelAction hit1;
    private QModelAction idle;
    public QModelAction getIdle() {
        return idle;
    }
    public EntityModelPlayer(String name, boolean b) {
        super(name);
        
        this.isMale = b;
        if (isMale) {
            texHair.setOptionCount("Texture #", 10);
            texBeard.setOptionCount("Texture #", 10);
        } else {
            texBeard.setOptionCount("", 0);
            texHair.setOptionCount("Texture #", 4);
        }
    }

    public QModelObject getObject(List<QModelObject> list, String name) {
        for (QModelObject g : list) {
            if (g.name.startsWith(name)) {
                return g;
            }
        }
        return null;
    }
    @Override
    public void onLoad(ModelQModel model) {
        actions.clear();
        hats.clear();
        hairs.clear();
        beards.clear();
        List<QModelObject> list = model.getObjects();
        ArrayList<String> hairNames = Lists.newArrayList();
        ArrayList<String> hatNames = Lists.newArrayList();
        ArrayList<String> beardNames = Lists.newArrayList();
        ArrayList<String> bagNames = Lists.newArrayList();
        bagNames.add("-");
        hatNames.add("-");
        hairNames.add("-");
        beardNames.add("-");
        bags.add(null);
        hats.add(null);
        hairs.add(null);
        beards.add(null);
        int idxHat=1;
        int idxHair=1;
        int idxBeard=1;
        int idxTool=1;
        int idxBag=1;
        for (QModelObject g : list) {
            String nLower = g.name.toLowerCase();
            if (nLower.contains("hat")) {
                hats.add(g);
                hatNames.add("Hat "+(idxHat++));
            } else if (nLower.contains("hair")) {
                hairs.add(g);
                hairNames.add("Hair "+(idxHair++));
            } else if (nLower.contains("beard")) {
                beards.add(g);
                beardNames.add("Beard "+(idxBeard++));
            } else if (nLower.contains("bag")) {
                bags.add(g);
                bagNames.add("Bag "+(idxBag++));
            } else if (nLower.startsWith("attach_")) {
            }
        }
        
        modelHat.setOptions(hatNames.toArray(new String[hatNames.size()]));
        modelBeard.setOptions(beardNames.toArray(new String[beardNames.size()]));
        modelHair.setOptions(hairNames.toArray(new String[hairNames.size()]));
        modelBag.setOptions(bagNames.toArray(new String[bagNames.size()]));
        modelHair.setDefaultVal(1);
        main_0 = getObject(list, "main_0");
        main_1 = getObject(list, "main_1");
        main_2 = getObject(list, "main_2");
        this.body = new QModelObject[] {
                main_0, main_1, main_2
        };
        

        QModelAction action = model.loader.listActions.get(0);
        this.run = action.split("run", 0, 20);
        for (QModelBone n : model.loader.listBones) {
            String s = n.name;
            QAnimationChannel channel = run.map.get(s);
            if (channel != null) {
                QModelBone p = n;
                boolean isLeg = false;
                int steps = 0;
                while (p != null) {
                    if (p.name.endsWith("Thigh")) {
                        isLeg = true; break;
                    }
                    steps++;
                    p = p.parent;
                }
                if (isLeg) {
                    channel.priority++;
                } else {
                    channel.priority--;
                }
            }
        }
        actions.add(this.run);
        actions.add(action.split("walk", 30, 60));
        actions.add(action.split("walk_back", 60, 90));
        actions.add(action.split("run_fast", 90, 110));
        actions.add(action.split("strafe_left", 120, 150));
        actions.add(action.split("strafe_right", 150, 180));
        actions.add(action.split("anim1", 180, 200));
        actions.add(this.idle=action.split("idle1", 190, 220));
        actions.add(action.split("look", 220, 300));
        actions.add(this.hit1=action.split("hit1", 300, 330));
        actions.add(action.split("hit2", 340, 380));
        actions.add(action.split("hit3", 380, 410));
        actions.add(action.split("saw", 420, 450));
        actions.add(action.split("hammer1", 460, 485));
        actions.add(action.split("hammer2", 500, 530));
        actions.add(action.split("shovel", 540, 565));
        actions.add(action.split("hands", 580, 610));
        actions.add(action.split("plant", 580, 650));
        actions.add(action.split("talk", 660, 720));
        actions.add(action.split("damage1", 730, 750));
        actions.add(action.split("damage2", 750, 770));
        actions.add(action.split("death", 770, 810));
        actions.add(this.jump=action.split("jump", 860, 885));
    }

    @Override
    public void setActions(QModelRender rend, QModelProperties properties, float fabs, float fTime) {
        Entity e = properties.entity;
        if (e != null) {
            properties.setAction(0, null);
            properties.setAction(1, null);
            DigController dig = Game.instance.dig;
            if (dig.isDigAnimation()/*e.timePunch > 0 && Game.absTime-e.timePunch < hit1.lenTime*/) {
                
                properties.setAction(0, hit1);
                properties.setActionOffset(0, e.timePunch);
                properties.setActionSpeed(0, 1.33f);
            } else if (e.pos.distanceSq(e.lastPos) > 1.0E-4) {
            } else {
                properties.setAction(0, idle);
            }
            if (e.timeJump > 0 && Game.absTime-e.timeJump < this.jump.lenTime) {
                properties.setAction(0, this.jump);
                properties.setActionOffset(0, e.timeJump);
            }
            if (e.pos.distanceSq(e.lastPos) > 1.0E-4) {
                properties.setAction(1, this.run);
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
         rotYBase = -rot.x-90;
         float rotYH = rotYBase - (-rot.y-90);
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
        this.model.setHeadOrientation(270 + rotYH, pitch);
        this.model.animateNodes(properties, fabs, fTime);
        
        int n = properties.getOption(this.modelSize.getId());
        int skinSel = properties.getOption(this.texSkin.getId());
        int faceSel = properties.getOption(this.texFace.getId());
        if (n < 0)
            n = 0;
        QModelObject body = this.body[n];
        body.bindTextureIdx(0, skinSel);
        body.bindTextureIdx(1, faceSel);
        rend.addObject(this.model, body);
        attach(rend, properties, hairs, this.modelHair, this.texHair);
        attach(rend, properties, hats, this.modelHat, this.texHat);
        attach(rend, properties, beards, this.modelBeard, this.texBeard);
        attach(rend, properties, bags, this.modelBag, this.texBag);

        if (properties.getModelAtt() != null) {
            Matrix4f matSlot = ((ModelRigged)this.model).weaponSlot.matDeform;
            BufferedMatrix m4 = Engine.getTempMatrix();
            m4.load(matSlot);
            m4.mulMat(rend.modelMat);
            m4.rotate(-180 * GameMath.PI_OVER_180, 1, 0, 0);
            m4.rotate(-140 * GameMath.PI_OVER_180, 0, 1, 0);
            m4.scale(13f);
            m4.translate(0, -1f, 0);
            rend.modelMat.load(m4);
            rend.modelMat.update();
            rend.normalMat.load(rend.modelMat);
            rend.normalMat.clearTranslation();
            rend.normalMat.invert();
            rend.normalMat.transpose();
            rend.normalMat.update();
            QModelObject qobj = properties.getModelAtt().getObjects().get(0);
            qobj.bindTextureIdx(0, 0);
            qobj.setAttachmentEmpty(null);
            rend.addObject(properties.getModelAtt(), qobj);
        }
    }
    
    

    private void attach(QModelRender rend, QModelProperties config, ArrayList<QModelObject> list, ModelOption modelOption, ModelOption texOption) {
        int modelSel = config.getOption(modelOption.getId());
        int texSel = config.getOption(texOption.getId());
        QModelObject model = modelSel >= list.size() || modelSel < 0 ? null : list.get(modelSel);
        if (model != null) {
            model.bindTextureIdx(0, texSel);
            rend.addObject(this.model, model);
        }
    }
    
    
    public Matrix4f getNormalMat() {
        return this.normalMat;
    }
    public Matrix4f getModelMat() {
        return this.modelMat;
    }

}
