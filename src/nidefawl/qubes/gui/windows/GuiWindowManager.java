package nidefawl.qubes.gui.windows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.*;
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
        if (GuiContext.canWindowsFocusChange) {
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
            GuiWindow window = (GuiWindow) windowClass.newInstance();
            window.initGui(true);
            addWindow(window, true);
            return window;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GuiWindow openWindow(final Class<?> windowClass) {
        return openWindow(windowClass, true);
    }

    public static <T> T getWindow(final Class<T> windowClass) {
        for (final GuiWindow w : windowList.values()) {
            if (w.getClass().equals(windowClass)) {
                return (T) w;
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
    public static void closeAll() {

        for (final GuiWindow GuiWindow : windowList.values()) {
            if (GuiWindow.visible)
                GuiWindow.close();
        }
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
//            mouseX = mouseGetX();
//            mouseY = mouseGetY();
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
        setTooltip(null);
        int highestIdx = getHighestIndex();
        GuiWindow mouseOver = getMouseOver(mouseX, mouseY);
        if (!GuiAction.isAct(GuiAction.ACTION_NONE)) {
            mouseOver = null;
        }
        Gui btnGui = (Gui) (Gui.selectedButton!=null&&(Gui.selectedButton.parent instanceof Gui)?Gui.selectedButton.parent:null);

        int a = 0;
        float windowDepth = 220F;
        float z = 0;
        highestIdx = getHighestIndex();
        int nDrawn = 0;
        for (; a < highestIdx; a++) {
            final GuiWindow w = windowList.get(a);
            if (w == null)
                continue;
            if (w == windowFocus)
                continue;
            if (!w.visible)
                continue;
            Engine.pxStack.push(0, 0, z);
            double mX = (w == mouseOver||w==btnGui) ? mouseX : -111;
            double mY = (w == mouseOver||w==btnGui) ? mouseY : -111;
            w.renderFrame(fTime, mX, mY);
            w.render(fTime, mX, mY);
            nDrawn++;
            z+=windowDepth;
            Engine.pxStack.pop();
            
        }
        z+=windowDepth;
        Engine.pxStack.push(0, 0, z);
//        GL11.glTranslatef(0, 0, windowDepth);
        if (windowFocus != null && windowFocus.visible) {
            double mX = (windowFocus == mouseOver||windowFocus==btnGui) ? mouseX : -111;
            double mY = (windowFocus == mouseOver||windowFocus==btnGui) ? mouseY : -111;
            windowFocus.renderFrame(fTime, mX, mY);
            windowFocus.render(fTime, mX, mY);
            nDrawn++;
        }
        Engine.pxStack.push(0, 0, windowDepth+windowDepth);
//        super.drawScreen(i, j, f);
        if (nDrawn > 0) {
            Player player = Game.instance != null ? Game.instance.getPlayer() : null;
            BaseStack stack = player == null? null:player.getInventory().getCarried();
            if (stack != null) {
                int slotW = 48;
                float inset = 4;
                float inset2 = 2;
                Engine.itemRender.drawItem(stack, (float)mouseX+inset-slotW/2, (float)mouseY+inset-slotW/2, slotW-inset*2, slotW-inset*2);
                Engine.itemRender.drawItemOverlay(stack, (float)mouseX+inset-slotW/2, (float)mouseY+inset-slotW/2, slotW-inset*2, slotW-inset*2);
            } else {
                renderTooltip(fTime, mouseX, mouseY);
            }
        }
        Engine.pxStack.pop();
        Engine.pxStack.pop();
        
    }
    void renderTooltip(float fTime, double mouseX, double mouseY) {
        if (tooltip != null) {
            Engine.pxStack.translate(0, 0, 100);
            tooltip.render(fTime, mouseX, mouseY);
        }
    }

    static Tooltip tooltip;
    public static void setTooltip(Tooltip tt) {
        if (tt != null) {
            if (dragged != null || resized != null || !GuiAction.isAct(GuiAction.ACTION_NONE))
                return;
        }
        tooltip = tt;
    }
    public static Tooltip getTooltip() {
        return tooltip;
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
        GuiWindow window = getMouseOver(GuiContext.mouseX, GuiContext.mouseY);
        if (window != null) {
            /*return*/ window.onMouseClick(button, action);
            return true;
        }
        if (Gui.selectedButton != null && Gui.selectedButton.parent instanceof GuiWindow) {
            ((Gui) Gui.selectedButton.parent).onMouseClick(button, action);
        }
        return false;
    }
    public static GuiWindow getMouseOver(double mX, double mY) {
        if (GuiContext.hasOverride) {
            return GuiContext.mouseOverOverride;
        }
        if (GameBase.baseInstance.isGrabbed()) {
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

    public static boolean onWheelScroll(double xoffset, double yoffset) {
        if (windowFocus != null && windowFocus.visible) {
            return windowFocus.onWheelScroll(xoffset, yoffset);
//            return true;
        }
        return false;
    }

    public static void onWindowClosed(final GuiWindow window) {
        if (Game.instance != null) {
            Game.instance.onGuiClosed(window, null);
        }
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
            onWindowManagerClose();
        }
    }
    public static void onWindowManagerClose() {
//        if (Game.instance != null)
//            GameBase.baseInstance.setGrabbed(Game.instance.getWorld()!=null);
    }

    public static void onWindowOpened(GuiWindow guiWindow) {
//        boolean anyVisible = anyWindowVisible();
//        if (anyVisible) {
//            GameBase.baseInstance.setGrabbed(false);
//        }
        if (Game.instance != null) {
            Game.instance.onGuiOpened(guiWindow, null);
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
