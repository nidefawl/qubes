package nidefawl.qubes.gui.controls;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

public class Button extends AbstractUI implements Renderable {
    private String       text;
    private FontRenderer font;
    
    public Button(int id, String text) {
        this.id = id;
        this.text = text;
        this.font = FontRenderer.get("Arial", 18, 0, 20);
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        this.hovered = this.mouseOver(mX, mY);
        if (!draw)
            return;
        Shaders.colored.enable();
        renderBox();
        Shaders.textured.enable();
        this.font.drawString(this.text, this.posX + this.width / 2, this.posY + this.height / 2 + font.getLineHeight() / 2, -1, true, 1.0F, 2);
        Shader.disable();
    }

    @Override
    public void initGui(boolean first) {

    }

}
