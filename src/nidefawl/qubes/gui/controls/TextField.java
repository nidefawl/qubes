package nidefawl.qubes.gui.controls;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

public class TextField extends AbstractUI implements Renderable {
    private FontRenderer font;
    private TextInput    inputRenderer;
    private Gui gui;

    public TextField(ITextEdit gui, int id, String text) {
        this.id = id;
        this.font = FontRenderer.get(null, 18, 0, 20);
        this.inputRenderer = new TextInput(this.font, gui);
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
        this.hovered = this.mouseOver(mX, mY);
        Shaders.colored.enable();
        renderOutlinedBox();
        Shaders.textured.enable();
        GL11.glDisable(GL11.GL_CULL_FACE);
        this.inputRenderer.focused = this.focused;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(this.posX + 2, Game.displayHeight-(posY+this.height), this.width - 7, height);
        this.inputRenderer.drawStringWithCursor(mX, mY, Mouse.isButtonDown(0));
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        //        this.font.drawString(this.text, this.posX + 3, this.posY + this.height / 2 + font.getLineHeight() / 2, -1, false, 1.0F, 0);
        Shader.disable();
    }

    public boolean mouseOver(double mX, double mY) {
        return mX >= this.posX && mX <= this.posX + this.width && mY >= this.posY && mY <= this.posY + this.height;
    }

    @Override
    public void initGui(boolean first) {

    }

    public boolean handleMouseUp(Gui gui, int action) {
        return gui.onGuiClicked(this);
    }

    public boolean handleMouseDown(Gui gui, int action) {
        this.focused = true;
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

}
