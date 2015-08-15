package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform2iARB;

public class Uniform2i extends AbstractUniform {

    private int lastX;
    private int lastY;

    public Uniform2i(String name, int loc) {
        super(name, loc);
    }

    public boolean set(int x, int y) {
        if (validLoc()) {
        if (x != lastX || y != lastY || first) {
            first = false;
            lastX = x;
            lastY = y;
            glUniform2iARB(this.loc, x, y);
            return true;
        }
        }
        return false;
    }

}
