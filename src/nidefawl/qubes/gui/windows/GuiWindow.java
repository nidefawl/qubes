package nidefawl.qubes.gui.windows;

import org.lwjgl.glfw.GLFW;
import nidefawl.qubes.Game;
import nidefawl.qubes.gui.Gui;

public abstract class GuiWindow extends Gui {

    public boolean allwaysVisible;
    public boolean visible;
    private boolean mouseOverResize;
    int[] bounds;
    public GuiWindow() {
    }
    @Override
    public void initGui(boolean first) {
        int width = 320;
        int height = 240;
        int xPos = (Game.guiWidth-width)/2;
        int yPos = (Game.guiHeight-height)/2;
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

    
    public void onDrag(double mX, double mY) {
        int displayWidth = Game.guiWidth;
        int displayHeight = Game.guiHeight;
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
        int displayWidth = Game.guiWidth;
        int displayHeight = Game.guiHeight;
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
        double mx = mouseGetX();
        double my = mouseGetY();
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
                return true;
            }
        }
        if (action == GLFW.GLFW_PRESS) {
                
            this.setFocus();
            if (GuiContext.canDragWindows) {
                if (mx>=this.posX&&mx<=this.posX+width&&my <= this.posY + titleBarHeight) {
                    GuiWindowManager.dragged = this;
                    return true;
                }
                if (mx>=this.posX&&mx<=this.posX+width&&canResize() && mouseOverResize(mx, my)) {
                    GuiWindowManager.resized = this;
                    return true;
                }
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
    public void render3D(float fTime, double mX, double mY) {
        this.renderFrame(fTime, mX, mY);
        this.render(fTime, mX, mY);
    }
}
