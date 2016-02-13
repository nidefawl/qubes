package nidefawl.qubes;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import nidefawl.qubes.server.StartServer;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;

@SideOnly(value = Side.SERVER)
public class BootServer {

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

    public static void main(String[] args) throws Exception {

        {
            File libs = new File("libs");
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
        StartServer.main(args);
    }
}
