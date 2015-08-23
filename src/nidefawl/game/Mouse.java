package nidefawl.game;

import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

public class Mouse {

	protected static double dy;
	protected static double dx;
	protected static double x;
	protected static double y;
	protected static double scrollDX;
	protected static double scrollDY;

    static DoubleBuffer bx;
    static DoubleBuffer by;
	public static void init() {

        bx = BufferUtils.createDoubleBuffer(1);
        by = BufferUtils.createDoubleBuffer(1);
	}
	public static double getDX() {
		return dx;
	}

	public static double getDY() {
		return dy;
	}

	public static boolean getState(int action) {
		return action == 1;
	}

	public static double getX() {
		return x;
	}

	public static double getY() {
		return y;
	}

	public static void setCursorPosition(int i, int j) {
		glfwSetCursorPos(GLGame.windowId, i, j);
	}

	static boolean isGrabbed;
	public static boolean isGrabbed() {
		return isGrabbed;
	}

	public static void setGrabbed(boolean b) {
		isGrabbed = b;
        GLFW.glfwSetInputMode(GLGame.windowId, GLFW.GLFW_CURSOR, b ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL);
	}

	public static boolean isButtonDown(int i) {
		return glfwGetMouseButton(GLGame.windowId, GLFW.GLFW_MOUSE_BUTTON_LEFT+i) == GLFW.GLFW_PRESS;
	}

	public static void update(int displayWidth, int displayHeight) {
		double centerX = displayWidth/2;
		double centerY = displayHeight/2;
		bx.position(0);
		by.position(0);
		GLFW.glfwGetCursorPos(GLGame.windowId, bx, by);
		x = bx.get();
		y = displayHeight-by.get();
        if (Mouse.isGrabbed) {
    		dx = x-centerX;
    		dy = y-centerY;
            glfwSetCursorPos(GLGame.windowId, centerX, centerY);
        } else {
        	dx = 0;
        	dy = 0;
        }
	}

}
