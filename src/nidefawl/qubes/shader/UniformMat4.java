package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBShaderObjects.glUniformMatrix4ARB;

import java.nio.FloatBuffer;

public class UniformMat4 extends AbstractUniform {

    private final float[] last = new float[16];
    boolean transpose = false;
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
            glUniformMatrix4ARB(this.loc, transpose, matrix);
            return true;
        }
        }
        return false;
    }

}
