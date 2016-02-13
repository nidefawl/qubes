package nidefawl.qubes.entity;

import java.util.HashMap;

import com.google.common.collect.Maps;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.TagType;

public class EntityProperties {
    HashMap<Integer, Integer> map = Maps.newHashMap();
    public int getOption(int i, int j) {
        Integer propVal = this.map.get(i);
        if (propVal == null) 
            return j;
        return propVal;
    }

    public Tag.Compound save() {
        Tag.Compound tag = new Tag.Compound();
        Tag.IntMap propList = new Tag.IntMap();
        propList.set(this.map);
        tag.set("properties", propList);
        return tag;
    }
    public void load(Tag.Compound tag) {
        Tag c = tag == null ? null : tag.get("properties");
        if (c != null && c.getType() == TagType.INT_MAP)
        {
            Tag.IntMap map = (Tag.IntMap)c;
            this.map.clear();
            this.map.putAll(map.getData());
        }
    }

    public void setOption(int i, int propVal) {
        this.map.put(i, propVal);
    }
}
