package nidefawl.qubes.gui.controls;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.util.Renderable;

public class Button extends AbstractUI implements Renderable {
    protected String       text;
    public FontRenderer font;
    
    public Button(int id, String text) {
        this.id = id;
        this.text = text;
        this.font = FontRenderer.get(0, Gui.FONT_SIZE_BUTTON, 0);
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        this.hovered = this.mouseOver(mX, mY);
        if (!draw)
            return;
        renderBox();
        Engine.setPipeStateColored2D();
        float yo=0.5f;
        drawLine(this.posX, this.posY, this.posX+width/2, this.posY, 4, 0, yo, -1, 0.05f, -1, 0.15f);
        drawLine(this.posX+width/2, this.posY, this.posX+width, this.posY, 4, 0, yo, -1, 0.15f, -1, 0.05f);
        drawLine(this.posX, this.posY + height, this.posX+width/2, this.posY + height, 4, 0, yo, -1, 0.1f, -1, 0.4f);
        drawLine(this.posX+width/2, this.posY + height, this.posX+width, this.posY + height, 4, 0, yo, -1, 0.4f, -1, 0.1f);
        if (this.text!=null&&!this.text.isEmpty()) {
            Engine.setPipeStateFontrenderer();
            this.font.drawString(this.text, this.posX + this.width / 2, this.posY + this.height - (this.height-font.getCharHeight())/2, -1, true, 1.0F, 2);
        }
    }

    @Override
    public void initGui(boolean first) {

    }
    public void setText(String text) {
        this.text = text;
    }

}
