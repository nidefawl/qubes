package nidefawl.qubes.world;

import java.util.*;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.worldgen.biome.HexBiome;
import nidefawl.qubes.worldgen.biome.HexBiomes;
import nidefawl.qubes.worldgen.biome.IBiomeManager;

public abstract class World implements IBlockWorld {
    public static final float MAX_XZ     = ChunkManager.MAX_CHUNK * Chunk.SIZE;
    public static final float MIN_XZ     = -MAX_XZ;
    public ArrayList<DynamicLight>         lights = new ArrayList<>();                                           // use fast array list

    public final int worldHeight;
    public final int worldHeightMinusOne;
    public final int worldHeightBits;
    public final int worldHeightBitsPlusFour;
    public final int worldSeaLevel;

    private long seed;
    
    private final ChunkManager chunkMgr;
    private final Random       rand;
    private final UUID         uuid;
    private int                id;
    public IWorldSettings settings;
    public IBiomeManager biomeManager;
    private final String name;
    public static final int    MAX_WORLDHEIGHT = 256;

    public World(IWorldSettings settings) {
        this.settings = settings;
        this.id = settings.getId();
        this.chunkMgr = makeChunkManager();
        this.seed = settings.getSeed();
        this.name = settings.getName();
        this.uuid = settings.getUUID();
        this.rand = new Random(seed);
        this.worldHeightBits = 8;
        this.worldHeightBitsPlusFour = worldHeightBits + 4;
        this.worldHeight = 1 << worldHeightBits;
        this.worldHeightMinusOne = (1 << worldHeightBits) - 1;
        this.worldSeaLevel = 59;//1 << (worldHeightBits - 1);
//        this.generator = new TerrainGenerator2(this, this.seed);

    }

    public abstract ChunkManager makeChunkManager();

    public float getSunAngle(float fTime) {
        //        if (time > 2000)
        //            time = 1200;
        //        if (time >1040) {
        //          fTime = 0;
        //        }
//                time = (int) (System.currentTimeMillis()/50L);
//                fTime = 0;
//        dayLen = 211500;
//        time = 138000;
////        time = 133000;
//        fTime=0;
//      dayLen = 4100;
        if (this.settings.isFixedTime()) {
            fTime = 0;
        }
      long time = this.settings.getTime();
      long dayLen = this.settings.getDayLen();
        float timeOffset = (time) % dayLen;
        float fSun = (timeOffset + fTime) / (float) dayLen + 0.25F;
        if (fSun < 0)
            fSun++;
        if (fSun > 1)
            fSun--;
        float f = 1.0F - (float) (Math.cos(fSun * Math.PI) + 1) / 2.0F;
        return fSun + (f - fSun) / 3.0F;
//                return 0.88f;
        //        float a = timeOffset / (float) dayLen;
        ////        a = 0f;
        ////        a*=0.3f;
        //        return a;
        ////        return 0.8f+a;
    }
    public abstract void tickUpdate();

    /**
     * Wrapper method for getType(int, int, int)
     * @param pos
     * @return block type id
     */
    public int getType(BlockPos pos) {
        return this.getType(pos.x, pos.y, pos.z);
    }
    

    /**
     * Wrapper method for getData(int, int, int)
     * @param pos
     * @return block data
     */
    public int getData(BlockPos pos) {
        return this.getData(pos.x, pos.y, pos.z);
    }
    @Override
    public BlockData getBlockData(int x, int y, int z) {
        if (y >= this.worldHeight)
            return null;
        if (y < 0)
            return null;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return null;
        }
        return c.getBlockData(x & 0xF, y, z & 0xF);
    }
    @Override
    public int getType(int x, int y, int z) {
        if (y >= this.worldHeight)
            return 0;
        if (y < 0)
            return 0;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return 0;
        }
        return c.getTypeId(x & 0xF, y, z & 0xF);
    }

    public int getWater(int x, int y, int z) {
        if (y >= this.worldHeight)
            return 0;
        if (y < 0)
            return 0;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return 0;
        }
        return c.getWater(x & 0xF, y, z & 0xF);
    }

    public int getData(int x, int y, int z) {
        if (y >= this.worldHeight)
            return 0;
        if (y < 0)
            return 0;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return 0;
        }
        return c.getData(x & 0xF, y, z & 0xF);
    }
    /* (non-Javadoc)
     * @see nidefawl.qubes.world.IBlockWorld#setType(int, int, int, int, int)
     */
    @Override
    public boolean setData(int x, int y, int z, int type, int render) {
        if (y >= this.worldHeight)
            return false;
        if (y < 0)
            return false;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return false;
        }
        if (c.setData(x & 0xF, y, z & 0xF, type)) {
            if ((render & Flags.LIGHT) != 0) {
                updateLight(x, y, z);
            }
            if ((render & Flags.MARK) != 0) {
                flagBlock(x, y, z);
            }   
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see nidefawl.qubes.world.IBlockWorld#setType(int, int, int, int, int)
     */
    @Override
    public boolean setType(int x, int y, int z, int type, int flags) {
        if (y >= this.worldHeight)
            return false;
        if (y < 0)
            return false;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return false;
        }
        if (c.setType(x & 0xF, y, z & 0xF, type)) {
            if ((flags & Flags.LIGHT) != 0) {
                updateLight(x, y, z);
            }
            if ((flags & Flags.MARK) != 0) {
                flagBlock(x, y, z);
            }   
        }
        return true;
    }
    @Override
    public boolean setBlockData(int x, int y, int z, BlockData bd, int flags) {
        if (y >= this.worldHeight)
            return false;
        if (y < 0)
            return false;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return false;
        }
        if (c.setBlockData(x & 0xF, y, z & 0xF, bd)) {
            if ((flags & Flags.LIGHT) != 0) {
                updateLight(x, y, z);
            }
            if ((flags & Flags.MARK) != 0) {
                flagBlock(x, y, z);
            }   
        }
        return true;
    }

    public boolean setBlockData(BlockPos pos, BlockData bd, int flags) {
        return setBlockData(pos.x, pos.y, pos.z, bd, flags);
    }
    
    @Override
    public boolean setTypeData(int x, int y, int z, int type, int data, int render) {
        if (y >= this.worldHeight)
            return false;
        if (y < 0)
            return false;
        Chunk c = getChunk(x >> 4, z >> 4);
        if (c == null) {
            return false;
        }
        if (c.setTypeData(x & 0xF, y, z & 0xF, type, data)) {
            updateLight(x, y, z);
            if ((render & Flags.MARK) != 0) {
                flagBlock(x, y, z);
            }   
        }
        return true;
    }
    public void updateLight(int x, int y, int z) {
        
    }

    public abstract void flagBlock(int x, int y, int z);

    public Chunk getChunk(int x, int z) {
        return chunkMgr.get(x, z);
    }


    public void onLoad() {
    }

    public void removeLight(int i) {
        if (this.lights.size() > 0) {
            this.lights.remove(this.lights.size()-1);
        }
    }

    public void addLight(Vector3f pos) {
//        float r = this.rand.nextFloat()*0.5f+0.5f;
//        float g = this.rand.nextFloat()*0.5f+0.5f;
//        float b = this.rand.nextFloat()*0.5f+0.5f;
        float r = 1;
        float g = 0.9f;
        float b = 0.8f;
        float intens = (0.5f+this.rand.nextFloat())*0.1f;
//        if (this.rand.nextInt(10) == 0) {
//            intens+=1;
//            intens = 2.7f;
//        }
//      intens += 10.7f;
        intens = 1.4f+(intens)*0.2f;
        DynamicLight light = new DynamicLight(pos, new Vector3f(r, g, b),  intens);
        this.lights.add(light);
    }

    public void spawnLights(BlockPos block) {
        for (int i = 0; i < 10; i++) {

            int range = 210;
            int x = block.x+this.rand.nextInt(range*2)-range;
            int z = block.z+this.rand.nextInt(range*2)-range;
            int y = getHeight(x, z);
            addLight(new Vector3f(x+0.5F, y+1.2f+rand.nextFloat()*3.0f, z+0.5F));
        }
    }

    @Override
    public int getHeight(int x, int z) {
        Chunk c = this.getChunk(x>>Chunk.SIZE_BITS, z>>Chunk.SIZE_BITS);
        if (c != null) {
            return c.getTopBlock(x&Chunk.MASK, z&Chunk.MASK);
        }
        return 0;
    }
    public ChunkManager getChunkManager() {
        return this.chunkMgr;
    }


    public UUID getUUID() {
        return this.uuid;
    }

    public long getSeed() {
        return this.seed;
    }

    public long getTime() {
        return this.settings.getTime();
    }

    public long getDayLength() {
        return this.settings.getDayLen();
    }
    
    public long getDayTime() {
        long time = this.settings.getTime();
        long dayLen = this.settings.getDayLen();
        return time % dayLen;
    }

    /** dynamically generated at boot time 
     * do not use for storage
     * @return a consistent world-id at runtime only
     */
    public int getId() {
        return this.id;
    }

    public Chunk getChunkFromBlock(int x, int z) {
        return getChunk(x>>Chunk.SIZE_BITS, z>>Chunk.SIZE_BITS);
    }

    public void updateLightHeightMap(Chunk chunk, int i, int j, int min, int max, boolean add) {
    }

    public Chunk getChunkIfNeightboursLoaded(int x, int z) {
        Chunk c = this.getChunkManager().get(x, z);
        if (c != null) {
            for (int _x = -1; _x < 2; _x++)
                for (int _z = -1; _z < 2; _z++) {
                    if (_x == 0 && _z == 0)
                        continue;
                    if (getChunkManager().get(x + _x, z + _z) == null)
                        return null;
                }
        }
        return c;
    }

    public boolean canSeeSky(int x, int y, int z) {
        Chunk c = getChunk(x >> Chunk.SIZE_BITS, z >> Chunk.SIZE_BITS);
        return c == null ? false : c.getHeightMap(x & Chunk.MASK, z & Chunk.MASK) <= y + 1;
    }

    public boolean isTransparent(int x, int y, int z) {
        Chunk c = getChunk(x >> Chunk.SIZE_BITS, z >> Chunk.SIZE_BITS);
        return c == null ? false : !Block.isOpaque(c.getTypeId(x & Chunk.MASK, y, z & Chunk.MASK));
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.world.IBlockWorld#getHeight(int, int)
     */
    public int getHeightMap(int x, int z) {
        Chunk c = getChunk(x >> Chunk.SIZE_BITS, z >> Chunk.SIZE_BITS);
        return c == null ? 0 : c.getHeightMap(x & Chunk.MASK, z & Chunk.MASK);
    }

    public int getLight(int i, int j, int k) {
        Chunk c = getChunk(i >> Chunk.SIZE_BITS, k >> Chunk.SIZE_BITS);
        if (c == null)
            return 0;
        return c.getLight(i & Chunk.MASK, j, k & Chunk.MASK);
    }


    @Override
    public Biome getBiome(int i, int k) {
        Chunk c = getChunk(i >> Chunk.SIZE_BITS, k >> Chunk.SIZE_BITS);
        if (c == null)
            return Biome.MEADOW_GREEN;
        return c.getBiome(i & Chunk.MASK, k & Chunk.MASK);
    }

    public void flagChunkLightUpdate(int x, int z) {
    }
    
    public String getName() {
        return this.name;
    }


    /**
     * @return the worlds settings (time, seed, name, ...)
     */
    public IWorldSettings getSettings() {
        return this.settings;
    }


    @Override
    public boolean isNormalBlock(int ix, int iy, int iz, int offsetId) {
        if (offsetId < 0) {
            offsetId = this.getType(ix, iy, iz);
        }
        return Block.get(offsetId).isNormalBlock(this, ix, iy, iz);
    }
    public Random getRand() {
        return this.rand;
    }

    public abstract Entity getEntity(int entId);

    public abstract List<Entity> getEntityList();


    @Override
    public int getBiomeFaceColor(int x, int y, int z, int faceDir, int pass, BiomeColor colorType) {
        return this.biomeManager.getBiomeFaceColor(this, x, y, z, faceDir, pass, colorType);
    }
    
    public HexBiome getHex(int x, int z) {
        return this.biomeManager.blockToHex(x, z);
    }
}
