package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform1iARB;

public class Uniform1i extends AbstractUniform {

    private int lastX;

    public Uniform1i(String name, int loc) {
        super(name, loc);
    }

    public boolean set(int x) {
        if (validLoc()) {
            if (x != lastX || first) {
                first = false;
                lastX = x;
                return set();
            }
        }
        return false;
    }

    @Override
    public boolean set() {
        if (validLoc()) {
            glUniform1iARB(this.loc, lastX);
            return true;
        }
        return false;
    }

}
