package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.world.World;

public class Block {

    public static final int BLOCK_MASK = 0xFF;
    public static final int NUM_BLOCKS = 256;
    public static final Block[] block = new Block[NUM_BLOCKS];
    public final static Block stone = new Block(1).setName("stone");
    public final static Block grass = new BlockGrass(2).setName("grass");
    public final static Block dirt = new Block(3).setName("dirt");
    public final static Block water = new BlockWater(4).setName("water");
    public final static Block sand = new Block(5).setName("sand");
    public final static Block ligth = new BlockLight(6).setName("light").setTextures("blank");
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
    public int getColor() {
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
    
    public AABB getCollisionBB(World world, int x, int y, int z, AABB aabb) {
        aabb.set(this.blockBounds);
        aabb.offset(x, y, z);
        return aabb;
    }
    public float getAlpha() {
        return 1;
    }
}
