/**
 * 
 */
package nidefawl.qubes.gl;


import java.nio.*;
import java.util.HashSet;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.jemalloc.JEmalloc;

import nidefawl.qubes.util.UnsafeHelper;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Memory {
    public static int mallocd = 0;
    final static boolean DEBUG_ALLOC = true;
    static HashSet<Long> ptrs = new HashSet<>();

    public static FloatBuffer createFloatBuffer(int i) {
        mallocd+=4*i;
        long __result = JEmalloc.nje_calloc(i, 4);
        if (DEBUG_ALLOC)
            ptrs.add(__result);
        ByteBuffer buf = UnsafeHelper.memByteBuffer(__result, i*4);
        return buf.asFloatBuffer();
    }
    
    public static IntBuffer createIntBuffer(int i) {
        mallocd+=4*i;
        long __result = JEmalloc.nje_calloc(i, 4);
        if (DEBUG_ALLOC)
            ptrs.add(__result);
        ByteBuffer buf = UnsafeHelper.memByteBuffer(__result, i*4);
        return buf.asIntBuffer();
    }
    
    public static ByteBuffer createByteBuffer(int i) {
        mallocd+=i;
        long __result = JEmalloc.nje_malloc(i);
        if (DEBUG_ALLOC)
            ptrs.add(__result);
        ByteBuffer buf = UnsafeHelper.memByteBuffer(__result, i);
        return buf;
    }
    
    public static DoubleBuffer createDoubleBuffer(int i) {
        mallocd+=8*i;
        long __result = JEmalloc.nje_calloc(i, 8);
        if (DEBUG_ALLOC)
            ptrs.add(__result);
        ByteBuffer buf = UnsafeHelper.memByteBuffer(__result, i*8);
        return buf.asDoubleBuffer();
    }

    public static FloatBuffer createFloatBufferAligned(int alignment, int len) {
        return createByteBufferAligned(alignment, len*4).asFloatBuffer();
    }

    /**
     * @param buffers 
     * @param i
     * @return
     */
    public static ByteBuffer createByteBufferAligned(int alignment, int len) {
        int alen = len / alignment;
        if (len % alignment != 0) {
            alen++;
        }
        len = alen * alignment;
        long ptr = JEmalloc.nje_aligned_alloc(alignment, len);
        if (DEBUG_ALLOC)
            ptrs.add(ptr);
        ByteBuffer buf = UnsafeHelper.memByteBuffer(ptr, len);
        mallocd += buf.capacity();
        return buf;
    }


    /**
     * @param len2 
     * @param i
     * @return
     */
    public static ByteBuffer reallocByteBufferAligned(ByteBuffer buf, int alignment, int len) {
        mallocd -= buf.capacity();
        long ptr = UnsafeHelper.memAddress0(buf);
        if (DEBUG_ALLOC)
            ptrs.add(ptr);
        return createByteBufferAligned(alignment, len);
    }

    /**
     * @param numColorTextures
     * @return
     */
    public static IntBuffer createIntBufferGC(int i) {
        ByteBuffer buf = BufferUtils.createByteBuffer(i*4);
        mallocd += buf.capacity();
        return buf.asIntBuffer();
    }

    /**
     * @param buf
     */
    public static void free(FloatBuffer buf) {
        mallocd -= buf.capacity()*4;
        long ptr = UnsafeHelper.memAddress0(buf);
        if (DEBUG_ALLOC) {
            if (!ptrs.remove(ptr)) {
                throw new IllegalArgumentException("Invalid buffer");
            }
        }
        JEmalloc.nje_free(ptr);
    }

    /**
     * @param buf
     */
    public static void free(IntBuffer buf) {
        mallocd -= buf.capacity()*4;
        long ptr = UnsafeHelper.memAddress0(buf);
        if (DEBUG_ALLOC) {
            if (!ptrs.remove(ptr)) {
                throw new IllegalArgumentException("Invalid buffer");
            }
        }
        JEmalloc.nje_free(ptr);
    }

    /**
     * @param buf
     */
    public static void free(ByteBuffer buf) {
        mallocd -= buf.capacity();
        long ptr = UnsafeHelper.memAddress0(buf);
        if (DEBUG_ALLOC) {
            if (!ptrs.remove(ptr)) {
                throw new IllegalArgumentException("Invalid buffer");
            }
        }
        JEmalloc.nje_free(ptr);
    }

    public static IntBuffer createIntBufferHeap(int i) {
        mallocd += i*4;
        return IntBuffer.allocate(i);
    }

    /**
     * @param i
     * @return
     */
    public static FloatBuffer createFloatBufferHeap(int i) {
        mallocd += i*4;
        return FloatBuffer.allocate(i);
    }

}
