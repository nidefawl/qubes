package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform4fARB;

public class Uniform4f extends AbstractUniform {

    private float lastX;
    private float lastY;
    private float lastZ;
    private float lastW;

    public Uniform4f(String name, int loc) {
        super(name, loc);
    }

    public boolean set(float x, float y, float z, float w) {
        if (validLoc()) {
        if (x != lastX || y != lastY || z != lastZ || w != lastW || first) {
            first = false;
            lastX = x;
            lastY = y;
            lastZ = z;
            lastW = w;
            return set();
        }
        }
        return false;
    }
    
    @Override
    public boolean set() {
        if (validLoc()) {
            glUniform4fARB(this.loc, lastX, lastY, lastZ, lastW);
            return true;
        }
        return false;
    }

}
