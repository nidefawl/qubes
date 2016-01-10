package nidefawl.qubes.gui.controls;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

public class Button extends AbstractUI implements Renderable {
    protected String       text;
    protected FontRenderer font;
    
    public Button(int id, String text) {
        this.id = id;
        this.text = text;
        this.font = FontRenderer.get(null, 18, 0, 20);
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        this.hovered = this.mouseOver(mX, mY);
        if (!draw)
            return;
        renderBox();
        if (this.text!=null&&!this.text.isEmpty()) {
            Shaders.textured.enable();
            this.font.drawString(this.text, this.posX + this.width / 2, this.posY + this.height / 2 + font.getLineHeight() / 2, -1, true, 1.0F, 2);
        }
    }

    @Override
    public void initGui(boolean first) {

    }

}
