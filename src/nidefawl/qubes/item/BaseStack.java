/**
 * 
 */
package nidefawl.qubes.item;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.network.StreamIO;
import nidefawl.qubes.util.GameError;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class BaseStack implements StreamIO {

    public abstract boolean isItem();
    public boolean isBlock() {
        return !isItem();
    }
    public abstract BaseStack copy();
    public abstract Tag save();
    public abstract boolean isFullyEqual(BaseStack s);
    public abstract boolean isEqualId(BaseStack s);
    /**
     * 
     */
    public static BaseStack load(Tag t) {
        if (t.getType() != Tag.TagType.COMPOUND)
            throw new GameError("Cannot load itemstack from tag that isn't type COMPOUNT");
        Tag.Compound cmp = (Tag.Compound) t;
        int type = cmp.getInt("stacktype");
        switch (type) {
            case 0:
                return new BlockStack(cmp);
            case 1:
                return new ItemStack(cmp);
        }
        throw new GameError("Invalid stack type");
    }
    public static boolean equalStacks(BaseStack a, BaseStack b) {
        if (a == null)
            return b == null;
        if (b == null)
            return a == null;
        return a.equals(b);
    }
    public abstract Item getItem();
    public abstract Block getBlock();
    public boolean is(Block b) {
        return getBlock()==b;
    }
    public boolean is(Item item) {
        return getItem()==item;
    }
    public String getName() {
        return getItem()!=null?getItem().getName():getBlock().getName();
    }

    static ItemStack  tmpStackItem  = new ItemStack();
    static BlockStack tmpStackBlock = new BlockStack();

    public static BaseStack getTemp(Object o) {
        if (o instanceof Item) {
            tmpStackItem.id=((Item)o).id;
            return tmpStackItem;
        }
        if (o instanceof Block) {
            tmpStackBlock.id=((Block)o).id;
            return tmpStackBlock;
        }
        return null;
    }
}
