package nidefawl.qubes.crafting;

import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gui.windows.GuiCrafting;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.network.packet.PacketSCraftingProgress;

public class CraftingManagerClient {

    private int id;
    private PlayerSelf player;
    private long startTime;
    private long endTime;
    private CraftingRecipe recipe;
    private long serverTime;
    private long recvTime;
    private int state;

    public CraftingManagerClient(PlayerSelf playerSelf, int id) {
        this.id = id;
        this.player = playerSelf;
    }

    public int getId() {
        return this.id;
    }

    public boolean isRunning() {
        return this.recipe != null;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }
    public CraftingRecipe getRecipe() {
        return this.recipe;
    }
    public int getState() {
        return this.state;
    }
    public long getRecvTime() {
        return this.recvTime;
    }
    public long getServerTime() {
        return this.serverTime;
    }


    public void handleRequest(int action, PacketSCraftingProgress p) {
        this.startTime = p.startTime;
        this.endTime = p.endTime;
        this.serverTime = p.currentTime;
        this.recvTime = System.currentTimeMillis();
        this.recipe = CraftingRecipes.getRecipeId(p.id);
        this.state = action;
//        switch (action) {
//            case 0: //idle/not running
//                return;
//            case 1: //starting
//                return;
//            case 2: //running
//                return;
//            case 3: //cancel
//                return;
//            case 4: //success/finish
//                return;
//        }
        System.out.println("handle");
        GuiCrafting w = GuiWindowManager.getWindow(GuiCrafting.class);
        if (w != null) {
            w.onRemoteUpdate(action);
        }
    }
}
