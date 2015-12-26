package nidefawl.qubes.input;

import org.lwjgl.glfw.GLFW;

public class Movement {
    public float strafe, forward;
    boolean grabbed = false;
    public int mX;
    public int mY;
    public float jump;
    public float sneak;
    
    public void update(double mdX, double mdY) {
        this.strafe = 0;
        this.forward = 0;
        this.jump = 0;
        this.sneak = 0;
        if (this.grabbed) {
            float mult = 1.0F;
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))
                mult = 0.01F;
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_W)) {
                this.forward += mult;
            }
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_S)) {
                this.forward-=mult;
            }
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_D)) {
                this.strafe-=mult;
            }
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_A)) {
                this.strafe+=mult;
            }
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))
                mult = 0.03F;
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) {
                this.sneak+=mult;
            }
            if (Keyboard.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
                this.jump+=mult;
            }
            this.mX += mdX;
            this.mY += mdY;
        }
    }

    public void setGrabbed(boolean b) {
        if (this.grabbed != b) {
            this.mX = 0;
            this.mY = 0;
            this.strafe = 0;
            this.forward = 0;
            this.sneak = 0;
            this.jump = 0;
        }
        this.grabbed = b;
    }

    public boolean grabbed() {
        return this.grabbed;
    }
}