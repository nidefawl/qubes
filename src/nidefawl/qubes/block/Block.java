package nidefawl.qubes.block;

import nidefawl.qubes.texture.BlockTextureArray;

public class Block {

    public static final int BLOCK_MASK = 0xFF;
    public static final int NUM_BLOCKS = 256;
    public static final Block[] block = new Block[NUM_BLOCKS];
    public final static Block stone = new Block(1).setName("stone");
    public final static Block grass = new BlockGrass(2).setName("grass");
    public final static Block dirt = new Block(3).setName("dirt");
    public final static Block water = new Block(4).setName("water").setBlocksLight(false);
    public final static Block sand = new Block(5).setName("sand");
    public final int id;
    private String name;
    private boolean blocksLight;
    String[] textures;
    
    Block(int id) {
        this.id = id;
        block[id] = this;
        this.blocksLight = true;
    }
    
    Block setName(String name) {
        this.name = name;
        return this;
    }
    public String[] getTextures() {
        return this.textures;
    }
    Block setBlocksLight(boolean b) {
        this.blocksLight = b;
        return this;
    }
    public String getName() {
        return name;
    }

    public static boolean blocksLight(int a) {
        return block[a] == null || block[a].blocksLight;
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
                    b.textures = new String[] { "/textures/blocks/"+b.name+".png" };
                }
            }
        }
    }
}
