package nidefawl.qubes.input;

import nidefawl.qubes.Game;
import nidefawl.qubes.network.packet.PacketCSetBlocks;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;

public class EditBlockTask {

    private BlockPos p1;
    private BlockPos p2;
    private int block;

    public EditBlockTask(BlockPos p1, BlockPos p2, int selBlock) {
        this.p1 = p1;
        this.p2 = p2;
        this.block = selBlock;
    }
    
    public void apply(World world) {
        Game.instance.sendPacket(new PacketCSetBlocks(world.getId(), p1, p2, this.block));

//        int w = p2.x-p1.x+1;
//        int h = p2.y-p1.y+1;
//        int l = p2.z-p1.z+1;
//        for (int x = 0; x < w; x++) {
//            for (int y = 0; y < h; y++) {
//                for (int z = 0; z < l; z++) {
//                    int blockX = p1.x+x;
//                    int blockY = p1.y+y;
//                    int blockZ = p1.z+z;
//                    world.setType(blockX, blockY, blockZ, block, Flags.MARK);
//                }
//            }
//            
//        }
    }

}
