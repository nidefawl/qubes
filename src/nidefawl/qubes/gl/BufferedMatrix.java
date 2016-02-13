package nidefawl.qubes.gl;

import java.nio.FloatBuffer;

import nidefawl.qubes.util.GameMath;
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
    
    public void update() {
        cur.rewind();
        curInv.rewind();
        store(cur);
        cur.rewind();
        GameMath.invertMat4x(cur, curInv);
        cur.rewind();
        curInv.rewind();
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
        inv.invert();
        curInv.rewind();
        inv.store(curInv);
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
