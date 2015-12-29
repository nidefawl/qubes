/**
 * 
 */
package nidefawl.qubes.gl;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class VertexBuffer {

    int[] buffer = new int[0];
    int[] triIdxBuffer = new int[0];
    int triIdxPos;
    int pos;
    int left = 0;
    int left2 = 0;
    public int vertexCount;
    public int faceCount;
    /**
     * @param i
     */
    public VertexBuffer(int i) {
        reset();
        realloc(i);
        int numQuads = (i/4);
        int numTris = numQuads*2;
        int numIdx = numTris*3;
        int len = ((numIdx>>2)+1)<<2;
        if (numIdx < 32) {
            len = 32;
        }
        reallocTriIdxBuffer(len);
    }

    /**
     * 
     */
    public void reset() {
        pos = 0;
        triIdxPos = 0;
        vertexCount = 0;
        faceCount = 0;
        left = this.buffer.length - this.pos;
        left2 = this.triIdxBuffer.length - this.triIdxPos;
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
        left = this.buffer.length - this.pos;
        return this.buffer.length;
    }
    /**
     * @param extraBufferLen
     * @return 
     */
    public int reallocTriIdxBuffer(int newSize) {
        if(triIdxBuffer.length!=0)
        System.out.println("realloc triIdxBuffer to length "+newSize);
        int newBuffer[] = new int[newSize];
        System.arraycopy(this.triIdxBuffer, 0, newBuffer, 0, this.triIdxBuffer.length);
        this.triIdxBuffer = newBuffer;
        left2 = this.triIdxBuffer.length - this.triIdxPos;
        return this.triIdxBuffer.length;
    }
    /**
     * @param floatToRawIntBits
     */
    public void put(int val) {
        this.buffer[this.pos++] = val;
        left--;
        if (left < 100) {
            int incr = this.buffer.length <= 1024 ? 512 : this.buffer.length>>1;
            int newSize = (this.buffer.length+incr);
            realloc(newSize);
        }
    }
    public void putIdx(int val) {
        this.triIdxBuffer[this.triIdxPos++] = val;
        left2--;
        if (left2 < 100) {
            int incr = this.triIdxBuffer.length <= 1024 ? 512 : this.triIdxBuffer.length>>1;
            int newSize = (this.triIdxBuffer.length+incr);
            reallocTriIdxBuffer(newSize);
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
        return this.pos;
    }
    /**
     * @return
     */
    public int getTriIdxPos() {
        return this.triIdxPos;
    }
    /**
     * @return
     */
    public int[] get() {
        return this.buffer;
    }
    /**
     * @return
     */
    public int[] getTriIdxBuffer() {
        return this.triIdxBuffer;
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
        vboIdxBuf.put(idx);
        return numIdx;
    }

    /**
     * @param vboBuf
     * @return 
     */
    public int putIn(ReallocIntBuffer vboBuf) {
        int intLen = this.pos;
        vboBuf.put(this.buffer, 0, intLen);
        return intLen;
    }

    public void incrIndex(int[] vertexIdx, int vIdxOut, int faces) {
        while (left2 < vIdxOut) {
            int incr = this.triIdxBuffer.length <= 1024 ? 512 : this.triIdxBuffer.length>>1;
            int newSize = (this.triIdxBuffer.length+incr);
            reallocTriIdxBuffer(newSize);
        }
        System.arraycopy(vertexIdx, 0, this.triIdxBuffer, this.triIdxPos, vIdxOut);
        this.triIdxPos += vIdxOut;
        this.faceCount += faces;
    }

    public void incVertCount(int vIdx) {
        this.vertexCount+=vIdx;
    }

    public void putTriIndex(int[] vertexIdx) {
        for (int i = 0; i < vertexIdx.length; i++) {
            int index = vertexIdx[i] + this.vertexCount;
            putIdx(index);
//            System.out.println(index);
        }
        increaseFace();
    }

}
