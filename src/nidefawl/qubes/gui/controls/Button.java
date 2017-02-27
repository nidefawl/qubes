package nidefawl.qubes.gui.controls;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.util.ITess;
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
        ITess tessellator = Engine.getTess();
        float yo=0.5f;
        tessellator.setColorF(-1, 0.05f);
        tessellator.add(this.posX, this.posY + yo, 4);
        tessellator.add(this.posX, this.posY - yo, 4);
        tessellator.setColorF(-1, 0.15f);
        tessellator.add(this.posX+width/2, this.posY - yo, 4);
        tessellator.add(this.posX+width/2, this.posY + yo, 4);
        tessellator.add(this.posX+width/2, this.posY + yo, 4);
        tessellator.add(this.posX+width/2, this.posY - yo, 4);
        tessellator.setColorF(-1, 0.05f);
        tessellator.add(this.posX+width, this.posY - yo, 4);
        tessellator.add(this.posX+width, this.posY + yo, 4);
        tessellator.drawQuads();
        tessellator.setColorF(-1, 0.1f);
        tessellator.add(this.posX, this.posY + height + yo);
        tessellator.add(this.posX, this.posY + height - yo);
        tessellator.setColorF(-1, 0.4f);
        tessellator.add(this.posX+width/2, this.posY + height - yo);
        tessellator.add(this.posX+width/2, this.posY + height + yo);
        tessellator.add(this.posX+width/2, this.posY + height + yo);
        tessellator.add(this.posX+width/2, this.posY + height - yo);
        tessellator.setColorF(-1, 0.1f);
        tessellator.add(this.posX+width, this.posY + height - yo);
        tessellator.add(this.posX+width, this.posY + height + yo);
        tessellator.drawQuads();
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
