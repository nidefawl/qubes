package nidefawl.qubes;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Side;

public class BootClient {
    public static final String JNI_LIBRARY_NAME = System.getProperty("org.lwjgl.libname", System.getProperty("os.arch").contains("64") ? "lwjgl" : "lwjgl32");
    public static final String LIB_BIN          = "qubes";
    static File temp = null;
    public static File createTempDirectory() throws IOException {
        final File temp;

        temp = File.createTempFile("temp", "qubes");

        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return (temp);
    }


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

    public static void cleanup() throws IOException {
        System.out.println("shutdown");
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        sysloader.close();
        Stack<File> list = new Stack<>();
        if (temp != null) {
            File[] f2 = temp.listFiles();
            for (int i = 0; f2 != null && i < f2.length; i++) {
                list.push(f2[i]);
            }
            ArrayList<File> directories = new ArrayList<>();
            while (!list.isEmpty()) {
                File f = list.pop();
                if (f.isDirectory()) {
                    File[] f3 = f.listFiles();
                    for (int i = 0; f3 != null && i < f3.length; i++) {
                        list.push(f3[i]);
                    }
                    directories.add(f);
                } else if (f.isFile()) {
                    if (!f.delete()) {
                        System.err.println("Cannot delete "+f.getAbsolutePath());
                    }
                }
            }
            for (int i = directories.size()-1; i >= 0; i--) {
                File dir = directories.get(i);
                if (!dir.delete()) {
                    System.err.println("Cannot delete "+dir.getAbsolutePath());
                }
            }
        }
    }

    public static void addLibsToClasspath(File libs) throws Exception {

        {
            if (!libs.isDirectory()) {
                System.err.println("libs folder not found");
                System.exit(1);
                return;
            }
            ArrayList<URL> list = new ArrayList<URL>();
            File[] fList = libs.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".jar");
                }
            });
            if (fList == null || fList.length <= 0) {
                System.err.println("libs folder is empty");
                System.exit(1);
                return;
            }
            System.out.println("Loading " + fList.length + " libraries!");
            for (int i = 0; i < fList.length; i++) {
                File f = fList[i];
                list.add(f.toURI().toURL());
            }
            addURLs(list.toArray(new URL[list.size()]));
        }
    }
    private static void loadDeps() {
        try {
            temp = createTempDirectory();
            System.out.println("Using temporary directory "+temp.getAbsolutePath());
            System.setProperty("org.lwjgl.librarypath", new File(temp, "natives").getAbsolutePath());
            InputStream in = BootClient.class.getResourceAsStream("/game_deps.jar");
            ZipInputStream zipIn = null;
            try {
                zipIn = new ZipInputStream(in);
                ZipEntry entry = zipIn.getNextEntry();
                while (entry != null) {
                    if (!entry.isDirectory()) {
                        File outFile = new File(temp, entry.getName());
                        if (outFile.exists() && !outFile.canWrite()) {
                            System.err.println("File "+outFile+" in use, skipping...");
                        } else {
                            outFile.getParentFile().mkdirs();
                            OutputStream os = null;
                            try {
                                os = new FileOutputStream(outFile);
                                os = new BufferedOutputStream(os);
                                byte[] bytesIn = new byte[4096];
                                int read = 0;
                                while ((read = zipIn.read(bytesIn)) != -1) {
                                    os.write(bytesIn, 0, read);
                                }
                                os.flush();
                            } finally {
                                if (os != null) {
                                    os.close();
                                }
                            }
                        }
                    }
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            } finally {
                if (zipIn != null) {
                    zipIn.close();
                }
            }
            addLibsToClasspath(temp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load required DLL", e);
        }

    }

    public static void main(String[] args) {
        try {
            System.loadLibrary(JNI_LIBRARY_NAME);
            System.out.println("Found lwjgl");
        } catch (UnsatisfiedLinkError e) {
            try {
                loadDeps();
                WorkingEnv.setClassPathAssets();
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cleanup();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }));
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        }
        try {
            WorkingEnv.init(Side.CLIENT, ".");
        } catch (Exception e) {
            System.err.println("Failed starting game");
            e.printStackTrace();
        }

        String name = "me" + (GameMath.randomI(System.currentTimeMillis()));
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && args[i].length() > 1) {
                if (i + 1 < args.length) {
                    if (args[i].substring(1).equalsIgnoreCase("name")) {
                        name = args[i + 1];
                    }
                }
            }
        }
        GameBase.appName = "-";
        Game.instance = new Game();
        Game.instance.getProfile().setName(name);
        ErrorHandler.setHandler(Game.instance);
        Game.instance.startGame();
        GameContext.setMainThread(Game.instance.getMainThread());
    }
}
