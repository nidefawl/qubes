package nidefawl.qubes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nidefawl.qubes.util.CrashInfo;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class NativeInterface {
    final static NativeInterface instance = new NativeInterface();
    private static boolean isPresent = false;
    public static NativeInterface getInstance() {
        return instance;
    }
    public static void start(int appId) {
        try {
            Class s = NativeClassLoader.getInstance().loadClass("nidefawl.qubes.BootClient");
            Field f = s.getDeclaredField("appId");
            f.set(null, appId);  
            isPresent = true;
            Object o = s.newInstance();
            String[] args = new String[0];
            Method m = s.getDeclaredMethod("main", args.getClass());
            m.invoke(o, new Object[] {args});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean isPresent() {
        return isPresent;
    }
    NativeInterface() {
    }
    public native void gameCrashed(CrashInfo info);
    public native void gameAlive();
}
