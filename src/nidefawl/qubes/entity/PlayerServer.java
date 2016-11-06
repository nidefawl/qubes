/**
 * 
 */
package nidefawl.qubes.entity;

import java.util.*;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import nidefawl.qubes.chat.ChatUser;
import nidefawl.qubes.chat.channel.GlobalChannel;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.crafting.CraftingCategory;
import nidefawl.qubes.crafting.CraftingManager;
import nidefawl.qubes.inventory.PlayerInventoryCrafting;
import nidefawl.qubes.inventory.slots.*;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.network.server.ServerHandlerPlay;
import nidefawl.qubes.player.EntityData;
import nidefawl.qubes.player.PlayerData;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.PlayerEntityTracker;
import nidefawl.qubes.server.commands.Command;
import nidefawl.qubes.server.commands.CommandException;
import nidefawl.qubes.server.commands.ICommandSource;
import nidefawl.qubes.server.compress.CompressChunks;
import nidefawl.qubes.server.compress.CompressThread;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.BlockPlacer;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.world.structure.mine.Mine;
import nidefawl.qubes.world.structure.tree.Tree;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */

@SideOnly(value=Side.SERVER)
public class PlayerServer extends Player implements ChatUser, ICommandSource {

    public ServerHandlerPlay         netHandler;
    public boolean                   flying;
    public int                       chunkX;
    public int                       chunkZ;
    public boolean                   chunkTracked;
    Set<Long>                        chunks         = Sets.newLinkedHashSet();
    Set<Long>                        sendChunks     = Sets.newLinkedHashSet();
    int                              lastLight      = 0;
    public UUID                      spawnWorld;
    private int                      chunkLoadDistance;
    private Set<String>              joinedChannels = Sets.newConcurrentHashSet();
    public BlockPlacer               blockPlace     = new BlockPlacer(this);
    public HashMap<UUID, Vector3f>   worldPositions = Maps.newHashMap();
    public final PlayerEntityTracker entTracker     = new PlayerEntityTracker(this);
    public final CraftingManager[] crafting = new CraftingManager[CraftingCategory.NUM_CATS];
    /**
     * 
     */
    public PlayerServer() {
        super();
        this.equipment = new BaseStack[1];
        this.slotsInventory = new SlotsInventory(this);
        for (int i = 0; i < CraftingCategory.NUM_CATS; i++) {
            this.slotsCrafting[i] = new SlotsCrafting(this, i+1);
            this.crafting[i] = new CraftingManager(this, i);
        }
    }
    public CraftingManager getCrafting(int id) {
        return id < 0 | id >= this.crafting.length ? null : this.crafting[id];
    }

    @Override
    public void tickUpdate() {
        if (!this.sendChunks.isEmpty()) {
            Iterator<Long> it = this.sendChunks.iterator();
            Set<Chunk> chunks = null;
            while (it.hasNext()) {
                long l = it.next();
                int x = GameMath.lhToX(l);
                int z = GameMath.lhToZ(l);
                Chunk c = this.world.getChunkManager().get(x, z);
                if (c != null) {
                    if (chunks == null) {
                        chunks = Sets.newLinkedHashSet();
                    }
//                    System.out.println("send chunk "+c);
                    chunks.add(c);
                    it.remove();
                }
            }
            if (chunks != null) {
//                System.out.println("got list with "+chunks.size()+" chunks for client ");
                Iterable<List<Chunk>> iter = Iterables.partition(chunks, 16);
                for (List<Chunk> list : iter) {
                    CompressThread.submit(new CompressChunks(this.world.getId(), list, new ServerHandlerPlay[] {this.netHandler}, true));
                }
            }
        }
        this.equipment[0] = this.inventory.getItem(0);
//        if (lastLight++>444) {
//            lastLight = 0;
//            Iterator<Long> it  = this.chunks.iterator();
//            Set<Chunk> chunks = null;
//            while (it.hasNext()) {
//                long l = it.next();
//                int x = GameMath.lhToX(l);
//                int z = GameMath.lhToZ(l);
//                Chunk c = this.world.getChunkManager().get(x, z);
//                if (c != null) {
//                    if (chunks == null) {
//                        chunks = Sets.newHashSet();
//                    }
////                    System.out.println("send chunk "+c);
//                    chunks.add(c);
//                    if (chunks.size() > 31)
//                        break;
//                }
//            }
//            if (chunks != null) {
////              System.out.println("got list with "+chunks.size()+" chunks for client ");
//              Iterable<List<Chunk>> iter = Iterables.partition(chunks, 16);
//              for (List<Chunk> list : iter) {
//              }
//          }
//        }
        this.entTracker.update();
        
        if (this.ticks1++%20==0) {
            HexBiome biome = this.world.getHex(GameMath.floor(this.pos.x), GameMath.floor(this.pos.z));
            if (biome != null) {
                ArrayList<AABB> trees = new ArrayList<>();
                HexBiome[] neighbours = biome.getClosest3(this.pos.x, this.pos.z);
                for (int i = 0; i < neighbours.length; i++) {
                    Collection<Tree> list = neighbours[i].getTrees().getRegions(GameMath.floor(this.pos.x), GameMath.floor(this.pos.z), 16);
                    Collection<Mine> list2 = neighbours[i].getMines().getRegions(GameMath.floor(this.pos.x), GameMath.floor(this.pos.z), 16);
                    if (list != null) {
                        for (Tree tree : list) {
                            AABB bb1 = new AABB(tree.bb);
                            AABB bb2 = new AABB(tree.trunkBB);
                            bb1.expandTo(1, 1, 1);  
                            bb2.expandTo(1, 1, 1);
                            trees.add(bb1);
                            trees.add(bb2);
                        }
                    }
                    if (list2 != null) {
                        for (Mine tree : list2) {
                            AABB bb1 = new AABB(tree.bb);
                            bb1.expandTo(1, 1, 1);  
                            trees.add(bb1);
                        }
                    }
                }
                this.sendPacket(new PacketSDebugBB(trees));
            }
//        ArrayList<AABB> list = new ArrayList<>();
//        for (Entity e : this.getWorld().entityList) {
//            list.add(e.aabb.copy());
        }
//        ((WorldServer) this.world).broadcastPacket(new PacketSDebugBB(list));
//        }
        for (int i = 0; i < CraftingCategory.NUM_CATS; i++) {
            this.crafting[i].update();
        }
    }

    public void load(EntityData edata) {
        PlayerData data = (PlayerData)edata;
        this.flying = data.flying;
        this.spawnWorld = data.world;
        this.chunkLoadDistance = data.chunkLoadDistance;
        this.joinedChannels.clear();
        this.joinedChannels.addAll(data.joinedChannels);
        this.worldPositions.clear();
        this.worldPositions.putAll(data.worldPositions);
        this.inventory.set(data.invStacks);
        for (int i = 0; i < this.inventoryCraft.length; i++) {
            this.inventoryCraft[i].set(data.invCraftStacks[i]);
            if (data.craftingStates[i] != null) {
                int id = data.craftingStates[i].getInt("id");
                this.crafting[id].load(data.craftingStates[i]);
            }
        }
        if (data.properties != null)
            this.properties.load(data.properties);
    }

    public EntityData save() {
        PlayerData data = new PlayerData();
        data.world = this.world != null ? this.world.getUUID() : null;
        if (data.world != null) {
            this.worldPositions.put(data.world, new Vector3f(this.pos));
        }
        data.worldPositions.putAll(this.worldPositions);
        data.flying = this.flying;
        data.joinedChannels = new HashSet<String>(this.joinedChannels);
        data.invStacks = this.inventory.copySlotStacks();
        for (int i = 0; i < this.inventoryCraft.length; i++) {
            data.invCraftStacks[i] = this.inventoryCraft[i].copySlotStacks();
            data.craftingStates[i] = this.crafting[i].save();
        }
        data.properties = this.properties.save();
        
        return data;
    }


    public int getChunkLoadDistance() {
        return this.chunkLoadDistance;
    }

    public void watchingChunk(long hash, int x, int z) {
        if (this.chunks.add(hash)) {
            this.sendChunks.add(hash);
            this.netHandler.sendPacket(new PacketSTrackChunk(this.world.getId(), x, z, true));
        }
    }

    public void unwatchingChunk(long hash, int x, int z) {
        if (this.chunks.remove(hash)) {
            this.sendChunks.remove(hash);
            if (this.world != null)
                this.netHandler.sendPacket(new PacketSTrackChunk(this.world.getId(), x, z, false));
        } else {
            System.err.println("Expected chunk set to contain "+x+","+z+" ("+hash+")");
        }
    }

    public void kick(String string) {
        this.netHandler.kick(string);
    }

    /**
     * @param chunkLoadDistance2
     */
    public void setChunkLoadDistance(int distance) {
        if (distance < 2 || distance > 32) {
            throw new IllegalArgumentException("Invalid chunk load distance");
        }
        this.chunkLoadDistance = distance;
    }


    @Override
    public Collection<String> getJoinedChannels() {
        return this.joinedChannels;
    }


    @Override
    public void preExecuteCommand(Command c) {
    }

    @Override
    public void onError(Command c, CommandException e) {
        
        if (e.getCause() != null) {
            sendMessage("An exception occured while executing '"+c.getName()+"': "+e.getMessage());
            e.getCause().printStackTrace();
        } else {
            sendMessage(e.getMessage());
        }
    }

    @Override
    public void onUnknownCommand(String cmd, String line) {
        sendMessage("Unknown command '"+cmd+"'");
    }

    @Override
    public GameServer getServer() {
        return ((WorldServer) this.world).getServer();
    }

    @Override
    public void sendMessage(String string) {
        this.netHandler.sendPacket(new PacketChatMessage(GlobalChannel.TAG, string));
    }
    
    @Override
    public String getChatName() {
        return getName();
    }

    @Override
    public void sendMessage(String channel, String string) {
        this.netHandler.sendPacket(new PacketChatMessage(channel, string));
    }

    public void sendPacket(Packet packet) {
        this.netHandler.sendPacket(packet);
    }
    
    /* (non-Javadoc)
     * @see nidefawl.qubes.entity.Entity#move(double, double, double)
     */
    @Override
    public void move(double x, double y, double z) {
        super.move(x, y, z);
        this.netHandler.resyncPosition();
    }

    @Override
    public Tag writeClientData(boolean isUpdate) {
        if (!isUpdate) {
            Tag.Compound tag = new Tag.Compound();
            tag.setString("name", this.name);
            tag.set("properties", this.properties.save());
            return tag;
        }
        return null;
    }


    public BaseStack recvItem(BaseStack itemStack) {
        BaseStack stack = this.slotsInventory.addStack(itemStack);
        return stack;
    }

    public void onWorldLeave() {
        this.sendChunks.clear();
    }

    public void syncInventory() {
        this.sendPacket(new PacketSInvSync(this.inventory.getId(), this.inventory.getSize(), this.inventory.copySlotStacks()));
        for (int i = 0; i < this.inventoryCraft.length; i++) {
            PlayerInventoryCrafting inv = this.inventoryCraft[i];
            this.sendPacket(new PacketSInvSync(inv.getId(), inv.getSize(), inv.copySlotStacks()));
        }
        this.sendPacket(new PacketSInvCarried(new SlotStack(0, this.inventory.carried)));
    }

    public void updatePostTick() {
        if (this.inventory.isDirty()) {
            HashSet<SlotStack> stacks = this.inventory.getUpdate();
            if (stacks != null)
                this.sendPacket(new PacketSInvSyncIncr(this.inventory.id, stacks));
        }
        for (int i = 0; i < this.inventoryCraft.length; i++) {
            PlayerInventoryCrafting inv = this.inventoryCraft[i];
            if (inv.isDirty()) {
                HashSet<SlotStack> stacks = inv.getUpdate();
                if (stacks != null)
                    this.sendPacket(new PacketSInvSyncIncr(inv.id, stacks));
            }
        }
    }

    public WorldServer getWorld() {
        return (WorldServer) this.world;
    }
}
