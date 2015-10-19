/**
 * 
 */
package nidefawl.qubes.block;

import java.util.Arrays;
import java.util.List;

import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.chunk.blockdata.BlockDataQuarterBlock;
import nidefawl.qubes.item.Stack;
import nidefawl.qubes.item.StackData;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockQuarterBlock extends BlockSliced {
    public final static short[] readOnly = new short[8];
    public final static int Q_DATA_TYPEID = 1;
    public final static int Q_SIZE = 8;
    public BlockQuarterBlock(int id) {
        super(id, true);
        this.textures = NO_TEXTURES;
    }
    short[] getSafeRead(IBlockWorld w, int x, int y, int z) {
        BlockData data = w.getBlockData(x, y, z);
        if (data != null && data.getTypeId() == Q_DATA_TYPEID) {
            return ((BlockDataQuarterBlock) data).blockIDs;
        }
        return readOnly;
    }
    
    
    BlockDataQuarterBlock getSafeCast(IBlockWorld w, int x, int y, int z) {
        BlockData data = w.getBlockData(x, y, z);
        if (data != null && data.getTypeId() == Q_DATA_TYPEID) {
            return ((BlockDataQuarterBlock) data);
        }
        return null;
    }
    
    @Override
    public void getQuarters(IBlockWorld w, int x, int y, int z, int[] quarters) {
        BlockData data = w.getBlockData(x, y, z);
        if (data != null && data.getTypeId() == Q_DATA_TYPEID) {
            ((BlockDataQuarterBlock) data).fillIntArr(quarters);
        } else {
            Arrays.fill(quarters, 0);
        }
    }
    @Override
    public boolean isOccludingBlock(IBlockWorld w, int x, int y, int z) {
        short[] blocks = getSafeRead(w, x, y, z);
        for (int i = 0; i < blocks.length; i++) {
            if (Block.get(blocks[i]).isTransparent()) {
                return false;
            }
        }
        return true;
    }
    @Override
    public int getBBs(World world, int ix, int iy, int iz, AABBFloat[] tmp) {
        BlockDataQuarterBlock data = getSafeCast(world, ix, iy, iz);
        if (data != null) {
           AABBFloat bb = null;
           int numBBs = 0;
           for (int y = 0; y < 2; y++)
           for (int x = 0; x < 2; x++)
           for (int z = 0; z < 2; z++) {
               Block block = Block.get(data.getType(x,y,z));
               if (block.isTransparent()||!block.isFullBB()) {
                   bb = null;
               } else {
                   if (bb == null) {
                       bb = tmp[numBBs++];
                       bb.set(x*0.5f, y*0.5f, z*0.5f, x*0.5f+0.5f, y*0.5f+0.5f, z*0.5f+0.5f);
                   } else {
                       bb.expandBounds(x*0.5f+0.5f, y*0.5f+0.5f, z*0.5f+0.5f);
                   }
               }
           }
           
           for (int i = 0; i < numBBs; i++)
               tmp[i].offset(ix, iy, iz);
               
           return numBBs;
        }
        return 0;
    }
    @Override
    public int setSelectionBB(World world, RayTraceIntersection r, BlockPos hitPos, AABBFloat selBB) {
        int x = r.q.x;
        int y = r.q.y;
        int z = r.q.z;
        selBB.set(x*0.5f, y*0.5f, z*0.5f, x*0.5f+0.5f, y*0.5f+0.5f, z*0.5f+0.5f);
        return 2;
    }
    
    public boolean raytrace(RayTrace rayTrace, World world, int ix, int iy, int iz, Vector3f origin, Vector3f direction, Vector3f dirFrac) {

        BlockDataQuarterBlock data = getSafeCast(world, ix, iy, iz);
        if (data != null) {
            boolean b=false;
            for (int y = 0; y < 2; y++)
            for (int x = 0; x < 2; x++)
            for (int z = 0; z < 2; z++) {
                Block block = Block.get(data.getType(x,y,z));
                if (block!=air) {
                    AABBFloat bb = rayTrace.getTempBB(); 
                    bb.set(x*0.5f, y*0.5f, z*0.5f, x*0.5f+0.5f, y*0.5f+0.5f, z*0.5f+0.5f);
                    bb.offset(ix, iy, iz);
                    rayTrace.quarter.set(x,y,z);
                    b |= bb.raytrace(rayTrace, origin, direction, dirFrac);
                }
            }
            return b;
        }
        return false;
    }
    @Override
    public boolean isNormalBlock(IBlockWorld chunkRenderCache, int ix, int iy, int iz) {
        short[] blocks = getSafeRead(chunkRenderCache, ix, iy, iz);
        for (int i = 0; i < blocks.length; i++) {
            if (Block.get(blocks[i]).isTransparent()) {
                return false;
            }
        }
        return true;
    }
    

    @Override
    public int getItems(List<Stack> l) {
        int a = l.size();
        {

            Stack st = new Stack(this.id);
            StackData data = new StackData();
            BlockDataQuarterBlock qBlock = new BlockDataQuarterBlock();
            qBlock.setTypeAndData(0, 0, 0, Block.stone.id, 0);
            data.setBlockData(qBlock);
            st.setStackdata(data);
            l.add(st);
        }
        {

            Stack st = new Stack(this.id);
            StackData data = new StackData();
            BlockDataQuarterBlock qBlock = new BlockDataQuarterBlock();
            qBlock.setTypeAndData(0, 0, 0, Block.stone.id, 0);
            qBlock.setTypeAndData(0, 0, 1, Block.stone.id, 0);
            qBlock.setTypeAndData(0, 1, 1, Block.stone.id, 0);
            qBlock.setTypeAndData(0, 1, 0, Block.stone.id, 0);
            data.setBlockData(qBlock);
            st.setStackdata(data);
            l.add(st);
        }
        {

            Stack st = new Stack(this.id);
            StackData data = new StackData();
            BlockDataQuarterBlock qBlock = new BlockDataQuarterBlock();
            qBlock.setTypeAndData(0, 0, 0, Block.stone.id, 0);
            qBlock.setTypeAndData(0, 1, 0, Block.stone.id, 0);
            qBlock.setTypeAndData(0, 0, 1, Block.stone.id, 0);
            qBlock.setTypeAndData(1, 0, 0, Block.stone.id, 0);
            qBlock.setTypeAndData(1, 1, 0, Block.stone.id, 0);
            qBlock.setTypeAndData(1, 0, 1, Block.stone.id, 0);
            qBlock.setTypeAndData(1, 1, 1, Block.stone.id, 0);
            data.setBlockData(qBlock);
            st.setStackdata(data);
            l.add(st);
        }
        {

            Stack st = new Stack(this.id);
            StackData data = new StackData();
            BlockDataQuarterBlock qBlock = new BlockDataQuarterBlock();
            for (int k = 0; k < 8; k++) {

                qBlock.setTypeAndData((k>>2)&1, (k>>1)&1, (k>>0)&1, Block.stone.id, 0);
            }
            data.setBlockData(qBlock);
            st.setStackdata(data);
            l.add(st);
        }
        return l.size()-a;
    }
    @Override
    public void postPlace(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        BlockData st = blockPlacer.getBlockData();
        if (st != null && st.getTypeId() == Q_DATA_TYPEID) {
            blockPlacer.getWorld().setBlockData(pos, st, Flags.MARK);
        }
    }
}
