package nidefawl.qubes.models.render;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.render.AbstractRenderer;

public abstract class QModelRender extends AbstractRenderer {

    public ArrayList<QModelObject> rendered = Lists.newArrayList();
    public ModelQModel model;
    public BufferedMatrix normalMat;
    public BufferedMatrix modelMat;
    public QModelRender() {
        this.normalMat = new BufferedMatrix();
        this.modelMat = new BufferedMatrix();
    }
    public void setModel(ModelQModel model) {
        this.model = model;
    }

    public abstract void initShaders();
    public abstract void reset();
    public abstract void render(float fTime);
    public abstract void addObject(QModelObject main);
    public void renderGroup(ModelQModel model, QModelObject obj, QModelGroup grp, float fTime) {
        model.render(obj.idx, grp.idx, fTime);
    }
}
