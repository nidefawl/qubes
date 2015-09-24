/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TreeGen1 implements IWorldGen {

    private int log;
    private int leaves;
    /**
     * 
     */
    public TreeGen1() {
        this.log = Block.log_oak.id;
        this.leaves = Block.leaves_oak.id;
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.worldgen.populator.IPopulator#generate(nidefawl.qubes.worldgen.populator.IBlockWorld)
     */
    @Override
    public boolean generate(IBlockWorld c, int x, int y, int z, Random rand) {
        int type = c.getType(x, y, z);
        if (!isSoil(type)) {
            return false;
        }
        y++;
        int height = rand.nextInt(3)+5;
        boolean high = rand.nextInt(5)==0;
        if (high) {
            height+=5+rand.nextInt(3);
        }
        for (int y2 = 0; y2 < height-3+1; y2++) {
            int type2 = c.getType(x, y+y2, z);
            if (!canReplace(type2)) {
                return false;
            }
        }
        for (int y2 = height-3; y2 < height+1; y2++) {
            if (!canReplaceAll(c, x, y+y2, z, -2, 2)) {
//                System.out.println("FAIL3");
                return false;
            }
        }
        int flags = Flags.MARK;
        for (int y2 = 0; y2 <= height-3; y2++) {
            c.setType(x, y+y2, z, this.log, flags);
        }
        if (high) {
            int i = -2;
            int j = 2;
            for (int x2 = i; x2 < j+1; x2++) {
                for (int z2 = i; z2 < j+1; z2++) {
                    if ((x2 == i || x2 == j) && (z2 == i || z2 ==j)) continue;
                    for (int y2 = height-5; y2 < height-2; y2++) {
                        c.setType(x+x2, y+y2, z+z2, this.leaves, flags);
                    }
                }
            }
             i = -1;
             j = 1;
             for (int x2 = i; x2 < j+1; x2++) {
                 for (int z2 = i; z2 < j+1; z2++) {
                     c.setType(x+x2, y+height-2, z+z2, this.leaves, flags);
                     c.setType(x+x2, y+height-6, z+z2, this.leaves, flags);
                 }
             }
             for (int x2 = i; x2 < j+1; x2++) {
                 for (int z2 = i; z2 < j+1; z2++) {
                     if ((x2 == i || x2 == j) && (z2 == i || z2 ==j)) continue;
                     c.setType(x+x2, y+height-1, z+z2, this.leaves, flags);
                 }
             }
             c.setType(x, y+height-0, z, this.leaves, flags);
        } else {
            int i = -1;
            int j = 1;
            for (int x2 = i; x2 < j+1; x2++) {
                for (int z2 = i; z2 < j+1; z2++) {
                    c.setType(x+x2, y+height-2, z+z2, this.leaves, flags);
                }
            }
            if (rand.nextInt(3) == 0) {
                for (int x2 = i; x2 < j+1; x2++) {
                    for (int z2 = i; z2 < j+1; z2++) {
                        c.setType(x+x2, y+height-1, z+z2, this.leaves, flags);
                    }
                }
            } else {
                c.setType(x-1, y+height-1, z, this.leaves, flags);
                c.setType(x+1, y+height-1, z, this.leaves, flags);
                c.setType(x, y+height-1, z-1, this.leaves, flags);
                c.setType(x, y+height-1, z+1, this.leaves, flags);
            }
            c.setType(x, y+height-0, z, this.leaves, flags);
        }
        return true; 
    }

    /**
     * @param c
     * @param x
     * @param y2
     * @param z
     * @param i
     * @param j
     * @return 
     */
    private boolean canReplaceAll(IBlockWorld c, int x, int y2, int z, int i, int j) {
        for (int x2 = i; x2 < j+1; x2++) {
            for (int z2 = i; z2 < j+1; z2++) {
                int type = c.getType(x+x2, y2, z+z2);
                if (!canReplace(type))
                    return false;
            }
        }
        return true;
    }

    /**
     * @param type2
     * @return
     */
    private boolean canReplace(int type) {
        return type == 0;
    }

    public boolean isSoil(int type) {
        return type == Block.dirt.id || type == Block.grass.id;
    }

}
