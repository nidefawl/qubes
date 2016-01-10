package nidefawl.qubes.crafting;

import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.inventory.slots.Slot;
import nidefawl.qubes.inventory.slots.SlotsCrafting;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketSCraftingProgress;
import nidefawl.qubes.util.GameError;

public class CraftingManager {
    private int id;
    private long startTime;
    private long endTime;
    private CraftingRecipe recipe;
    private PlayerServer player;
    

    public CraftingManager(PlayerServer playerServer, int id) {
        this.id = id;
        this.player = playerServer;
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

    public void handleRequest(int action) {
        switch (action) {
            case 0:
                player.sendPacket(getStatePacket(isRunning() ? 2 : 0));
                return;
            case 1:
                craft();
                return;
            case 2:
                stopCrafting();
                player.slotsCrafting.unlock();
                player.sendPacket(getStatePacket(3));
                return;
                
        }
    }

    private void stopCrafting() {
        this.recipe = null;
        this.startTime = 0;
        this.endTime = 0;
    }

    public void craft() {
        SlotsCrafting slots = player.slotsCrafting;
        this.recipe = CraftingRecipes.findRecipe(slots);
        if (this.recipe != null) {
            if (initCrafting(this.recipe)) {
                player.slotsCrafting.lock();
                this.startTime = player.getServer().getServerTime();
                this.endTime = this.startTime + recipe.getTime();
                player.sendPacket(getStatePacket(1));
            } else {
                this.recipe = null;
                player.sendPacket(getStatePacket(3));
            }
            return;
        }
    }

    public boolean update() {
        if (this.recipe != null) {
            if (this.getEndTime()<=this.player.getServer().getServerTime()) {
                player.slotsCrafting.unlock();
                SlotsCrafting slots = player.slotsCrafting;
                Slot s = slots.getResult();
                if (s.getItem()!=null) {
                    if (!s.transferTo(player.slotsInventory)) {
                        player.slotsCrafting.lock();
                        return false;   
                    }
                }
                for (int i = 0; i < slots.getInputSize(); i++) {
                    slots.getSlot(i).drain();
                }
//                BaseStack left = s.putStack(this.recipe.getOut().copy());
//                if (left != null) {
//                    // should not happen!!!
//                    throw new GameError("Result slot was expected to be empty");
//                }
                stopCrafting();
                player.sendPacket(getStatePacket(4));
                return true;
            }
        }
        return false;   
    }

    private boolean initCrafting(CraftingRecipe recipe) {
        SlotsCrafting slots = player.slotsCrafting;
        Slot s = slots.getResult();
        if (s.getItem()!=null) {
            if (!s.transferTo(player.slotsInventory)) {
                return false;   
            }
        }
        for (int i = 0; i < slots.getInputSize(); i++) {
            s = slots.getSlot(i);
            BaseStack stack = s.getItem();
            if (stack != null) {
                Item item = stack.getItem();
                if (!recipe.isInput(item)) {
                    if (!s.transferTo(player.slotsInventory)) {
                        return false;   
                    }
                }
            }
        }
        return true;
    }

    public PacketSCraftingProgress getStatePacket(int state) {
        PacketSCraftingProgress ps = new PacketSCraftingProgress();
        ps.id = this.getId();
        ps.action = state;
        ps.recipe = this.recipe == null ? -1 : this.recipe.getId();
        ps.startTime = this.getStartTime();
        ps.endTime = this.getEndTime();
        ps.currentTime = player.getServer().getServerTime();
        return ps;
    }

}
