package nidefawl.qubes.input;

import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;

import java.nio.DoubleBuffer;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.Memory;

public class Mouse {
    public final static boolean DISABLE_MOUSE_INPUT = false; 

	protected static double dy;
	protected static double dx;
	protected static double x;
	protected static double y;
	public static double scrollDX;
	public static double scrollDY;

    private static double lastX, lastY;
    static DoubleBuffer bx;
    static DoubleBuffer by;
	public static void init() {

        bx = Memory.createDoubleBuffer(1);
        by = Memory.createDoubleBuffer(1);
        lastX = lastY = -1;
        setLastPos();
	}
	public static double getDX() {
	    double _dx = dx;
	    dx = 0;
		return _dx;
	}

	public static double getDY() {
        double _dy = dy;
        dy = 0;
        return _dy;
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
		glfwSetCursorPos(GameBase.windowId, i, j);
	}

	static boolean isGrabbed;
	public static boolean isGrabbed() {
		return isGrabbed;
	}

	public static void setGrabbed(boolean b) {
        if (b != isGrabbed) {
            setLastPos();
        }
		isGrabbed = b;
        GLFW.glfwSetInputMode(GameBase.windowId, GLFW.GLFW_CURSOR, b ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
	}

	private static void setLastPos() {
        bx.position(0);
        by.position(0);
        GLFW.glfwGetCursorPos(GameBase.windowId, bx, by);    
        double bX = bx.get();
        double bY = by.get();
        lastX = bX;
        lastY = bY;
        if (DISABLE_MOUSE_INPUT)
            lastX = lastY = 0;
    }
    public static boolean isButtonDown(int i) {
		return glfwGetMouseButton(GameBase.windowId, GLFW.GLFW_MOUSE_BUTTON_LEFT+i) == GLFW.GLFW_PRESS;
	}

	public static void update(double mx, double my) {
	    if (DISABLE_MOUSE_INPUT)
	        mx = my = 0;
		x = mx;
		y = my;
		
		//make sure input doesn't jump when loosing windows focus
//		if (isGrabbed) {
            dx += x-lastX;
            dy += y-lastY;
//		} else {
//		    dx = 0;
//		    dy = 0;
//		}
        lastX = mx;
        lastY = my;
	}

}
