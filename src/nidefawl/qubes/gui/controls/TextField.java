package nidefawl.qubes.gui.controls;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.util.Renderable;

public class TextField extends AbstractUI implements Renderable {
    private TextInput    inputRenderer;
    private Gui gui;

    public TextField(ITextEdit gui, int id, String text) {
        super();
        this.id = id;
        this.inputRenderer = new TextInput(FontRenderer.get(0, 18, 0), gui);
        this.inputRenderer.editText = text;
        this.focused = false;
    }

    @Override
    public void setPos(int x, int y) {
        super.setPos(x, y);
        this.inputRenderer.xPos = this.posX + 4;
        this.inputRenderer.yPos = this.posY + 4;
    }

    @Override
    public void setSize(int w, int h) {
        super.setSize(w, h);
        this.inputRenderer.width = this.width - 8;
        this.inputRenderer.height = this.height - 8;
    }
    @Override
    public void render(float fTime, double mX, double mY) {
        if (!this.draw) {
            return;
        }
        this.hovered = this.mouseOver(mX, mY);
        renderOutlinedBox();
        Engine.setPipeStateFontrenderer();
//        GL11.glDisable(GL11.GL_CULL_FACE);
        this.inputRenderer.focused = this.focused;
        Engine.enableScissors();
        Engine.pxStack.setScissors(posX + 2, posY, width - 7, height);
        this.inputRenderer.drawStringWithCursor(mX, mY, Mouse.isButtonDown(0));
        Engine.disableScissors();
//        GL11.glEnable(GL11.GL_CULL_FACE);
        //        this.font.drawString(this.text, this.posX + 3, this.posY + this.height / 2 + font.getLineHeight() / 2, -1, false, 1.0F, 0);
    }

    public boolean mouseOver(double mX, double mY) {
        return this.enabled && this.draw && mX >= this.posX && mX <= this.posX + this.width && mY >= this.posY && mY <= this.posY + this.height;
    }

    @Override
    public void initGui(boolean first) {

    }

    public boolean handleMouseUp(Gui gui, int action) {
        return gui.onGuiClicked(this);
    }

    public boolean handleMouseDown(Gui gui, int action) {
        if (!enabled)
            return false;
        if (GuiContext.input != null) {
            GuiContext.input.focused=false;
        }
        this.focused = true;
        GuiContext.input=this;
        //        return gui.onGuiClicked(this);
        return true;
    }

    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (this.focused) {
            this.inputRenderer.onKeyPress(key, scancode, action, mods);
            return true;
        }
        return false;
    }

    public boolean onTextInput(int codepoint) {
        if (this.focused) {
            this.inputRenderer.onTextInput(codepoint);
            return true;
        }
        return false;
    }

    public String getText() {
        return this.inputRenderer.editText;
    }

    /**
     * @return
     */
    public TextInput getTextInput() {
        return this.inputRenderer;
    }

    public boolean isFocusedAndContext() {
        if (this.focused) {
            if (this.parent instanceof Gui) {
                if (GameBase.baseInstance.getGui()==this.parent) {
                    return true;
                }
            }
            if (this.parent instanceof GuiWindow) {
                GuiWindow w = (GuiWindow)this.parent;
                if (w.visible && GuiWindowManager.anyWindowVisible() && w.hasFocus()) {
                    return true;
                }
            }
        }
        return false;
    }

}
