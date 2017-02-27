package nidefawl.qubes.gui.controls;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.shader.Shaders;
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
        Engine.lineWidth(1.0F);
        Shaders.colored.enable();
        Tess tessellator = Tess.instance;
        //          GL11.glBegin(GL11.GL_LINE_STRIP);
        int yo=0;
        tessellator.setColorF(-1, 0.05f);
        tessellator.add(this.posX, this.posY + yo, 4);
        tessellator.setColorF(-1, 0.15f);
        tessellator.add(this.posX+width/2, this.posY + yo, 4);
        tessellator.setColorF(-1, 0.05f);
        tessellator.add(this.posX+width, this.posY + yo, 4);
        tessellator.draw(GL11.GL_LINE_STRIP);
        yo=height;
        tessellator.setColorF(-1, 0.1f);
        tessellator.add(this.posX, this.posY + yo);
        tessellator.setColorF(-1, 0.4f);
        tessellator.add(this.posX+width/2, this.posY + yo);
        tessellator.setColorF(-1, 0.1f);
        tessellator.add(this.posX+width, this.posY + yo);
        tessellator.draw(GL11.GL_LINE_STRIP);
        if (this.checked) {
            float w = 8.0f;
            if (this.width < 20) {
                w = 4.0f;
            }
            Engine.lineWidth(w);
            tessellator.setColorF(color6, 0.8f);
            yo=3;
            tessellator.add(this.posX + yo, this.posY + yo);
            tessellator.add(this.posX + height-yo, this.posY + width-yo);
            tessellator.add(this.posX + yo, this.posY + width-yo);
            tessellator.add(this.posX + height-yo, this.posY +yo);
            tessellator.draw(GL11.GL_LINES);
            Engine.lineWidth(w/2.0f);
//            this.posX++;
            tessellator.setOffset(0, 0.3f, 0);
            tessellator.setColorF(color5, 0.5f);
            yo=5;
            tessellator.add(this.posX + yo, this.posY + yo);
            tessellator.add(this.posX + height-yo, this.posY + width-yo);
            tessellator.add(this.posX + yo, this.posY + width-yo);
            tessellator.add(this.posX + height-yo, this.posY +yo);
            tessellator.draw(GL11.GL_LINES);

            tessellator.setOffset(0, 0, 0);

//            this.posX--;
            
        }
        if (this.text!=null&&!this.text.isEmpty()) {
            Engine.setPipeStateFontrenderer();

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
