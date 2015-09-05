package nidefawl.qubes.player;

import java.util.UUID;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.vec.Vector3f;

public class PlayerData {

    public Vector3f pos = new Vector3f();
    public UUID world;
    public boolean flying;

    public void load(Tag.Compound t) {
        this.pos = t.getVec3("pos");
        this.world = t.getUUID("world");
        this.flying = t.getByte("fly") != 0;
    }

    public Tag save() {
        Tag.Compound cmp = new Tag.Compound();
        cmp.setVec3("pos", this.pos);
        if (this.world != null) {
            cmp.setUUID("world", this.world);    
        }
        cmp.setByte("fly", this.flying ? 1 : 0); 
        return cmp;
    }

}
