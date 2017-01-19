package nidefawl.qubes.models;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.render.QModelDirectRender;
import nidefawl.qubes.models.render.QModelRender;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

public class EntityModelTest extends EntityModel {

    final Matrix4f modelMat = new Matrix4f();
    final Matrix4f normalMat = new Matrix4f();
    boolean isMale = false;
    ArrayList<QModelObject> headThings = Lists.newArrayList();
    final ModelOption modelHeadThing = new ModelOption(this, "Head thing");
    private QModelObject main_0;
    private QModelObject test1;
    public EntityModelTest(String name) {
        super(name);
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
        headThings.clear();
        List<QModelObject> list = model.getObjects();
        ArrayList<String> headThingsStr = Lists.newArrayList();
        headThingsStr.add("-");
        headThings.add(null);
        int idxHat=1;
        for (QModelObject g : list) {
            if (g.name.contains("HeadTh")) {
                headThings.add(g);
                headThingsStr.add("HeadThing "+(idxHat++));
            }
        }
        modelHeadThing.setOptions(headThingsStr.toArray(new String[headThingsStr.size()]));
        modelHeadThing.setDefaultVal(1);
        main_0 = getObject(list, "main_0");
        test1 = getObject(list, "test1");
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
        float scale = 1.0f / 100.0f;
        rend.modelMat.setIdentity();
        rend.modelMat.scale(scale);
        rend.modelMat.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
        rend.modelMat.rotate(-90 * GameMath.PI_OVER_180, 0, 0, 1);
        rend.modelMat.update();
        rend.normalMat.load(rend.modelMat);
        rend.normalMat.invert();
        rend.normalMat.transpose();
        rend.normalMat.update();
        this.model.animate(properties, fabs, fTime);
        this.model.setHeadOrientation(270 + headYaw, pitch);
        this.model.animateNodes(properties, fabs, fTime);
        QModelObject body = this.main_0;
        rend.addObject(body);
        if (this.test1 != null) {
            rend.addObject(this.test1);
        }
        attach(rend, properties, headThings, this.modelHeadThing, null);

    }

    private void attach(QModelRender rend, QModelProperties config, ArrayList<QModelObject> list, ModelOption modelOption, ModelOption texOption) {
        int modelSel = config.getOption(modelOption.getId());
        int texSel = texOption == null ? -1 : config.getOption(texOption.getId());
        QModelObject model = list.get(modelSel);
        if (model != null) {
            if (texSel > -1)
                model.bindTextureIdx(0, texSel);
            rend.addObject(model);
        }
    }
    
    public Matrix4f getNormalMat() {
        return this.normalMat;
    }
    public Matrix4f getModelMat() {
        return this.modelMat;
    }

}
