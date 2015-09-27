package nidefawl.qubes.input;

import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.MapMaker;

import nidefawl.qubes.GameBase;

public class Keyboard {
    public static boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(GameBase.windowId, key) == GLFW.GLFW_PRESS;
    }

    public static boolean getState(int action) {
        return action == GLFW.GLFW_PRESS;
    }

    static final Map<Integer, Keybinding> keybindings = new MapMaker().makeMap();

    /**
     * @param keybinding
     */
    public static void addKeyBinding(Keybinding keybinding) {
        keybindings.put(keybinding.getKey(), keybinding);
    }

    /**
     * @param key
     * @return
     */
    public static Keybinding getKeyBinding(int key) {
        return keybindings.get(key);
    }

}
