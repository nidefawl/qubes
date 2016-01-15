package nidefawl.qubes.gl;

import org.lwjgl.opengl.GL15;

public class TesselatorState extends AbstractTesselatorState {

    final GLVBO vbo = new GLVBO(GL15.GL_STATIC_DRAW);
    @Override
    public GLVBO getVBO() {
        return vbo;
    }

}
