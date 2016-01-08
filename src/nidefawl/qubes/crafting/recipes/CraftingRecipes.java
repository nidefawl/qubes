package nidefawl.qubes.crafting.recipes;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.item.Item;

public abstract class CraftingRecipes {
    static int NEXT_ID = 0;
    private static CraftingRecipe[] recipesArray;

    public static void init() {
        int len = Item.log.getItems().size();
        ArrayList<CraftingRecipe> list = Lists.newArrayList();
        for (int i = 0; i < len; i++) {
            list.add(new CraftingRecipe(NEXT_ID++, Item.log.getItem(i), Item.plank.getItem(i)));    
        }
        recipesArray = new CraftingRecipe[NEXT_ID]; 
        for (int i = 0; i < len; i++) {
            recipesArray[list.get(i).getId()] = list.get(i);
        }
    }

    public static CraftingRecipe findRecipe(SlotsCrafting slots) {
        for (int i = 0; i < recipesArray.length; i++) {
            if (recipesArray[i].matches(slots)) {
                return recipesArray[i];
            }
        }
        return null;
    }

    public static CraftingRecipe getRecipeId(int id) {
        return id < 0 ? null : recipesArray[id];
    }

}
