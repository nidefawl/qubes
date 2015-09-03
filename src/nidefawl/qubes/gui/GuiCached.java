package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import org.lwjgl.opengl.*;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public class GuiCached extends Gui {
    private Gui gui;
    private FrameBuffer fbDbg;
    private boolean refresh;

    public GuiCached(Gui gui) {
        this.gui = gui;
    }
    @Override
    public void setPos(int x, int y) {
        this.gui.setPos(x, y);
    }

    @Override
    public void setSize(int w, int h) {
        if (fbDbg != null) fbDbg.cleanUp();
        fbDbg = new FrameBuffer(w, h);
        fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16);
        fbDbg.setFilter(GL_COLOR_ATTACHMENT0, GL_NEAREST, GL_NEAREST);
        fbDbg.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        fbDbg.setup();
        this.gui.setSize(w, h);
    }

    public void render(float fTime) {
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        if (refresh) {
            refresh = false;
            fbDbg.bind();
            fbDbg.clearFrameBuffer();
            GL14.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            this.gui.render(fTime);
            FrameBuffer.unbindFramebuffer();
        }
        Shaders.textured.enable();
        GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, fbDbg.getTexture(0));
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        Engine.drawFullscreenQuad();
        Shader.disable();
    }
    public void update(float fTime) {
        this.gui.update(fTime);
        this.refresh = true;
    }
    
}
