/**
 * 
 */
package nidefawl.qubes.server;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.InterfaceMatchProcessor;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.worldgen.populator.IChunkPopulator;
import nidefawl.qubes.worldgen.terrain.ITerrainGen;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class PluginLoader {

    public static void addURLs(URL[] u) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            for (int i = 0; i < u.length; i++) {
                method.invoke(sysloader, u[i]);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    public static void loadPlugins(File file) {
        if (!file.isDirectory()) {
            System.err.println("plugins folder not found");
            return;
        }
        ArrayList<URL> list = new ArrayList<URL>();
        File[] fList = file.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".jar");
            }
        });
        if (fList == null || fList.length <= 0) {
            System.err.println("plugins folder is empty");
            return;
        }
        System.out.println("Loading " + fList.length + " plugin"+(fList.length!=1?"s":"")+"!");
        for (int i = 0; i < fList.length; i++) {
            File f = fList[i];
            try {
                list.add(f.toURI().toURL());
            } catch (Exception e) {
                throw new GameError("Failed loading plugin file "+f, e);
            }
        }
        try {
            addURLs(list.toArray(new URL[list.size()]));
        } catch (Exception e) {
            throw new GameError("Failed loading plugins", e);
        }
        
    }
    public static String getFinalStaticField(Class<?> klass, String fName) {
        Field[] fields = klass.getDeclaredFields();
        for (int i = 0; fields != null && i < fields.length; i++) {
            Field field = fields[i];
            if ( !field.getType().equals(String.class) )
                continue;
            int modifiers = field.getModifiers();
            if ( !(java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers)) )
                continue;
            if (field.getName().equals(fName)) {
                field.setAccessible(true);
                try {
                    return (String) field.get(null);
                } catch (Exception e) {
                    throw new GameError("Failed getting final static String field from class "+klass, e);
                }
            }
        }
        return null;
    }
    public static void registerModules() {
        final Map<Class<?>, String> errors = Maps.newHashMap();
        FastClasspathScanner scanner = new FastClasspathScanner();
        scanner.matchClassesImplementing(ITerrainGen.class, new InterfaceMatchProcessor<ITerrainGen>() {
            @Override
            public void processMatch(Class<? extends ITerrainGen> implementingClass) {
                String name = getFinalStaticField(implementingClass, "GENERATOR_NAME");
                if (name == null) {
                    errors.put(implementingClass, "Did not find 'final static String GENERATOR_NAME'");
                    return;
                }
                Class<? extends ITerrainGen> alreadyDef = GameRegistry.terrainGenerators.get(name);
                if (alreadyDef != null) {
                    errors.put(implementingClass, "Generator with name '"+name+"' already defined ("+alreadyDef+")");
                    return;
                }
                GameRegistry.terrainGenerators.put(name, implementingClass);
            }
        }).matchClassesImplementing(IChunkPopulator.class, new InterfaceMatchProcessor<IChunkPopulator>() {
            @Override
            public void processMatch(Class<? extends IChunkPopulator> implementingClass) {
                String name = getFinalStaticField(implementingClass, "POPULATOR_NAME");
                if (name == null) {
                    errors.put(implementingClass, "Did not find 'final static String POPULATOR_NAME'");
                    return;
                }
                Class<? extends IChunkPopulator> alreadyDef = GameRegistry.terrainPopulators.get(name);
                if (alreadyDef != null) {
                    errors.put(implementingClass, "Generator with name '"+name+"' already defined ("+alreadyDef+")");
                    return;
                }
                GameRegistry.terrainPopulators.put(name, implementingClass);
            }
        });
        scanner.scan();
        for (Entry<Class<?>, String> e : errors.entrySet()) {
            System.err.println("Failed loading "+e.getKey()+": "+e.getValue());
        }
        if (!errors.isEmpty()) {
            throw new GameError("There were errors loading plugins, please fix them");
        }
    }

}
