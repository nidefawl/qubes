/**
 * 
 */
package nidefawl.qubes.util;

import java.nio.Buffer;


/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class UnsafeHelper {
    private static final long            ADDRESS;
    private static final long            CAPACITY;
    private static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            Class.forName("sun.misc.Unsafe");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("sun.misc.Unsafe not present, required for operation");
        }
        UNSAFE = getUnsafeInstance();
        if (UNSAFE == null) {
            throw new RuntimeException("sun.misc.Unsafe.getUnsafe() == null, required for operation");
        }
        ADDRESS = UNSAFE.objectFieldOffset(getDeclaredField(Buffer.class, "address"));
        CAPACITY = UNSAFE.objectFieldOffset(getDeclaredField(Buffer.class, "capacity"));
        if (CAPACITY == 0 || ADDRESS == 0) {
            throw new RuntimeException("Did not find required sun.misc.Unsafe fields");
        }
    }
    private static sun.misc.Unsafe getUnsafeInstance() {
        java.lang.reflect.Field[] fields = sun.misc.Unsafe.class.getDeclaredFields();

        /*
        Different runtimes use different names for the Unsafe singleton,
        so we cannot use .getDeclaredField and we scan instead. For example:

        Oracle: theUnsafe
        PERC : m_unsafe_instance
        Android: THE_ONE
        */
        for ( java.lang.reflect.Field field : fields ) {
            if ( !field.getType().equals(sun.misc.Unsafe.class) )
                continue;

            int modifiers = field.getModifiers();
            if ( !(java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers)) )
                continue;

            field.setAccessible(true);
            try {
                return (sun.misc.Unsafe)field.get(null);
            } catch (IllegalAccessException e) {
                // ignore
            }
            break;
        }

        throw new UnsupportedOperationException();
    }

    static java.lang.reflect.Field getDeclaredField(Class<?> root, String fieldName){
        Class<?> type = root;

        do {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        } while (type != null);

        return null;
    }

    public static long alloc(long size) {
        return UNSAFE.allocateMemory(size);
    }
    public static void free(long addr) {
        UNSAFE.freeMemory(addr);
    }
    
    public static void copyDoubleArray(long addr, double[] out) {
        for (int i = 0; i < out.length; i++) {
            long off = addr+(i*8);
            out[i] = UNSAFE.getDouble(off);
        }
    }

}
