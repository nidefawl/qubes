package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform1fARB;

public class Uniform1f extends AbstractUniform {

    private float lastX;

    public Uniform1f(String name, int loc) {
        super(name, loc);
    }

    public boolean set(float x) {
        if (validLoc()) {
        if (x != lastX || first) {
            first = false;
            lastX = x;
            glUniform1fARB(this.loc, x);
            return true;
        }
        }
        return false;
    }

}
