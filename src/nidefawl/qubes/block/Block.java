package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.world.World;

public class Block {

    public static final int BLOCK_MASK = 0xFF;
    public static final int NUM_BLOCKS = 256;
    public static final Block[] block = new Block[NUM_BLOCKS];
    public final static Block stone = new Block(1).setName("stone");
    public final static Block grass = new BlockGrass(2).setName("grass").setTextures("grass_top", "grass_side", "grass_side_overlay");
    public final static Block dirt = new Block(3).setName("dirt");
    public final static Block water = new BlockWater(4).setName("water");
    public final static Block sand = new BlockSand(5).setName("sand");
    public final static Block glowstone = new BlockGlowStone(6).setName("solid").setTextures("glowstone");
    public final static Block log_acacia = new BlockLog(7).setName("log_acacia").setTextures("log_acacia", "log_acacia_top");
    public final static Block log_birch = new BlockLog(8).setName("log_birch").setTextures("log_birch", "log_birch_top");
    public final static Block log_jungle = new BlockLog(9).setName("log_jungle").setTextures("log_jungle", "log_jungle_top");
    public final static Block log_spruce = new BlockLog(10).setName("log_spruce").setTextures("log_spruce", "log_spruce_top");
    public final static Block log_oak = new BlockLog(11).setName("log_oak").setTextures("log_oak", "log_oak_top");
    public final static Block leaves_acacia = new BlockLeaves(12).setName("leaves_acacia");
    public final static Block leaves_birch = new BlockLeaves(13).setName("leaves_birch");
    public final static Block leaves_jungle = new BlockLeaves(14).setName("leaves_jungle");
    public final static Block leaves_spruce = new BlockLeaves(15).setName("leaves_spruce");
    public final static Block leaves_oak = new BlockLeaves(16).setName("leaves_oak");
//    public final static Block ligth = new BlockSolidColor(6).setName("solid").setTextures("blank");
    public final int id;
    private String name;
    private final boolean transparent;
    String[] textures;
    final AABB blockBounds = new AABB(0, 0, 0, 1, 1, 1);

    Block(int id, boolean transparent) {
        this.id = id;
        block[id] = this;
        this.transparent = transparent;
    }
    Block(int id) {
        this(id, false);
    }
    
    Block setName(String name) {
        this.name = name;
        return this;
    }
    public String[] getTextures() {
        return this.textures;
    }

    public Block setTextures(String...list) {
        for (int a = 0; a < list.length; a++) {
            list[a] = "textures/blocks/" + list[a] + ".png";
        }
        this.textures = list;
        return this;
    }
    
    public String getName() {
        return name;
    }

    public boolean isTransparent() {
        return transparent;
    }
    public int getColorFromSide(int side) {
        return 0xFFFFFF;
    }
    public int getTextureFromSide(int faceDir) {
        return BlockTextureArray.getInstance().getTextureIdx(this.id, 0);
    }
    static {
        for (int i = 0; i < Block.block.length; i++) {
            Block b = Block.block[i];
            if (b != null) {
                if (b.textures == null) {
                    b.textures = new String[] { "textures/blocks/"+b.name+".png" };
                }
            }
        }
    }
    public int getRenderPass() {
        return 0;
    }
    public static boolean isValid(int i) {
        return i > 0 && i < block.length && block[i] != null;
    }
    public static Block get(int i) {
        return isValid(i) ? block[i] : null;
    }
    public boolean applyAO() {
        return true;
    }
    public boolean isOccluding() {
        return true;
    }
    
    public AABB getCollisionBB(World world, int x, int y, int z, AABB aabb) {
        aabb.set(this.blockBounds);
        aabb.offset(x, y, z);
        return aabb;
    }
    public float getAlpha() {
        return 1;
    }
    public int getLightValue() {
        return 0;
    }
    public final static boolean isOpaque(int typeId) {
        return typeId != 0 && !block[typeId].isTransparent();
    }
}
