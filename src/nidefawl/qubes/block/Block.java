package nidefawl.qubes.block;

public class Block {

    public static final short BLOCK_MASK = 0xFF;
    public static final short NUM_BLOCKS = 256;
    public static final Block[] block = new Block[NUM_BLOCKS];
    public final static Block stone = new Block(1).setName("stone");
    public final static Block grass = new Block(2).setName("grass");
    public final static Block dirt = new Block(3).setName("dirt");
    public final static Block water = new Block(4).setName("water").setBlocksLight(false);
    public final static Block sand = new Block(5).setName("sand");
    public final int id;
    private String name;
    private boolean blocksLight;
    
    Block(int id) {
        this.id = id;
        block[id] = this;
        this.blocksLight = true;
    }
    Block setName(String name) {
        this.name = name;
        return this;
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

}
