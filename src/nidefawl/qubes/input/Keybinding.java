/**
 * 
 */
package nidefawl.qubes.input;

import org.lwjgl.glfw.GLFW;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class Keybinding {
    private boolean enabled;
    private int     key;
    private boolean isPressed;
    private String name;
    private boolean hasCallback = true;
    private boolean staticBinding = false;
    private int defaultkey;

    public Keybinding(String name, int key) {
        this.name = name;
        this.enabled = true;
        this.defaultkey = key;
        this.key = key;
        this.isPressed = false;
    }
    public int getDefaultkey() {
        return this.defaultkey;
    }
    public Keybinding setNoCallBack() {
        this.hasCallback = false;
        return this;
    }

    public Keybinding setStatic() {
        this.staticBinding = true;
        return this;
    }
    public boolean isStaticBinding() {
        return this.staticBinding;
    }
    public boolean hasCallback() {
        return this.hasCallback;
    }
    public void fire() {
        if (this.hasCallback) {
            this.isPressed=false;
            this.update(GLFW.GLFW_PRESS);
            this.update(GLFW.GLFW_RELEASE);
        }
    }
    
    public String getName() {
        return this.name;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @return the isPressed
     */
    public boolean isPressed() {
        return this.isPressed;
    }

    /**
     * @return the key
     */
    public int getKey() {
        return this.key;
    }

    /**
     * @param enabled
     *            the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(int key) {
        this.key = key;
    }

    /**
     * @param isPressed
     *            the isPressed to set
     */
    public void setPressed(boolean isPressed) {
        this.isPressed = isPressed;
    }

    public void onDown() {}
    public void onUp() {}
    public void onRepeat() {}

    /**
     * @param action
     */
    public void update(int action) {
        boolean wasPressed = this.isPressed;
        this.isPressed = action != GLFW.GLFW_RELEASE;
        if (action != GLFW.GLFW_RELEASE) {
            onRepeat();
        }
        if (wasPressed != isPressed) {
            if (isPressed) {
                onDown();
            } else {
                onUp();
            }
        }
    }

}
