package nidefawl.qubes.network.server;

import nidefawl.qubes.chat.ChannelManager;
import nidefawl.qubes.crafting.CraftingCategory;
import nidefawl.qubes.crafting.CraftingManager;
import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.inventory.slots.*;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.server.PlayerManager;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.WorldServer;

public class ServerHandlerPlay extends ServerHandler {
    protected final PlayerServer player;
    private int posSyncSent;
    private int posSyncRecv;

    public ServerHandlerPlay(PlayerServer player, ServerHandlerLogin login) {
        super(login.server, login.netServer, login.conn);
        this.player = player;
        this.state = login.state;
    }

    @Override
    public void handlePing(PacketPing p) {

    }
    @Override
    public void update() {
        if (this.state == STATE_CONNECTED) {
            this.state = STATE_PLAYING;
            WorldServer world = this.server.getWorld(player.spawnWorld);
            int flags = 0;
            if (this.player.flying) {
                flags |= 1;
            }
            sendPacket(new PacketSSpawnInWorld(player.id, world.getWorldType(), world.settings, this.player.pos, flags));
            world.addPlayer(player);
            player.syncInventory();
            PacketSWorldBiomes biomes = world.getBiomeManager().getPacket();
            if (biomes != null) {
                biomes.worldID = world.getId();
                sendPacket(biomes);
            }
            sendPacket(new PacketChatChannels(this.player.getJoinedChannels()));
        }
        super.update();
    }

    @Override
    public void handleSwitchWorld(PacketCSwitchWorld packetCSwitchWorld) {
        int idx = packetCSwitchWorld.flags;
        WorldServer[] worlds = this.server.getWorlds();
        if (idx<0||idx>=worlds.length) idx = 0;
        WorldServer worldCurrent = (WorldServer) this.player.world;
        worldCurrent.removePlayer(this.player);
        this.player.onWorldLeave();
        WorldServer world = worlds[idx];
        int flags = 0;
        if (this.player.flying) {
            flags |= 1;
        }
        sendPacket(new PacketSSpawnInWorld(player.id, world.getWorldType(), world.settings, this.player.pos, flags));
        world.addPlayer(player);
        player.syncInventory();
        player.sendMessage("You are now in world "+world.getName());
        PacketSWorldBiomes biomes = world.getBiomeManager().getPacket();
        if (biomes != null) {
            biomes.worldID = world.getId();
            sendPacket(biomes);
        }
    }

    @Override
    public String getHandlerName() {
        return this.handlerName;
    }
    
    @Override
    public void handleDisconnect(PacketDisconnect packetDisconnect) {
        this.conn.disconnect(Connection.REMOTE, packetDisconnect.message);
    }

    @Override
    public void onDisconnect(int from, String reason) {
        try {
            PlayerManager mgr = this.server.getPlayerManager();
            mgr.removePlayer(this.player);
            WorldServer world = (WorldServer) this.player.world;
            if (world != null) {
                world.removePlayer(this.player);
            }
            ChannelManager mgr2 = this.server.getChatChannelMgr();
            mgr2.removeUser(player);
        } catch (Exception e) {
            ErrorHandler.setException(new GameError("Failed removing player", e));
        }
    }

    public void sendPacket(Packet packet) {
        this.conn.sendPacket(packet);
    }

    public void handleSetBlock(PacketCSetBlock p) {
        player.blockPlace.tryPlace(p.pos, p.fpos, p.stack, p.face);
        
    }

    public void handleSetBlocks(PacketCSetBlocks p) {

        boolean hollow = (p.flags & 0x1) != 0;
        int w = p.x2 - p.x + 1;
        int h = p.y2 - p.y + 1;
        int l = p.z2 - p.z + 1;
        BlockPos pos = new BlockPos();
        if (hollow) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    {
                        int blockX = p.x+x;
                        int blockY = p.y+y;
                        int blockZ = p.z ;
                        pos.set(blockX, blockY, blockZ);
                        player.blockPlace.tryPlace(pos, p.fpos, p.stack, p.face);
                    }
                    {
                        int blockX = p.x + x;
                        int blockY = p.y + y;
                        int blockZ = p.z + l-1;
                        pos.set(blockX, blockY, blockZ);
                        player.blockPlace.tryPlace(pos, p.fpos, p.stack, p.face);
                    }
                }
            }
            for (int z = 0; z < l; z++) {
                for (int y = 0; y < h; y++) {
                    {
                        int blockX = p.x;
                        int blockY = p.y+y;
                        int blockZ = p.z+z;
                        pos.set(blockX, blockY, blockZ);
                        player.blockPlace.tryPlace(pos, p.fpos, p.stack, p.face);
                    }
                    {
                        int blockX = p.x + w-1;
                        int blockY = p.y + y;
                        int blockZ = p.z + z;
                        pos.set(blockX, blockY, blockZ);
                        player.blockPlace.tryPlace(pos, p.fpos, p.stack, p.face);
                    }
                }
            }
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < l; z++) {
//                    {
//                        int blockX = p1.x+x;
//                        int blockY = p1.y;
//                        int blockZ = p1.z+z;
//                        this.player.world.setTypeData(blockX, blockY, blockZ, type, data, flags);
//                    }
                    {
                        int blockX = p.x + x;
                        int blockY = p.y + h-1;
                        int blockZ = p.z + z;
                        pos.set(blockX, blockY, blockZ);
                        player.blockPlace.tryPlace(pos, p.fpos, p.stack, p.face);
                    }
                }
            }
        } else {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < l; z++) {
                    for (int y = 0; y < h; y++) {
                        if (hollow) {
                            if (x != 0 && x != w-1)
                                continue;
                            if (y != 0 && y != h-1)
                                continue;
                            if (z != 0 && z != l-1)
                                continue;
                        }
                        int blockX = p.x + x;
                        int blockY = p.y + y;
                        int blockZ = p.z + z;
                        pos.set(blockX, blockY, blockZ);
                        this.player.world.setTypeData(blockX, blockY, blockZ, 0, 0, p.stack.id==0?(Flags.LIGHT|Flags.MARK):0);
                        if (p.stack.id > 0)
                        player.blockPlace.tryPlace(pos, p.fpos, p.stack, p.face);
                    }
                }

            }
        }
    }

    @Override
    public boolean isValidWorld(AbstractPacketWorldRef packet) {
        return packet.getWorldId() == this.player.world.getId();
    }

    public Player getPlayer() {
        return this.player;
    }

    public void handleChat(PacketChatMessage c) {
        String channel = c.channel.trim();
        String msg = c.message.trim();
        if (msg.startsWith("/")) {
            msg = msg.substring(1);
            this.server.getCommandHandler().handle(this.player, msg);
            return;
        }
        this.server.getChatChannelMgr().handlePlayerChat(this.player, channel, msg);
    }

    @Override
    public void handleMovement(PacketCMovement packetMovement) {
        if (this.posSyncRecv == this.posSyncSent) {
            this.player.pos = packetMovement.pos;
            boolean hitground = (packetMovement.flags & 0x1) != 0;
            boolean fly = (packetMovement.flags & 0x2) != 0;
            this.player.hitGround = hitground;
            this.player.flying = fly;
            this.player.yaw = packetMovement.yaw;
            this.player.pitch = packetMovement.pitch;
        }
    }

    public void handleTeleportAck(PacketCTeleportAck packetCTeleportAck) {
        this.posSyncRecv = packetCTeleportAck.sync;
    }

    public void resyncPosition() {
        this.posSyncSent++;
        int flags = 0;
        if (this.player.flying) {
            flags |= 1;
        }
        this.sendPacket(new PacketSTeleport(
                this.player.world.getId(), 
                this.posSyncSent,
                this.player.pos, 
                this.player.yaw, 
                this.player.pitch, 
                flags));
    }

    public void handleDigState(PacketCDigState p) {
        player.blockPlace.tryMine(p.pos, p.fpos, p.stack, p.face, p.stage);
    }

    public void handleInvClick(PacketCInvClick p) {
        Slots slots = player.getSlots(p.id);
        if (slots == null) {
            //TODO: kick + log
            return;
        }
        Slot slot = slots.getSlot(p.idx);
        if (slot == null) {
            //TODO: kick + log
            return;
        }
        BaseStack result = slots.slotClicked(slot, p.button, p.action);
        if (!BaseStack.equalStacks(result, p.stack)) {
            System.err.println("transaction not in sync");
            System.err.println("client "+p.stack);
            System.err.println("server "+result);
            //TODO: resync + log
            this.player.syncInventory();
        }
    }
    public void handleCrafting(PacketCCrafting p) {
        CraftingRecipe recipe = CraftingRecipes.getRecipeId(p.recipeid);
        CraftingCategory cat = CraftingCategory.getCatId(p.catid);
        if (cat == null && recipe != null) {
            cat = recipe.getCategory();
        }
        if (cat == null) {
            kick("Invalid packet. Code 0x2001");
            return;
        }
        CraftingManager mgr = player.getCrafting(cat.getId());
        if (mgr == null) {
            kick("Invalid packet. Code 0x2002");
            return;
        }
        System.err.println("mgr "+mgr.getId());
        int result = mgr.handleRequest(cat, recipe, p.action, p.amount);
        if (result > 0x2000) {
            kick("Invalid packet. Code "+String.format("0x%05X", result));
            return;
        }
        if (result == 1) {
            //crafting inventory contains items but no crafting is undefined
            //this should never happen (unless in dev)
        }
        if (result == 2) {
            //unknown recipe
        }
        if (result == 3) {
            //tried to start crafting while still crafting
        }
        if (result == 4) {
            //tried to stop crafting while not crafting
        }
        if (result == 5) {
            //unmatched recipe (missing input items)
        }
        if (result == 6) {
            //tried to stop crafting after finished
        }
        if (result == 7) {
            //invalid amount
        }
        System.out.println("result "+result);
    }
    public void handleInvTransaction(PacketCInvTransaction p) {
        Slots slots = player.getSlots(p.id);
        if (slots == null) {
            //TODO: kick + log
            return;
        }
        switch (p.action) {
            case 1:
                if (slots instanceof SlotsCrafting) {
                    int n = ((SlotsCrafting)slots).transferSlots((SlotsInventoryBase) player.getSlots(0));
                    if (n != 0) {
                        //no inv space!
                        return;
                    }
                    return;
                }
                kick("Invalid packet. Code 0x2011");
                break;
            default:
                kick("Invalid packet. Code 0x2012");
                return;
        }
    }
}
