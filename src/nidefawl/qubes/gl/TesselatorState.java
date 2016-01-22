package nidefawl.qubes.gl;

import org.lwjgl.opengl.GL15;

public class TesselatorState extends AbstractTesselatorState {

    final GLVBO vbo;
    public TesselatorState(int usage) {
        this.vbo = new GLVBO(usage);
    }
    @Override
    public GLVBO getVBO() {
        return vbo;
    }

}
