package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform1fARB;

public class Uniform1f extends AbstractUniform {

    private float lastX;

    public Uniform1f(String name, int loc) {
        super(name, loc);
    }

    public boolean set(float x) {
        if (x != lastX || first) {
            first = false;
            lastX = x;
            return set();
        }
        return false;
    }

    @Override
    public boolean set() {
        if (validLoc()) {
            glUniform1fARB(this.loc, lastX);
            return true;
        }
        return false;
    }

}
