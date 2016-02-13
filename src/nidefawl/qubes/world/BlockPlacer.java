/**
 * 
 */
package nidefawl.qubes.world;

import nidefawl.qubes.biomes.HexBiome;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockQuarterBlock;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.chunk.blockdata.BlockDataQuarterBlock;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.item.*;
import nidefawl.qubes.network.packet.PacketSDigState;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.worldgen.trees.Tree;

/**
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */

@SideOnly(value=Side.SERVER)
public class BlockPlacer {
    private PlayerServer player;
    BlockStack stack;
    long startTime = 0L;
    private HexBiome biome;
    private Tree tree;
    private int resolved;
    private BlockPos pos;

    /**
     * @param serverHandlerPlay
     */
    public BlockPlacer(PlayerServer player) {
        this.player = player;
    }
    /**
     * @return the player
     */
    public Player getPlayer() {
        return this.player;
    }
    public World getWorld() {
        return this.player.world;
    }

    public Tree getTree() {
        if ((resolved&2)==0) {
            resolved|=2;
            HexBiome biome = getBiome();
            if (biome != null) {
                HexBiome[] biomes = biome.getClosest3(pos.x, pos.z);
                for (HexBiome b : biomes) {
                    this.tree = b.getTree(pos.x, pos.y, pos.z);
                    if (this.tree != null) {
                        break;
                    }
                }
            }
        }
        return this.tree;
    }
    public HexBiome getBiome() {
        if ((resolved&1)==0) {
            resolved|=1;
            this.biome = getWorld().getHex(pos.x, pos.z);
        }
        return this.biome;
    }


    public void tryMine(BlockPos pos, Vector3f fpos, BaseStack stack, int face, int clientStage) {
        World w = getWorld();
        boolean isQuarter = (face & 8) != 0;
        if (pos.y < 0 || pos.y >= w.worldHeight) {
            return;
        }
        if (isQuarter) {
            return;
        }
        if (stack == null || !stack.isItem()) {
//            this.player.kick("Invalid stack received");
            return;
        }
        this.pos = pos;
        this.resolved = 0;
   
        int typeAt = getWorld().getType(pos);
        Block bAgainst = Block.get(typeAt);
        ItemStack itemstack = (ItemStack) stack;
        int transaction = clientStage>>2;
        int stage = clientStage&3;
        if (stage > 0 && bAgainst.canMineWith(this, w, pos, this.player, itemstack)) {
            long l = (System.currentTimeMillis()-startTime)/50;
            l/=15;
            if (stage > 1) {
                bAgainst.onBlockMine(this, w, pos, this.player, itemstack); 
            } else {
                player.sendPacket(new PacketSDigState(w.getId(), transaction<<2|1));
                startTime = System.currentTimeMillis();
            }
        } else {
            player.sendPacket(new PacketSDigState(w.getId(), transaction<<2|0));
        }
        
    }
    public void tryPlace(BlockPos pos, Vector3f fpos, BlockStack stack, int face) {
        World w = this.player.world;
//        int x, int y, int z, float fx, float fy, float fz
        this.stack = stack;
        Block b = stack.getBlock();
        if (b == null) {
            this.player.kick("Invalid block received");
            return;
        }
        boolean isQuarter = (face & 8) != 0;
        face &= 0x7;
        BlockPos qPos = pos.copy();
        if (isQuarter) {
            pos.x = pos.x>>1;
            pos.y = pos.y>>1;
            pos.z = pos.z>>1;
        }
        if (pos.y < 0 || pos.y >= w.worldHeight) {
            return;
        }
        int typeAt = getWorld().getType(pos);
        Block bAgainst = Block.get(typeAt);
        if (isQuarter) {
            int offset = bAgainst.isReplaceable() ? -1 : face;
            //            BlockPos against = pos.copy();
            if (b != Block.air)
                qPos.offset(offset);
            pos.x = qPos.x >> 1;
            pos.y = qPos.y >> 1;
            pos.z = qPos.z >> 1;
            int typeAt2 = getWorld().getType(pos);
            Block b2 = Block.get(typeAt2);
            if (b2 != Block.quarter) {
                if (b2.isReplaceable()) {
                    getWorld().setType(pos.x, pos.y, pos.z, Block.quarter.id, 0);
                    typeAt2 = getWorld().getType(pos);
                    b2 = Block.get(typeAt2);
                }
            }
            if (b2 == Block.quarter) {
                qPos.x &= 1;
                qPos.y &= 1;
                qPos.z &= 1;
//                if (posQ.x > 1 || posQ.x < 0 || posQ.y > 1 || posQ.y < 0 || posQ.z > 1 || posQ.z < 0) {
//                    return;
//                }
                BlockData data = w.getBlockData(pos.x, pos.y, pos.z);
                BlockDataQuarterBlock qdata = null;
                if (data != null && data.getTypeId() == BlockQuarterBlock.Q_DATA_TYPEID) {
                    qdata = (BlockDataQuarterBlock) data;
                }
                if (qdata == null) {
                    qdata = new BlockDataQuarterBlock();
                    w.setBlockData(pos, qdata, 0);
                }
                qdata.setType(qPos.x, qPos.y, qPos.z, this.stack.id);
                w.flagBlock(pos.x, pos.y, pos.z);

            }
//            if (1!=2)
//                return;
//            float x = fpos.x-pos.x;
//            float y = fpos.y-pos.y;
//            float z = fpos.z-pos.z;
//            System.out.println("fpos "+fpos);
//            System.out.println("bAgainst "+bAgainst);
//            boolean b1 = true;
//            int qX = 0; //(x > 0.5 && b1) || face == Dir.DIR_POS_X ? 1 : 0;
//            int qY = 0; //(y > 0.5 && b1) || face == Dir.DIR_POS_Y ? 1 : 0;
//            int qZ = 0; //(z > 0.5 && b1) || face == Dir.DIR_POS_Z ? 1 : 0;
//            switch (face) {
//                case Dir.DIR_POS_X:
//                    if (bAgainst == Block.quarter) {
//                        
//                    } else {
//                        
//                    }
//                    break;
//                case Dir.DIR_POS_Y:
//                    break;
//                case Dir.DIR_POS_Z:
//                    break;
//                case Dir.DIR_NEG_X:
//                    break;
//                case Dir.DIR_NEG_Y:
//                    break;
//                case Dir.DIR_NEG_Z:
//                    break;
//            }
//            BlockPos posQ = new BlockPos(qX, qY, qZ);
//            System.out.println("posQ 1 "+posQ);
//            System.out.println("face "+Dir.toFaceName(face));
//            if (b != Block.air)
//                posQ.offset(face);
//            if (bAgainst != Block.quarter || (posQ.x > 1 || posQ.x < 0 || posQ.y > 1 || posQ.y < 0 || posQ.z > 1 || posQ.z < 0)) {
//                int offset = bAgainst == null ? -1 : bAgainst.placeOffset(this, pos, fpos, face, b.id, stack.data);
//
//                pos.offset(offset);
//                typeAt = getWorld().getType(pos);
//                bAgainst = Block.get(typeAt);
//                if (bAgainst != Block.quarter) {
//                    Block bSelf = Block.get(getWorld().getType(pos));
//                    if (bSelf.isReplaceable()) {
//                        getWorld().setType(pos.x, pos.y, pos.z, Block.quarter.id, 0);
//                        typeAt = getWorld().getType(pos);
//                        bAgainst = Block.get(typeAt);
//                    }
//                }
//            
//                posQ.x &= 1;
//                posQ.y &= 1;
//                posQ.z &= 1;
//            }
//            if (bAgainst == Block.quarter) {
//                if (posQ.x > 1 || posQ.x < 0 || posQ.y > 1 || posQ.y < 0 || posQ.z > 1 || posQ.z < 0) {
//                    return;
//                }
//                BlockData data = w.getBlockData(pos.x, pos.y, pos.z);
//                BlockDataQuarterBlock qdata = null;
//                if (data != null && data.getTypeId() == BlockQuarterBlock.Q_DATA_TYPEID) {
//                     qdata = (BlockDataQuarterBlock) data;
//                }
//                if (qdata == null) {
//                    qdata = new BlockDataQuarterBlock();
//                    w.setBlockData(pos, qdata, 0);
//                }
//                qdata.setType(posQ.x, posQ.y, posQ.z, this.stack.id);
//                w.flagBlock(pos.x, pos.y, pos.z);
//            } else {
//            }
            return;
        }
        if (b == Block.air) {
            tryHarvest(pos.x, pos.y, pos.z);
        } else {
            int offset = bAgainst == null ? -1 : bAgainst.placeOffset(this, pos, fpos, face, b.id, stack.data);
            BlockPos against = pos.copy();
            pos.offset(offset);
            if (b.canPlaceAt(this, against, pos, fpos, face, b.id, stack.data)) {
                int data = b.prePlace(this, pos, fpos, face, b.id, stack.data);
                b.place(this, pos, fpos, face, b.id, data);
                b.postPlace(this, pos, fpos, face, b.id, data);
            }

        }
        
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param type
     * @param data
     */
    private void tryHarvest(int x, int y, int z) {
        World w = this.player.world;
        if (y < 0 || y >= w.worldHeight) {
            return;
        }
        int type = getWorld().getType(x, y, z);
        Block block = Block.get(type);
        if (block != null) {
            getWorld().setType(x, y, z, 0, Flags.MARK|Flags.LIGHT);
        }
    }
    /**
     * @param pos2 
     * @param blockPlace 
     * @param x
     * @param y
     * @param z
     * @param type 
     * @param data 
     * @param data2 
     * @return
     */
    public boolean canPlaceDefault(Block blockPlace, BlockPos posAgainst, BlockPos pos, int offset, int type, int data) {

        int wTypeAgainst = getWorld().getType(posAgainst);
        Block blockAgainst = Block.get(wTypeAgainst);
        if (blockPlace.isReplaceable() && blockAgainst.isReplaceable()) {
            if (blockPlace == blockAgainst) {
                return false;
            }
        }
        int wTypeAt = getWorld().getType(pos);
        Block blockAt = Block.get(wTypeAt);
        return blockAt.isReplaceable();
    }
    /**
     * @param x
     * @param y
     * @param z
     * @param type
     * @param data
     */
    public void placeDefault(BlockPos pos, int offset, int type, int data) {
        Block block = Block.get(type);
        if (block != null) {
            getWorld().setTypeData(pos.x, pos.y, pos.z, type, data, Flags.MARK);
        }
        
    }
    /**
     * @return
     */
    public BlockData getBlockData() {
        if (this.stack == null)
            return null;
        StackData d = this.stack.getStackdata();
        if (d == null)
            return null;
        return d.getBlockData();
    }

}
