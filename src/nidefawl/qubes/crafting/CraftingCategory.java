package nidefawl.qubes.crafting;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.crafting.recipes.CraftingRecipe;

public class CraftingCategory {
    public static final int NUM_CATS = 4;
    public final static CraftingCategory[] categories = new CraftingCategory[NUM_CATS];
    public final static CraftingCategory tools = new CraftingCategory(0, "tools");
    public final static CraftingCategory wood = new CraftingCategory(1, "wood");
    public final static CraftingCategory stone = new CraftingCategory(2, "stone");
    public final static CraftingCategory blocks = new CraftingCategory(3, "blocks");
    public static CraftingCategory getCatId(int catid) {
        return catid<0||catid>=categories.length?null:categories[catid];
    }
    private String name;
    private int id;
    private ArrayList<CraftingRecipe> recipes = Lists.newArrayList();
    public final HashMap<String,ArrayList<CraftingRecipe>> map = Maps.newLinkedHashMap();
    public CraftingCategory(int id, String name) {
        categories[id]=this;
        this.id = id;
        this.name = name;
    }
    public int getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }

    public void addRecipe(String string, CraftingRecipe recipe) {
        recipe.setCategory(this, string);
        this.recipes.add(recipe);
        ArrayList<CraftingRecipe> list = map.get(string);
        if (list == null) {
            list = Lists.newArrayList();
            map.put(string, list);
        }
        list.add(recipe);
    }
}
