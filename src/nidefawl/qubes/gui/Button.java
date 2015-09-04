package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

public class Button extends AbstractUI implements Renderable {
    public int           id;
    private String       text;
    private FontRenderer font;
    private int          color  = 0x999999;
    private float        alpha  = 1.0F;
    private int          color2 = 0x888888;
    private float        alpha2 = 0.8F;
    private int          color3  = 0x999999;
    private float        alpha3  = 1.0F;
    private int          color4 = 0x888888;
    private float        alpha4 = 0.8F;

    public Button(int id, String text) {
        this.id = id;
        this.text = text;
        this.font = FontRenderer.get("Arial", 18, 0, 20);
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        this.hovered = this.mouseOver(mX, mY);
        Shaders.colored.enable();
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
        Shaders.textured.enable();
        this.font.drawString(this.text, this.posX + this.width / 2, this.posY + this.height / 2 + font.getLineHeight() / 2, -1, true, 1.0F, 2);
        Shader.disable();
    }

    @Override
    public void initGui(boolean first) {

    }

}
