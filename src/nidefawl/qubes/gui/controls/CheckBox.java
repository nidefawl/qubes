package nidefawl.qubes.gui.controls;


import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.render.gui.LineGUI;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Renderable;

public class CheckBox extends AbstractUI implements Renderable {
    public boolean checked;
    protected String       text;
    public FontRenderer font;
    public float stringWidth;
    public float titleWidth = 32.0f;
    public boolean drawTitle = true;
    public boolean titleLeft = true;
    public int colorDisabled;

    public int           textColorDisabled = 0xa0a0a0;
    
    public CheckBox(int id, String text) {
        this.id = id;
        this.text = text;
        this.font = FontRenderer.get(0, Gui.FONT_SIZE_BUTTON, 0);
        this.id = id;
        this.height = GameMath.round(this.font.getLineHeight()+4);
        this.stringWidth = this.font.getStringWidth(this.text);
        titleWidth=GameMath.round(Math.max(stringWidth+6, titleWidth));
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        this.hovered = this.mouseOver(mX, mY);
        if (!draw)
            return;
        renderBox();
        Engine.setPipeStateColored2D();

        LineGUI.INST.start(1F);
        LineGUI.INST.add(this.posX, this.posY, 4, -1, 0.05f);
        LineGUI.INST.add(this.posX+width/2, this.posY, 4, -1, 0.15f);
        LineGUI.INST.add(this.posX+width, this.posY, 4, -1, 0.05f);
        LineGUI.INST.drawLines();

        LineGUI.INST.start(1F);
        LineGUI.INST.add(this.posX, this.posY+height, 4, -1, 0.1f);
        LineGUI.INST.add(this.posX+width/2, this.posY+height, 4, -1, 0.4f);
        LineGUI.INST.add(this.posX+width, this.posY+height, 4, -1, 0.1f);
        LineGUI.INST.drawLines();
        
        if (this.checked) {
            float w = 4.0f;
            if (this.width < 20) {
                w = 4.0f;
            }
            int yo = 3;
            float a1 = 0.4f;
            LineGUI.INST.start(w);
            LineGUI.INST.add(this.posX + yo, this.posY + yo, 0, color5, a1);
            LineGUI.INST.add(this.posX + height-yo, this.posY + width-yo, 0, color5, a1);
            LineGUI.INST.drawLines();
            LineGUI.INST.start(w);
            LineGUI.INST.add(this.posX + yo, this.posY + width-yo, 0, color5, a1);
            LineGUI.INST.add(this.posX + height-yo, this.posY +yo, 0, color5, a1);
            LineGUI.INST.drawLines();
            yo=3;
            LineGUI.INST.start(w/1.5f);
            LineGUI.INST.add(this.posX + yo, this.posY + yo + 0.3f, 0, color6, 1f);
            LineGUI.INST.add(this.posX + height-yo, this.posY + width-yo + 0.3f, 0, color6, 1f);
            LineGUI.INST.drawLines();
            LineGUI.INST.start(w/1.5f);
            LineGUI.INST.add(this.posX + yo, this.posY + width-yo + 0.3f, 0, color6, 1f);
            LineGUI.INST.add(this.posX + height-yo, this.posY +yo + 0.3f, 0, color6, 1f);
            LineGUI.INST.drawLines();
            
        }
        if (this.text!=null&&!this.text.isEmpty()) {

            this.font.maxWidth = -1;
            if (this.drawTitle) {
                if (titleLeft) {
                    this.font.drawString(this.text, this.posX - titleWidth, this.posY  + this.font.centerY(this.height), this.enabled ? 0xFFFFFF : colorDisabled, true, 1.0f);

                } else {
                    this.font.drawString(this.text, this.posX + this.width + 15, this.posY  + this.font.centerY(this.height), this.enabled ? 0xFFFFFF : colorDisabled, true, 1.0f);
                }

            }
        }
    }

    @Override
    public void initGui(boolean first) {

    }

}
