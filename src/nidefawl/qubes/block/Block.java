package nidefawl.qubes.block;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import nidefawl.qubes.entity.Player;
import nidefawl.qubes.meshing.ChunkRenderCache;
import nidefawl.qubes.meshing.SlicedBlockFaceInfo;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

public class Block {

    public static final int BLOCK_MASK = 0xFF;
    public static final int NUM_BLOCKS = 256;
    public static int HIGHEST_BLOCK_ID = 0;
    private static short[] registeredblocks;
    public static final Block[] block = new Block[NUM_BLOCKS];
    public final static String[] NO_TEXTURES = new String[0];
    public final static Block air = new BlockAir(0).setName("air");
    public final static Block stone = new Block(1).setName("stone");
    public final static Block grass = new BlockGrass(2).setName("grass").setTextures("grass_top", "grass_side", "grass_side_overlay");
    public final static Block dirt = new Block(3).setName("dirt");
    public final static Block water = new BlockWater(4).setName("water");
    public final static Block sand = new BlockSand(5).setName("sand");
    public final static Block glowstone = new BlockLit(6).setName("glowstone").setTextures("glowstone");
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
    public final static Block longgrass = new BlockLongGrass(17).setName("longgrass").setTextures("tallgrass");
    public final static Block slab = new BlockSlab(18, stone).setName("stoneslab").setTextureMode(BlockTextureMode.TOP_BOTTOM).setTextures("stone_slab_side", "stone_slab_top", "stone_slab_top");
    public final static Block stairs = new BlockStairs(19, stone).setName("stonestairs");

    public static void preInit() {
//        ArrayList<Block> bs = new ArrayList<>();
//        for (Block b : block) {
//            if (b != null) {
//                if (b.isFullBB() && b.getRenderType() == 0 && b.getRenderPass() == 0)
//                    bs.add(b);
//            }
//        }
//        int idx = HIGHEST_BLOCK_ID+1;
//        for (Block b : bs){
//            new BlockSlab(idx++, b).setName(b.getName()+"_slab").setTextures(new String[0]);
//        }
//        for (Block b : bs){
//            new BlockStairs(idx++, b).setName(b.getName()+"_stairs").setTextures(new String[0]);
//        }
    
    }
    public static void postInit() {
        for (int i = 0; i < Block.block.length; i++) {
            Block b = Block.block[i];
            if (b != null) {
                if (b.textures == null) {
                    b.textures = new String[] { "textures/blocks/"+b.name+".png" };
                }
                if (b.getLODPass() == WorldRenderer.PASS_LOD && b.getRenderType() == 0) {
                    throw new GameError("Block cannot be in LOD pass and be meshed (rendertype = 0)");
                }
            }
        }

        ArrayList<Short> list = Lists.newArrayList();
        for (int i = 0; i < block.length; i++) {
            if (i == 0 || block[i] != null) {
                list.add(Short.valueOf((short)i));
            }
        }
        short[] data = new short[list.size()];
        for (int i = 0; i < data.length; i++) {
            data[i] = list.get(i);
        }
        registeredblocks = data;
    }

    public final int id;
    private String name;
    private final boolean transparent;
    protected String[] textures;
    final AABBFloat blockBounds = new AABBFloat(0, 0, 0, 1, 1, 1);
    private BlockTextureMode textureMode = BlockTextureMode.DEFAULT;

    public Block(int id, boolean transparent) {
        if (id < 0) {
            id = HIGHEST_BLOCK_ID+1;
        }
        this.id = id;
        if (this.id > HIGHEST_BLOCK_ID)
            HIGHEST_BLOCK_ID = this.id;
        block[id] = this;
        this.transparent = transparent;
    }
    public Block(int id) {
        this(id, false);
    }
    
    public Block setName(String name) {
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

    public Block setAbsTextures(String...list) {
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
    /**
     * @param textureMode the textureMode to set
     * @return 
     */
    public Block setTextureMode(BlockTextureMode textureMode) {
        this.textureMode = textureMode;
        return this;
    }
    public int getTexture(int faceDir, int dataVal) {
        switch (this.textureMode) {
            case TOP:
                return BlockTextureArray.getInstance().getTextureIdx(this.id, faceDir == Dir.DIR_POS_Y ? 1 : 0);
            case TOP_BOTTOM:
                return BlockTextureArray.getInstance().getTextureIdx(this.id, faceDir == Dir.DIR_POS_Y ? 1 : faceDir == Dir.DIR_NEG_Y ? 2 : 0);
            case DEFAULT:
                break;
        }
        return BlockTextureArray.getInstance().getTextureIdx(this.id, 0);
    }
    public int getLODPass() {
        return WorldRenderer.PASS_SOLID;
    }
    public int getRenderPass() {
        return 0;
    }
    public int getRenderType() {
        return 0;
    }
    public final static boolean isValid(int i) {
        return i > 0 && i < block.length && block[i] != null;
    }
    public final static Block get(int i) {
        return block[i];
    }
    public boolean applyAO() {
        return true;
    }
    public boolean isOccluding() {
        return true;
    }
    
    public int getBBs(World world, int x, int y, int z, AABBFloat[] tmp) {
        AABBFloat bb = tmp[0];
        bb.set(this.blockBounds);
        bb.offset(x, y, z);
        return 1;
    }
    public float getAlpha() {
        return 1;
    }
    public int getLightValue() {
        return 0;
    }
    public final static boolean isOpaque(int typeId) {
        return !block[typeId].isTransparent();
    }
    
    public AABBFloat getRenderBlockBounds(IBlockWorld w, int ix, int iy, int iz, AABBFloat bb) {
        bb.set(0, 0, 0, 1, 1, 1);
        return bb;
    }
    public boolean isVisibleBounds(IBlockWorld w, int axis, int side, AABBFloat bb) {
        if (axis == 0) {
            if (side == 1 && bb.minX>0) {
                return true;
            }
            if (side == 0 && bb.maxX<1) {
                return true;
            }
        }
        if (axis == 1) {
            if (side == 1 && bb.minY>0) {
                return true;
            }
            if (side == 0 && bb.maxY<1) {
                return true;
            }
        }
        if (axis == 2) {
            if (side == 1 && bb.minZ>0) {
                return true;
            }
            if (side == 0 && bb.maxZ<1) {
                return true;
            }
        }
        return false;
        
    }
    public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
        if (isVisibleBounds(w, axis, side, bb)) {
            return true;
        }
        return !w.isNormalBlock(ix, iy, iz, -1);
    }
    /**
     * @param chunkRenderCache
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    public boolean isNormalBlock(IBlockWorld chunkRenderCache, int ix, int iy, int iz) {
        return !isTransparent();
    }
    /**
     * @return
     */
    public boolean isSlab() {
        return false;
    }
    /**
     * @return
     */
    public boolean isStairs() {
        return false;
    }
    
    /**
     * @param blockPlacer
     * @param x
     * @param y
     * @param z
     * @param type 
     * @param data 
     * @return
     */
    public boolean canPlaceAt(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        return blockPlacer.canPlaceDefault(pos, offset, type, data);
    }
    /**
     * @param blockPlacer
     * @param x
     * @param y
     * @param z
     * @param type 
     * @param data 
     * @return
     */
    public int prePlace(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        return data;
    }
    /**
     * @param blockPlacer
     * @param x
     * @param y
     * @param z
     * @param type 
     * @param data 
     */
    public void postPlace(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        return;
    }
    /**
     * @param blockPlacer
     * @param x
     * @param y
     * @param z
     * @param type 
     * @param data 
     */
    public void place(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        blockPlacer.placeDefault(pos, offset, type, data);
    }
    /**
     * @return
     */
    public boolean isReplaceable() {
        return false;
    }
    public int placeOffset(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        if (isReplaceable()) {
            return -1;
        }
        return offset;
    }
    /**
     * @param rayTrace
     * @param world
     * @param x
     * @param y
     * @param z
     * @param origin
     * @param direction 
     * @param dirFrac (1/direction)
     * @return 
     */
    public boolean raytrace(RayTrace rayTrace, World world, int x, int y, int z, Vector3f origin, Vector3f direction, Vector3f dirFrac) {
        AABBFloat bb =this.getRenderBlockBounds(world, x, y, z, rayTrace.getTempBB());
        if (bb != null) {
            bb.offset(x, y, z);
            return bb.raytrace(rayTrace, origin, direction, dirFrac);
        }
        return false;
    }
    public boolean isFullBB() {
        return true;
    }
    public boolean isOccludingBlock(IBlockWorld w, int x, int y, int z) {
        return isOccluding();
    }
    /**
     * @return
     */
    public static short[] getAllRegistered() {
        return registeredblocks;
    }
    
    public void getQuarters(IBlockWorld w, int x, int y, int z, int[] quarters) {
        for (int i = 0; i < quarters.length; i++) {
            quarters[i] = this.id;
        }
    }
    
    public int getRenderShadow() {
        return 1;
    }
}
