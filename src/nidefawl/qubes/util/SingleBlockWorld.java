/**
 * 
 */
package nidefawl.qubes.util;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SingleBlockWorld implements IBlockWorld {

    private int id;
    private int data;
    final BlockPos pos = new BlockPos();
    private int airId;
    private int airData;
    private int light;
    BlockData bdata=null;
    Biome biome = Biome.MEADOW_GREEN;
    
    public SingleBlockWorld() {
    }
    
    public void set(int x, int y, int z, int id, int data) {
        this.pos.set(x, y, z);
        this.id = id;
        this.data = data;
    }
    public void setAirBlock(int id, int data) {
        this.airId = id;
        this.airData = data;
    }

    public void setLight(int light) {
        this.light = light;
    }

    @Override
    public int getType(int x, int y, int z) {
        if (is(x,y,z)) {
            return this.id;
        }
        return this.airId;
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return
     */
    public boolean is(int x, int y, int z) {
        return this.pos.x==x&&this.pos.y==y&&this.pos.z==z;
    }

    @Override
    public boolean setType(int x, int y, int z, int type, int flags) {
        return false;
    }

    @Override
    public int getHeight(int x, int z) {
        return 0;
    }

    @Override
    public boolean setData(int x, int y, int z, int type, int render) {
        return false;
    }

    @Override
    public int getData(int x, int y, int z) {
        if (is(x, y, z))
            return this.data;
        return this.airData;
    }

    @Override
    public boolean isNormalBlock(int x, int y, int z, int offsetId) {
        if (offsetId < 0) {
            offsetId = this.getType(x, y, z);
        }
        return Block.get(offsetId).isNormalBlock(this, x, y, z);
    }

    @Override
    public boolean setTypeData(int x, int y, int z, int type, int data, int render) {
        return false;
    }

    @Override
    public int getLight(int x, int y, int i) {
        return this.light;
    }

    @Override
    public BlockData getBlockData(int x, int y, int z) {
        return bdata;
    }

    /**
     * @param bdata2
     */
    public void setBlockData(BlockData bdata) {
        this.bdata = bdata;
    }

    @Override
    public boolean setBlockData(int x, int y, int z, BlockData bd, int flags){
        return false;
    }

    @Override
    public Biome getBiome(int x, int z) {
        return this.biome;
    }



    @Override
    public int getBiomeFaceColor(int x, int y, int z, int faceDir, int pass, BiomeColor colorType) {
        return this.biome.getFaceColor(colorType);
    }

}
