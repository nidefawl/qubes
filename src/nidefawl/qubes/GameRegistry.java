/**
 * 
 */
package nidefawl.qubes;

import java.lang.reflect.Constructor;
import java.util.Map;

import com.google.common.collect.Maps;

import nidefawl.qubes.util.*;
import nidefawl.qubes.world.*;
import nidefawl.qubes.world.biomes.IBiomeManager;
import nidefawl.qubes.worldgen.WorldGenInit;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;

/** 
 * This class is used to register terrain generators and other "pluggable" stuff
 * 
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */

@SideOnly(value=Side.SERVER)
public class GameRegistry {

    final static Map<String, Class<? extends ITerrainGen>> terrainGenerators = Maps.newHashMap();
    final static Map<String, Class<? extends IChunkPopulator>> terrainPopulators = Maps.newHashMap();

    /**
     * @param worldServer
     * @param settings
     * @return
     */
    public static WorldGenInit newGenerator(WorldServer worldServer, WorldSettings settings) {
        String gen = settings.getString("generator", "terrain");
        Class<? extends ITerrainGen> genKlass = terrainGenerators.get(gen);
        if (genKlass == null) {
            throw new IllegalArgumentException("Undefined terraingenerator "+gen);
        }
        try {
            Constructor<? extends ITerrainGen> cstr = genKlass.getDeclaredConstructor(WorldServer.class, long.class, WorldSettings.class);
            ITerrainGen igen = cstr.newInstance(worldServer, settings.getSeed(), settings);
            return igen.getWorldGen(worldServer, settings.getSeed(), settings);
        } catch (Exception e) {
            throw new GameError("Cannot create terrain generator "+gen, e);
        }
    }

    /**
     * @param name
     * @return
     */
    public static Class<? extends ITerrainGen> getTerrainGen(String name) {
        return terrainGenerators.get(name);
    }

    /**
     * @param name
     * @return
     */
    public static Class<? extends IChunkPopulator> getTerrainPop(String name) {
        return terrainPopulators.get(name);
    }
    
    public static void registerChunkPopulator(String name, Class<? extends IChunkPopulator> implementingClass) {
        Class<? extends IChunkPopulator> alreadyDef = GameRegistry.getTerrainPop(name);
        if (alreadyDef != null) {
            throw new GameError("Generator with name '" + name + "' already defined (" + alreadyDef + ")");
        }
        terrainPopulators.put(name, implementingClass);
    }
    public static void registerTerrainGenerator(String name, Class<? extends ITerrainGen> implementingClass) {
        Class<? extends ITerrainGen> alreadyDef = GameRegistry.getTerrainGen(name);
        if (alreadyDef != null) {
            throw new GameError("Generator with name '"+name+"' already defined ("+alreadyDef+")");
        }
        terrainGenerators.put(name, implementingClass);
     
    }
}
