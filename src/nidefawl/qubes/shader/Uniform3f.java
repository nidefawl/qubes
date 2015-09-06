package nidefawl.qubes.shader;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform3fARB;

import nidefawl.qubes.vec.Vector3f;

public class Uniform3f extends AbstractUniform {

    private float lastX;
    private float lastY;
    private float lastZ;

    public Uniform3f(String name, int loc) {
        super(name, loc);
    }

    public boolean set(float x, float y, float z) {
        if (validLoc()) {
        if (x != lastX || y != lastY || z != lastZ || first) {
            first = false;
            lastX = x;
            lastY = y;
            lastZ = z;
            return set();
        }
        }
        return false;
    }
    
    @Override
    public boolean set() {
        if (validLoc()) {
            glUniform3fARB(this.loc, lastX, lastY, lastZ);
            return true;
        }
        return false;
    }

    public void set(Vector3f loc) {
        this.set(loc.x, loc.y, loc.z);
    }

}
