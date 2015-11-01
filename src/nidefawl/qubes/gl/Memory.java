/**
 * 
 */
package nidefawl.qubes.gl;

import static org.lwjgl.system.jemalloc.JEmalloc.je_calloc;
import static org.lwjgl.system.jemalloc.JEmalloc.je_free;
import static org.lwjgl.system.jemalloc.JEmalloc.je_malloc;

import java.nio.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Memory {
    public static int mallocd = 0;

    public static FloatBuffer createFloatBuffer(int i) {
        mallocd+=4*i;
        return je_calloc(i, 4).asFloatBuffer();
    }
    
    public static IntBuffer createIntBuffer(int i) {
        mallocd+=4*i;
        return je_calloc(i, 4).asIntBuffer();
    }
    
    public static ByteBuffer createByteBuffer(int i) {
        mallocd+=i;
        return je_malloc(i);
    }
    
    public static DoubleBuffer createDoubleBuffer(int i) {
        mallocd+=8*i;
        return je_calloc(i, 8).asDoubleBuffer();
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
        ByteBuffer buf = MemoryUtil.memAlignedAlloc(alignment, len);
        mallocd += buf.capacity();
        return buf;
    }


    /**
     * @param len2 
     * @param i
     * @return
     */
    public static ByteBuffer reallocByteBufferAligned(ByteBuffer buffers, int alignment, int len) {
        mallocd -= buffers.capacity();
        MemoryUtil.memAlignedFree(buffers);
        return createByteBufferAligned(alignment, len);
    }



    public static FloatBuffer createFloatBufferGC(int i) {
        ByteBuffer buf = BufferUtils.createAlignedByteBufferPage(i*4);
        mallocd += buf.capacity();
        return buf.asFloatBuffer();
    }
    /**
     * @param numColorTextures
     * @return
     */
    public static IntBuffer createIntBufferGC(int i) {
        ByteBuffer buf = BufferUtils.createAlignedByteBufferPage(i*4);
        mallocd += buf.capacity();
        return buf.asIntBuffer();
    }

    /**
     * @param buf
     */
    public static void free(FloatBuffer buf) {
        mallocd -= buf.capacity();
        MemoryUtil.memFree(buf);
    }

    /**
     * @param buf
     */
    public static void free(ByteBuffer buf) {
        mallocd -= buf.capacity();
        MemoryUtil.memFree(buf);
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
