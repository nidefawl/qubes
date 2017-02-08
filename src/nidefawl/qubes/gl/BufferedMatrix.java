package nidefawl.qubes.gl;

import java.nio.FloatBuffer;

import nidefawl.qubes.vec.Matrix4f;

public class BufferedMatrix extends Matrix4f {
    
    final Matrix4f inv = new Matrix4f();
    boolean needInv = false;
    FloatBuffer cur = Memory.createFloatBuffer(16);
    FloatBuffer curInv = Memory.createFloatBuffer(16);
    
    public BufferedMatrix() {
        super();
        setIdentity();
        store(cur);
        createInv();
        cur.rewind();
        curInv.rewind();
        needInv = false;
    }
    
    /**
     * Calculates the inverted matrix and puts both m and m^-1 matrices into native memory.
     * 
     */
    public void update() {
        cur.rewind();
        store(cur);
        cur.rewind();
        needInv = true;
    }

    public FloatBuffer get() {
        cur.rewind();
        return cur;
    }

    public FloatBuffer getInv() {
        if (needInv) {
            createInv();
        }
        curInv.rewind();
        return curInv;
    }
    public Matrix4f getInvMat4() {
        if (needInv) {
            createInv();
        }
        return inv;
    }

    private void createInv() {
        needInv = false;
        inv.load(this);
        inv.invertDoublePrecision();
        curInv.rewind();
        inv.store(curInv);
        curInv.rewind();
    }

    /**
     * 
     */
    public void free() {
        Memory.free(this.cur);
        Memory.free(this.curInv);
        this.cur=null;
        this.curInv=null;
    }

}
