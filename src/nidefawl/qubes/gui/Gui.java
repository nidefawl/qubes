package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.AbstractUIOverlay;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.PopupHolder;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

public abstract class Gui extends AbstractUI implements PopupHolder {
    ArrayList<AbstractUI> buttons   = new ArrayList<>();
    public boolean    firstOpen = true;
    public AbstractUIOverlay popup;
    

    @Override
    public AbstractUIOverlay getPopup() {
        return this.popup;
    }

    @Override
    public void setPopup(AbstractUIOverlay popup) {
        if (selectedButton == popup) {
            selectedButton = null;
        }
        this.popup = popup;
    }

    public void renderButtons(float fTime, double mX, double mY) {
        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).render(fTime, mX, mY);
        }
        if (this.popup != null) {
            this.popup.render(fTime, mX, mY);
        }
    }

    public void onClose() {
    }

    public boolean onMouseClick(int button, int action) {
        if (action == GLFW.GLFW_PRESS) {
            for (int i = 0; i < this.buttons.size(); i++) {
                AbstractUI b = this.buttons.get(i);
                if (b.enabled && b.draw) {
                    b.focused = false;
                }
            }
        }
        if (selectedButton != null && action == 0) {
            if (selectedButton.enabled && selectedButton.mouseOver(Mouse.getX(), Mouse.getY())) {
                selectedButton.handleMouseUp(this, action);
            }
            selectedButton = null;
            return true;
        } else if (selectedButton == null && action == 1){
            if (this.popup != null) {
                if (this.popup.mouseOver(Mouse.getX(), Mouse.getY())) {
                    if (this.popup != null && !this.popup.handleMouseDown(this, action)) {
                        selectedButton = this.popup;
                    }
                    return true;
                }
            }
            for (int i = 0; i < this.buttons.size(); i++) {
                AbstractUI b = this.buttons.get(i);
                if (b.enabled && b.mouseOver(Mouse.getX(), Mouse.getY())) {
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

    public boolean onGuiClicked(AbstractUI element) {
        return false;
    }

    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && Game.instance.getWorld() != null) {
            Game.instance.showGUI(null);
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
        Shaders.colored.enable();
        Tess.instance.setColorF(0x1f1f1f, a);
        Tess.instance.add(this.posX, this.posY+this.height);
        Tess.instance.add(this.posX+this.width, this.posY+this.height);
        Tess.instance.add(this.posX+this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
    }
}
