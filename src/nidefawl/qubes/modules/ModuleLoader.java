/**
 * 
 */
package nidefawl.qubes.modules;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.SubclassMatchProcessor;
import nidefawl.qubes.event.Events;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.GameError;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ModuleLoader {
    final static Set<Module> modules = Sets.newHashSet();
    static Module[] modulesArray;

    public static void addURLs(URL...u) throws IOException {
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

    public static void scanModules(File file) {
        if (file.isDirectory()) {
            ArrayList<URL> list = new ArrayList<URL>();
            File[] fList = file.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".jar");
                }
            });
            if (fList == null || fList.length <= 0) {
                System.err.println("modules directory is empty");
                return;
            }
            for (int i = 0; i < fList.length; i++) {
                File f = fList[i];
                try {
                    list.add(f.toURI().toURL());
                } catch (Exception e) {
                    throw new GameError("Failed loading module file "+f, e);
                }
            }
            try {
                addURLs(list.toArray(new URL[list.size()]));
            } catch (Exception e) {
                throw new GameError("Failed loading modules", e);
            }
        }
        scanModules();
        
    }
    static void scanModules() {
        final Set<Class<? extends Module>> module = Sets.newHashSet();
        FastClasspathScanner scanner = new FastClasspathScanner();
        scanner.matchSubclassesOf(Module.class, new SubclassMatchProcessor<Module>() {
            @Override
            public void processMatch(Class<? extends Module> arg0) {
                module.add(arg0);
            }
        });
        scanner.scan();

        for (Class<? extends Module> e : module) {
            try {
                Constructor<? extends Module> cstr = e.getConstructor();
                Module m = cstr.newInstance();
                modules.add(m);
            } catch (Exception e2) {
                throw new GameError("Failed loading "+e+": "+e2.getMessage(), e2);
            }
        }
    }
    public static void loadModules() {
        for (Module m : modules) {
            try {
                System.out.println("Loading module "+m.getModuleName());
                m.onLoad(GameContext.getSide());
            } catch (Exception e) {
                throw new GameError("Failed loading module "+m.getModuleName(), e);
            }
        }
        modulesArray = modules.toArray(new Module[modules.size()]);
    }

    public static String getFinalStaticField(Class<?> klass, String fName) {
        Field[] fields = klass.getDeclaredFields();
        for (int i = 0; fields != null && i < fields.length; i++) {
            Field field = fields[i];
            if (!field.getType().equals(String.class))
                continue;
            int modifiers = field.getModifiers();
            if (!(java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers)))
                continue;
            if (field.getName().equals(fName)) {
                field.setAccessible(true);
                try {
                    return (String) field.get(null);
                } catch (Exception e) {
                    throw new GameError("Failed getting final static String field from class " + klass, e);
                }
            }
        }
        return null;
    }

    /**
     * @return
     */
    public static Module[] getModulesArray() {
        return modulesArray;
    }
}
