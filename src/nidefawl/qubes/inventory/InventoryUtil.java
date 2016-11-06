/**
 * 
 */
package nidefawl.qubes.inventory;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.inventory.slots.SlotStack;
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
    public static Tag writeToTag(List<SlotStack> stacks) {
        Tag.TagList list = new Tag.TagList();
        for (SlotStack stack : stacks) {
            if (stack.stack != null) {
                Compound cmp = new Compound();
                cmp.setInt("slot", stack.slot);
                cmp.set("stack", stack.stack.save());
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
    public static List<SlotStack> readFromTag(Tag tagInv) {
        if (tagInv.getType() != Tag.TagType.LIST)
            throw new GameError("Cannot read inventory from tag type that isn't LIST");
        Tag.TagList list = (Tag.TagList) tagInv;
        if (list.getSize() == 0) {
            return Collections.emptyList();
        }
        if (list.getListTagType() != Tag.TagType.COMPOUND)
            throw new GameError("Cannot read inventory from list that isn't type COMPOUND");
        List<SlotStack> stacks = Lists.newArrayList();
        for (Tag t : list.getList()) {
            Tag.Compound compound = (Tag.Compound) t;
            int i = compound.getInt("slot");
            Tag tagStack = compound.get("stack");
            stacks.add(new SlotStack(i, BaseStack.load(tagStack)));
        }
        return stacks;
    }

    public static int count(BaseInventory inv, BaseStack needle) {
        int j=0;
        for (int i = 0; i < inv.stacks.length; i++) {
            BaseStack stack = inv.stacks[i];
            if (stack!=null&&stack.isEqualId(needle)) {
                j++;
            }
        }
        return j;
    }

    public static boolean isEqual(BaseStack[] equipment, BaseStack[] equip) {
        if (equipment.length != equip.length) {
            return false;
        }
        for (int i = 0; i < equipment.length; i++) {
            if (!BaseStack.equalStacks(equip[i], equipment[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean copy(BaseStack[] equip, BaseStack[] equipment) {
        if (equip.length != equipment.length) {
            throw new IllegalArgumentException("Must provide equal sized arrays");
        }
        boolean isEqual = true;
        for (int i = 0; isEqual && i < equipment.length; i++) {
            BaseStack stack1 = equip[i];
            BaseStack stack2 = equipment[i];
            boolean eqstack = BaseStack.equalStacks(stack1, stack2);
            isEqual &= eqstack;
            if (!eqstack) {
                equipment[i] = stack1 == null ? null : stack1.copy();
            }
        }
        return isEqual;
    }

}
