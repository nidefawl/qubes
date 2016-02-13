package nidefawl.qubes.gui.controls;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
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
        GL11.glLineWidth(1.0F);
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
        if (this.text!=null&&!this.text.isEmpty()) {
            Shaders.textured.enable();
            this.font.drawString(this.text, this.posX + this.width / 2, this.posY + this.height - (this.height-font.getCharHeight())/2, -1, true, 1.0F, 2);
        }
    }

    @Override
    public void initGui(boolean first) {

    }

}
