package nidefawl.qubes.worldgen.structure;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TripletLongHash;
import nidefawl.qubes.vec.AABBInt;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.ChunkPos;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.world.structure.mine.Mine;

public class MineGen extends StructureGen {

    final static int       r      = 3;
    final static int       l      = r * 2 + 1;
    private BlockPos       pos;
    private int[]          lvl;
    private int            height;
    private int            max;
    private int            min;
    private int            dir;
    private Random         rand;
    private int            chunkX;
    private int            chunkZ;
    public final Set<Long> blocks = new HashSet<>();

    private BlockPos       minePos;

    public MineGen() {
    }
    

    @Override
    public List<ChunkPos> prepare(WorldServer world, int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        List<ChunkPos> list = Lists.newLinkedList();
        for (int rx = -2; rx <= 2; rx++)
            for (int rz = -2; rz <= 2; rz++) {
                list.add(new ChunkPos(chunkX+rx, chunkZ+rz));
            }
        return list;
    }
    @Override
    public int generate(WorldServer world) {
        this.rand = new Random(chunkX*23914879234857L+chunkZ*2398L+234L);
        for (int attempts = 0; attempts < 128; attempts++) {
            int x = chunkX<<Chunk.SIZE_BITS|this.rand.nextInt(Chunk.SIZE);
            int z = chunkZ<<Chunk.SIZE_BITS|this.rand.nextInt(Chunk.SIZE);
            int h = world.getHeight(x, z);
            x+=-3+rand.nextInt(7);
            z+=-3+rand.nextInt(7);
            Block top = Block.get(world.getType(x, h+1, z));
            Block ground = Block.get(world.getType(x, h, z));
//            System.out.println(top+" - "+ground);
            if (top.isReplaceable() && !ground.isReplaceable()) {
                int lvl[] = new int[l*l];
                for (int x1 = -r; x1 <= r; x1++) {
                    int idxx = x1+r;
                    for (int z1 = -r; z1 <= r; z1++) {
                        int idxz = z1+r;
                        for (int y1 = -4; y1 <= 4; y1++) {
                            Block g = Block.get(world.getType(x+x1, h+y1, z+z1));
//                          System.out.println(""+x+","+z+" = "+g);
                            if (g.isReplaceable()) {
                                break;
                            }
                            int idx = idxz*l+idxx;
                            lvl[idx] = h+y1+1;
                        }
                    }
                }
                this.max = 0;
                this.min = world.worldHeight;
                for (int x1 = 0; x1 < l; x1++) {
                    for (int z1 = 0; z1 < l; z1++) {
                        int a = lvl[z1*l+x1];
                        min = Math.min(min, a);
                        max = Math.max(max, a);
                    }   
                }
                this.height = max-min;
                if (height < 5) {
                    int attempts2 = 0;
                    while (attempts2++ < 10) {
                        this.lvl = lvl;
                        this.pos = new BlockPos(x, h, z);
                        this.dir = this.rand.nextInt(4);
                        if (canBuild(world)) {
                            gen(world);
                            Mine m = getMine();
                            m.regen(world);
                            world.getHex(x, z).registerMine(m);
                            return 1;
                        }
                        x+=-3+rand.nextInt(7);
                        z+=-3+rand.nextInt(7);
                    }
                }
            }
        }
        return 0;
    }

    boolean canBuild(World world) {
        int x = pos.x;
        int z = pos.z;
        int under = 0;
        for (int x1 = 0; x1 < l; x1++) {
            for (int z1 = 0; z1 < l; z1++) {
                if (world.getHeight(x-r+x1, z-r+z1)>min+10) {
                    under++;
                }
            }
        }
        if (under < 10) {
            
            return false;
        }
        x = pos.x;
        z = pos.z;
        switch (dir) {
            case 0:
                x-=r+4;
                break;
            case 1:
                z-=r+4;
                break;
            case 2:
                x+=r+4;
                break;
            case 3:
                z+=r+4;
                break;
        }
        Block block = Block.get(world.getType(x, min+4, z));
        if (!block.isReplaceable()) {
            return false;
        }
        int ra = r+7;
        int len = ra*2+1;
        for (int a = 2; a < 8; a++) {
            int b = a+1;
            int y = min-a*3;
             x = pos.x;
             z = pos.z;
            switch (dir) {
                case 0:
                    x+=3*b;
                    break;
                case 1:
                    z+=3*b;
                    break;
                case 2:
                    x-=3*b;
                    break;
                case 3:
                    z-=3*b;
                    break;
            }
            for (int x1 = 0; x1 < len; x1++) {
                for (int z1 = 0; z1 < len; z1++) {
                    for (int y1 = 0; y1 < 10; y1++) {
                        block = Block.get(world.getType(x-ra+x1, y+y1, z-ra+z1));
                        if (block.isReplaceable()) {
                            return false;
                        }
                    }
                }
            }
            if (ra > 1) {
                ra--;
                len=ra*2+1;   
            }
        }
        return true;
    }
    
    public int gen(WorldServer world) {

        int nLow1=0;
        int nLow2=1;
        for (int x1 = 0; x1 < l; x1++) {
            for (int z1 = 0; z1 < l; z1++) {
                int a = lvl[z1*l+x1];
                if (a == min) {
                    nLow1++;
                } else if (a == min+1) {
                    nLow2++;
                }
            }   
        }
        int x = this.pos.x;
        int z = this.pos.z;
        HexBiome hex = world.getHex(x, z);
//        int ar
        Block air = Block.air;
        
        Block stone = Block.get(hex.biome.getStone(world, x, min, z, hex, rand));
        Block fill = air;
        if (nLow1<nLow2) {
            fill = stone;
        }
        for (int x1 = 0; x1 < l; x1++) {
            for (int z1 = 0; z1 < l; z1++) {
                world.setType(x-r+x1, min, z-r+z1, fill.id, Flags.MARK); 
                if (fill == Block.air) {
                    world.setType(x-r+x1, min-1, z-r+z1, stone.id, Flags.MARK); 
                }
            }
        }
        int ra = r+7;
        int len = ra*2+1;
        for (int a = 0; a < 8; a++) {
            int b = a+1;
            int y = min-a*3;
            x = pos.x;
            z = pos.z;
            switch (dir) {
                case 0:
                    x+=3*b;
                    break;
                case 1:
                    z+=3*b;
                    break;
                case 2:
                    x-=3*b;
                    break;
                case 3:
                    z-=3*b;
                    break;
            }
            for (int x1 = 0; x1 < len; x1++) {
                for (int z1 = 0; z1 < len; z1++) {
                    for (int y1 = 0; y1 < 9; y1++) {
                        int i = GameMath.distSq3Di(x1, y1, z1, ra, y1<4?y1:4, ra);
                        float d = i == 0 ? 0 : GameMath.sqrtf(i);
                        if (d+this.rand.nextFloat()*0.4 < Math.min(ra, 7)*0.8) {
                            world.setType(x-ra+x1, y+y1, z-ra+z1, stone.id, Flags.MARK);
                        }
                    }
                }
            }
            if (ra > 1) {
                ra--;
                len=ra*2+1;   
            }
        }
        x = pos.x;
        z = pos.z;
        switch (dir) {
            case 0:
                x-=r+4;
                break;
            case 1:
                z-=r+4;
                break;
            case 2:
                x+=r+4;
                break;
            case 3:
                z+=r+4;
                break;
        }
        int e = 1;
        int y = min+4;
        
        for (int a = -3; a < 20; a++) {
            int l = 0;
            int w = 0;
            switch (dir) {
                case 0:
                    x++;
                    l=e;
                    break;
                case 1:
                    z++;
                    w=e;
                    break;
                case 2:
                    x--;
                    l=e;
                    break;
                case 3:
                    z--;
                    w=e;
                    break;
            }
            if (a == 5) {
                e++;
            }
            if (a%3==2) {
                y--;
            }
            for (int x1 = -w; x1 <= w; x1++) {
                for (int z1 = -l; z1 <= l; z1++) {
                    for (int y1 = 0; y1 < 5; y1++) {
                        world.setType(x+x1, y+y1, z+z1, 0, Flags.MARK);
                        long la = TripletLongHash.toHash(x+x1, y+y1, z+z1);
                        if (a > 5) {
                            this.blocks.add(la);
                        }
                    }
                }
            }
            if (a == 19)
                this.minePos = new BlockPos(x, y, z);
        }
        return 0;
    }

    public Mine getMine() {
        if (this.blocks.isEmpty()) {
            return null;
        }
        Mine mine = new Mine();
        Iterator<Long> it = this.blocks.iterator();
        
        long[] blocks = new long[this.blocks.size()];
        int pos = 0;
        AABBInt treeBB = null;
        while (it.hasNext()) {
            long l = it.next();
            int x1 = TripletLongHash.getX(l);
            int y1 = TripletLongHash.getY(l);
            int z1 = TripletLongHash.getZ(l);
            if (treeBB == null) {
                treeBB = new AABBInt(x1, y1, z1, x1, y1, z1);
            } else {
                treeBB.minX = Math.min(x1, treeBB.minX);
                treeBB.minY = Math.min(y1, treeBB.minY);
                treeBB.minZ = Math.min(z1, treeBB.minZ);
                treeBB.maxX = Math.max(x1, treeBB.maxX);
                treeBB.maxY = Math.max(y1, treeBB.maxY);
                treeBB.maxZ = Math.max(z1, treeBB.maxZ);
            }
            blocks[pos++] = l;
        }
        
        mine.bb.set(treeBB);
        mine.setBlocks(blocks);
        mine.dir = this.dir;
        mine.pos = this.minePos;
        return mine;
    }
}
