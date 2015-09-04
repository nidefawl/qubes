package nidefawl.qubes.gui;

import java.util.ArrayList;

import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.util.Renderable;

public abstract class Gui extends AbstractUI {
    ArrayList<AbstractUI> buttons   = new ArrayList<>();
    public boolean    firstOpen = true;

    public void renderButtons(float fTime, double mX, double mY) {
        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).render(fTime, mX, mY);
        }
    }

    public void onClose() {
    }

    public boolean onMouseClick(int button, int action) {
        if (selectedButton != null && action == 0) {
            if (selectedButton.enabled && selectedButton.draw && selectedButton.mouseOver(Mouse.getX(), Mouse.getY())) {
                selectedButton.handleMouseUp(this, action);
            }
            selectedButton = null;
            return true;
        } else if (selectedButton == null && action == 1){
            for (int i = 0; i < this.buttons.size(); i++) {
                AbstractUI b = this.buttons.get(i);
                if (b.enabled && b.draw && b.mouseOver(Mouse.getX(), Mouse.getY())) {
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
        for (int i = 0; i < this.buttons.size(); i++) {
            AbstractUI b = this.buttons.get(i);
            if (b.enabled && b.draw) {
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
            if (b.enabled && b.draw) {
                if (b.onTextInput(codepoint)) {
                    return true;
                }
            }
        }
        return false;
    }
}
