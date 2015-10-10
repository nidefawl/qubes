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
    int index;
    int left = 0;
    int vertexCount;
    int faceCount;
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
}
