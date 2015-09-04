package nidefawl.qubes.gui;

import nidefawl.qubes.util.Renderable;

public abstract class AbstractUI implements Renderable {
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

    public void update(float dTime) {
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
}
