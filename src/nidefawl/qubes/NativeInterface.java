package nidefawl.qubes;

import java.lang.reflect.Method;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class NativeInterface {
    final static NativeInterface instance = new NativeInterface();
    public static NativeInterface getInstance() {
        return instance;
    }
    NativeInterface() {
    }
    public native void gameAlive();
    public static void start() {
        try {
            Class s = NativeClassLoader.getInstance().loadClass("nidefawl.qubes.BootClient");
            Object o = s.newInstance();
            String[] args = new String[0];
            Method m = s.getDeclaredMethod("main", args.getClass());
            m.invoke(o, new Object[] {args});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
