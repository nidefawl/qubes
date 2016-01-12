package nidefawl.qubes.crafting;

import java.util.HashMap;

import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.inventory.slots.SlotsInventoryBase;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.nbt.Tag;

public abstract class CraftingManagerBase {

    protected int id;
    protected int amount;
    protected long startTime;
    protected long endTime;
    protected boolean finished = false;
    protected CraftingRecipe recipe;

    public Tag.Compound save() {
        Tag.Compound tag = new Tag.Compound();
        int recipeid = recipe != null ? recipe.getId() : -1;
        tag.setInt("recipeid", recipeid);
        tag.setBoolean("finished", this.finished);
        tag.setLong("starttime", this.startTime);
        tag.setLong("endTime", this.endTime);
        tag.setInt("id", id);
        
        return tag;
    }
    public void load(Tag.Compound tag) {
        int recipeid = tag.getInt("recipeid");
        this.finished = tag.getBoolean("finished");
        this.startTime = tag.getLong("startTime");
        this.endTime = tag.getLong("endTime");
        this.recipe = CraftingRecipes.getRecipeId(recipeid);
    }
    /** does not yet work with special items */
    public int calcMaxAmount(CraftingRecipe recipe, SlotsInventoryBase inv) {
        HashMap<Integer, Integer> map = inv.getInv().getSortedStacks();
        if (map.isEmpty()) {
            return 0;
        }
        BaseStack[] input = recipe.getIn();
        int max = 100;
        for (int i = 0; i < input.length; i++) {
            int h = input[i].getTypeHash();
            Integer n = map.get(h);
            if (n == null)
                return 0;
            max = Math.min(n/input[i].getSize(), max);
        }
        return max;
    }
    public final int getAmount() {
        return this.amount;
    }
    public final int getId() {
        return this.id;
    }
    public final long getStartTime() {
        return this.startTime;
    }
    public final long getEndTime() {
        return this.endTime;
    }
    public final CraftingRecipe getRecipe() {
        return this.recipe;
    }
    public final boolean isRunning() {
        return this.recipe != null;
    }
    public final boolean isFinished() {
        return this.finished;
    }
    
}
