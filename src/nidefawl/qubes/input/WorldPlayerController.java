package nidefawl.qubes.input;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.blockdata.BlockDataQuarterBlock;
import nidefawl.qubes.item.BlockStack;
import nidefawl.qubes.network.packet.PacketCSetBlock;
import nidefawl.qubes.network.packet.PacketCSetBlocks;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;

public class WorldPlayerController {
    
    public WorldPlayerController() {
    }

    
    /**
     * @param hit
     * @param button 
     * @param quarterMode 
     */
    public void blockClicked(World world, RayTraceIntersection hit, int button, boolean quarterMode) {
        if (Game.instance.statsOverlay != null) {
            Game.instance.statsOverlay.blockClicked(hit);
        }
        int faceHit = hit.face;
        BlockPos pos = hit.blockPos;
        BlockStack stack = button == 0 ? new BlockStack(0) : Game.instance.selBlock.copy();
        if (quarterMode) {
            faceHit |= 0x8;
            pos = new BlockPos();
            pos.set(hit.blockPos);
            pos.x*=2;
            pos.y*=2;
            pos.z*=2;
            pos.x+=hit.q.x;
            pos.y+=hit.q.y;
            pos.z+=hit.q.z;
        }
        Game.instance.sendPacket(new PacketCSetBlock(Game.instance.getWorld().getId(), pos, hit.pos, faceHit, stack));
    }

    public void pickBlock(World world, BlockPos p, RayTraceIntersection hit) {
        int type = world.getType(p);
        int d = world.getData(p);
        if (type == Block.quarter.id) {
            
            BlockDataQuarterBlock q = (BlockDataQuarterBlock) world.getBlockData(p.x, p.y, p.z);
            if (q != null) {
                type = q.getType(hit.q.x, hit.q.y, hit.q.z);
                d = q.getData(hit.q.x, hit.q.y, hit.q.z);
                Game.instance.selBlock.id = type;
                Game.instance.selBlock.data = d;
            }
            return;
        }
        Game.instance.selBlock.id = type;
        Game.instance.selBlock.data = d;
    
    }


    public void setMultiple(World world, RayTraceIntersection hit, int button, BlockPos min, BlockPos max) {
        int faceHit = hit.face;
        boolean hollow = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT);
        BlockStack stack = button == 0 ? new BlockStack(0) : Game.instance.selBlock.copy();
        Game.instance.sendPacket(new PacketCSetBlocks(world.getId(), min, max, hit.pos, faceHit, stack, hollow));

    }
}
