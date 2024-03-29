package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.blocklight.LightChunkCache;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.meshing.BlockSurface;
import nidefawl.qubes.models.qmodel.ModelBlock;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

public class Block {

    public static final int BLOCK_BITS = 10;
    public static final int NUM_BLOCKS = 1<<BLOCK_BITS;
    public static final int BLOCK_MASK = NUM_BLOCKS-1;
    private static Block[] registeredblocks;
    private static short[] registeredblockIds;
    public static final Block[] block = new Block[NUM_BLOCKS];
    public final static String[] NO_TEXTURES = new String[0];
    public final static Block air = new BlockAir("air");
    public final static Block grass = new BlockGrass("grass").setTextures("ground/grass", "ground/grass_side", "ground/grass_side_overlay").setNormalMaps("ground/grass_side_normalmap");
    public final static Block dirt = new Block("dirt").setTextures("ground/dirt").setNormalMaps("ground/dirt_normalmap").setCategory(BlockCategory.GROUND);
    public final static BlockGroupStones stones = new BlockGroupStones();
    public final static Block water = new BlockWater("water/water_still");
    public final static Block sand = new BlockSand("sand").setTextures("ground/sand");
    public final static Block sand_red = new BlockSand("red_sand").setTextures("ground/sand_red");
    public final static Block snow = new Block("snow").setTextures("ground/snow").setCategory(BlockCategory.GROUND);
    public final static Block ice = new BlockIce("ice").setTextures("ground/ice").setCategory(BlockCategory.GROUND);
    public final static Block gravel = new BlockGravel("gravel");
//    public final static Block glass = new BlockGlass("glass").setTextures("glass/glass_0", "glass/glass_1").setTextureMode(BlockTextureMode.SUBTYPED_TEX_PER_TYPE);


    public final static BlockGroupLogs logs = new BlockGroupLogs();
    public final static BlockGroup wood = new BlockGroupWood();
    public final static BlockGroupLeaves leaves = new BlockGroupLeaves();

    public final static Block grassbush = new BlockGrassBush("grassbush").setTextures("plants/grassbush");
    public final static Block heath = new BlockGrassBush("heath").setTextures("plants/heath");
    public final static Block aloe_vera = new BlockGrassBush("aloe_vera").setTextures("plants/aloe_vera");
    public final static Block nasturtium = new BlockGrassBush("nasturtium").setTextures("plants/nasturtium");
    public final static Block thingrass = new BlockGrassBush("thingrass").setTextures("plants/thingrass");
    public final static Block vines = new BlockVine("vine").setTextures("plants/vines");
    public final static Block treemoss = new BlockVine("treemoss").setTextures("plants/treemoss");
    public final static Block quarter = new BlockQuarterBlock("quarter");
    
    public final static BlockGroupOres ores = new BlockGroupOres(stones);
    public final static BlockGroup bricks = new BlockGroupBricks(stones);
    public final static BlockGroup stonebricks = new BlockGroupStoneBricks(stones);
    public final static BlockGroup smoothstones = new BlockGroupSmoothStones(stones);
    public final static BlockGroup stonepath = new BlockGroupStonePath(stones);
    public final static BlockGroup cobblestones = new BlockGroupCobbleStones(stones);
    public final static BlockGroup slabs = new BlockGroupSlabs(stones, stonepath, cobblestones, smoothstones, stonebricks, bricks, logs, wood);
    public final static BlockGroup stairs = new BlockGroupStairs(stones, stonepath, cobblestones, smoothstones, stonebricks, bricks, logs, wood);
    public final static BlockGroup walls = new BlockGroupWalls(stones, stonepath, cobblestones, smoothstones, stonebricks, bricks, logs, wood);
    public final static BlockGroup fences = new BlockGroupFences(stones, stonepath, cobblestones, smoothstones, stonebricks, bricks, logs, wood);

    public final static BlockGroup parquets = new BlockGroupParquets();
    public final static Block flower_fmn_black = new BlockFlowerFMN("forget_me_not_black").setTextures("flowers/bush_forget_me_not_black.layer0","flowers/bush_forget_me_not_black.layer1","flowers/bush_forget_me_not_black.layer2");
    public final static Block flower_fmn_blue = new BlockFlowerFMN("forget_me_not_blue").setTextures("flowers/bush_forget_me_not_blue.layer0","flowers/bush_forget_me_not_blue.layer1","flowers/bush_forget_me_not_blue.layer2");
    public final static Block flower_compositae_camille = new BlockPlantCrossedSquares("compositae_camille", true).setTextures("flowers/compositae_stem", "flowers/compositae_camille");
    public final static Block flower_compositae_milkspice = new BlockPlantCrossedSquares("compositae_milkspice", true).setTextures("flowers/compositae_stem", "flowers/compositae_milkspice");
    public final static Block flower_compositae_pinkpanther = new BlockPlantCrossedSquares("compositae_pinkpanther", true).setTextures("flowers/compositae_stem", "flowers/compositae_pinkpanther");
    public final static Block flower_compositae_tigerteeth = new BlockPlantCrossedSquares("compositae_tigerteeth", true).setTextures("flowers/compositae_stem", "flowers/compositae_tigerteeth");
    public final static Block flower_violet = new BlockPlantCrossedSquares("violet", true).setTextures("flowers/violet.layer0", "flowers/violet.layer1");
    public final static Block flower_rose = new BlockPlantCrossedSquares("rose", true).setTextures("flowers/rose_stem", "flowers/rose_red");
    public final static Block flower_poppy1 = new BlockPlantCrossedSquares("poppy_blood", true).setTextures("flowers/poppy_stem", "flowers/poppy_blood");
    public final static Block flower_poppy2 = new BlockPlantCrossedSquares("poppy_grey", true).setTextures("flowers/poppy_stem", "flowers/poppy_grey");
    public final static Block flower_poppy3 = new BlockPlantCrossedSquares("poppy_blue", true).setTextures("flowers/poppy_stem", "flowers/poppy_blue");
    public final static Block flower_oxmorina_blue = new BlockPlantCrossedSquares("oxmorina", true).setTextures("flowers/oxmorina_stem", "flowers/oxmorina_blue");
    public final static Block flower_cup_0 = new BlockPlantCrossedSquares("cup_black", true).setTextures("flowers/cup_stem", "flowers/cup_black");
    public final static Block flower_cup_1 = new BlockPlantCrossedSquares("cup_blue", true).setTextures("flowers/cup_stem", "flowers/cup_blue");
    public final static Block flower_cup_2 = new BlockPlantCrossedSquares("cup_cyan", true).setTextures("flowers/cup_stem", "flowers/cup_cyan");
    public final static Block flower_cup_3 = new BlockPlantCrossedSquares("cup_black", true).setTextures("flowers/cup_stem", "flowers/cup_black");
    public final static Block flower_cup_4 = new BlockPlantCrossedSquares("cup_dark_yellow", true).setTextures("flowers/cup_stem", "flowers/cup_dark_yellow");
    public final static Block flower_cup_5 = new BlockPlantCrossedSquares("cup_orange", true).setTextures("flowers/cup_stem", "flowers/cup_orange");
    public final static Block flower_cup_6 = new BlockPlantCrossedSquares("cup_pink", true).setTextures("flowers/cup_stem", "flowers/cup_pink");
    public final static Block flower_cup_7 = new BlockPlantCrossedSquares("cup_red", true).setTextures("flowers/cup_stem", "flowers/cup_red");
    public final static Block flower_star_frost = new BlockPlantCrossedSquares("star_frost", true).setTextures("flowers/star_stem", "flowers/star_frost");
    public final static Block flower_star_sundown = new BlockPlantCrossedSquares("star_sundown", true).setTextures("flowers/star_stem", "flowers/star_sundown");
    public final static Block flower_dandelion = new BlockPlantCrossedSquares("dandelion", true).setTextures("flowers/dandelion_stem", "flowers/dandelion");
    public final static Block flower_lotus = new BlockPlantCrossedSquares("lotus", true).setTextures("flowers/lotus_narcotic_stem", "flowers/lotus_narcotic");
    public final static Block flower_lavender = new BlockPlantCrossedSquares("lavender", true).setTextures("flowers/lavender_stem", "flowers/lavender");
    public final static Block flower_tulip1 = new BlockPlantCrossedSquares("tulip_red", true).setTextures("flowers/tulip_stem", "flowers/tulip_red");
    public final static Block flower_tulip2 = new BlockPlantCrossedSquares("tulip_lilac", true).setTextures("flowers/tulip_stem", "flowers/tulip_lilac");
    public final static Block flower_tulip3 = new BlockPlantCrossedSquares("tulip_darkblue", true).setTextures("flowers/tulip_stem", "flowers/tulip_darkblue");
    public final static Block flower_tulip4 = new BlockPlantCrossedSquares("tulip_orange", true).setTextures("flowers/tulip_stem", "flowers/tulip_orange");
    public final static Block flower_tulip5 = new BlockPlantCrossedSquares("tulip_pink", true).setTextures("flowers/tulip_stem", "flowers/tulip_pink");
    public final static Block flower_tulip6 = new BlockPlantCrossedSquares("tulip_white", true).setTextures("flowers/tulip_stem", "flowers/tulip_white");
    public final static Block flower_nomades = new BlockPlantCrossedSquares("nomades", true).setTextures("flowers/nomades_stem", "flowers/nomades");
    public final static Block flower_sheeps_meal = new BlockPlantCrossedSquares("sheeps_meal", true).setTextures("flowers/sheeps_meal_stem", "flowers/sheeps_meal");
    public final static Block rhubarb = new BlockPlantCrossedSquares("rhubarb", true).setTextures("plants/rhubarb_leaves", "plants/rhubarb_stem");

    public final static Block fern1 = new BlockDoublePlant("fern1").setTextures("plants/double_plant_fern_1.lower", "plants/double_plant_fern_1.upper");
    public final static Block fern2 = new BlockDoublePlant("fern2").setTextures("plants/double_plant_fern_2.lower", "plants/double_plant_fern_2.upper");
    public final static Block fern3 = new BlockDoublePlant("fern3").setTextures("plants/double_plant_fern_3.lower", "plants/double_plant_fern_3.upper");
    public final static Block fern4 = new BlockDoublePlant("fern4").setTextures("plants/double_plant_fern_4.lower", "plants/double_plant_fern_4.upper");
    
    public final static Block double_heath = new BlockDoublePlant("double_heath").setTextures("plants/double_heath.lower", "plants/double_heath.upper");
    public final static Block tallgrass1 = new BlockDoublePlant("tallgrass1").setTextures("plants/double_grass_1.lower", "plants/double_grass_1.upper");
    public final static Block tallgrass2 = new BlockDoublePlant("tallgrass2").setTextures("plants/double_grass_2.lower", "plants/double_grass_2.upper");
    public final static Block cattail = new BlockDoubleCatTail("cattail").setTextures("plants/double_cattail.lower", "plants/double_cattail.upper", "plants/cattail_top");
    public final static Block waterlily = new BlockWaterLily("waterlily").setTextures("flowers/water_rose");

    public final static Block pad = new BlockPlantFlat("pad").setTextures("plants/pad");
    public final static BlockGroup modelled = new BlockGroupModelledStones(stones, stonepath, cobblestones, smoothstones, stonebricks, bricks);
    public final static BlockGroup modelled_hedge = new BlockGroupModelledHedge(leaves);
//    public final static Block torch = new BlockTorch("torch").setTextures("torch/torch_on");
//    public final static Block test = new Block("test").setTextures("ground/test").setCategory(BlockCategory.GROUND);

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
                    b.textures = new String[] { b.name };
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
    protected String[] normalMaps = NO_TEXTURES;
    protected final AABBFloat blockBounds = new AABBFloat(0, 0, 0, 1, 1, 1);
    protected BlockTextureMode textureMode = BlockTextureMode.DEFAULT;
    protected BlockCategory blockCategory = BlockCategory.UNASSIGNED;
    public ModelBlock[] loadedModels;
    private String[] models;
    private BlockGroup blockGroup;

    public Block(String name, boolean transparent) {
        if (name.contains(" ")) {
            throw new GameError("Names must not contain spaces");
        }
        this.id = IDMappingBlocks.get(name);
        this.name = name;
        block[id] = this;
        this.transparent = transparent;
        init();
    }
    public BlockGroup getBlockGroup() {
        return this.blockGroup;
    }

    protected Block setBlockGroup(BlockGroup group) {
        this.blockGroup = group;
        return this;
    }
    
    protected Block setCategory(BlockCategory blockCategory) {
        this.blockCategory = blockCategory;
        return this;
    }
    public BlockCategory getBlockCategory() {
        return this.blockCategory;
    }
    
    public void init() {
    }
    public Block(String id) {
        this(id, false);
    }
    
    public Block setName(String name) {
//        this.name = name;
        return this;
    }
    public String[] getTextures() {
        return this.textures;
    }
    public String[] getNormalMaps() {
        return this.normalMaps;
    }

    public Block setTextures(String...list) {
        this.textures = list;
        return this;
    }
    public Block setNormalMaps(String...list) {
        this.normalMaps = list;
        return this;
    }

    protected Block setModels(String... models) {
        this.models = models;
        return this;
    }
    public String[] getModels() {
        return this.models;
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
                return TextureArrays.blockTextureArray.getTextureIdx(this.id, dataVal % this.textures.length);
            case TOP:
                return TextureArrays.blockTextureArray.getTextureIdx(this.id, faceDir == Dir.DIR_POS_Y ? 1 : 0);
            case TOP_BOTTOM:
                return TextureArrays.blockTextureArray.getTextureIdx(this.id, faceDir == Dir.DIR_POS_Y ? 1 : faceDir == Dir.DIR_NEG_Y ? 2 : 0);
            case DEFAULT:
            default:
                break;
        }
        return TextureArrays.blockTextureArray.getTextureIdx(this.id, 0);
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

    public boolean canStayAt(IBlockWorld w, int x, int y, int z) {
        return true;
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
    
    public int getItems(List<BlockStack> l) {
        if (this.textureMode == BlockTextureMode.SUBTYPED_TEX_PER_TYPE) {
            for (int i = 0; i < this.textures.length; i++) {
                l.add(new BlockStack(this.id, i));
            }
            return this.textures.length;
        } else {
            l.add(new BlockStack(this.id));
        }
        return 1;
    }
    
    public int getMeshedColor(BlockSurface bs) {
        return bs.faceColor;
    }
    
    public int getTextureByIdx(int idx) {
        return TextureArrays.blockTextureArray.getTextureIdx(this.id, idx);
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
    
    public boolean skipTexturePassSide(IBlockWorld w, int x, int y, int z, int axis, int side, int texPass) {
        return false;
    }
    
    public boolean isWaving() {
        return false;
    }
    
    public int getInvRenderData(BlockStack stack) {
        return stack.data;
    }
    /**
     * @return
     */
    public float getInvRenderRotation() {
        return 0;
    }
    public boolean canMineWith(BlockPlacer placer, World w, BlockPos pos, PlayerServer player, ItemStack itemstack) {
        return itemstack.getItem().canMine(placer, this, w, pos, player, itemstack);
    }
    public void onBlockMine(BlockPlacer placer, World w, BlockPos pos, PlayerServer player, ItemStack itemstack) {
    }
    public void onUpdate(World w, int ix, int iy, int iz, int from) {
        
    }
    public ModelBlock getBlockModel(IBlockWorld w, int ix, int iy, int iz, int texturepass) {
        return loadedModels[0];
    }
    public int getNormalMap(int texture) {
        if (this.normalMaps.length > 0) {
            return TextureArrays.blockNormalMapArray.getTextureIdx(this.id, 0);
        }
        return 0;
    }
    public float getRoughness(int texture) {
        return 0.05f;
    }
    public Block getBaseBlock() {
        return this;
    }
    public boolean canWalkThru(IBlockWorld w, int ix, int iy, int iz, Entity e) {
        return isReplaceable();
    }
    public boolean renderMeshedAndNormal() {
        return false;
    }
}
