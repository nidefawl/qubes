package nidefawl.qubes.gl;

import java.nio.FloatBuffer;

import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;

public class BufferedMatrix extends Matrix4f {
    
    FloatBuffer cur = Memory.createFloatBuffer(16);
    FloatBuffer last = Memory.createFloatBuffer(16);
    FloatBuffer curInv = Memory.createFloatBuffer(16);
    FloatBuffer lastInv = Memory.createFloatBuffer(16);
    public BufferedMatrix() {
        super();
        setIdentity();
        store(cur);
        store(last);
        last.rewind();
        cur.rewind();
        GameMath.invertMat4x(cur, curInv);
        GameMath.invertMat4x(last, lastInv);
        cur.rewind();
        last.rewind();
        curInv.rewind();
        lastInv.rewind();
    }
    
    public void update() {
        cur.rewind();
        last.rewind();
        curInv.rewind();
        lastInv.rewind();
        last.put(cur);
        lastInv.put(curInv);
        cur.rewind();
        last.rewind();
        curInv.rewind();
        lastInv.rewind();
        store(cur);
        cur.rewind();
        GameMath.invertMat4x(cur, curInv);
        cur.rewind();
        curInv.rewind();
    }

    public FloatBuffer get() {
        cur.rewind();
        return cur;
    }

    public FloatBuffer getInv() {
        curInv.rewind();
        return curInv;
    }

    public FloatBuffer getPrev() {
        last.rewind();
        return last;
    }

    /**
     * 
     */
    public void free() {
        Memory.free(this.cur);
        Memory.free(this.last);
        Memory.free(this.curInv);
        Memory.free(this.lastInv);
    }

}
