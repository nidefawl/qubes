package nidefawl.qubes.entity;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.TagType;

public class EntityProperties {
    public static final int MAX_PROPERTIES = 16;
    int[] properties = new int[MAX_PROPERTIES];
    
    public int getOption(int i, int j) {
        return properties[i];
    }

    public Tag.Compound save() {
        Tag.Compound tag = new Tag.Compound();
        Tag.ByteArray propList = new Tag.ByteArray();
        byte[] props = new byte[this.properties.length];
        for (int i = 0; i < props.length; i++) {
            props[i] = (byte) this.properties[i];
        }
        propList.setArray(props);
        tag.set("properties", propList);
        return tag;
    }
    public void load(Tag.Compound tag) {
        Tag c = tag == null ? null : tag.get("properties");
        if (c != null && c.getType() == TagType.BYTEARRAY)
        {
            Tag.ByteArray map = (Tag.ByteArray)c;
            byte[] props = map.getArray();
            for (int i = 0; i < props.length; i++) {
                this.properties[i] = props[i]&0xFF;
            }
        }
    }

    public void setOption(int i, int propVal) {
        if (properties.length<i) {
            throw new IllegalArgumentException("property idx >= "+this.properties.length);
        }
        properties[i] = propVal;
    }
}
