/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.Random;

import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface IWorldGen {

     public boolean generate(IBlockWorld c, int x, int y, int z, Random rand);
}
