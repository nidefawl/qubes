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
            int incr = 1<<23;//8Mb
            int newSize = (this.buffer.length+incr);
            realloc(newSize);
        }
    }
    public void putIdx(int val) {
        this.triIdxBuffer[this.triIdxPos++] = val;
        left2--;
        if (left2 < 100) {
            int incr = 1<<22;//4Mb
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
    public int getPos() {
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
     * @param directBuf
     * @return 
     */
    public int storeVertexData(ReallocIntBuffer directBuf) {
        directBuf.put(this.buffer, 0, this.pos);
        return this.pos;
    }
    /**
     * @param directBuf
     * @return 
     */
    public int storeIndexData(ReallocIntBuffer directBuf) {
        directBuf.put(this.triIdxBuffer, 0, this.triIdxPos);
        return this.triIdxPos;
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

    public void makeTriIdx() {
        for (int i = 0; i < this.faceCount; i++) {
            int vIdx = i*4;
            putIdx(vIdx+0);
            putIdx(vIdx+1);
            putIdx(vIdx+2);
            putIdx(vIdx+2);
            putIdx(vIdx+3);
            putIdx(vIdx+0);
        }
    }

}
