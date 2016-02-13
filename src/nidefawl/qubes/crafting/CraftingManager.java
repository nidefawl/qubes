package nidefawl.qubes.crafting;

import java.util.List;

import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.inventory.InventoryUtil;
import nidefawl.qubes.inventory.slots.*;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.network.packet.Packet;
import nidefawl.qubes.network.packet.PacketSCraftingProgress;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;

@SideOnly(value=Side.SERVER)
public class CraftingManager extends CraftingManagerBase {
    protected PlayerServer player;
    

    public CraftingManager(PlayerServer playerServer, int id) {
        this.id = id;
        this.player = playerServer;
    }

    public int handleRequest(CraftingCategory cat, CraftingRecipe recipe, int action, int amount) {
        switch (action) {
            case 0:
              player.sendPacket(getStatePacket(isRunning() ? 2 : 0));
              return 0;
            case 1:
              return craft(cat, recipe, amount);
            case 2:
                if (finished) {
                    return 6;
                }
                if (this.recipe == null) {
                    return 4;
                }
                reset();
                ((SlotsCrafting) player.getSlots(this.id+1)).lock();
                player.sendPacket(getStatePacket(2));
                return 0;
                
        }
        return 0x2004;
    }

    private void reset() {
        this.recipe = null;
        this.startTime = 0;
        this.endTime = 0;
        this.amount = 0;
        this.finished=false;
    }

    public int craft(CraftingCategory cat, CraftingRecipe recipe, int amount) {
        if (this.recipe != null) {
            System.err.println("finished "+getId()+" "+finished);
            return 3;
        }
        if (amount <= 0) {
            return 7;
        }
        if (recipe != null) {
            SlotsInventoryBase inv = (SlotsInventoryBase) this.player.getSlots(0);
            SlotsCrafting crafting = (SlotsCrafting) this.player.getSlots(this.id+1);
            int n = crafting.getNumItems();
            if (n != 0) {
                int n2 = crafting.transferSlots(inv);
                if (n2 != 0) {
                    player.sendPacket(getStatePacket(3));
                    //no inv space!
                    return 1;
                }
            }
            n = findInputs(recipe, inv, crafting, amount);
            if (n != 0) {
                player.sendPacket(getStatePacket(3));
                return 5;
            }
            ((SlotsCrafting) player.getSlots(this.id+1)).lock();
            this.startTime = player.getServer().getServerTime();
            this.endTime = this.startTime + recipe.getTime()*amount;
            this.recipe = recipe;
            this.amount = amount;
            player.sendPacket(getStatePacket(1));
            return 0;
        }
        return 0x2005;
    }

    public boolean update() {
        if (this.recipe != null) {
            if (this.getEndTime()<=this.player.getServer().getServerTime()) {
                if (!finished) {
                    finished = true;
                    SlotsCrafting slots = (SlotsCrafting) player.getSlots(this.id+1);
                    slots.unlock();
                    for (int i = 0; i < slots.getInputSize(); i++) {
                        slots.getSlot(i).drain();
                    }
                    BaseStack[] output = this.recipe.getOut();
                    for (int i = 0; i < output.length; i++) {
                        BaseStack out = output[i];
                        BaseStack result = out.copy();
                        if (result.isBlock()) {
                            result.setSize(amount*result.getSize());
                        }
                        BaseStack left = slots.addStack(result);
                        if (left != null) {
                            // should not happen!!!
                            throw new GameError("Result slot was expected to be empty");
                        }
                    }
                    player.sendPacket(getStatePacket(4));
                }
            }
        }
        if (finished) {
            SlotsCrafting crafting = (SlotsCrafting) this.player.getSlots(this.id+1);
            int n = crafting.getNumItems();
            if (n == 0) {
                reset();
                ((SlotsCrafting) player.getSlots(this.id+1)).lock();
                player.sendPacket(getStatePacket(2));
            }
        }
        return true;
    }

    private int findInputs(CraftingRecipe recipe, SlotsInventoryBase inv, SlotsCrafting crafting, int amount) {
        if (amount <= 0) {
            return 7;
        }
        int n = calcMaxAmount(recipe, inv);
        if (n < amount) {
            return 1;
        }
        BaseStack[] binput = recipe.getIn();
        BaseStack[] input = new BaseStack[binput.length];
        BaseStack[] inputTransferred = new BaseStack[binput.length];
        for (int i = 0; i < binput.length; i++) {
            input[i] = binput[i].copy();
            input[i].setSize(binput[i].getSize()*amount);
            inputTransferred[i] = binput[i].copy();
            inputTransferred[i].setSize(0);
        }
        List<Slot> listSlots = inv.getSlots();
        for (int i = 0; i < inputTransferred.length; i++) {
            BaseStack needle = input[i];
            BaseStack accum = inputTransferred[i];
            for (int j = 0; j < listSlots.size(); j++) {
                Slot slot = listSlots.get(j);
                BaseStack stack = slot.getItem();
                if (stack != null)
                System.out.println("stack["+j+"] "+stack.getId());
                if (stack!=null&&stack.isEqualId(needle)) {
                    System.out.println("isEqualId "+stack);
                    int req = needle.getSize()-accum.getSize();
                    int transfer = Math.min(req, stack.getSize());
                    System.out.println("transfer "+transfer);
                    stack.size-=transfer;
                    if (stack.size < 0) {
                        throw new GameError("NEGATIVE STACK SIZE");
                    }
                    if (stack.size==0) {
                        slot.putStack(null);
                    }
                    slot.flag();
                    accum.size+=transfer;
                    req-=transfer;
                    if (req <= 0) {
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < input.length; i++) {
            
            //assert
            if (inputTransferred[i].getSize()!=input[i].getSize()) {
                throw new GameError("Transferred items mismatch "+inputTransferred[i].getSize()+" - "+input[i].getSize());
            }
            crafting.addStack(inputTransferred[i]);
        }
        return 0;
    }

    public PacketSCraftingProgress getStatePacket(int state) {
        System.err.println("finished "+getId()+" "+isFinished());
        System.err.println(isRunning()+"/"+isFinished());
        PacketSCraftingProgress ps = new PacketSCraftingProgress();
        ps.id = this.getId();
        ps.action = state;
        ps.amount = this.amount;
        ps.recipe = this.recipe == null ? -1 : this.recipe.getId();
        ps.startTime = this.getStartTime();
        ps.endTime = this.getEndTime();
        ps.finished = this.finished;
        ps.currentTime = player.getServer().getServerTime();
        return ps;
    }

}
