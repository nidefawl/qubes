package nidefawl.qubes.crafting.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.block.*;
import nidefawl.qubes.crafting.CraftingCategory;
import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;

public abstract class CraftingRecipes {
    public final static ArrayList<CraftingRecipe> all = Lists.newArrayList();
    private static CraftingRecipe[] recipesArray;

    public static void add(CraftingRecipe craftingRecipe) {
        all.add(craftingRecipe);
    }

    public static void init() {
        {
            int len = Item.log.getItems().size();
            for (int i = 0; i < len; i++) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(i)));
                recipe.setOutput(new ItemStack(Item.plank.getItem(i)));
                CraftingCategory.wood.addRecipe("planks", recipe);
            }
            for (Block b : Block.logs.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(((BlockLog)b).getIndex())));
                recipe.setOutput(new BlockStack(b, 8));
                CraftingCategory.wood.addRecipe("logs", recipe);  
            }
            for (Block b : Block.wood.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.plank.getItem(((BlockWood)b).getIndex())));
                recipe.setOutput(new BlockStack(b, 8));
                CraftingCategory.wood.addRecipe("wood", recipe);  
            }
            for (Block b : Block.stones.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(0)));
                recipe.setOutput(new BlockStack(b));
                CraftingCategory.stone.addRecipe("stones", recipe);
            }
            for (Block b : Block.bricks.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(0)));
                recipe.setOutput(new BlockStack(b));
                CraftingCategory.stone.addRecipe("bricks", recipe);
            }
            for (Block b : Block.stonebricks.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(0)));
                recipe.setOutput(new BlockStack(b));
                CraftingCategory.stone.addRecipe("stonebricks", recipe);
            }
            for (Block b : Block.smoothstones.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(0)));
                recipe.setOutput(new BlockStack(b));
                CraftingCategory.stone.addRecipe("smoothstones", recipe);
            }
            for (Block b : Block.cobblestones.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(0)));
                recipe.setOutput(new BlockStack(b));
                CraftingCategory.stone.addRecipe("cobblestones", recipe);
            }
            for (Block b : Block.slabs.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(0)));
                recipe.setOutput(new BlockStack(b, 2));
                CraftingCategory.blocks.addRecipe("slabs", recipe);
            }
            for (Block b : Block.stairs.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new BlockStack(b.getBaseBlock()));
                recipe.setOutput(new BlockStack(b));
                CraftingCategory.blocks.addRecipe("stairs", recipe);
            }
            for (Block b : Block.walls.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new BlockStack(b.getBaseBlock()));
                recipe.setOutput(new BlockStack(b, 2));
                CraftingCategory.blocks.addRecipe("walls", recipe);
            }
            for (Block b : Block.fences.getBlocks()) {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new BlockStack(b.getBaseBlock()));
                recipe.setOutput(new BlockStack(b, 3));
                CraftingCategory.blocks.addRecipe("fences", recipe);
            }
            {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(0)));
                recipe.setOutput(new ItemStack(Item.pickaxe));
                CraftingCategory.tools.addRecipe("pickaxes", recipe);
            }
            {
                CraftingRecipe recipe = new CraftingRecipe();
                recipe.setInput(new ItemStack(Item.log.getItem(0)));
                recipe.setOutput(new ItemStack(Item.axe));
                CraftingCategory.tools.addRecipe("axes", recipe);
            }
        }
        recipesArray = new CraftingRecipe[all.size()];
        for (CraftingRecipe r : all) {
            recipesArray[r.getId()] = r;
        }
    }

    public static CraftingRecipe getRecipeId(int id) {
        return id < 0 ? null : recipesArray[id];
    }

    public static CraftingRecipe[] getAll() {
        return recipesArray;
    }

}
