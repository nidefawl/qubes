package nidefawl.qubes.gui.controls;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.render.gui.BoxGUI;
import nidefawl.qubes.shader.Shaders;

public class ScrollList extends Gui {
    float scrollOffset=0f;
    final public Button scrollbarbutton;
    public float scrollY;

    public ScrollList(Gui parent) {
        this();
        this.parent = parent;
    }
    public ScrollList() {
        scrollbarbutton = new Button(-2, "");
    }
    float getMinY() {
        float minY = Float.MAX_VALUE;
        for (AbstractUI element : this.buttons) {
            if (element.draw && element.posY<minY) minY = element.posY;
        }
        return minY;
    }
    float getMaxY() {
        float maxY = -Float.MAX_VALUE;
        for (AbstractUI element : this.buttons) {
            if (element.draw && element.posY+element.height > maxY) {
                maxY = element.posY+element.height;
            }
        }
        return maxY;
    }
    float getContentHeight() {
        float minY = Float.MAX_VALUE;
        float maxY = -minY;
        for (AbstractUI element : this.buttons) {
            if (element.draw && element.posY<minY) minY = element.posY;
            if (element.draw && element.posY+element.height > maxY) {
                maxY = element.posY+element.height;
            }
        }
        return maxY>minY?maxY-minY:0;
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        GuiContext.scrolllist = this;
        float minY = 0;
        float maxY = 0;
        for (AbstractUI element : this.buttons) {
            if (element.draw && element.posY<minY) minY = element.posY;
            if (element.draw && element.posY+element.height > maxY) {
                maxY = element.posY+element.height;
            }
        }
        float contentheight = maxY>minY?maxY-minY:0;
        float yPos = minY;
        if (contentheight>this.height) {
            yPos += (contentheight-this.height)*scrollOffset;
//            System.out.println(contentheight);
        }
        yPos = (int)yPos;
        this.scrollY=yPos;
        Shaders.colored.enable();

        int border = 10;
        int scrollBarW = (int) (16);
        int scrollBarG = 5;

        this.posX -= border / 2;
        this.posY -= border / 2;
        this.width += border;
        this.width -= scrollBarG + 4;
        this.height += border;

        BoxGUI.setFade(0.1f);
        renderBox(false, true, color2, color3);
        BoxGUI.setFade(0.3f);
        this.posX += border / 2;
        this.posY += border / 2;
        this.width -= border;
        this.width += scrollBarG + 4;
        this.height -= border;
        border /= 2;
        this.posX -= border / 2;
        this.posY -= border / 2;
        this.width += border;
        this.height += border;
        this.posX += border / 2;
        this.posY += border / 2;
        this.width -= border;
        this.height -= border;
        Engine.pxStack.setScissors(this.posX-2, this.posY-2, this.width+2, this.height+2);
        Engine.enableScissors();
        Engine.pxStack.push(0, -yPos, 0);
        super.renderButtons(fTime, mX, mY+yPos);
        Engine.pxStack.pop();
        Engine.disableScissors();
        Engine.setPipeStateFontrenderer();
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
        shadowSigma=2f;
        BoxGUI.setFade(0.1f);
        renderRoundedBoxShadow(scX, scY, 1, scrollBarW, scH, color4, this.alpha2, true);
        scX+=1;
        scrollBarW-=2;
        renderRoundedBoxShadow(scX, scY, 1, scrollBarW, scH, color3, this.alpha, false);
        BoxGUI.setFade(0.3f);
        resetShape();
        int scrollbarOffsetY = (int) (this.scrollOffset*(scH-scrollbarHeight));
        this.scrollbarbutton.setPos(scX-this.parent.posX+this.getWindowPosX(), scY+scrollbarOffsetY-this.parent.posY+this.getWindowPosY());
//        this.scrollbarbutton.setPos(scX+this.parent.posX-this.getWindowPosX(), scY+scrollbarOffsetY+this.parent.posY-this.getWindowPosY());

        this.scrollbarbutton.setSize(scrollBarW, (int) scrollbarHeight);
//        renderRoundedBoxShadow(scX, scY+scrollBarW/2+scrollbarOffsetY, 1, scrollBarW, scrollBarW*1.4f, -1, 1.0f, true);
    }

    @Override
    public void initGui(boolean first) {
    }
    @Override
    public boolean onMouseClick(int button, int action) {
        double mx=mouseGetX()-(getWindowPosX());
        double my=mouseGetY()-(getWindowPosY());
        if (mx>=posX&&mx<=posX+this.width &&my>=posY&&my<=posY+this.height) {
            return super.onMouseClick(button, action);    
        } else {
        }
        return false;
    }

    public boolean onWheelScroll(double xoffset, double yoffset) {
        float h = getContentHeight();
        h = h<=0?1:h;
        this.scrollOffset = (float) Math.min(1, Math.max(scrollOffset-yoffset/(h*0.02f), 0));
        return true;
    }

    public double mouseOffsetY() {
        float contentheight = getContentHeight();
        double yOff = super.mouseOffsetY();
        if (contentheight>this.height) {
            yOff -= (contentheight-this.height)*scrollOffset;
        }
        return yOff;
    }
    @Override
    public boolean onGuiClicked(AbstractUI element) {
        return ((Gui) parent).onGuiClicked(element);
    }

}
