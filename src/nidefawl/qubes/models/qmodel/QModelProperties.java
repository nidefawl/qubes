package nidefawl.qubes.models.qmodel;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.qmodel.animation.QAnimationChannel;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.vec.Vector3f;

public class QModelProperties {
    public HashMap<Integer, Integer> options = Maps.newHashMap();
    public Vector3f pos = new Vector3f();
    public Vector3f rot = new Vector3f();
    public Entity entity;
    private ArrayList<QModelAction> actions = Lists.newArrayList();
    private ArrayList<Float> actionOffsets = Lists.newArrayList();
    public QModelProperties() {
    }
    public int getOption(int id) {
        Integer i = this.options.get(id);
        return i == null ? 0 : i.intValue();
    }
    public int getOption(int id, int defaultOption) {
        Integer i = this.options.get(id);
        return i == null ? defaultOption : i.intValue();
    }


    public void clear() {
       this.options.clear();
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
    public ArrayList<QModelAction> getActions() {
        return this.actions;
    }
    public void setActionOffset(int idx, float f) {
            
        this.actionOffsets.set(idx, f);
    }
    public void setAction(int idx, QModelAction act) {
        while (this.actions.size() <= idx)
            this.actions.add(null);
        while (this.actionOffsets.size() <= idx)
            this.actionOffsets.add(Float.valueOf(0));
        this.actions.set(idx, act);
        this.actionOffsets.set(idx, 0.0F);
    }
    public int getChannelIdx(String name) {
        int idx=-1;
        QAnimationChannel anim=null;
        for (int i = 0; i < actions.size(); i++) {
            QModelAction action = actions.get(i);
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
        return idx < 0  || idx >= this.actions.size() ? null : this.actions.get(idx).map.get(name);
    }
    public QModelAction getAction(int idx) {
        return idx < 0  || idx >= this.actions.size() ? null : this.actions.get(idx);
    }
    public Float getActionOffset(int idx) {
        Float f = idx < 0 || idx >= this.actionOffsets.size() ? null : this.actionOffsets.get(idx);;
        return f == null ? 0 : f.floatValue();
    }
}
