package nidefawl.qubes.input;

import java.util.HashMap;
import org.lwjgl.glfw.GLFW;

import com.google.common.collect.Maps;

import nidefawl.qubes.GameBase;

public class Keyboard {
    static final HashMap<Integer, String> keyNames = Maps.newHashMap();
    static {
        keyNames.put(GLFW.GLFW_KEY_UNKNOWN, "UNKNOWN");
        keyNames.put(GLFW.GLFW_KEY_SPACE, "SPACE");
        keyNames.put(GLFW.GLFW_KEY_APOSTROPHE, "APOSTROPHE");
        keyNames.put(GLFW.GLFW_KEY_COMMA, "COMMA");
        keyNames.put(GLFW.GLFW_KEY_MINUS, "MINUS");
        keyNames.put(GLFW.GLFW_KEY_PERIOD, "PERIOD");
        keyNames.put(GLFW.GLFW_KEY_SLASH, "SLASH");
        keyNames.put(GLFW.GLFW_KEY_0, "0");
        keyNames.put(GLFW.GLFW_KEY_1, "1");
        keyNames.put(GLFW.GLFW_KEY_2, "2");
        keyNames.put(GLFW.GLFW_KEY_3, "3");
        keyNames.put(GLFW.GLFW_KEY_4, "4");
        keyNames.put(GLFW.GLFW_KEY_5, "5");
        keyNames.put(GLFW.GLFW_KEY_6, "6");
        keyNames.put(GLFW.GLFW_KEY_7, "7");
        keyNames.put(GLFW.GLFW_KEY_8, "8");
        keyNames.put(GLFW.GLFW_KEY_9, "9");
        keyNames.put(GLFW.GLFW_KEY_SEMICOLON, "SEMICOLON");
        keyNames.put(GLFW.GLFW_KEY_EQUAL, "EQUAL");
        keyNames.put(GLFW.GLFW_KEY_A, "A");
        keyNames.put(GLFW.GLFW_KEY_B, "B");
        keyNames.put(GLFW.GLFW_KEY_C, "C");
        keyNames.put(GLFW.GLFW_KEY_D, "D");
        keyNames.put(GLFW.GLFW_KEY_E, "E");
        keyNames.put(GLFW.GLFW_KEY_F, "F");
        keyNames.put(GLFW.GLFW_KEY_G, "G");
        keyNames.put(GLFW.GLFW_KEY_H, "H");
        keyNames.put(GLFW.GLFW_KEY_I, "I");
        keyNames.put(GLFW.GLFW_KEY_J, "J");
        keyNames.put(GLFW.GLFW_KEY_K, "K");
        keyNames.put(GLFW.GLFW_KEY_L, "L");
        keyNames.put(GLFW.GLFW_KEY_M, "M");
        keyNames.put(GLFW.GLFW_KEY_N, "N");
        keyNames.put(GLFW.GLFW_KEY_O, "O");
        keyNames.put(GLFW.GLFW_KEY_P, "P");
        keyNames.put(GLFW.GLFW_KEY_Q, "Q");
        keyNames.put(GLFW.GLFW_KEY_R, "R");
        keyNames.put(GLFW.GLFW_KEY_S, "S");
        keyNames.put(GLFW.GLFW_KEY_T, "T");
        keyNames.put(GLFW.GLFW_KEY_U, "U");
        keyNames.put(GLFW.GLFW_KEY_V, "V");
        keyNames.put(GLFW.GLFW_KEY_W, "W");
        keyNames.put(GLFW.GLFW_KEY_X, "X");
        keyNames.put(GLFW.GLFW_KEY_Y, "Y");
        keyNames.put(GLFW.GLFW_KEY_Z, "Z");
        keyNames.put(GLFW.GLFW_KEY_LEFT_BRACKET, "LEFT_BRACKET");
        keyNames.put(GLFW.GLFW_KEY_BACKSLASH, "BACKSLASH");
        keyNames.put(GLFW.GLFW_KEY_RIGHT_BRACKET, "RIGHT_BRACKET");
        keyNames.put(GLFW.GLFW_KEY_GRAVE_ACCENT, "GRAVE_ACCENT");
        keyNames.put(GLFW.GLFW_KEY_WORLD_1, "WORLD_1");
        keyNames.put(GLFW.GLFW_KEY_WORLD_2, "WORLD_2");
        keyNames.put(GLFW.GLFW_KEY_ESCAPE, "ESCAPE");
        keyNames.put(GLFW.GLFW_KEY_ENTER, "ENTER");
        keyNames.put(GLFW.GLFW_KEY_TAB, "TAB");
        keyNames.put(GLFW.GLFW_KEY_BACKSPACE, "BACKSPACE");
        keyNames.put(GLFW.GLFW_KEY_INSERT, "INSERT");
        keyNames.put(GLFW.GLFW_KEY_DELETE, "DELETE");
        keyNames.put(GLFW.GLFW_KEY_RIGHT, "RIGHT");
        keyNames.put(GLFW.GLFW_KEY_LEFT, "LEFT");
        keyNames.put(GLFW.GLFW_KEY_DOWN, "DOWN");
        keyNames.put(GLFW.GLFW_KEY_UP, "UP");
        keyNames.put(GLFW.GLFW_KEY_PAGE_UP, "PAGE_UP");
        keyNames.put(GLFW.GLFW_KEY_PAGE_DOWN, "PAGE_DOWN");
        keyNames.put(GLFW.GLFW_KEY_HOME, "HOME");
        keyNames.put(GLFW.GLFW_KEY_END, "END");
        keyNames.put(GLFW.GLFW_KEY_CAPS_LOCK, "CAPS_LOCK");
        keyNames.put(GLFW.GLFW_KEY_SCROLL_LOCK, "SCROLL_LOCK");
        keyNames.put(GLFW.GLFW_KEY_NUM_LOCK, "NUM_LOCK");
        keyNames.put(GLFW.GLFW_KEY_PRINT_SCREEN, "PRINT_SCREEN");
        keyNames.put(GLFW.GLFW_KEY_PAUSE, "PAUSE");
        keyNames.put(GLFW.GLFW_KEY_F1, "F1");
        keyNames.put(GLFW.GLFW_KEY_F2, "F2");
        keyNames.put(GLFW.GLFW_KEY_F3, "F3");
        keyNames.put(GLFW.GLFW_KEY_F4, "F4");
        keyNames.put(GLFW.GLFW_KEY_F5, "F5");
        keyNames.put(GLFW.GLFW_KEY_F6, "F6");
        keyNames.put(GLFW.GLFW_KEY_F7, "F7");
        keyNames.put(GLFW.GLFW_KEY_F8, "F8");
        keyNames.put(GLFW.GLFW_KEY_F9, "F9");
        keyNames.put(GLFW.GLFW_KEY_F10, "F10");
        keyNames.put(GLFW.GLFW_KEY_F11, "F11");
        keyNames.put(GLFW.GLFW_KEY_F12, "F12");
        keyNames.put(GLFW.GLFW_KEY_F13, "F13");
        keyNames.put(GLFW.GLFW_KEY_F14, "F14");
        keyNames.put(GLFW.GLFW_KEY_F15, "F15");
        keyNames.put(GLFW.GLFW_KEY_F16, "F16");
        keyNames.put(GLFW.GLFW_KEY_F17, "F17");
        keyNames.put(GLFW.GLFW_KEY_F18, "F18");
        keyNames.put(GLFW.GLFW_KEY_F19, "F19");
        keyNames.put(GLFW.GLFW_KEY_F20, "F20");
        keyNames.put(GLFW.GLFW_KEY_F21, "F21");
        keyNames.put(GLFW.GLFW_KEY_F22, "F22");
        keyNames.put(GLFW.GLFW_KEY_F23, "F23");
        keyNames.put(GLFW.GLFW_KEY_F24, "F24");
        keyNames.put(GLFW.GLFW_KEY_F25, "F25");
        keyNames.put(GLFW.GLFW_KEY_KP_0, "KP_0");
        keyNames.put(GLFW.GLFW_KEY_KP_1, "KP_1");
        keyNames.put(GLFW.GLFW_KEY_KP_2, "KP_2");
        keyNames.put(GLFW.GLFW_KEY_KP_3, "KP_3");
        keyNames.put(GLFW.GLFW_KEY_KP_4, "KP_4");
        keyNames.put(GLFW.GLFW_KEY_KP_5, "KP_5");
        keyNames.put(GLFW.GLFW_KEY_KP_6, "KP_6");
        keyNames.put(GLFW.GLFW_KEY_KP_7, "KP_7");
        keyNames.put(GLFW.GLFW_KEY_KP_8, "KP_8");
        keyNames.put(GLFW.GLFW_KEY_KP_9, "KP_9");
        keyNames.put(GLFW.GLFW_KEY_KP_DECIMAL, "KP_DECIMAL");
        keyNames.put(GLFW.GLFW_KEY_KP_DIVIDE, "KP_DIVIDE");
        keyNames.put(GLFW.GLFW_KEY_KP_MULTIPLY, "KP_MULTIPLY");
        keyNames.put(GLFW.GLFW_KEY_KP_SUBTRACT, "KP_SUBTRACT");
        keyNames.put(GLFW.GLFW_KEY_KP_ADD, "KP_ADD");
        keyNames.put(GLFW.GLFW_KEY_KP_ENTER, "KP_ENTER");
        keyNames.put(GLFW.GLFW_KEY_KP_EQUAL, "KP_EQUAL");
        keyNames.put(GLFW.GLFW_KEY_LEFT_SHIFT, "LEFT_SHIFT");
        keyNames.put(GLFW.GLFW_KEY_LEFT_CONTROL, "LEFT_CONTROL");
        keyNames.put(GLFW.GLFW_KEY_LEFT_ALT, "LEFT_ALT");
        keyNames.put(GLFW.GLFW_KEY_LEFT_SUPER, "LEFT_SUPER");
        keyNames.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RIGHT_SHIFT");
        keyNames.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RIGHT_CONTROL");
        keyNames.put(GLFW.GLFW_KEY_RIGHT_ALT, "RIGHT_ALT");
        keyNames.put(GLFW.GLFW_KEY_RIGHT_SUPER, "RIGHT_SUPER");
        keyNames.put(GLFW.GLFW_KEY_MENU, "MENU");
    }
    public static boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(GameBase.windowId, key) == GLFW.GLFW_PRESS;
    }

    public static boolean getState(int action) {
        return action == GLFW.GLFW_PRESS;
    }

    public static String getKeyName(int key) {
        String s = keyNames.get(key);
        return s == null ? ""+key : s;
    }

}
