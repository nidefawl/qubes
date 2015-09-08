package nidefawl.qubes.blocklight;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TripletLongHash;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.WorldServer;

public class BlockLightUpdate {

    final static int STACK_SIZE = 1024*128;
    final long[] stackRemove = new long[STACK_SIZE];
    final long[] stackAdd = new long[STACK_SIZE];
    final int[] stackRemoveLight = new int[STACK_SIZE]; // TODO: combine in single stack, i guess
    public int numBlocksUpdate;

    public BlockLightUpdate() {
    }


    public void updateChunk(LightChunkCache cache, int cx, int cz, int type) {
//        this.
        Chunk c = cache.get(cx, cz);
        cx <<= Chunk.SIZE_BITS;
        cz <<= Chunk.SIZE_BITS;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int x2 = x + cx;
                int z2 = z + cz;
                int y = c.getHeightMap(x, z);
                updateBlock(cache, x2, y, z2, type);
                for (int y3 = cache.worldHeightMin1; y3 > 0; y3--) {
                    for (int i = 0; i < 4; i++) {
                        int bX = Dir.getDirX(i) + x2;
                        int bZ = Dir.getDirZ(i) + z2;
                        if (cache.isTransparent(bX, y3, bZ) && !cache.canSeeSky(bX, y3, bZ)) {
                            updateBlock(cache, bX, y3, bZ, type);
                        }
                    }
                }
            }
        }
    }



    public void updateBlock(LightChunkCache cache, int x, int y, int z, int type) {
        int idxRemove = 0;
        int idxAdd = 0;
        System.out.println(type);
//        qRemove.clear();
//        q2.clear();
//        qAdd.clear();
        int current = cache.getLight(x, y, z, type);
        int newLevel = getNewLightLevel(cache, x, y, z, type);
        long[] stackRem = this.stackRemove;
        int[] stackRemLight = this.stackRemoveLight;
        long[] stackAdd = this.stackAdd;
        
        if (current > newLevel) {
            stackRem[idxRemove] = TripletLongHash.toHash(x, y, z);
            stackRemLight[idxRemove] = current;
            idxRemove++;
        } else if (current < newLevel) {
            stackAdd[idxAdd++] = TripletLongHash.toHash(x, y, z);
        } else {
        }
        setLight(cache, x, y, z, type, newLevel, 0);

        int max = 4 + 32 * 4;
        int i1 = 0;
        while (i1 < idxRemove) {
            long l = stackRem[i1];
            int lvl = stackRemLight[i1];
            i1++;
            int x2 = TripletLongHash.getX(l);
            int y2 = TripletLongHash.getY(l);
            int z2 = TripletLongHash.getZ(l);
            for (int s = 0; s < 6; s++) {
                int dX = Dir.getDirX(s);
                int dY = Dir.getDirY(s);
                int dZ = Dir.getDirZ(s);
                int dirX = x2 + dX;
                int dirY = y2 + dY;
                int dirZ = z2 + dZ;
                if (!cache.hasBlock(dirX, dirZ)) {
                    int dx = dirX-x;
                    int dy = dirY-y;
                    int dz = dirZ-z;
                    System.err.println("reached cache boundaries drain, d: "+dx+","+dy+","+dz);
                    continue;
                }
                int dist = GameMath.distSq3Di(x2, y2, z2, x, y, z);
                if (dist < max && idxRemove < stackRem.length) {
                    int nLvl = cache.getLight(dirX, dirY, dirZ, type);
                    if (nLvl > 0 && nLvl < lvl) {
                        cache.setLight(dirX, dirY, dirZ, type, 0, 0);
                        stackRem[idxRemove] = TripletLongHash.toHash(dirX, dirY, dirZ);
                        stackRemLight[idxRemove] = nLvl;
                        idxRemove++;
//                        qRemove.push(TripletLongHash.toHash(dirX, dirY, dirZ));
//                        q2.push(nLvl);
                    } else if (nLvl >= lvl) {
//                        if (nLvl==15&&type==1&&!cache.canSeeSky(dirX, dirY, dirZ)) {
//                            System.out.println("skip fully lit block that is not sky block");
//                            continue;
//                        }
                        stackAdd[idxAdd++] = TripletLongHash.toHash(dirX, dirY, dirZ);
                    }
                }
            }
        }
        int i2 = 0;
        int prevLVL = 0;
        int lx = 0;
        int ly = 0;
        int lz = 0;
        while (i2 < idxAdd) {
            long l = stackAdd[i2++];
            int x2 = TripletLongHash.getX(l);
            int y2 = TripletLongHash.getY(l);
            int z2 = TripletLongHash.getZ(l);
            int lvl = cache.getLight(x2, y2, z2, type);
            for (int s = 0; s < 6; s++) {
                int dirX = Dir.getDirX(s);
                int dirY = Dir.getDirY(s);
                int dirZ = Dir.getDirZ(s);
                newLevel = type == 1 && dirY < 0 && lvl == 0xF ? lvl : lvl - 1;
                dirX += x2;
                dirY += y2;
                dirZ += z2;
                if (!cache.hasBlock(dirX, dirZ)) {
                    int dx = dirX-x;
                    int dy = dirY-y;
                    int dz = dirZ-z;
                    System.err.println("reached cache boundaries fill, d: "+dx+","+dy+","+dz+" - "+lvl);
                    System.err.println(dirX+","+dirY+","+dirZ);
                    System.err.println(lx+","+ly+","+lz+" - "+prevLVL);
                    continue;
                }
                if (dirY > 0 && dirY < cache.worldHeightMin1) {
                    if (cache.isTransparent(dirX, dirY, dirZ)) {
                        int lvl1 = cache.getLight(dirX, dirY, dirZ, type);
                        if (lvl1 + 2 <= lvl) {
//                            if (lvl1==15&&type==1&&!cache.canSeeSky(dirX, dirY, dirZ)) {
//                                System.out.println("skip fully lit block that is not sky block");
//                                continue;
//                            }
                            setLight(cache, dirX, dirY, dirZ, type, newLevel, 0);
                            int dist = GameMath.distSq3Di(dirX, dirY, dirZ, x, y, z);
                            if (dist < max && idxAdd < stackAdd.length) {
                                lx = dirX;
                                ly= dirY;
                                lz = dirZ;
                                stackAdd[idxAdd++] = TripletLongHash.toHash(dirX, dirY, dirZ);
                                prevLVL = lvl1;
                            }
                        }
                    }
                }
            }
        }

    }

    private void setLight(LightChunkCache cache, int x, int y, int z, int type, int value, int flags) {
        cache.setLight(x, y, z, type, value, flags);
        numBlocksUpdate++;
    }


    private int getNewLightLevel(LightChunkCache cache, int i, int j, int k, int type) {
        int id = cache.getTypeId(i, j, k);
        int lvl = 0;
        if (type == 0) {
            lvl = id != 0 ? Block.block[id].getLightValue() : 0;
        }
        if (id != 0 && !Block.block[id].isTransparent()) {
            return lvl;
        }
        int h = cache.getHeight(i, k);
        if (type == 1 && j + 1 >= h) {
            return 0xF;
        }
        
        for (int s = 0; s < 6; s++) {
            int dirX = Dir.getDirX(s) + i;
            int dirY = Dir.getDirY(s) + j;
            int dirZ = Dir.getDirZ(s) + k;
            int lvl2 = cache.getLight(dirX, dirY, dirZ, type) - 1;
            if (lvl2 > lvl)
                lvl = lvl2;

        }
        return lvl;
    }
}
