package nidefawl.qubes.gl;

public class TesselatorState extends AbstractTesselatorState {

    final GLVBO vbo = new GLVBO();
    @Override
    public GLVBO getVBO() {
        return vbo;
    }

}
