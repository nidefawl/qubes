package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL30.glUniform1ui;

public class Uniform1ui extends AbstractUniform {

    private long lastX;

    public Uniform1ui(String name, int loc) {
        super(name, loc);
    }

    public boolean set(long x) {
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
            glUniform1ui(this.loc, (int)lastX);
            return true;
        }
        return false;
    }

}
