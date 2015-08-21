package nidefawl.qubes.gl;

public interface IFrameBuffer {

    public abstract void cleanUp();

    public abstract void unbindCurrentFrameBuffer();

    public abstract void bind();

    public abstract int getTexture(int i);

    public abstract int getDepthTex();

    public abstract void setDrawAll();

    public abstract void clearFrameBuffer();

}