package nidefawl.qubes.input;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Movement {
    public float strafe, forward;
    boolean grabbed = false;
    public int mX;
    public int mY;
    public int jump;
    public int sneak;
    
    public void update(double mdX, double mdY) {
        this.strafe = 0;
        this.forward = 0;
        this.jump = 0;
        this.sneak = 0;
        if (this.grabbed) {

            if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                this.forward++;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                this.forward--;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                this.strafe--;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                this.strafe++;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                this.sneak++;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                this.jump++;
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