/**
 * 
 */
package nidefawl.qubes.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ReallocIntBuffer {

    ByteBuffer buffers;
    IntBuffer intbuffers;
    
    /**
     * @param i
     */
    public ReallocIntBuffer(int i) {
        if (i > 0) {
            reallocBuffer(i);
        }
    }

    /**
     * 
     */
    public ReallocIntBuffer() {
    }

    public void reallocBuffer(int intLen) {
        intLen *= 4;
        if (buffers == null || buffers.capacity() < intLen) {
            if (intLen*2 < 2*1024*1024) {
                intLen = intLen*2;
            }
            if (buffers != null) {
                buffers = Memory.reallocByteBufferAligned(buffers, 64, intLen);
            } else {
                buffers = Memory.createByteBufferAligned(64, intLen);
            }
            intbuffers = buffers.asIntBuffer();
        }
    }
    public void release() {
        if (this.buffers != null) {
            Memory.free(this.buffers);
            this.buffers = null;
            this.intbuffers = null;
        }
    }

    /**
     * @param idx
     */
    public void put(int[] buffer) {
        put(buffer, 0, buffer.length);
    }

    /**
     * @param buffer
     * @param i
     * @param intLen
     */
    public void put(int[] buffer, int offset, int len) {
        reallocBuffer(offset+len);
        intbuffers.clear();
        intbuffers.put(buffer, offset, len);
        buffers.position(0).limit(len*4);
    }

    /**
     * @return
     */
    public ByteBuffer getByteBuf() {
        return this.buffers;
    }
}
