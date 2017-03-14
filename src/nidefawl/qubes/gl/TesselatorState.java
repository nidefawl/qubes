package nidefawl.qubes.gl;

import org.lwjgl.opengl.GL15;

public class TesselatorState extends AbstractTesselatorState {

    final GLVBO vbo;
    final GLVBO vboIdx;
    private int usage;
    public TesselatorState(int usage) {
        this.usage = usage;
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
    @Override
    public boolean isDynamic() {
        return this.usage == GL15.GL_DYNAMIC_DRAW;
    }

}
