package nidefawl.qubes.gui.windows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.GuiAction;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Renderable;

public class GuiWindowManager implements Renderable  {
    static final GuiWindowManager singleton = new GuiWindowManager();
    private final static Map<Integer, GuiWindow> windowList = new ConcurrentHashMap<Integer, GuiWindow>();
    private static GuiWindow windowFocus;
    public static GuiWindowManager getInstance() {
        return singleton;
    }

    public static void setWindowFocus(final GuiWindow newFocus) {
        do_setWindowFocus(newFocus);
        boolean hasAlwaysOnTop = false;
        for (final Integer a : windowList.keySet()) {
            if (windowList.get(a).allwaysVisible)
                hasAlwaysOnTop = true;
        }
        if (hasAlwaysOnTop) {
            ArrayList<GuiWindow> onTop = new ArrayList<GuiWindow>();
            int low = -1;
            for (final Integer a : windowList.keySet()) {
                if (windowList.get(a).allwaysVisible) {
                    onTop.add(windowList.get(a));
                }
            }
            boolean hasNormalWindowsOnTop = false;
            for (final Integer a : windowList.keySet()) {
                if (a > low) {
                    if (!windowList.get(a).allwaysVisible) {
                        hasNormalWindowsOnTop = true;
                        break;
                    }
                }
            }
            if (hasNormalWindowsOnTop) {
                for (GuiWindow toRemove : onTop) {
                    do_setWindowFocus(toRemove);
                }
            }
        }
    }

    public static void do_setWindowFocus(final GuiWindow newFocus) {
        if (windowFocus != null && windowFocus != newFocus) {
            windowFocus.onDefocus();
            windowFocus = newFocus;
            if (windowFocus != null)
                windowFocus.onFocus();
        } else {
            windowFocus = newFocus;
        }

        int high = 0;
        for (final Integer a : windowList.keySet()) {
            if (a > high)
                high = a;
        }
        if (windowFocus != null && windowList.get(high) != windowFocus) {
            Integer indexNow = null;
            for (final Integer a : windowList.keySet()) {
                if (windowList.get(a) == windowFocus) {
                    indexNow = a;
                    break;
                }
            }
            if (indexNow != null) {
                final GuiWindow w = windowList.remove(indexNow);
                final int newIndex = high + 1;
                windowList.put(newIndex, w);
            }
        }
    }
    public static int getHighestIndex() {
        int high = 0;
        for (final Integer a : windowList.keySet()) {
            if (a > high)
                high = a;
        }
        return high;
    }

//    public static GuiWindow getWindowID(final int id, final boolean create, final boolean openNow) {
//        return getWindowID(id, create, openNow, true);
//    }
//
//    public static GuiWindow getWindowID(final int id, final boolean create, final boolean openNow, boolean openWindowManager) {
//        for (final GuiWindow w : windowList.values()) {
//            if (w.getClass() == GuiWindow.class && ((GuiWindow) w).id == id) {
//                if (openNow)
//                    w.open();
//                return (GuiWindow) w;
//            }
//        }
//        if (create) {
//            final GuiWindow popupWindow = new GuiWindow(id, Minecraft.theMinecraft);
//            windowList.put(getHighestIndex() + 1, popupWindow);
//            if (openNow)
//                popupWindow.open();
//            return popupWindow;
//        }
//        return null;
//    }

    public static GuiWindow openWindow(final Class<?> windowClass, final boolean closeIfOpen) {
        for (final GuiWindow w : windowList.values()) {
            if (w.getClass().equals(windowClass)) {

                if (!w.visible) {
                    w.open();
                } else if (closeIfOpen) {
                    w.close();
                }
                return w;
            }
        }
        try {
            GuiWindow window = (GuiWindow) windowClass.getDeclaredConstructors()[0].newInstance();
            window.initGui(true);
            addWindow(window, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GuiWindow openWindow(final Class<?> windowClass) {
        return openWindow(windowClass, true);
    }

    public static GuiWindow getWindow(final Class<?> windowClass) {
        for (final GuiWindow w : windowList.values()) {
            if (w.getClass().equals(windowClass)) {
                return w;
            }
        }
        return null;
    }

    public static boolean anyWindowVisible() {
        for (final GuiWindow GuiWindow : windowList.values()) {
            if (GuiWindow.visible)
                return true;
        }
        return false;
    }


    public static GuiWindow addWindow(final GuiWindow w, final boolean open) {
        windowList.put(getHighestIndex() + 1, w);
        if (open) {
            w.open();
        }
        return w;
    }

    public static GuiWindow removeWindow(final GuiWindow toRemove) {
        final Iterator<GuiWindow> it = windowList.values().iterator();
        while (it.hasNext()) {
            final GuiWindow w = it.next();
            if (w == toRemove) {
                it.remove();
                if (windowList.size() > 0) {
                    windowList.get(0).setFocus();
                }
                return w;
            }
        }
        return null;
    }

    static GuiWindow dragged = null;
    static GuiWindow resized = null;
//
//    public void drawAlwaysVisibleWindows(float f) {
//        float windowDepth = 100F;
//        int mouseX = -9999;
//        int mouseY = -9999;
//        boolean handleMouse = GuiAction.isAct(GuiAction.ACTION_NONE) && mc.currentScreen instanceof GuiChatCland;
//        if (handleMouse) {
//            mouseX = Mouse.getX();
//            mouseY = Mouse.getY();
//        }
//        boolean has = false;
//        for (GuiWindow w : windowList.values()) {
//            if (w == null)
//                continue;
//            if (w.isAllwaysVisible()) {
//                int windowX = w.getX();
//                int windowY = w.getY();
//
//                int rmouseY = (mc.displayHeight - mouseY);
//                int focusMouseY = ((int) (rmouseY / scale)) - windowY;
//                int focusMouseX = ((int) (mouseX / scale)) - windowX;
//                w.drawWindow(focusMouseX, focusMouseY, f);
//                GL11.glTranslatef(0, 0, windowDepth);
//                has = true;
//            }
//        }
//        if (has && handleMouse) {
//            for (; Mouse.next(); ) {
//                boolean handled = false;
//                if (windowFocus != null) {
//                    handled = windowFocus.handleMouseInput();
//                }
//                if (!handled) {
//                    int highest = getHighestIndex();
//                    for (int start = highest; start >= 0; start--) {
//                        final GuiWindow w = windowList.get(start);
//                        if (w == null)
//                            continue;
//                        if (w == windowFocus)
//                            continue;
//                        if (!w.isAllwaysVisible())
//                            continue;
//                        if (w.handleMouseInput()) {
//                            handled = true;
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//    }

    public void render(float fTime, double x, double y) {
        drawWindows(fTime, x, y);
    }

    public void drawWindows(float fTime, double mouseX, double mouseY) {

        int highestIdx = getHighestIndex();
        GuiWindow mouseOver = getMouseOver(mouseX, mouseY);
        if (!GuiAction.isAct(GuiAction.ACTION_NONE)) {
            mouseOver = null;
        }
        

        int a = 0;
        float windowDepth = 30F;
        float z = 0;
        highestIdx = getHighestIndex();
        for (; a < highestIdx; a++) {
            final GuiWindow w = windowList.get(a);
            if (w == null)
                continue;
            if (w == windowFocus)
                continue;
            if (!w.visible)
                continue;
            Engine.pxStack.push(0, 0, z);
            double mX = w == mouseOver ? mouseX : -111;
            double mY = w == mouseOver ? mouseY : -111;
            w.renderFrame(fTime, mX, mY);
            w.render(fTime, mX, mY);
            z+=windowDepth;
            Engine.pxStack.pop();
            
        }
        z+=windowDepth;
        Engine.pxStack.push(0, 0, z);
//        GL11.glTranslatef(0, 0, windowDepth);
        if (windowFocus != null && windowFocus.visible) {
            double mX = windowFocus == mouseOver ? mouseX : -111;
            double mY = windowFocus == mouseOver ? mouseY : -111;
            windowFocus.renderFrame(fTime, mX, mY);
            windowFocus.render(fTime, mX, mY);
        }
        Engine.pxStack.push(0, 0, windowDepth);
//        super.drawScreen(i, j, f);

        Player player = Game.instance.getPlayer();
        BaseStack stack = player.getInventory().getCarried();
        if (stack != null) {
            int slotW = 48;
            float inset = 4;
            float inset2 = 2;
            Engine.itemRender.drawItem(stack, (float)mouseX+inset-slotW/2, (float)mouseY+inset-slotW/2, slotW-inset*2, slotW-inset*2);
            Shaders.textured.enable();
            Engine.itemRender.drawItemOverlay(stack, (float)mouseX+inset-slotW/2, (float)mouseY+inset-slotW/2, slotW-inset*2, slotW-inset*2);
        }
        Engine.pxStack.pop();
        Engine.pxStack.pop();
        
    }

    @Override
    public void initGui(boolean first) {
    }

    public static boolean requiresTextInput() {
        if (windowFocus != null && windowFocus.visible) {
            return windowFocus.requiresTextInput();
        }
        return false;
    }

    public static boolean onTextInput(int codepoint) {
        if (windowFocus != null && windowFocus.visible) {
            return windowFocus.onTextInput(codepoint);
        }
        return false;
    }

    public static GuiWindow getWindowFocus() {
        return windowFocus;
    }
    public static boolean onMouseClick(int button, int action) {
        if (action == GLFW.GLFW_RELEASE) {
            dragged = resized = null;
        }
        GuiWindow window = getMouseOver(Mouse.getX(), Mouse.getY());
        if (window != null) {
            /*return*/ window.onMouseClick(button, action);
            return true;
        }
        return false;
    }
    public static GuiWindow getMouseOver(double mX, double mY) {
        if (Game.instance.movement.grabbed()) {
            return null;
        }
        int highest = getHighestIndex();
        for (int start = highest; start >= 0; start--) {
            final GuiWindow w = windowList.get(start);
            if (w == null)
                continue;
            if (!w.visible)
                continue;
            if (!w.mouseOver(mX, mY))
                continue;
            return w;
        }
        return null;
    }
    public static void mouseMove(double mX, double mY) {
        if (GuiAction.isAct(GuiAction.ACTION_NONE)) {
            if (dragged != null) {
                dragged.onDrag(mX, mY);
            }
            if (resized != null) {
                resized.onResize(mX, mY);
            }
        }
        return ;
    }
    public static boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (windowFocus != null && windowFocus.visible) {
            return windowFocus.onKeyPress(key, scancode, action, mods);
//            return true;
        }
        return false;
    }
    public static void onWindowClosed(final GuiWindow window) {
        if (window.removeOnClose()) {
            windowList.values().remove(window);
        }
        int highestIDX = -1;
        GuiWindow top = null;
        for (final Integer a : windowList.keySet()) {
            GuiWindow w = windowList.get(a);

            if (w.visible && (highestIDX < 0 || a > highestIDX)) {
                top = w;
                highestIDX = a;
            }
        }
        if (top != null) {
            top.setFocus();
            return;
        }
        boolean anyVisible = anyWindowVisible();
        if (!anyVisible) {
            Game.instance.setGrabbed(true);
        }
    }
    public static void onWindowOpened(GuiWindow guiWindow) {
        boolean anyVisible = anyWindowVisible();
        if (anyVisible) {
            Game.instance.setGrabbed(false);
        }
    }
    public static void update() {
        for (final Integer a : windowList.keySet()) {
            GuiWindow w = windowList.get(a);
            if (w != null)
                w.update();
        }
    }


}