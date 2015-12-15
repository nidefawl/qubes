package nidefawl.qubes.player;

import java.util.*;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import nidefawl.qubes.inventory.InventoryUtil;
import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.vec.Vector3f;

public class PlayerData {

    public UUID world;
    public boolean flying;
    public int chunkLoadDistance;
    public HashSet<String> joinedChannels = Sets.newHashSet();
    public HashMap<UUID, Vector3f> worldPositions = Maps.newHashMap();
    public PlayerInventory inv = new PlayerInventory();

    public void load(Tag.Compound t) {
        this.world = t.getUUID("world");
        this.flying = t.getByte("fly") != 0;
        List list = t.getList("chatchannels");
        list = Tag.unwrapStringList(list);
        this.joinedChannels = new HashSet<>(list);
        Tag.Compound cmpWorldPos = (Compound) t.get("worldpositions");
        if (cmpWorldPos != null) {
            Map<String, Tag> map = cmpWorldPos.getMap();
            for (String s : map.keySet()) {
                UUID uuid = StringUtil.parseUUID(s, null);
                if (uuid != null) {
                    this.worldPositions.put(uuid, cmpWorldPos.getVec3(s));
                }
            }
        }
        int size = t.getInt("invsize");
        Tag tagInv = t.get("inventory");
        if (tagInv != null) {
            InventoryUtil.readFromTag(tagInv, inv.stacks);
        }
    }

    public Tag save() {
        Tag.Compound cmp = new Tag.Compound();
        if (this.world != null) {
            cmp.setUUID("world", this.world);    
        }
        cmp.setByte("fly", this.flying ? 1 : 0);
        cmp.setList("chatchannels", Tag.wrapStringList(this.joinedChannels));
        Tag.Compound cmpWorldPos = new Tag.Compound();
        for (UUID uuid : this.worldPositions.keySet()) {
            cmpWorldPos.setVec3(uuid.toString(), this.worldPositions.get(uuid));
        }
        cmp.set("worldpositions", cmpWorldPos);
        cmp.setInt("invsize", inv.inventorySize);
        Tag tagInv = InventoryUtil.writeToTag(inv.stacks);
        cmp.set("inventory", tagInv);
        return cmp;
    }

}
