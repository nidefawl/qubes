package nidefawl.qubes.gl;

public interface IFrameBuffer {

    public abstract void cleanUp();

    public abstract void unbindCurrentFrameBuffer();

    public abstract void bind();

    public abstract void setupTexture(int texture, int format);

    public abstract int getTexture(int i);

    public abstract int getDepthTex();

    public abstract void clearDepth();

    public abstract void clear(int n, float r, float g, float b, float a);

    public abstract void setDrawAll();

    public abstract void clearFrameBuffer();

}