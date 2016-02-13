package nidefawl.qubes.player;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import nidefawl.qubes.crafting.CraftingCategory;
import nidefawl.qubes.inventory.InventoryUtil;
import nidefawl.qubes.inventory.slots.SlotStack;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.vec.Vector3f;

@SideOnly(value = Side.SERVER)
public class PlayerData extends EntityData {

    public UUID world;
    public boolean flying;
    public int chunkLoadDistance;
    public HashSet<String> joinedChannels = Sets.newHashSet();
    public HashMap<UUID, Vector3f> worldPositions = Maps.newHashMap();
    public List<SlotStack> invStacks = Lists.newArrayList();
    public List[] invCraftStacks = new List[CraftingCategory.NUM_CATS];
    public Compound[] craftingStates = new Tag.Compound[CraftingCategory.NUM_CATS];
    public Compound properties;
    public PlayerData() {
        for (int i = 0; i < invCraftStacks.length; i++) {
            invCraftStacks[i] = Lists.newArrayList();
        }
    }

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
        Tag tagInv = t.get("inventory");
        if (tagInv != null) {
            invStacks = InventoryUtil.readFromTag(tagInv);
        }
        for (int i = 0; i < this.invCraftStacks.length; i++) {
            Tag tagInvCraft = t.get("inventorycraft_"+i);
            if (tagInvCraft != null) {
                invCraftStacks[i] = InventoryUtil.readFromTag(tagInvCraft);
            }
            this.craftingStates[i] = (Compound) t.get("craftingstates_"+i);
        }
        this.properties = (Compound) t.get("properties");
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
        Tag tagInv = InventoryUtil.writeToTag(invStacks);
        cmp.set("inventory", tagInv);
        for (int i = 0; i < this.invCraftStacks.length; i++) {
            Tag tagInvCraft = InventoryUtil.writeToTag(invCraftStacks[i]);
            cmp.set("inventorycraft_"+i, tagInvCraft);
            cmp.set("craftingstates_"+i, this.craftingStates[i]);
        }
        System.out.println("properties "+properties.getMap());
        cmp.set("properties", this.properties);
        return cmp;
    }

}
