package nidefawl.engine.shader;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform2fARB;

public class Uniform2f extends AbstractUniform {

    private float lastX;
    private float lastY;

    public Uniform2f(String name, int loc) {
        super(name, loc);
    }

    public boolean set(float x, float y) {
        if (x != lastX || y != lastY || first) {
            first = false;
            lastX = x;
            lastY = y;
            glUniform2fARB(this.loc, x, y);
            return true;
        }
        return false;
    }

}
