package nidefawl.qubes.gl;

public class TesselatorState extends AbstractTesselatorState {

    final GLVBO vbo;
    final GLVBO vboIdx;
    public TesselatorState(int usage) {
        this.vbo = new GLVBO(usage);
        this.vboIdx = new GLVBO(usage);
    }
    @Override
    public GLVBO getVBO() {
        return vbo;
    }
    @Override
    public GLVBO getVBOIndices() {
        return vboIdx;
    }
    public void release() {
        this.vbo.release();
        this.vboIdx.release();
    }

}
