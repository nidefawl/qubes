package nidefawl.qubes.gl;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;

public class BufferedMatrix extends Matrix4f {
    private static final long serialVersionUID = 6952943666759024629L;
    
    FloatBuffer cur = BufferUtils.createFloatBuffer(16);
    FloatBuffer last = BufferUtils.createFloatBuffer(16);
    FloatBuffer curInv = BufferUtils.createFloatBuffer(16);
    FloatBuffer lastInv = BufferUtils.createFloatBuffer(16);
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
}
