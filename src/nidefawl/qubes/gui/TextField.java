package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

public class TextField extends AbstractUI implements Renderable {
    private FontRenderer font;
    private int          color  = 0x999999;
    private float        alpha  = 1.0F;
    private int          color2 = 0x888888;
    private float        alpha2 = 0.8F;
    private int          color3 = 0x999999;
    private float        alpha3 = 1.0F;
    private int          color4 = 0x888888;
    private float        alpha4 = 0.8F;
    private TextInput    inputRenderer;
    private Gui gui;

    public TextField(ITextEdit gui, int id, String text) {
        this.id = id;
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.inputRenderer = new TextInput(this.font, gui);
        this.inputRenderer.editText = text;
        this.focused = false;
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        this.hovered = this.mouseOver(mX, mY);
        Shaders.colored.enable();
        color3 = 0xbababa;
        color4 = 0x888888;
        int c1 = this.hovered || this.focused ? this.color3 : this.color;
        int c2 = this.hovered || this.focused ? this.color4 : this.color2;
        //        if (selectedButton == this) {
        //            c1 = 0xeaeaea;
        //            c2 = 0x383838;
        //        }
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
        Shaders.textured.enable();
        GL11.glDisable(GL11.GL_CULL_FACE);
        this.inputRenderer.xPos = this.posX + 4;
        this.inputRenderer.yPos = this.posY + 4;
        this.inputRenderer.width = this.width - 8;
        this.inputRenderer.height = this.height - 8;
        this.inputRenderer.focused = this.focused;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(this.posX + 2, this.posY - this.height, this.width - 7, this.height);
        this.inputRenderer.drawStringWithCursor(mX, mY, Mouse.isButtonDown(0));
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
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
        System.out.println("focus");
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

}
