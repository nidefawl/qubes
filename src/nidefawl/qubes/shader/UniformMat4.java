package nidefawl.qubes.shader;


import java.nio.FloatBuffer;

import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Memory;

public class UniformMat4 extends AbstractUniform {

    private final float[] last = new float[16];
    boolean transpose = false;
    private FloatBuffer buf;
    public UniformMat4(String name, int loc) {
        super(name, loc);
    }

    public boolean set(FloatBuffer matrix, boolean transpose) {
        if (validLoc()) {
        matrix.position(0).limit(16);
        int n = 0;
        for (int i = 0; i < 16; i++) {
            float l = matrix.get(i);
            if (l != last[i])
            {
                last[i] = l;
                n++;
            }
        }
        if (first || n > 0 || transpose != this.transpose) {
            this.first = false;
            this.transpose = transpose;
            matrix.position(0).limit(16);
            GL.glUniformMatrix4fvARB(this.loc, transpose, matrix);
            return true;
        }
        }
        return false;
    }
    
    @Override
    public boolean set() {
        if (validLoc()) {
            if (buf == null) {
                buf = Memory.createFloatBuffer(16);
            }
            buf.clear();
            buf.put(last);
            buf.position(0).limit(16);
            GL.glUniformMatrix4fvARB(this.loc, transpose, buf);
            return true;
        }
        return false;
    }
    public void release() {
        if (buf != null) {
            Memory.free(buf);
            buf = null;
        }
    }

}
