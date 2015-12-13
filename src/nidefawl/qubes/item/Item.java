/**
 * 
 */
package nidefawl.qubes.item;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.vec.AABBFloat;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Item {
    public static final int ITEM_MASK = 0x1FF;
    public static final int NUM_ITEMS = 512;
    public static int HIGHEST_ITEM_ID = 0;
    private static Item[] registereditems;
    private static short[] registereditemIds;
    public static final Item[] item = new Item[NUM_ITEMS];
    public static final Item pickaxe = new Item(1).setName("pickaxe").setTextures("tools/pick");
    /**
     * @return
     */
    public static Item[] getRegisteredIDs() {
        return registereditems;
    }
    public Item(int id) {
        this(id, false);
    }

    public final int id;
    private String name;
    private final boolean transparent;
    protected String[] textures;
    protected final AABBFloat blockBounds = new AABBFloat(0, 0, 0, 1, 1, 1);

    public Item(int id, boolean transparent) {
        if (id < 0) {
            id = HIGHEST_ITEM_ID+1;
        }
        this.id = id;
        if (this.id > HIGHEST_ITEM_ID)
            HIGHEST_ITEM_ID = this.id;
        item[id] = this;
        this.transparent = transparent;
        init();
    }
    
    public void init() {
    }

    
    public Item setName(String name) {
        this.name = name;
        return this;
    }
    public String[] getTextures() {
        return this.textures;
    }

    public Item setTextures(String... list) {
        for (int a = 0; a < list.length; a++) {
            list[a] = "textures/items/" + list[a] + ".png";
        }
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
        
        for (int i = 0; i < Item.item.length; i++) {
            Item b = Item.item[i];
            if (b != null) {
                if (b.textures == null) {
                    b.textures = new String[] { "textures/blocks/"+b.name+".png" };
                }
            }
        }
        
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
    
}
