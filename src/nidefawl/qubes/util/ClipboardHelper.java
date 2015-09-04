package nidefawl.qubes.util;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;

public class ClipboardHelper {

    public static void setClipboardString(String s) {
        GLFW.glfwSetClipboardString(Game.windowId, s);
    }

    public static String getClipboardString() {
        String s = ""+GLFW.glfwGetClipboardString(Game.windowId);
        return s;
    }
}
