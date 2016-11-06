package nidefawl.qubes.models.qmodel;

import java.util.Arrays;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.EntityProperties;
import nidefawl.qubes.models.qmodel.animation.QAnimationChannel;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.vec.Vector3f;

public class QModelProperties {
    final static int MAX_PROPERTIES = EntityProperties.MAX_PROPERTIES;
    final static int MAX_ACTIONS = 2;
    int[] properties = new int[MAX_PROPERTIES];
    private final QModelAction[] actions = new QModelAction[MAX_ACTIONS];
    private final float[] actionOffsets = new float[MAX_ACTIONS];
    private final float[] actionSpeed = new float[MAX_ACTIONS];
    public Vector3f pos = new Vector3f();
    public Vector3f rot = new Vector3f();
    public Entity entity;
    ModelQModel modelAtt = null;
    public QModelProperties() {
    }
    public int getOption(int id) {
        return this.properties[id];
    }


    public void clear() {
        Arrays.fill(this.properties, 0);
    }

    public void setRot(Vector3f rot) {
        this.rot.set(rot);
    }

    public void setPos(Vector3f pos) {
        this.pos.set(pos);
    }
    public void setEntity(Entity e) {
        this.entity = e;
    }

    public void setActionOffset(int idx, float f) {
        this.actionOffsets[idx] = f;
    }
    public void setActionSpeed(int idx, float f) {
        this.actionSpeed[idx] = f;
    }
    public void setAction(int idx, QModelAction act) {
        this.actions[idx] = act;
        this.actionOffsets[idx] = 0;
        this.actionSpeed[idx] = 1;
    }
    public int getChannelIdx(String name) {
        int idx=-1;
        QAnimationChannel anim=null;
        for (int i = 0; i < MAX_ACTIONS; i++) {
            QModelAction action = actions[i];
            if (action == null) {
                continue;
            }
            QAnimationChannel anim2 = action.map.get(name);
            if (anim == null || (anim2 != null && anim2.priority > anim.priority)) {
                anim = anim2;
                idx = i;
            }
        }
        return idx;
    }
    public QAnimationChannel getActionChannel(int idx, String name) {
        return idx < 0  || idx >= MAX_ACTIONS ? null : this.actions[idx].map.get(name);
    }
    public QModelAction getAction(int idx) {
        return idx < 0  || idx >= MAX_ACTIONS ? null : this.actions[idx];
    }
    public float getActionOffset(int idx) {
        return idx < 0  || idx >= MAX_ACTIONS ? 0.0f : this.actionOffsets[idx];
    }
    public float getActionSpeed(int idx) {
        return idx < 0  || idx >= MAX_ACTIONS ? 0.0f : this.actionSpeed[idx];
    }
    public void setModelAtt(ModelQModel modelAtt) {
        this.modelAtt = modelAtt;
    }
    public ModelQModel getModelAtt() {
        return this.modelAtt;
    }
    public void setOption(int id, int value) {
        this.properties[id] = value;
    }
    public void setProperties(int[] properties) {
        System.arraycopy(properties, 0, this.properties, 0, properties.length);
    }
}
