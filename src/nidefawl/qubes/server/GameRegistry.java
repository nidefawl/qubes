/**
 * 
 */
package nidefawl.qubes.server;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;

import com.google.common.collect.Maps;

import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.*;

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
            throw new RuntimeException("Cannot create terrain generator "+gen, e);
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
            throw new RuntimeException("Cannot create terrain populator "+gen, e);
        }
    }
}
