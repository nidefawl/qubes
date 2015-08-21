package nidefawl.qubes.render;

public abstract class FinalRendererBase {

    public abstract void init();

    public abstract void initShaders();

    public abstract void render(float fTime);

    public abstract void renderFinal(float fTime);

    public abstract void release();

    public abstract void resize(int displayWidth, int displayHeight);

}
