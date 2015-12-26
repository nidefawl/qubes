package nidefawl.qubes.gui;

public class GuiAction {
    public static final int  ACTION_NONE              = -1;
    public static final int  ACTION_RESIZE            = -3;
    public static final int  ACTION_TABCLOSE          = -18;
    public static final int  ACTION_TABSWAP           = 1;
    public static final int  ACTION_TABCLICK          = -5;
    public static final int  ACTION_DRAGSCROLLBAR     = -2;
    public static final int  ACTION_TABMENU           = -19;
    public static final int  ACTION_USERLIST          = -20;
    public static final int  ACTION_DRAGUSERSCROLLBAR = -21;
    public static final int  ACTION_USERMENU          = -22;
    public static final int  ACTION_DRAG_OBJ          = -23;
    private static int       mouseAction              = -1;
    public static boolean isAct(int action) {
        return mouseAction == action || (action == ACTION_TABSWAP && mouseAction >= 0);
    }

    public static void setAct(int action) {
        mouseAction = action;
    }

}
