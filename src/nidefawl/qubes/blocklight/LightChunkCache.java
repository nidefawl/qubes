/**
 * Cache neighbour chunks for rendering
 * 
 * @author Michael
 *
 */
package nidefawl.qubes.blocklight;

import static nidefawl.qubes.chunk.Chunk.*;
import java.util.Arrays;
import java.util.HashSet;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.ChunkManager;
import nidefawl.qubes.server.PlayerChunkTracker;
import nidefawl.qubes.util.TripletLongHash;
import nidefawl.qubes.util.TripletShortHash;
import nidefawl.qubes.vec.BlockBoundingBox;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;

public class LightChunkCache {
    public final static int NEXT    = 1;
    public final static int WIDTH   = 1 + (NEXT * 2);
    final public Chunk[]    chunks  = new Chunk[WIDTH * WIDTH];//TODO: are corners required?
    public int              lastX;
    public int              lastZ;
    public boolean          isValid = false;

    final int[] flaggedNumBlocks = new int[WIDTH * WIDTH];

    final BlockBoundingBox[] flaggedBlocks = new BlockBoundingBox[WIDTH * WIDTH];
    public int worldHeightMin1;
    private int nUse;
    static int drainedChunks;
    
    public LightChunkCache() {
        for (int i = 0; i < flaggedBlocks.length; i++) {
            flaggedBlocks[i] = new BlockBoundingBox();
        }
    }

    public void resetFlaggedBlocks() {
        Arrays.fill(this.flaggedNumBlocks, 0);
        for (int i = 0; i < flaggedBlocks.length; i++) {
            flaggedBlocks[i].reset();
        }
    }

    public void invalidate() {
        this.isValid = false;
        Arrays.fill(this.chunks, null);
    }

    final int idx(int x, int z) {
        x -= this.lastX;
        z -= this.lastZ;
        if (x < -NEXT || x > NEXT || z < -NEXT || z > NEXT)
            throw new IndexOutOfBoundsException("Invalid chunk queried (" + x + "," + z + ") is not in this cache (" + lastX + "," + lastZ + ")");

        return (x + NEXT) * WIDTH + (z + NEXT);
    }

    public Chunk get(int x, int z) {
        return this.chunks[idx(x, z)];
    }

    public boolean canSeeSky(int x, int y, int z) {
        Chunk c = get(x >> SIZE_BITS, z >> SIZE_BITS);
        return c == null ? false : c.getHeightMap(x & MASK, z & MASK) <= y + 1;
    }

    public boolean isTransparent(int x, int y, int z) {
        Chunk c = get(x >> SIZE_BITS, z >> SIZE_BITS);
        return c == null ? false : !Block.isOpaque(c.getTypeId(x & MASK, y, z & MASK));
    }

    public int getHeight(int x, int z) {
        Chunk c = get(x >> SIZE_BITS, z >> SIZE_BITS);
        return c == null ? 0 : c.getHeightMap(x & MASK, z & MASK);
    }

    public int getLight(int i, int j, int k, int type) {
        Chunk c = get(i >> SIZE_BITS, k >> SIZE_BITS);
        if (c == null)
            return 0;
        return c.getLight(i & MASK, j, k & MASK, type);
    }


    /** call with relative coords */
    public int getTypeId(int i, int j, int k) {

        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0;
        }
        Chunk chunk = get(i >> SIZE_BITS, k >> SIZE_BITS);
        return chunk != null ? chunk.getTypeId(i & 0xF, j, k & 0xF) : 0;
    }

    public int getLight(int i, int j, int k, int type, int value, int flags) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return 0xF0;
        }
        Chunk chunk = get(i >> SIZE_BITS, k >> SIZE_BITS);
        return chunk != null ? chunk.getLight(i & 0xF, j, k & 0xF, type) : 0;
    }

    public boolean setLight(int i, int j, int k, int type, int value, int flags) {
        if (j < 0 || j >= World.MAX_WORLDHEIGHT) {
            return false;
        }
        int idx = idx(i >> SIZE_BITS, k >> SIZE_BITS);
        Chunk chunk = this.chunks[idx];
        if (chunk != null) {
            this.flaggedBlocks[idx].flag(i & 0xF, j, k & 0xF);
            flaggedNumBlocks[idx]++;
            if (chunk.setLight(i & 0xF, j, k & 0xF, type, value)) {
//                if (idx2 < flaggedBlocks.length) {
//                    flaggedBlocks[idx][idx2] = TripletShortHash.toHash(i, j, k);
//                }
                return true;
            }
        }
        return false;
    }

    public boolean isValid(WorldServer world, int basechunkX, int basechunkZ) {
        if (basechunkX == this.lastX && basechunkZ == this.lastZ && this.isValid) {
            for (int i = 0; i < this.chunks.length; i++) {
                Chunk c = this.chunks[i];
                if (c.isUnloading || !c.isValid) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    public boolean cache(WorldServer world, int basechunkX, int basechunkZ) {
        invalidate();
        this.worldHeightMin1 = world.worldHeightMinusOne;
        ChunkManager mgr = world.getChunkManager();
        for (int x = -NEXT; x < NEXT + 1; x++) {
            for (int z = -NEXT; z < NEXT + 1; z++) {
                Chunk c = mgr.get(basechunkX + x, basechunkZ + z);
                if (c == null) {
                    return false;
                }
                this.chunks[(x + NEXT) * WIDTH + (z + NEXT)] = c;
            }
        }
        this.lastX = basechunkX;
        this.lastZ = basechunkZ;
        this.isValid = true;
        return true;
    }
//    public void getFlaggedBlocks(int i) {
//        
//    }

    /**
     * @param playerChunkTracker
     */
    public void drainFlagged(PlayerChunkTracker playerChunkTracker) {
//        System.out.println("drainFlagged");
        for (int x = -NEXT; x < NEXT + 1; x++) {
            for (int z = -NEXT; z < NEXT + 1; z++) {
                int idx = (x + NEXT) * WIDTH + (z + NEXT);
                int num = this.flaggedNumBlocks[idx];
                if (num > 0) {
                    this.flaggedNumBlocks[idx] = 0;
                    this.drainedChunks++;
                    playerChunkTracker.flagLights(lastX + x, lastZ + z, this.flaggedBlocks[idx]);
                }
            }
        }
        resetFlaggedBlocks();
    }

    /** Checks if this cache contains a chunk for the given block coordinate
     * @param dirX
     * @param dirZ
     * @return true if this cache contains a chunk for the given block coordinate
     */
    public boolean hasBlock(int dirX, int dirZ) {
        dirX >>= SIZE_BITS;
        dirZ >>= SIZE_BITS;
        dirX -= this.lastX;
        dirZ -= this.lastZ;
        return !(dirX < -NEXT || dirX > NEXT || dirZ < -NEXT || dirZ > NEXT);
    }

    /**
     * 
     */
    public void flagUsed() {
        nUse++;
    }

    /**
     * @return
     */
    public int getNumUses() {
        return nUse;
    }

//    public void getFlaggedBlocks(HashSet<Long> flaggedBlocks) {
//        for (int x = -NEXT; x < NEXT + 1; x++) {
//            for (int z = -NEXT; z < NEXT + 1; z++) {
//                int idx = (x + NEXT) * WIDTH + (z + NEXT);
//                int num = this.flaggedNumBlocks[idx];
//                if (num > 0) {
//                    BlockBoundingBox flagged = this.flaggedBlocks[idx];
//                    int cXBlock = (this.lastX+x)<<SIZE_BITS;
//                    int cZBlock = (this.lastZ+z)<<SIZE_BITS;
//                    for (int i = 0; i < num; i++) {
//                        short c = flagged[i];
//                        int bx = TripletShortHash.getX(c);
//                        int by = TripletShortHash.getY(c);
//                        int bz = TripletShortHash.getZ(c);
//                        long l = TripletLongHash.toHash(cXBlock|bx, by, cZBlock|bz);
//                        flaggedBlocks.add(l);
//                    }
//                }
//            }
//        }
//    }
}
