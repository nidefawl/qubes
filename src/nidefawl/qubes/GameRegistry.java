/**
 * 
 */
package nidefawl.qubes;

import java.lang.reflect.Constructor;
import java.util.Map;

import com.google.common.collect.Maps;

import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.*;
import nidefawl.qubes.worldgen.biome.IBiomeManager;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;

/** 
 * This class is used to register terrain generators and other "pluggable" stuff
 * 
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class GameRegistry {

    final static Map<String, Class<? extends ITerrainGen>> terrainGenerators = Maps.newHashMap();
    final static Map<String, Class<? extends IChunkPopulator>> terrainPopulators = Maps.newHashMap();

    /**
     * @param worldServer
     * @param settings
     * @return
     */
    public static ITerrainGen newGenerator(WorldServer worldServer, WorldSettings settings) {
        String gen = settings.getString("generator", "terrain");
        Class<? extends ITerrainGen> genKlass = terrainGenerators.get(gen);
        try {
            Constructor<? extends ITerrainGen> cstr = genKlass.getDeclaredConstructor(WorldServer.class, long.class, WorldSettings.class);
            ITerrainGen igen = cstr.newInstance(worldServer, settings.getSeed(), settings);
            return igen;
        } catch (Exception e) {
            throw new GameError("Cannot create terrain generator "+gen, e);
        }
    }

    public static IBiomeManager newBiomeManager(WorldServer worldServer, ITerrainGen generator, WorldSettings settings) {
        Class<? extends IBiomeManager> genKlass = generator.getBiomeManager();
        if (genKlass == null) {
            throw new IllegalArgumentException("Please define a biomemanager for terrain gen "+generator);
        }
        try {
            Constructor<? extends IBiomeManager> cstr = genKlass.getDeclaredConstructor(World.class, long.class, IWorldSettings.class);
            IBiomeManager igen = cstr.newInstance(worldServer, settings.getSeed(), settings);
            return igen;
        } catch (Exception e) {
            throw new GameError("Cannot create terrain populator "+genKlass, e);
        }
    }

    public static IChunkPopulator newPopulator(WorldServer worldServer, ITerrainGen generator, WorldSettings settings) {
        String gen = settings.getString("populator", null);
        Class<? extends IChunkPopulator> genKlass = generator.getPopulator();
        if (gen != null) {
            genKlass = terrainPopulators.get(gen);
            if (genKlass == null) {
                throw new IllegalArgumentException("No such terrain populator "+gen);
            }
        }
        if (genKlass == null) {
            throw new IllegalArgumentException("Please define a terrain populator for world "+worldServer.getName());
        }
        try {
            Constructor<? extends IChunkPopulator> cstr = genKlass.getDeclaredConstructor(WorldServer.class, long.class, WorldSettings.class);
            IChunkPopulator igen = cstr.newInstance(worldServer, settings.getSeed(), settings);
            return igen;
        } catch (Exception e) {
            throw new GameError("Cannot create terrain populator "+gen, e);
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
