/**
 * 
 */
package nidefawl.qubes.util;

import java.nio.*;

import org.lwjgl.system.Checks;


/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class UnsafeHelper {
    private static final long            ADDRESS;
    private static final long            CAPACITY;
    private static final sun.misc.Unsafe UNSAFE;
    protected static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());

    // These are all aligned instances and should only be set to naturally aligned memory addresses
    protected static final ShortBuffer  SHORT_BUFFER  = BYTE_BUFFER.asShortBuffer();
    protected static final CharBuffer   CHAR_BUFFER   = BYTE_BUFFER.asCharBuffer();
    protected static final IntBuffer    INT_BUFFER    = BYTE_BUFFER.asIntBuffer();
    protected static final LongBuffer   LONG_BUFFER   = BYTE_BUFFER.asLongBuffer();
    protected static final FloatBuffer  FLOAT_BUFFER  = BYTE_BUFFER.asFloatBuffer();
    protected static final DoubleBuffer DOUBLE_BUFFER = BYTE_BUFFER.asDoubleBuffer();

    private static final long
        PARENT_BYTE,
        PARENT_SHORT,
        PARENT_CHAR,
        PARENT_INT,
        PARENT_LONG,
        PARENT_FLOAT,
        PARENT_DOUBLE;


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

        ByteBuffer parent = BYTE_BUFFER;
        if ( !(parent instanceof sun.nio.ch.DirectBuffer) )
            throw new UnsupportedOperationException();
        try {
            PARENT_BYTE = UNSAFE.objectFieldOffset(getField(BYTE_BUFFER.slice(), parent));
            PARENT_SHORT = UNSAFE.objectFieldOffset(getField(SHORT_BUFFER, parent));
            PARENT_CHAR = UNSAFE.objectFieldOffset(getField(CHAR_BUFFER, parent));
            PARENT_INT = UNSAFE.objectFieldOffset(getField(INT_BUFFER, parent));
            PARENT_LONG = UNSAFE.objectFieldOffset(getField(LONG_BUFFER, parent));
            PARENT_FLOAT = UNSAFE.objectFieldOffset(getField(FLOAT_BUFFER, parent));
            PARENT_DOUBLE = UNSAFE.objectFieldOffset(getField(DOUBLE_BUFFER, parent));
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    static java.lang.reflect.Field getField(Buffer buffer, Object value) throws NoSuchFieldException {
        Class<?> type = buffer.getClass();

        do {
            for ( java.lang.reflect.Field field : type.getDeclaredFields() ) {
                if ( java.lang.reflect.Modifier.isStatic(field.getModifiers()) )
                    continue;

                if ( !field.getType().isAssignableFrom(value.getClass()) )
                    continue;

                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(buffer);
                    if ( fieldValue == value )
                        return field;
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }

            type = type.getSuperclass();
        } while ( type != null );

        throw new NoSuchFieldException(String.format(
            "The specified value does not exist as a field in %s or any of its superclasses.",
            buffer.getClass().getSimpleName()
        ));
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
    public final static ByteBuffer memByteBuffer(long address, int capacity) {
        try {
            ByteBuffer buffer = (ByteBuffer)UNSAFE.allocateInstance(BYTE_BUFFER.getClass());
            buffer.order(ByteOrder.nativeOrder());
            return memSetupBuffer(buffer, address, capacity);
        } catch (InstantiationException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    public static long memAddress0(Buffer buffer) {
        return UNSAFE.getLong(buffer, ADDRESS);
    }

    private static <T extends Buffer> T setup(T buffer, long address, int capacity, long parentField) {
        UNSAFE.putLong(buffer, ADDRESS, address);
        UNSAFE.putInt(buffer, CAPACITY, capacity);

        UNSAFE.putObject(buffer, parentField, null);

        buffer.clear();
        return buffer;
    }

    public static ByteBuffer memSetupBuffer(ByteBuffer buffer, long address, int capacity) {
        // If we allowed this, the ByteBuffer's malloc'ed memory might never be freed.
        if ( Checks.DEBUG && ((sun.nio.ch.DirectBuffer)buffer).cleaner() != null )
            throw new IllegalArgumentException("Instances created through ByteBuffer.allocateDirect cannot be modified.");

        return setup(buffer, address, capacity, PARENT_BYTE);
    }
    public static void copyDoubleArray(long addr, double[] out) {
        for (int i = 0; i < out.length; i++) {
            long off = addr+(i*8);
            out[i] = UNSAFE.getDouble(off);
        }
    }
    public static long getLong(long addr) {
        return UNSAFE.getLong(addr);
    }

}
