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
        if (!name.startsWith("java")) {
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
//      System.out.println("loadGameResource: "+name+" = "+(data == null ? "null" : "[b len "+data.length));
        return data;
    }
    public static void setLoader() {
        try {
            System.out.println("Triggered class loader");
            instance = new NativeClassLoader();
            Thread.currentThread().setContextClassLoader(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static NativeClassLoader instance;
    /**
     * @return the instance
     */
    public static NativeClassLoader getInstance() {
        return instance;
    }
}
