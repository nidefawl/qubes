/**
 * 
 */
package nidefawl.qubes.item;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
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
}
