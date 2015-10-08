/**
 * 
 */
package nidefawl.qubes.block;

import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class BlockAir extends Block {

    
    public BlockAir(int id) {
        this(id, true);
    }
    BlockAir(int id, boolean transparent) {
        super(id, transparent);
        this.textures = NO_TEXTURES;
    }

    public Block setTextures(String...list) {
        return this;
    }

    public Block setAbsTextures(String...list) {
        return this;
    }
    
    public int getTextureFromSide(int faceDir) {
        return 0;
    }
    
    public int getRenderPass() {
        return -1;
    }
    public int getRenderType() {
        return -1;
    }

    public boolean applyAO() {
        return false;
    }
    public boolean isOccluding() {
        return false;
    }
    
    public int getBBs(World world, int x, int y, int z, AABBFloat[] tmp) {
        return 0;
    }
    public float getAlpha() {
        return 1;
    }
    public int getLightValue() {
        return 0;
    }
    
    
    public boolean isVisibleBounds(IBlockWorld w, int axis, int side, AABBFloat bb) {
        return true;
    }
    
    public boolean isFaceVisible(IBlockWorld w, int ix, int iy, int iz, int axis, int side, Block block, AABBFloat bb) {
        return true;
    }

    public boolean isNormalBlock(IBlockWorld chunkRenderCache, int ix, int iy, int iz) {
        return false;
    }

    public boolean isReplaceable() {
        return true;
    }

    public boolean raytrace(RayTrace rayTrace, World world, int x, int y, int z, Vector3f origin, Vector3f direction, Vector3f dirFrac) {

        return false;
    }
    public boolean isFullBB() {
        return true;
    }

}
