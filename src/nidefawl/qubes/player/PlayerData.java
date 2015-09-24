package nidefawl.qubes.player;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.vec.Vector3f;

public class PlayerData {

    public Vector3f pos = new Vector3f();
    public UUID world;
    public boolean flying;
    public int chunkLoadDistance;
    public HashSet<String> joinedChannels = new HashSet<>();

    public void load(Tag.Compound t) {
        this.pos = t.getVec3("pos");
        this.world = t.getUUID("world");
        this.flying = t.getByte("fly") != 0;
        List list = t.getList("chatchannels");
        list = Tag.unwrapStringList(list);
        this.joinedChannels = new HashSet<>(list);
    }

    public Tag save() {
        Tag.Compound cmp = new Tag.Compound();
        cmp.setVec3("pos", this.pos);
        if (this.world != null) {
            cmp.setUUID("world", this.world);    
        }
        cmp.setByte("fly", this.flying ? 1 : 0);
        cmp.setList("chatchannels", Tag.wrapStringList(this.joinedChannels));
        return cmp;
    }

}
