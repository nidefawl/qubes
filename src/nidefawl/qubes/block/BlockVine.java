/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.item.Stack;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 */
public class BlockVine extends Block {

    /**
     * @param id
     */
    public BlockVine(int id) {
        super(id, true);
        setCategory(BlockCategory.VINE);
    }

    @Override
    public int getRenderType() {
        return 4;
    }

    @Override
    public int getLODPass() {
        return WorldRenderer.PASS_LOD;
    }

    @Override
    public boolean canPlaceAt(BlockPlacer blockPlacer, BlockPos against, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        //not on top or bottom faces
        if (offset >> 1 == 1) {
            return false;
        }
        return super.canPlaceAt(blockPlacer, against, pos, fpos, offset, type, data);
    }

    @Override
    public int prePlace(BlockPlacer blockPlacer, BlockPos pos, Vector3f fpos, int offset, int type, int data) {
        int axis = offset >> 1;
        int side = offset & 1;
        int rotdata = 0;
        if (axis == 1) {
            int rot = blockPlacer.getPlayer().getLookDir();
            switch (rot) {
                case Dir.DIR_NEG_X:
                    rotdata = 1;
                    break;
                case Dir.DIR_POS_X:
                    rotdata = 4;
                    break;
                case Dir.DIR_NEG_Z:
                    rotdata = 2;
                    break;
                case Dir.DIR_POS_Z:
                    rotdata = 8;
                    break;
            }
        } else {
            switch (axis + side) {
                case 1:
                    rotdata = 8;
                    break;
                case 2:
                    rotdata = 4;
                    break;
                case 3:
                    rotdata = 1;
                    break;
                case 0:
                    rotdata = 2;
                    break;
            }
        }
        return rotdata;
    }

    @Override
    public boolean isOccluding() {

        return false;
    }

    @Override
    public boolean isFullBB() {
        return false;
    }

    @Override
    public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
        return true;
    }

    @Override
    public int getRenderShadow() {
        return 0;
    }

    @Override
    public boolean isReplaceable() {
        return true;
    }

    @Override
    public boolean raytrace(RayTrace rayTrace, World world, int x, int y, int z, Vector3f origin, Vector3f direction, Vector3f dirFrac) {
        AABBFloat bb = rayTrace.getTempBB();
        int i = this.setSelectionBB(world, x, y, z, bb);
        if (i > 0) {
            bb.offset(x, y, z);
            return bb.raytrace(rayTrace, origin, direction, dirFrac);
        }
        return false;
    }

    @Override
    public int getBBs(World world, int x, int y, int z, AABBFloat[] tmp) {
        return 0;
    }

    @Override
    public int setSelectionBB(World world, RayTraceIntersection r, BlockPos hitPos, AABBFloat selBB) {
        return setSelectionBB(world, hitPos.x, hitPos.y, hitPos.z, selBB);
    }

    private int setSelectionBB(World world, int x, int y, int z, AABBFloat selBB) {
        selBB.set(this.blockBounds);//FULL BB
        int flags = world.getData(x, y, z) & 0xF;
        float thickness = 0.05f;
        float minOffset = 0.01f;
        for (int j = 0; j < 4; j++) {
            int bit = 1 << j;
            if ((flags & bit) != 0) {
                switch (j) {
                    default:
                    case 2:
                        //neg z
                        selBB.set(0, 0, 0, 1, 1, thickness);
                        selBB.offset(0, 0, minOffset);
                        return 1;
                    case 1:
                        //pos x
                        selBB.set(0, 0, 0, thickness, 1, 1);
                        selBB.offset(minOffset, 0, 0);
                        return 1;
                    case 0:
                        //pos z
                        selBB.set(0, 0, 1 - thickness, 1, 1, 1);
                        selBB.offset(0, 0, -minOffset);
                        return 1;
                    case 3:
                        //neg x
                        selBB.set(1 - thickness, 0, 0, 1, 1, 1);
                        selBB.offset(-minOffset, 0, 0);
                        return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public int getFaceColor(IBlockWorld w, int x, int y, int z, int faceDir, int pass) {
        return w.getBiomeFaceColor(x, y, z, faceDir, pass, BiomeColor.FOLIAGE);
    }
    
    public int getInvRenderData(Stack stack) {
        return 4;
    }
    /**
     * @return
     */
    public float getInvRenderRotation() {
        return GameMath.PI_OVER_180*45;
    }
}
