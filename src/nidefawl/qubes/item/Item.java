/**
 * 
 */
package nidefawl.qubes.item;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockLog;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.models.ItemModel;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.texture.array.impl.gl.ItemTextureArrayGL;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.structure.tree.Tree;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Item {
    public static final int ITEM_MASK = 0x1FF;
    public static final int NUM_ITEMS = 512;
    private static Item[] registereditems;
    private static short[] registereditemIds;
    public static final Item[] item = new Item[NUM_ITEMS];
    public static final Item pickaxe = new Item("pickaxe").setTextures("tools/pick").setModel(ItemModel.modelPickaxe);
    public static final Item axe = new Item("axe").setTextures("tools/axe").setModel(ItemModel.modelAxe);
    public static final ItemGroupLog log = new ItemGroupLog();
    public static final ItemGroupPlank plank = new ItemGroupPlank();
    public static final ItemGroupStones stones = new ItemGroupStones();
    /**
     * @return
     */
    public static Item[] getRegisteredIDs() {
        return registereditems;
    }
    public Item(String name) {
        this(name, false);
    }

    public final int id;
    private final String name;
    private final boolean transparent;
    protected String[] textures;
    protected final AABBFloat blockBounds = new AABBFloat(0, 0, 0, 1, 1, 1);
    private ItemModel itemModel;
    private ItemGroup itemGroup;
    public ItemGroup getItemGroup() {
        return this.itemGroup;
    }
    public void setItemGroup(ItemGroup itemGroup) {
        this.itemGroup = itemGroup;
    }

    public Item(String name, boolean transparent) {
        if (name.contains(" ")) {
            throw new GameError("Names must not contain spaces");
        }
        this.name = name;
        this.id = IDMappingItems.get(name);
        item[id] = this;
        this.transparent = transparent;
        init();
    }
    
    public void init() {
    }


    public Item setModel(ItemModel itemModel) {
        this.itemModel = itemModel;
        return this;
    }
    
    public String[] getTextures() {
        return this.textures;
    }

    public Item setTextures(String... list) {
        this.textures = list;
        return this;
    }

    public Item setAbsTextures(String...list) {
        this.textures = list;
        return this;
    }
    
    public String getName() {
        return name;
    }
    /**
     * 
     */
    public static void preInit() {
        
        ArrayList<Item> list = Lists.newArrayList();
        for (int i = 0; i < Item.item.length; i++) {
            if (item[i] != null) {
                list.add(item[i]);
            }
        }
        
        registereditems = list.toArray(new Item[list.size()]);
        registereditemIds = new short[registereditems.length];
        for (int i = 0; i < registereditems.length; i++) {
            registereditemIds[i] = (short) registereditems[i].id;
        }
    
    }

    public static void postInit() {
    }

    public ArrayList<AssetTexture> resolveTextures(AssetManager mgr) {
        ArrayList<AssetTexture> textures = new ArrayList<>();
        for (String s : this.textures) {
            AssetTexture tex = mgr.loadPNGAsset(s, false);
            textures.add(tex);
        }
        return textures;
    }

    public static Item get(int id) {
        return item[id];
    }
    /**
     * @param itemStack
     * @return
     */
    public int getTexture(ItemStack itemStack) {
        return TextureArrays.itemTextureArray.getTextureIdx(this.id, 0);
    } 
    
    /**
     * @return the itemModel
     */
    public ItemModel getItemModel() {
        return this.itemModel;
    }
    public boolean canMine(BlockPlacer placer, Block block, World w, BlockPos pos, PlayerServer player, ItemStack itemstack) {
        //Move to subclass
        if (pickaxe==this) {
            return Block.ores.getBlocks().contains(block);
        }
        if (axe==this) {
            Tree tree = placer.getTree();
            if (tree == null) {
                return false;
            }
            return block instanceof BlockLog;
        }
        return false;
    }
    
}
