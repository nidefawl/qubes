package nidefawl.qubes.gui;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.Renderable;

public abstract class AbstractUI implements Renderable {
    public AbstractUI parent;
    public int           id;
    public int width;
    public int height;
    public int posX;
    public int posY;
    public int[] overridebounds = new int[4];
    public boolean hovered = false;
    public boolean enabled = true;
    public boolean draw = true;
    public boolean focused = false;
    public int zIndex = 0;
    public static AbstractUI selectedButton;
    public void saveBounds() {
        overridebounds[0] = posX;
        overridebounds[1] = posY;
        overridebounds[2] = width;
        overridebounds[3] = height;
    }
    public void restoreBounds() {
        posX=overridebounds[0];
        posY=overridebounds[1];
        width=overridebounds[2];
        height=overridebounds[3];
    }
    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
    }
    public void setPos(int x, int y) {
        this.posX = x;
        this.posY = y;
    }
    public boolean hasElement(AbstractUI element) {
        return false;
    }
    public void add(AbstractUI element) {
    }

    public void update() {
    }

    public boolean mouseOver(double mX, double mY) {
        return this.enabled && (selectedButton == null || selectedButton == this) && mX >= this.posX && mX <= this.posX + this.width && mY >= this.posY && mY <= this.posY + this.height;
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
    public int          color3  = 0xbababa;
    public float        alpha3  = 1.0F;
    public int          color4 = 0x888888;
    public int          color5 = 0xeaeaea;
    public int          color6 = 0x383838;
    public float        alpha4 = 0.8F;
    public float boxSigma = 0.25f;
    public float shadowSigma = 4;
    public float round = 4;
    public int extendx = 0;
    public int extendy = 0;
    public void resetShape() {
        boxSigma = 0.25f;
        shadowSigma = 4;
        round = 4;
        extendx = 0;
        extendy = 0;
    }
    
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
            Engine.enableDepthMask(false);
            Engine.drawQuad();
            Engine.enableDepthMask(true);
        } else {
            Shaders.gui.setProgramUniform1f("corner", round);
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
        Shaders.gui.setProgramUniform1f("zpos", 0);
        Shaders.gui.setProgramUniform4f("color", r, g, b, alpha);
        Shaders.gui.setProgramUniform1f("sigma", 0.6f);
        Shaders.gui.setProgramUniform1f("corner", round+2);
        Engine.drawQuad();
        if (drawShadow) {
            x+=extendx;
            w-=extendx*2;
            y+=extendy;
            h-=extendy*2;
            Shaders.gui.setProgramUniform1f("zpos", z+1);
            Shaders.gui.setProgramUniform4f("box", x, y+0, x+w, y+h);
//            Shaders.gui.setProgramUniform4f("color", 1-r, 1-g, 1-b, alpha);
            float br = 0.08f;
            Shaders.gui.setProgramUniform4f("color", br,br,br, alpha);
            Shaders.gui.setProgramUniform1f("sigma", shadowSigma);
            Shaders.gui.setProgramUniform1f("corner", round);
//            GL11.glDepthMask(false);
            Engine.drawQuad();
//            GL11.glDepthMask(true);
        }
    }
    public void renderOutlinedBox() {
        int c1 = this.hovered || this.focused ? this.color3 : this.color;
        int c2 = this.hovered || this.focused ? this.color4 : this.color2;
        Shaders.gui.enable();
        int extend = 2;
        this.posX -= extend;
        this.width += extend*2;
        this.posY -= extend;
        this.height += extend*2;
        Shaders.gui.enable();
        renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c2, this.alpha2, true);
        this.width -= extend*2;
        this.posX += extend;
        this.height -= extend*2;
        this.posY += extend;
        renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c1, this.alpha, false);
    }
    public void renderBox() {
        int c1 = this.hovered ? this.color3 : this.color;
        int c2 = this.hovered ? this.color4 : this.color2;
        if (selectedButton == this) {
            c1 = color5;
            c2 = color6;
        }
        renderBox(true, false, c1, c2);
    }
    public void renderBox(boolean addShadow, boolean inverse, int c1, int c2, int i) {
        
    }
    public void renderBox(boolean addShadow, boolean inverse, int c1, int c2) {
        int extend = 2;
        this.posX -= extend;
        this.width += extend*2;
        this.posY -= extend;
        this.height += extend*2;
        Shaders.gui.enable();
        if (inverse)
            renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c1, this.alpha, false);
        else
            renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c2, this.alpha2, addShadow);    
        
        this.width -= extend*2;
        this.posX += extend;
        this.height -= extend*2;
        this.posY += extend;
        if (inverse)
            renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c2, this.alpha2, addShadow);    
        else 
            renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c1, this.alpha, false);
        
    }
    public void renderBox2(boolean addShadow, boolean inverse, int c1, int c2) {
        int extend = 4;
        this.posX -= extend;
        this.width += extend*2;
        this.posY -= extend;
        this.height += extend*2;
        Shaders.gui.enable();
        if (inverse)
            renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c1, this.alpha, false);
        else
            renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c2, this.alpha2, addShadow);    
        
        this.width -= extend*2;
        this.posX += extend;
        this.height -= extend*2;
        this.posY += extend;
        if (inverse)
            renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c2, this.alpha2, addShadow);    
        else 
            renderRoundedBoxShadow(this.posX, this.posY, 0, this.width, this.height, c1, this.alpha, false);
        
    }
    public int getWindowPosX() {
        return this.parent != null ? this.parent.getWindowPosX() : 0;
    }
    public int getWindowPosY() {
        return this.parent != null ? this.parent.getWindowPosY() : 0;
    }

    public void renderSlotBackground(float x, float y, float z, float w, float h, int color, float alpha, boolean shadow, float i) {
        shadowSigma = 4;
        extendx = 1;
        extendy = 1;
        this.round = i;
//        renderRoundedBoxShadowInverse(x, y, z, w, h, color, alpha, shadow);
        shadowSigma = 0.4f;
        extendx = 2;
        extendy = 2;
        extendx = 1;
        extendy = 1;

        renderRoundedBoxShadowInverse(x, y, z, w, h, color, alpha, shadow);
        resetShape();
    }
}
