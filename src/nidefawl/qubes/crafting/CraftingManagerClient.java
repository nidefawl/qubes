package nidefawl.qubes.crafting;

import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gui.crafting.GuiCraftingProgressEntry;
import nidefawl.qubes.gui.crafting.GuiCraftingSelect;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.network.packet.PacketSCraftingProgress;

public class CraftingManagerClient extends CraftingManagerBase {
    public GuiCraftingProgressEntry guiElement;
    private PlayerSelf player;
    private long recvTime;
    private int state;

    public CraftingManagerClient(PlayerSelf playerSelf, int id) {
        this.id = id;
        this.player = playerSelf;
        guiElement = new GuiCraftingProgressEntry(this);
    }
    public GuiCraftingProgressEntry getGuiElement() {
        return this.guiElement;
    }

    public int getState() {
        return this.state;
    }


    public void handleRequest(int action, PacketSCraftingProgress p) {
        long relativeEnd=p.endTime-p.currentTime;
        long relativeStart=p.startTime-p.currentTime;
        this.startTime = System.currentTimeMillis()+relativeStart;
        this.endTime = System.currentTimeMillis()+relativeEnd;
        this.recvTime = System.currentTimeMillis();
        this.amount = p.amount;
        this.recipe = CraftingRecipes.getRecipeId(p.recipe);
        this.finished = p.finished;
        this.state = action;
        this.guiElement.updateState();
        GuiCraftingSelect w = GuiWindowManager.getWindow(GuiCraftingSelect.class);
        if (w != null) {
            w.onRemoteUpdate(this, action);
        }
    }
}
