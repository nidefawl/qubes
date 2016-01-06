package nidefawl.qubes.gui.controls;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shaders;

public class ScrollList extends Gui {
    final public FontRenderer font;
    float scrollOffset=0f;
    final public Button scrollbarbutton;

    public ScrollList(Gui parent) {
        this.parent = parent;
        this.font = FontRenderer.get(null, 18, 0, 20);
        scrollbarbutton = new Button(-2, "");
    }
    float getMinY() {
        float minY = Float.MAX_VALUE;
        for (AbstractUI element : this.buttons) {
            if (element.posY<minY) minY = element.posY;
        }
        return minY;
    }
    float getMaxY() {
        float maxY = -Float.MAX_VALUE;
        for (AbstractUI element : this.buttons) {
            if (element.posY+element.height > maxY) {
                maxY = element.posY+element.height;
            }
        }
        return maxY;
    }
    float getContentHeight() {
        float minY = Float.MAX_VALUE;
        float maxY = -minY;
        for (AbstractUI element : this.buttons) {
            if (element.posY<minY) minY = element.posY;
            if (element.posY+element.height > maxY) {
                maxY = element.posY+element.height;
            }
        }
        return maxY>minY?maxY-minY:0;
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        float minY = 0;
        float maxY = 0;
        for (AbstractUI element : this.buttons) {
            if (element.posY<minY) minY = element.posY;
            if (element.posY+element.height > maxY) {
                maxY = element.posY+element.height;
            }
        }
        float contentheight = maxY>minY?maxY-minY:0;
        float yPos = minY;
        if (contentheight>this.height) {
            yPos += (contentheight-this.height)*scrollOffset;
//            System.out.println(contentheight);
        }
        Shaders.colored.enable();
        int border = 10;
        int scrollBarW = (int) (round*1.4f);
        int scrollBarG = 5;
        this.posX-=border/2;
        this.posY-=border/2;
        this.width+=border;
        this.width-=scrollBarG+scrollBarW;
        this.height+=border;
//        this.width+=(int)(scrollBarW*1f)+4;
        renderBox();
//        this.width-=(int)(scrollBarW*1f)+4;
        this.posX+=border/2;
        this.posY+=border/2;
        this.width-=border;
        this.width+=scrollBarG+scrollBarW;
        this.height-=border;
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        border/=2;
        this.posX-=border/2;
        this.posY-=border/2;
        this.width+=border;
        this.height+=border;
        GL11.glScissor(this.posX, Game.displayHeight-(posY+this.height), this.width, height);
        this.posX+=border/2;
        this.posY+=border/2;
        this.width-=border;
        this.height-=border;
        Engine.pxStack.push(0, -yPos, 0);
        super.renderButtons(fTime, mX, mY+yPos);
        Engine.pxStack.pop();
        Shaders.textured.enable();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopAttrib();
        Shaders.gui.enable();
        int scX = this.posX+this.width;
        int scY = this.posY-border;
        int scH = this.height+border*2;
        float scrollbarHeight = Math.round(scrollBarW*1.4f);
        if (selectedButton == this.scrollbarbutton) {
            float offsetY = (float) (mY-(scY+scrollbarHeight/2.0f));
            offsetY /= scH-scrollbarHeight;
            this.scrollOffset = Math.min(1, Math.max(offsetY, 0));
        }
        int c1 = this.hovered ? this.color3 : this.color;
        int c2 = this.hovered ? this.color4 : this.color2;
        if (selectedButton == this) {
            c1 = 0xeaeaea;
            c2 = 0x383838;
        }
        scX-=1;
        scrollBarW+=2;
        renderRoundedBoxShadow(scX, scY, 1, scrollBarW, scH, c2, this.alpha2, true);
        scX+=1;
        scrollBarW-=2;
        renderRoundedBoxShadow(scX, scY, 1, scrollBarW, scH, c1, this.alpha, false);
        int scrollbarOffsetY = (int) (this.scrollOffset*(scH-scrollbarHeight));
        this.scrollbarbutton.setPos(scX, scY+scrollbarOffsetY);
        this.scrollbarbutton.setSize(scrollBarW, (int) scrollbarHeight);
//        renderRoundedBoxShadow(scX, scY+scrollBarW/2+scrollbarOffsetY, 1, scrollBarW, scrollBarW*1.4f, -1, 1.0f, true);
    }

    @Override
    public void initGui(boolean first) {
    }
    @Override
    public boolean onMouseClick(int button, int action) {
        double mx=Mouse.getX();
        double my=Mouse.getY();
        if (mx>=posX&&mx<=posX+this.width &&my>=posY&&my<=posY+this.height) {
            return super.onMouseClick(button, action);    
        }
        return false;
    }

    public void onWheelScroll(double xoffset, double yoffset) {
        float h = getContentHeight();
        h = h<=0?1:h;
        this.scrollOffset = (float) Math.min(1, Math.max(scrollOffset-yoffset/(h*0.02f), 0));
    }

    public double mouseOffsetY() {
        float contentheight = getContentHeight();
        int yOff = this.posY;
        if (contentheight>this.height) {
            yOff -= (contentheight-this.height)*scrollOffset;
        }
        return yOff;
    }
    @Override
    public boolean onGuiClicked(AbstractUI element) {
        return ((Gui) parent).onGuiClicked(element);
    }

    public double mouseOffsetX() {
        return this.posX;
    }

}
