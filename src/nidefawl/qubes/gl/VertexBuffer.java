/**
 * 
 */
package nidefawl.qubes.gl;

import java.nio.IntBuffer;

import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.render.region.RegionRenderer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class VertexBuffer {

    int[] buffer = new int[0];
    int index;
    int left = 0;
    public int vertexCount;
    public int faceCount;
    /**
     * @param i
     */
    public VertexBuffer(int i) {
        reset();
        realloc(i);
    }

    /**
     * 
     */
    public void reset() {
        index = 0;
        vertexCount = 0;
        faceCount = 0;
        left = this.buffer.length - this.index;
    }
    
    /**
     * @param extraBufferLen
     * @return 
     */
    public int realloc(int newSize) {
        if(buffer.length!=0)
        System.out.println("realloc buffer to length "+newSize);
        int newBuffer[] = new int[newSize];
        System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.length);
        this.buffer = newBuffer;
        left = this.buffer.length - this.index;
        return this.buffer.length;
    }
    /**
     * @param floatToRawIntBits
     */
    public void put(int val) {
        this.buffer[this.index++] = val;
        left--;
        if (left < 100) {
            int incr = this.buffer.length <= 1024 ? 512 : this.buffer.length>>1;
            int newSize = (this.buffer.length+incr);
            realloc(newSize);
        }
    }
    /**
     * 
     */
    public void increaseVert() {
        this.vertexCount++;
    }
    /**
     * 
     */
    public void increaseFace() {
        this.faceCount++;
    }
    /**
     * @return
     */
    public int getVertexCount() {
        return this.vertexCount;
    }
    /**
     * @return
     */
    public int getFaceCount() {
        return this.faceCount;
    }
    /**
     * @return
     */
    public int getIndex() {
        return this.index;
    }
    /**
     * @return
     */
    public int[] get() {
        return this.buffer;
    }

    /**
     * @param i
     * @param vboIdxBuf
     * @return
     */
    public static int createIndex(int elementCount, ReallocIntBuffer vboIdxBuf) {
        int numTriangles = elementCount;
        int numQuads = numTriangles/2;
        int numIdx = numQuads*6;
        int[] idx = new int[numIdx];
        int nTriangleIdx = 0;
        for (int i = 0; i < numQuads; i++) {
            int vIdx = i*4;
            idx[nTriangleIdx++] = vIdx+0;
            idx[nTriangleIdx++] = vIdx+1;
            idx[nTriangleIdx++] = vIdx+2;
            idx[nTriangleIdx++] = vIdx+2;
            idx[nTriangleIdx++] = vIdx+3;
            idx[nTriangleIdx++] = vIdx+0;
        }
        vboIdxBuf.reallocBuffer(numIdx);
        vboIdxBuf.put(idx);
        return numIdx;
    }

    /**
     * @param vboBuf
     * @return 
     */
    public int putIn(ReallocIntBuffer vboBuf) {
        int intLen = this.index;
        vboBuf.reallocBuffer(intLen);
        vboBuf.put(this.buffer, 0, intLen);
        return intLen;
    }
}
