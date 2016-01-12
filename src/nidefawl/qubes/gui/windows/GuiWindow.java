package nidefawl.qubes.gui.windows;

import org.lwjgl.glfw.GLFW;
import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;

public abstract class GuiWindow extends Gui {

    public static final int titleBarHeight = 26;
    public boolean allwaysVisible;
    public boolean visible;
    private boolean mouseOverResize;
    final public FontRenderer font;
    int[] bounds;
    public GuiWindow() {
        this.font = FontRenderer.get(null, 22, 0, 26);
    }
    @Override
    public void initGui(boolean first) {
        int width = 320;
        int height = 240;
        int xPos = (Game.displayWidth-width)/2;
        int yPos = (Game.displayHeight-height)/2;
        setPos(xPos, yPos);
        setSize(width, height);
    }
    public String getTitle() {
        return "";
    }

    public void onDefocus() {
        if (this.popup != null) {
            this.popup = null;
        }
    }

    public void onFocus() {
    }


    public void setFocus() {
        GuiWindowManager.setWindowFocus(this);
    }

    public boolean hasFocus() {
        return GuiWindowManager.getWindowFocus() == this;
    }
    public void open() {
        this.visible = true;
        initGui(false);
        this.setFocus();
        GuiWindowManager.onWindowOpened(this);
    }

    public void close() {
        onClose();
        this.visible = false;
        GuiWindowManager.onWindowClosed(this);
    }
    @Override
    public void onClose() {
        super.onClose();
        this.bounds = new int[] { this.posX, this.posY, this.width, this.height };
    }
    
    public boolean mouseOver(double mx, double my) {
        if (this.visible) {
            if (this.posX <= mx && this.posX + this.width >= mx && this.posY-1 <= my && this.posY + this.height+4 >= my) {
                return true;
            }
            for (int i = 0; i < this.prebackground.size(); i++) {
                if (this.prebackground.get(i).mouseOver(mx-posX, my-posY)) {
                    return true;
                }
            }
        }
        return false;
    }
    public boolean mouseOverResize(double mx, double my) {
        return mx > this.posX+this.width - 14 && mx < this.posX+this.width && my > this.posY+this.height - 14 && my < this.posY+this.height;
    }

    public boolean removeOnClose() {
        return false;
    }


    private boolean doesPopupHandleClick(double mx, double my) {
        return false;
    }

    protected void updateBounds() {
    }

    protected boolean canResize() {
        return this.popup == null;
    }

    
    public void renderFrame(float fTime, double mX, double mY) {
        renderBackgroundElements(fTime, mX, mY);
        this.posY+=5;
        this.round = 1;
        this.extendx = 0;
        this.round = 2;
        Shaders.gui.enable();
        this.shadowSigma = 3f;
        this.round = 3;
        int bw = 3;
        int out=2;
        float alpha = 0.8f;
        int color = -1;//this.hasFocus() ? -1 : 0x9a9a9a;
        if (this.hasFocus()) {
            int n = 4;
            Shaders.gui.enable();
            Shaders.gui.setProgramUniform1f("zpos", -18);
            Shaders.gui.setProgramUniform4f("box", posX-n, posY-n, posX+width+n, posY+height+n);
//          Shaders.gui.setProgramUniform4f("color", 1-r, 1-g, 1-b, alpha);
//          float blink = GameMath.sin((((Game.ticksran+fTime)/20f)%20.0f)*GameMath.PI*2f)*0.5f+0.5f;
//          float c = 0.8f*blink;
//            Shaders.gui.setProgramUniform1f("sigma", 4+c*5);
//        Shaders.gui.setProgramUniform4f("color", c,c,c, c);
//        shadowSigma = c*10;
            float c = 0.1f;
            Shaders.gui.setProgramUniform4f("color", c,c,c, 0.3f);
            Shaders.gui.setProgramUniform1f("sigma", 2);
            Shaders.gui.setProgramUniform1f("corner", 8);
            Engine.drawQuad();
        }
        int z = -10;

        resetShape();
        alpha = 0.3f;
        Shaders.gui.setProgramUniform1f("fade", 0.1f);
        renderBox();
        Shaders.gui.setProgramUniform1f("fade", 0.3f);
        out=-30;
        alpha = 1;
        color = this.hasFocus() ? 0xdadada : 0xbababa;
//        color = 0xdadada;
        this.shadowSigma = 4f;
        int titleWidth = 220;
        int top = (posY-4);
        int left = posX+width/2-titleWidth/2;
        renderRoundedBoxShadow(left, top, 5, titleWidth, titleBarHeight, color, 1f, true);
        resetShape();
//        GL11.glDepthFunc(GL11.GL_EQUAL);
//        this.shadowSigma = 3f;
//        this.round = 32;
//        renderRoundedBoxShadow(posX-titleBarHeight, posY-out*2-1, 5, width/3+4, titleBarHeight+out*2, -1, 0.7f, true);
//        GL11.glDepthFunc(GL11.GL_LEQUAL);
        resetShape();
        Shaders.textured.enable();
        Engine.pxStack.push(0, 0, 6);
        font.drawString(getTitle(), posX+width/2, posY + titleBarHeight-7, -1, true, 1f, 2);
        Engine.pxStack.pop();
        this.posY-=5;
    }
    public void onDrag(double mX, double mY) {
        int displayWidth = Game.displayWidth;
        int displayHeight = Game.displayHeight;
        this.posX += mX;
        this.posY -= mY; //crap
        if (this.posX + this.width / 2 < 0)
            this.posX = (int) (-this.width / 2);
        if (this.posX + this.width / 2 > displayWidth) {
            this.posX = displayWidth - this.width / 2;
        }
        if ((this.posY ) < 0)
            this.posY = (int) (0);
        if (this.posY > displayHeight)
            this.posY = displayHeight;
        boolean updateBounds = false;
        if (this.height > displayHeight) {
            this.height = displayHeight;
            updateBounds = true;
        }
        if (this.width > displayWidth) {
            this.width = displayWidth;
            updateBounds = true;
        }
        if (updateBounds) {
            updateBounds();
        }
    }
    public void onResize(double mX, double mY) {
        int displayWidth = Game.displayWidth;
        int displayHeight = Game.displayHeight;
        if (canResize()) {
            // XXX: calculate relative mouse position and resize according to
            // the calculated x/y coords
            this.width += mX;
            this.height -= mY;
            if (this.height > displayHeight - 20)
                this.height = displayHeight - 20;
            if (this.width > displayWidth - 20)
                this.width = displayWidth - 20;
            this.updateBounds();
        }
    }
    public boolean onMouseClick(int button, int action) {
        double mx = Mouse.getX();
        double my = Mouse.getY();
        if (this.popup != null) {
            float popupMx=(float) (mx-(this.posX + this.popup.posX));
            float popupMy=(float) (my-(this.posY + this.popup.posY));
            if (popupMx > 0 && popupMx < this.popup.width && popupMy > 0 && popupMy <= this.popup.height) {
                if (popup instanceof Gui) {
                    ((Gui) this.popup).onMouseClick(button, action);
                    return true;    
                }
            } else {
                setPopup(null);
            }
        }
        if (action == GLFW.GLFW_PRESS) {
                
            this.setFocus();
                
            if (mx>=this.posX&&mx<=this.posX+width&&my <= this.posY + titleBarHeight) {
                GuiWindowManager.dragged = this;
                return true;
            }
            if (mx>=this.posX&&mx<=this.posX+width&&canResize() && mouseOverResize(mx, my)) {
                GuiWindowManager.resized = this;
                return true;
            }
        }
//        return false;
            return super.onMouseClick(button, action);
    }
    public int getWindowPosX() {
        return this.posX;
    }
    public int getWindowPosY() {
        return this.posY;
    }
}
