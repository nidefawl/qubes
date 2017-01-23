package nidefawl.qubes.gl;

public class TesselatorState extends AbstractTesselatorState {

    final GLVBO vbo;
    public TesselatorState(int usage) {
        this.vbo = new GLVBO(usage);
    }
    @Override
    public GLVBO getVBO() {
        return vbo;
    }
    public void release() {
        this.vbo.release();
    }

}
