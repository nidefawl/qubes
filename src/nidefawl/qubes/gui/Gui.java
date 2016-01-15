package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.crafting.CraftingManagerClient;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.PopupHolder;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shaders;

public abstract class Gui extends AbstractUI implements PopupHolder {
    public ArrayList<AbstractUI> buttons   = new ArrayList<>();
    public ArrayList<AbstractUI> prebackground   = new ArrayList<>();
    public boolean    firstOpen = true;
    public AbstractUI popup;
    public static final int slotW = 48;
    public static final int slotBDist = 2;
    

    @Override
    public AbstractUI getPopup() {
        return this.popup;
    }
    public boolean hasElement(AbstractUI element) {
        return buttons.contains(element) /*|| this.popup == element */;
    }
    public void add(AbstractUI element) {
        this.buttons.add(element);
        element.parent = this;
        sortElements();
    }
    public void addBackground(AbstractUI element) {
        this.buttons.add(element);
        this.prebackground.add(element);
        element.parent = this;
        sortElements();
    }
    public AbstractUI getElement(int i) {
        return i>=0&&i<this.buttons.size()?this.buttons.get(i):null;
    }
    public void clearElements() {
        this.buttons.clear();
        this.prebackground.clear();
    }
    public void sortElements() {
        Comparator<AbstractUI> comparator = new Comparator<AbstractUI>() {
            @Override
            public int compare(AbstractUI o1, AbstractUI o2) {
                int n = Integer.compare(o1.zIndex,  o2.zIndex);
                if (n != 0)
                    return n;
                return Integer.compare(o1.id, o2.id);
            }
        };
        Collections.sort(this.prebackground, comparator);
        Collections.sort(this.buttons, comparator);
    }

    public void remove(AbstractUI element) {
        boolean b = this.buttons.remove(element);
        if (b) {
            element.parent=null;
        }
    }
    @Override
    public void setPopup(AbstractUI popup) {
        if (selectedButton == popup) {
            selectedButton = null;
        }
        this.popup = popup;
        if (popup != null) {
            popup.parent = this;
            popup.initGui(false);   
        }
    }

    public void renderBackgroundElements(float fTime, double mX, double mY) {
        if (prebackground.isEmpty())
            return;
        Engine.pxStack.push(this.posX, this.posY, 0);
        for (int i = 0; i < this.prebackground.size(); i++) {
            this.prebackground.get(i).render(fTime, mX-this.posX, mY-this.posY);
        }
        Engine.pxStack.pop();
    }
    public void renderButtons(float fTime, double mX, double mY) {
        Engine.pxStack.push(this.posX, this.posY, 2);
        double mx = mX;
        double my = mY;
        if (this.popup != null) {
            mx -= (this.posX + this.popup.posX);
            my -= (this.posY + this.popup.posY);
            if (mx > 0 && mx < this.popup.width && my > Math.min(this.posX + this.popup.posX, 0) && my <= this.popup.height) {
                mX-=1000;
                mY-=1000;
            }
            mx += (this.posX + this.popup.posX);
            my += (this.posY + this.popup.posY);
        }
        int lastZ=Integer.MIN_VALUE;
        for (int i = 0; i < this.buttons.size(); i++) {
            AbstractUI btn = this.buttons.get(i);
            if (this.prebackground.contains(btn)) {
                continue;
            }
            if (btn.zIndex!=lastZ) {
                lastZ = btn.zIndex;
                Engine.pxStack.translate(0, 0, 8);
            }
            btn.render(fTime, mX-this.posX, mY-this.posY);
        }
        Engine.pxStack.pop();
        if (this.popup != null) {
            Engine.pxStack.push(this.posX, this.posY, 200);
            this.popup.render(fTime, mx-this.posX, my-this.posY);
            Engine.pxStack.pop();
        }
    }
    @Override
    public void update() {
        super.update();
        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).update();
        }
    }

    public void onClose() {
    }

    public boolean onMouseClick(int button, int action) {
        double mx=Mouse.getX()-mouseOffsetX();
        double my=Mouse.getY()-mouseOffsetY();
        if (action == GLFW.GLFW_PRESS) {
            for (int i = 0; i < this.buttons.size(); i++) {
                AbstractUI b = this.buttons.get(i);
                if (b.enabled && b.draw) {
                    b.focused = false;
                }
            }
        }
        if (selectedButton != null && action == 0) {
            if (selectedButton.parent != null && selectedButton.parent != this) {
                return false;
            }
            if (selectedButton.enabled && selectedButton.mouseOver(mx, my)) {
                selectedButton.handleMouseUp(this, action);
            }
            selectedButton = null;
            return true;
        } else if (selectedButton == null && action == 1){
            if (this.popup != null) {
                if (this.popup.mouseOver(mx, my)) {
                    if (this.popup != null && !this.popup.handleMouseDown(this, action)) {
                        selectedButton = this.popup;
                    }
                    return true;
                }
            }
            for (int i = this.buttons.size()-1; i >= 0; i--) {
                AbstractUI b = this.buttons.get(i);
                if (b.enabled && b.mouseOver(mx, my)) {
                    if (!b.handleMouseDown(this, action)) {
                        selectedButton = b;    
                    }
                    return true;
                }
            }
        }
        selectedButton = null;
        return false;
    }

    public double mouseOffsetY() {
        return this.posY+(this.parent!=null?this.parent.getWindowPosY():0);
    }

    public double mouseOffsetX() {
        return this.posX+(this.parent!=null?this.parent.getWindowPosX():0);
    }

    public boolean onGuiClicked(AbstractUI element) {
        return false;
    }

    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            close();
            return true;
        }
        if (this.popup != null) {
            if (this.popup.onKeyPress(key, scancode, action, mods)) {
                return true;
            }
        }
        for (int i = 0; i < this.buttons.size(); i++) {
            AbstractUI b = this.buttons.get(i);
            if (b.enabled) {
                if (b.onKeyPress(key, scancode, action, mods)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void close() {
        Game.instance.showGUI((Gui) this.parent);
    }

    public boolean onTextInput(int codepoint) {
        for (int i = 0; i < this.buttons.size(); i++) {
            AbstractUI b = this.buttons.get(i);
            if (b.enabled) {
                if (b.onTextInput(codepoint)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean requiresTextInput() {
        return false;
    }
    public void renderBackground(float fTime, double mX, double mY, boolean b, float a) {
        if (Game.instance.getWorld() != null) {
            a = 0.7f;
        } else {
            a = 1.0f;
        }
        Shaders.colored.enable();
        Tess.instance.setColorF(0x1f1f1f, a);
        Tess.instance.add(this.posX, this.posY+this.height);
        Tess.instance.add(this.posX+this.width, this.posY+this.height);
        Tess.instance.add(this.posX+this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
    }

    protected void updateBounds() {
    }

    protected boolean canResize() {
        return false;
    }

    public boolean onWheelScroll(double xoffset, double yoffset) {
        return false;
    }
}
