package nidefawl.qubes;

import java.lang.reflect.Method;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class NativeClassLoader extends ClassLoader {
    /**
     * 
     */
    public NativeClassLoader() {
        super();
    }
    native byte[] cppLoadClass ( String name );
    native byte[] cppLoadResource ( String name );
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("nidefawl")) {
//            System.out.println(getClass().getSimpleName()+": "+name);
            byte[] classData = cppLoadClass(name);
            if (classData != null) {
                return (defineClass(name, classData, 0, classData.length));
            }
        }
        return super.findClass(name);
    }
    public static byte[] loadGameResource(String name) {
        byte[] data = instance == null ? null : instance.cppLoadResource(name);
      System.out.println("loadGameResource: "+name+" = "+(data == null ? "null" : "[b len "+data.length));
        return data;
    }
    private static NativeClassLoader instance;
    public static void start() {
        try {
            instance = new NativeClassLoader();
            System.out.println("Triggered class loader");
            Thread.currentThread().setContextClassLoader(instance);
            Class s = instance.loadClass("nidefawl.qubes.BootClient");
            Object o = s.newInstance();
            String[] args = new String[0];
            Method m = s.getDeclaredMethod("main", args.getClass());
            m.invoke(o, new Object[] {args});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
