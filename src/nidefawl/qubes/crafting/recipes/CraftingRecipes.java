package nidefawl.qubes.crafting.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.block.*;
import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;

public abstract class CraftingRecipes {
    static int NEXT_ID = 0;
    public final static HashMap<String,ArrayList<CraftingRecipe>> map = Maps.newHashMap();
    private static CraftingRecipe[] recipesArray;

    public static void init() {
        {
            ArrayList<CraftingRecipe> list = Lists.newArrayList();
            int len = Item.log.getItems().size();
            for (int i = 0; i < len; i++) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(i)), new ItemStack(Item.plank.getItem(i))));    
            }
            map.put("planks", list);
            list = Lists.newArrayList();
            for (Block b : Block.stones.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("stones", list);
            list = Lists.newArrayList();
            for (Block b : Block.logs.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("logs", list);
            list = Lists.newArrayList();
            for (Block b : Block.wood.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("wood", list);
            list = Lists.newArrayList();
            for (Block b : Block.bricks.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("bricks", list);
            list = Lists.newArrayList();
            for (Block b : Block.stonebricks.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("stonebricks", list);
            list = Lists.newArrayList();
            for (Block b : Block.smoothstones.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("smoothstones", list);
            list = Lists.newArrayList();
            for (Block b : Block.cobblestones.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("cobblestones", list);
            list = Lists.newArrayList();
            for (Block b : Block.slabs.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("slabs", list);
            list = Lists.newArrayList();
            for (Block b : Block.stairs.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("stairs", list);
            list = Lists.newArrayList();
            for (Block b : Block.walls.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("walls", list);
            list = Lists.newArrayList();
            for (Block b : Block.fences.getBlocks()) {
                list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new BlockStack(b)));    
            }
            map.put("fences", list);
            list = Lists.newArrayList();
            list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new ItemStack(Item.pickaxe)));   
            map.put("pickaxes", list);
            list = Lists.newArrayList();
            list.add(new CraftingRecipe(NEXT_ID++, new ItemStack(Item.log.getItem(0)), new ItemStack(Item.axe)));   
            map.put("axes", list);
        }
        recipesArray = new CraftingRecipe[NEXT_ID];
        for (ArrayList<CraftingRecipe> list : map.values()) {
            for (CraftingRecipe r : list) {
                recipesArray[r.getId()] = r;
            }
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

    public static CraftingRecipe[] getAll() {
        return recipesArray;
    }

    public static List<CraftingRecipe> getList(String string) {
        return map.get(string);
    }

}
