package nidefawl.qubes.input;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.network.packet.PacketCSetBlocks;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;

public class EditBlockTask {

    private BlockPos p1;
    private BlockPos p2;
    private BlockStack stack;
    public boolean hollow = false;

    public EditBlockTask(BlockPos p1, BlockPos p2, BlockStack stack) {
        this.p1 = p1;
        this.p2 = p2;
        this.stack = stack;
    }
    
    public void apply(World world) {

        int w = p2.x-p1.x+1;
        int h = p2.y-p1.y+1;
        int l = p2.z-p1.z+1;
        int flags = Flags.MARK|Flags.LIGHT;
        Block block = stack.getBlock();
        int type = block.id;
        if (hollow) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    {
                        int blockX = p1.x+x;
                        int blockY = p1.y+y;
                        int blockZ = p1.z ;
                        world.setType(blockX, blockY, blockZ, type, flags);
                    }
                    {
                        int blockX = p1.x + x;
                        int blockY = p1.y + y;
                        int blockZ = p1.z + l-1;
                        world.setType(blockX, blockY, blockZ, type, flags);
                    }
                }
            }
            for (int z = 0; z < l; z++) {
                for (int y = 0; y < h; y++) {
                    {
                        int blockX = p1.x;
                        int blockY = p1.y+y;
                        int blockZ = p1.z+z;
                        world.setType(blockX, blockY, blockZ, type, flags);
                    }
                    {
                        int blockX = p1.x + w-1;
                        int blockY = p1.y + y;
                        int blockZ = p1.z + z;
                        world.setType(blockX, blockY, blockZ, type, flags);
                    }
                }
            }
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < l; z++) {
//                    {
//                        int blockX = p1.x+x;
//                        int blockY = p1.y;
//                        int blockZ = p1.z+z;
//                        world.setType(blockX, blockY, blockZ, type, flags);
//                    }
                    {
                        int blockX = p1.x + x;
                        int blockY = p1.y + h-1;
                        int blockZ = p1.z + z;
                        world.setType(blockX, blockY, blockZ, type, flags);
                    }
                }
            }} else {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < l; z++) {
                    for (int y = 0; y < h; y++) {
                        int blockX = p1.x+x;
                        int blockY = p1.y+y;
                        int blockZ = p1.z+z;
                        
                        world.setType(blockX, blockY, blockZ, type, flags);
                    }
                }
                
            }
        }
    }

}
