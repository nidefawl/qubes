package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

public abstract class AbstractUI implements Renderable {
    public int           id;
    public int width;
    public int height;
    public int posX;
    public int posY;
    public boolean hovered = false;
    public boolean enabled = true;
    public boolean draw = true;
    public boolean focused = false;
    public static AbstractUI selectedButton;

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
    }
    public void setPos(int x, int y) {
        this.posX = x;
        this.posY = y;
    }

    public void update() {
    }

    public boolean mouseOver(double mX, double mY) {
        return mX >= this.posX && mX <= this.posX + this.width && mY >= this.posY && mY <= this.posY + this.height;
    }

    public boolean handleMouseUp(Gui gui, int action) {
        return gui.onGuiClicked(this);
    }
    public boolean handleMouseDown(Gui gui, int action) {
        return false;
    }
    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        return false;
    }
    public boolean onTextInput(int codepoint) {
        return false;
    }
    
    public void setFocus() {
        this.focused = true;
    }
    

    public int          color  = 0x999999;
    public float        alpha  = 1.0F;
    public int          color2 = 0x888888;
    public float        alpha2 = 0.8F;
    public int          color3  = 0x999999;
    public float        alpha3  = 1.0F;
    public int          color4 = 0x888888;
    public float        alpha4 = 0.8F;

    public void renderOutlinedBox() {
        color3 = 0xbababa;
        color4 = 0x888888;
        int c1 = this.hovered || this.focused ? this.color3 : this.color;
        int c2 = this.hovered || this.focused ? this.color4 : this.color2;
        Tess.instance.setColorF(c1, 0.6f);
        Tess.instance.add(this.posX, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        this.posX -= 1;
        this.width += 2;
        this.posY -= 1;
        this.height += 2;
        int inset = 2;
        Tess.instance.setColorF(c1, 1f);
        Tess.instance.add(this.posX, this.posY + inset);
        Tess.instance.add(this.posX + this.width, this.posY + inset);
        Tess.instance.add(this.posX + this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.add(this.posX, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height - inset);
        Tess.instance.add(this.posX, this.posY + this.height - inset);

        Tess.instance.add(this.posX + inset, this.posY + inset);
        Tess.instance.add(this.posX, this.posY + inset);
        Tess.instance.add(this.posX, this.posY + this.height - inset);
        Tess.instance.add(this.posX + inset, this.posY + this.height - inset);

        Tess.instance.add(this.posX + this.width, this.posY + inset);
        Tess.instance.add(this.posX + this.width - inset, this.posY + inset);
        Tess.instance.add(this.posX + this.width - inset, this.posY + this.height - inset);
        Tess.instance.add(this.posX + this.width, this.posY + this.height - inset);
        this.width -= 2;
        this.posX += 1;
        this.height -= 2;
        this.posY += 1;
        Tess.instance.draw(GL_QUADS);
    }
    public void renderBox() {
        color3 = 0xbababa;
        color4 = 0x888888;
        int c1 = this.hovered ? this.color3 : this.color;
        int c2 = this.hovered ? this.color4 : this.color2;
        if (selectedButton == this) {
            c1 = 0xeaeaea;
            c2 = 0x383838;
        }
        this.posX -= 1;
        this.width += 2;
        this.posY -= 1;
        this.height += 2;
        Tess.instance.setColorF(c2, this.alpha2);
        Tess.instance.add(this.posX, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        this.width -= 2;
        this.posX += 1;
        this.height -= 2;
        this.posY += 1;
        Tess.instance.setColorF(c1, this.alpha);
        Tess.instance.add(this.posX, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY + this.height);
        Tess.instance.add(this.posX + this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
        GL11.glLineWidth(2);
        Tess tessellator = Tess.instance;
        tessellator.setColorRGBAF(0,0,0,0.6F);
        tessellator.add(this.posX + this.width, this.posY, 0.0f);
        tessellator.add(this.posX-1, this.posY, 0.0f);
        tessellator.add(this.posX, this.posY, 0.0f);
        tessellator.add(this.posX, this.posY + this.height, 0.0f);
        tessellator.setColorRGBAF(1,1,1,0.2F);
        tessellator.add(this.posX, this.posY + this.height, 0.0f);
        tessellator.add(this.posX + this.width, this.posY + this.height, 0.0f);
        tessellator.add(this.posX + this.width, this.posY + this.height, 0.0f);
        tessellator.add(this.posX + this.width, this.posY, 0.0f);
        tessellator.draw(GL11.GL_LINES);
    }
}
