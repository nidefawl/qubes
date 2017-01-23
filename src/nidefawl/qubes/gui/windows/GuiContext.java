package nidefawl.qubes.gui.windows;

import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.gui.controls.TextField;

public class GuiContext {
    
    public static TextField input = null;
    public static ScrollList scrolllist = null;
    public static double mouseX;
    public static double mouseY;
    public static boolean hasOverride;
    public static GuiWindow mouseOverOverride;
    public static boolean canDragWindows = true;
    public static boolean canWindowsFocusChange = true;

}
