package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.*;
import nidefawl.qubes.blocklight.LightChunkCache;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.item.Stack;
import nidefawl.qubes.meshing.BlockSurface;
import nidefawl.qubes.meshing.ChunkRenderCache;
import nidefawl.qubes.meshing.SlicedBlockFaceInfo;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.texture.BlockTextureArray;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

public class Block {

    public static final int BLOCK_MASK = 0x1FF;
    public static final int NUM_BLOCKS = 512;
    public static int HIGHEST_BLOCK_ID = 0;
    private static Block[] registeredblocks;
    private static short[] registeredblockIds;
    public static final Block[] block = new Block[NUM_BLOCKS];
    public final static String[] NO_TEXTURES = new String[0];
    public final static Block air = new BlockAir(0).setName("air");
    public final static Block granite = new Block(-1).setName("granite").setTextures("rocks/granite");//.setTextureMode(BlockTextureMode.ALTERNATING);
    public final static Block basalt = new Block(-1).setName("basalt").setTextures("rocks/basalt");//.setTextureMode(BlockTextureMode.ALTERNATING);
    public final static Block diorite = new Block(-1).setName("diorite").setTextures("rocks/diorite");//.setTextureMode(BlockTextureMode.ALTERNATING);
    public final static Block marble = new Block(-1).setName("marble").setTextures("rocks/marble");//.setTextureMode(BlockTextureMode.ALTERNATING);
    public final static Block sandstone = new Block(-1).setName("sandstone").setTextures("rocks/sandstone", "rocks/sandstone_red", "rocks/sandstone_darkred").setTextureMode(BlockTextureMode.SUBTYPED_TEX_PER_TYPE);
    public final static Block grass = new BlockGrass(-1).setName("grass").setTextures("ground/grass", "ground/grass_side", "ground/grass_side_overlay");
    public final static Block dirt = new Block(-1).setName("dirt").setTextures("ground/dirt");
    public final static Block water = new BlockWater(-1).setName("water/water_still");
    public final static Block sand = new BlockSand(-1).setName("sand/sand");
//    public final static Block glowstone = new BlockLit(-1).setName("glowstone").setTextures("glowstone");
    public final static Block log = new BlockLog(-1).setName("log").setTextures("logs/log", "logs/log_top");
    public final static Block leaves = new BlockLeaves(-1).setName("leaves/leaves");
    public final static Block grassbush = new BlockLongGrass(-1).setName("grassbush").setTextures("plants/grassbush");
    public final static Block slab_granite = new BlockSlab(-1, granite).setName("slab_granite");
    public final static Block stairs_granite = new BlockStairs(-1, granite).setName("stairs_granite");
    public final static Block vines = new BlockVine(-1).setName("vine").setTextures("leaves/leaves");
    public final static Block fence_log = new BlockFence(-1, log).setName("fence_log").setAbsTextures(NO_TEXTURES);
    public final static Block wall_log = new BlockWall(-1, log).setName("wall_log").setAbsTextures(NO_TEXTURES);
    public final static Block quarter = new BlockQuarterBlock(-1).setName("quarter");
    public final static Block cobble = new Block(-1).setName("cobble").setTextures("stones/cobblestone");
    public final static Block obsidian = new Block(-1).setName("obsidian").setTextures("rocks/obsidian");
    public final static Block bedrock = new Block(-1).setName("bedrock").setTextures("rocks/bedrock");
    public final static Block ore_diamond = new Block(-1).setName("ore_diamond").setTextures("rocks/ore_diamond");
    public final static Block ore_gold = new Block(-1).setName("ore_gold").setTextures("rocks/ore_gold");
    public final static Block ore_silver = new Block(-1).setName("ore_silver").setTextures("rocks/ore_silver");
    public final static Block brick = new Block(-1).setName("brick").setTextures("stones/brick_clay", "stones/brick_granite", "stones/brick_obsidian").setTextureMode(BlockTextureMode.SUBTYPED_TEX_PER_TYPE);
    public final static Block smoothstone_granite = new Block(-1).setName("smoothstone").setTextures("stones/stone_granite_border","stones/stone_granite_smooth_border").setTextureMode(BlockTextureMode.SUBTYPED_TEX_PER_TYPE);
    public final static Block stonebrick_granite = new Block(-1).setName("stonebrick_granite").setTextures("stones/stonebrick_granite_smooth","stones/stonebrick_granite_rough","stones/stonebrick_granite_rough_cracked","stones/stonebrick_granite_rough_cracked_mossy").setTextureMode(BlockTextureMode.SUBTYPED_TEX_PER_TYPE);
    public final static Block stonepath_granite = new Block(-1).setName("stonepath_granite").setTextures("stones/stonepath_granite");
    public final static Block slab_brick_clay = new BlockSlab(-1, brick, 0).setName("slab_brick_clay");
    public final static Block slab_brick_granite = new BlockSlab(-1, brick, 1).setName("slab_brick_granite");
    public final static Block slab_brick_obsidian = new BlockSlab(-1, brick, 2).setName("slab_brick_obsidian");
    public final static Block slab_smoothstone_granite = new BlockSlab(-1, smoothstone_granite, 0).setName("slab_smoothstone_granite");
    public final static Block slab_smoothstone_granite_border = new BlockSlab(-1, smoothstone_granite, 1).setName("slab_smoothstone_granite_border");
    public final static Block slab_stonebrick_granite_smooth = new BlockSlab(-1, stonebrick_granite, 0).setName("slab_stonebrick_granite_smooth");
    public final static Block slab_stonebrick_granite_rough = new BlockSlab(-1, stonebrick_granite, 1).setName("slab_stonebrick_granite_rough");
    public final static Block slab_stonebrick_granite_rough_cracked = new BlockSlab(-1, stonebrick_granite, 2).setName("slab_stonebrick_granite_rough_cracked");
    public final static Block slab_stonebrick_granite_rough_cracked_mossy = new BlockSlab(-1, stonebrick_granite, 3).setName("slab_stonebrick_granite_rough_cracked_mossy");

    public final static Block stairs_brick_clay = new BlockStairs(-1, brick, 0).setName("stairs_brick_clay");
    public final static Block stairs_brick_granite = new BlockStairs(-1, brick, 1).setName("stairs_brick_granite");
    public final static Block stairs_brick_obsidian = new BlockStairs(-1, brick, 2).setName("stairs_brick_obsidian");
    
    public final static Block stairs_smoothstone_granite = new BlockStairs(-1, smoothstone_granite, 0).setName("stairs_smoothstone_granite");
    public final static Block stairs_smoothstone_granite_border = new BlockStairs(-1, smoothstone_granite, 1).setName("stairs_smoothstone_granite_border");
    public final static Block stairs_stonebrick_granite_smooth = new BlockStairs(-1, stonebrick_granite, 0).setName("stairs_stonebrick_granite_smooth");
    public final static Block stairs_stonebrick_granite_rough = new BlockStairs(-1, stonebrick_granite, 1).setName("stairs_stonebrick_granite_rough");
    public final static Block stairs_stonebrick_granite_rough_cracked = new BlockStairs(-1, stonebrick_granite, 2).setName("stairs_stonebrick_granite_rough_cracked");
    public final static Block stairs_stonebrick_granite_rough_cracked_mossy = new BlockStairs(-1, stonebrick_granite, 3).setName("stairs_stonebrick_granite_rough_cracked_mossy");
    public final static Block wall_brick = new BlockWall(-1, brick).setName("wall_brick").setAbsTextures(NO_TEXTURES);
    public final static Block wall_smoothstone_granite = new BlockWall(-1, smoothstone_granite).setName("wall_smoothstone_granite").setAbsTextures(NO_TEXTURES);
    public final static Block wall_stonebrick_granite = new BlockWall(-1, stonebrick_granite).setName("wall_stonebrick_granite").setAbsTextures(NO_TEXTURES);
    public final static Block flower_fmn_black = new BlockFlowerFMN(-1).setName("forget_me_not_black").setTextures("flowers/bush_forget_me_not_black.layer0","flowers/bush_forget_me_not_black.layer1","flowers/bush_forget_me_not_black.layer2");
    public final static Block flower_fmn_blue = new BlockFlowerFMN(-1).setName("forget_me_not_blue").setTextures("flowers/bush_forget_me_not_blue.layer0","flowers/bush_forget_me_not_blue.layer1","flowers/bush_forget_me_not_blue.layer2");
    public final static Block flower_compositae_camille = new BlockPlantCrossedSquares(-1, true).setName("compositae_camille").setTextures("flowers/compositae_stem", "flowers/compositae_camille");
    public final static Block flower_compositae_milkspice = new BlockPlantCrossedSquares(-1, true).setName("compositae_milkspice").setTextures("flowers/compositae_stem", "flowers/compositae_milkspice");
    public final static Block flower_compositae_pinkpanther = new BlockPlantCrossedSquares(-1, true).setName("compositae_pinkpanther").setTextures("flowers/compositae_stem", "flowers/compositae_pinkpanther");
    public final static Block flower_compositae_tigerteeth = new BlockPlantCrossedSquares(-1, true).setName("compositae_tigerteeth").setTextures("flowers/compositae_stem", "flowers/compositae_tigerteeth");
    public final static Block flower_violet = new BlockPlantCrossedSquares(-1, true).setName("violet").setTextures("flowers/violet.layer0", "flowers/violet.layer1");

    
    public static void preInit() {
        
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
                    throw new GameError("Block cannot be in LOD pass and be meshed (rendertype = 0): "+b.toString());
                }
            }
        }

        ArrayList<Block> list = Lists.newArrayList();
        for (int i = 0; i < block.length; i++) {
            if (block[i] != null) {
                list.add(block[i]);
            }
        }
        registeredblocks = list.toArray(new Block[list.size()]);
        registeredblockIds = new short[registeredblocks.length];
        for (int i = 0; i < registeredblockIds.length; i++) {
            registeredblockIds[i] = (short) registeredblocks[i].id;
        }
    }

    public final int id;
    private String name;
    private final boolean transparent;
    protected String[] textures;
    protected final AABBFloat blockBounds = new AABBFloat(0, 0, 0, 1, 1, 1);
    protected BlockTextureMode textureMode = BlockTextureMode.DEFAULT;

    public Block(int id, boolean transparent) {
        if (id < 0) {
            id = HIGHEST_BLOCK_ID+1;
        }
        this.id = id;
        if (this.id > HIGHEST_BLOCK_ID)
            HIGHEST_BLOCK_ID = this.id;
        block[id] = this;
        this.transparent = transparent;
        init();
    }
    
    public void init() {
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
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int multiTexturePass) {
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
    public int getTexturePasses() {
        return 1;
    }
//    public int getFaceTexture(IBlockWorld w, int x, int y, int z, int dataVal, int faceDir, int multiTexturePass) {
//        if (this.textureMode == BlockTextureMode.ALTERNATING) {
//            long l = x;
//            l<<=27;
//            l|=z;
//            l<<=10;
//            l|=y;
//            long a = GameMath.randomI(l);
//            int d = GameMath.randomI(a)&(Integer.MAX_VALUE);
////            int l = GameMath.randomI(x<<(37)|x<<10|y)&(Integer.MAX_VALUE);
//            int idx = ((int)d)%BlockTextureArray.getInstance().getNumTex(this.id);
//            return BlockTextureArray.getInstance().getTextureIdx(this.id, idx);
//        }
//        return getTexture(faceDir, dataVal, multiTexturePass);
//    }
    public int getTexture(int faceDir, int dataVal, int pass) {
        switch (this.textureMode) {
            case SUBTYPED_TEX_PER_TYPE:
                return BlockTextureArray.getInstance().getTextureIdx(this.id, dataVal % this.textures.length);
            case TOP:
                return BlockTextureArray.getInstance().getTextureIdx(this.id, faceDir == Dir.DIR_POS_Y ? 1 : 0);
            case TOP_BOTTOM:
                return BlockTextureArray.getInstance().getTextureIdx(this.id, faceDir == Dir.DIR_POS_Y ? 1 : faceDir == Dir.DIR_NEG_Y ? 2 : 0);
            case DEFAULT:
            default:
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
        bb.set(this.blockBounds);
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
     * @param against
     * @param type 
     * @param data 
     * @param x
     * @param y
     * @param z
     * @return
     */
    public boolean canPlaceAt(BlockPlacer blockPlacer, BlockPos against, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        return blockPlacer.canPlaceDefault(this, against, pos, offset, type, data);
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
    public static short[] getRegisteredIDs() {
        return registeredblockIds;
    }

    /**
     * @return
     */
    public static Block[] getRegisteredBlocks() {
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
    
    @Override
    public String toString() {
        String blockInfo = this.getClass().getName()+"[ID "+this.id;
        blockInfo += ", "+this.getName()+"]";
        return blockInfo;
    }

    public int setSelectionBB(World world, RayTraceIntersection r, BlockPos hitPos, AABBFloat selBB) {
        if (isFullBB()) {
            return 0;
        }
        getRenderBlockBounds(world, hitPos.x, hitPos.y, hitPos.z, selBB);
        return 1;
    }

    public boolean canBlockConnect(IBlockWorld w, int ix, int iy, int iz, BlockConnect connect, int axis, int i) {
        return isFullBB() && !isReplaceable() && !isTransparent();
    }
    
    public int getItems(List<Stack> l) {
        if (this.textureMode == BlockTextureMode.SUBTYPED_TEX_PER_TYPE) {
            for (int i = 0; i < this.textures.length; i++) {
                l.add(new Stack(this.id, i));
            }
            return this.textures.length;
        } else {
            l.add(new Stack(this.id));
        }
        return 1;
    }
    
    public int getMeshedColor(BlockSurface bs) {
        return bs.faceColor;
    }
    
    public int getTextureByIdx(int idx) {
        return BlockTextureArray.getInstance().getTextureIdx(this.id, idx);
    }
    
    public int getLightLoss(LightChunkCache c, int i, int j, int k, int type) {
        return 1;
    }
    /**
     * @param mgr 
     * @return
     */
    public ArrayList<AssetTexture> resolveTextures(AssetManager mgr) {
        ArrayList<AssetTexture> textures = new ArrayList<>();
//        if (this.textureMode == BlockTextureMode.ALTERNATING) {
//            for (int i = 0; i < 16; i++) {
//                String texPath = String.format("textures/blocks/%s_var%d.png", this.textures[0], i);
//                AssetTexture tex = mgr.loadPNGAsset(texPath, i!=0);
//                if (tex == null) {
//                    break;
//                }
//                textures.add(tex);
//            }
//            if (textures.size() == 1) {
//                throw new GameError("BlockTextureMode.ALTERNATING requires more than 1 texture!");
//            }
//            return textures;
//        }
        for (String s : this.textures) {
            AssetTexture tex = mgr.loadPNGAsset(s, false);
            textures.add(tex);
        }
        return textures;
    }
}
