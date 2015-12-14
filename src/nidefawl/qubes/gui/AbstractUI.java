package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TextureUtil;
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
        return this.enabled && mX >= this.posX && mX <= this.posX + this.width && mY >= this.posY && mY <= this.posY + this.height;
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
        this.focused = this.enabled;
    }
    

    public int          color  = 0x999999;
    public float        alpha  = 1.0F;
    public int          color2 = 0x888888;
    public float        alpha2 = 0.8F;
    public int          color3  = 0x999999;
    public float        alpha3  = 1.0F;
    public int          color4 = 0x888888;
    public float        alpha4 = 0.8F;
    public float boxSigma = 0.25f;
    public float shadowSigma = 4;
    public float round = 15;
    public int extendx = 3;
    public int extendy = 0;
    
    public void renderRoundedBoxShadow(float x, float y, float z, float w, float h, int rgba, float alpha, boolean drawShadow) {
        float r = TextureUtil.getR(rgba);
        float g = TextureUtil.getG(rgba);
        float b = TextureUtil.getB(rgba);
        x-=extendx;
        w+=extendx*2;
        y-=extendy;
        h+=extendy*2;
        if (drawShadow) {
            Shaders.gui.setProgramUniform1f("zpos", z-1);
            Shaders.gui.setProgramUniform4f("box", x, y+1, x+w, y+h);
//            Shaders.gui.setProgramUniform4f("color", 1-r, 1-g, 1-b, alpha);
            Shaders.gui.setProgramUniform4f("color", 0,0,0, alpha);
            Shaders.gui.setProgramUniform1f("sigma", shadowSigma);
            Shaders.gui.setProgramUniform1f("corner", round);
            GL11.glDepthMask(false);
            Engine.drawQuad();
            GL11.glDepthMask(true);
        }
        Shaders.gui.setProgramUniform4f("box", x, y, x+w, y+h);
        Shaders.gui.setProgramUniform1f("zpos", z);
        Shaders.gui.setProgramUniform4f("color", r, g, b, alpha);
        Shaders.gui.setProgramUniform1f("sigma", boxSigma);
        Engine.drawQuad();
    }
    public void renderRoundedBoxShadowInverse(float x, float y, float z, float w, float h, int rgba, float alpha, boolean drawShadow) {
        float r = TextureUtil.getR(rgba);
        float g = TextureUtil.getG(rgba);
        float b = TextureUtil.getB(rgba);
        Shaders.gui.setProgramUniform4f("box", x, y, x+w, y+h);
        Shaders.gui.setProgramUniform1f("zpos", z);
        Shaders.gui.setProgramUniform4f("color", r, g, b, alpha);
        Shaders.gui.setProgramUniform1f("sigma", 0.6f);
        Shaders.gui.setProgramUniform1f("corner", round+2);
        Engine.drawQuad();
        if (drawShadow) {
            x+=extendx;
            w-=extendx*2;
            y+=extendy;
            h-=extendy*2;
            Shaders.gui.setProgramUniform1f("zpos", z+2);
            Shaders.gui.setProgramUniform4f("box", x, y+1, x+w, y+h);
//            Shaders.gui.setProgramUniform4f("color", 1-r, 1-g, 1-b, alpha);
            Shaders.gui.setProgramUniform4f("color", 0,0,0, alpha);
            Shaders.gui.setProgramUniform1f("sigma", shadowSigma);
            Shaders.gui.setProgramUniform1f("corner", round);
            GL11.glDepthMask(false);
            Engine.drawQuad();
            GL11.glDepthMask(true);
        }
    }
    public void renderOutlinedBox() {
        color3 = 0xbababa;
        color4 = 0x888888;
        int c1 = this.hovered || this.focused ? this.color3 : this.color;
        int c2 = this.hovered || this.focused ? this.color4 : this.color2;
        Shaders.gui.enable();
        this.posX -= 1;
        this.width += 2;
        this.posY -= 1;
        this.height += 2;
        Shaders.gui.enable();
        renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c2, 0.6f, true);
        this.width -= 2;
        this.posX += 1;
        this.height -= 2;
        this.posY += 1;
        renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c1, 1f, false);
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
        Shaders.gui.enable();
        renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c2, this.alpha2, true);
        this.width -= 2;
        this.posX += 1;
        this.height -= 2;
        this.posY += 1;

        renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c1, this.alpha, false);
    }
}
