/**
 * 
 */
package nidefawl.qubes.inventory;

import java.util.Arrays;

import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.util.GameError;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class InventoryUtil {

    /**
     * @param stacks
     * @return
     */
    public static Tag writeToTag(BaseStack[] stacks) {
        Tag.TagList list = new Tag.TagList();
        for (int i = 0; i < stacks.length; i++) {
            BaseStack stack = stacks[i];
            if (stack != null) {
                Compound cmp = new Compound();
                cmp.setInt("slot", i);
                cmp.set("stack", stack.save());
                list.add(cmp);
            }
        }
        return list;
    }

    /**
     * @param tagInv
     * @param size
     * @return
     */
    public static void readFromTag(Tag tagInv, BaseStack[] stacks) {
        if (tagInv.getType() != Tag.TagType.LIST)
            throw new GameError("Cannot read inventory from tag type that isn't LIST");
        Tag.TagList list = (Tag.TagList) tagInv;
        if (list.getListTagType() != Tag.TagType.COMPOUND)
            throw new GameError("Cannot read inventory from list that isn't type COMPOUND");
        Arrays.fill(stacks, null);
        for (Tag t : list.getList()) {
            Tag.Compound compound = (Tag.Compound) t;
            int i = compound.getInt("slot");
            Tag tagStack = compound.get("stack");
            stacks[i] = BaseStack.load(tagStack);
        }
    }

}
