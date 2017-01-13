package nidefawl.qubes.server;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.MapMaker;

import nidefawl.qubes.chat.channel.GlobalChannel;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.entity.EntityType;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.inventory.slots.SlotStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.nbt.TagReader;
import nidefawl.qubes.player.PlayerData;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;

@SideOnly(value = Side.SERVER)
public class PlayerManager {
    private File directory;
    private Map<String, PlayerServer> players = new MapMaker().makeMap();
    private Map<String, PlayerServer> playersLowerCase = new MapMaker().makeMap();
    private PlayerServer[] serverPlayers = new PlayerServer[0];
    private GameServer server;

    public PlayerManager(GameServer server) {
        this.server = server;
    }

    public void init() {
        this.directory = WorkingEnv.getPlayerData();
    }
    public File getPlayerFile(String name) {
        return new File(this.directory, name+".player");
    }

    public PlayerData loadPlayer(String name) {
        File f = getPlayerFile(name);
        PlayerData data = new PlayerData();
        if (f.exists()) {
            try {
                Tag t = TagReader.readTagFromFile(f);
                data.load((Compound) t);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            data.invStacks.add(new SlotStack(1, new ItemStack(Item.axe)));
            data.invStacks.add(new SlotStack(2, new ItemStack(Item.pickaxe)));
        }
        return data;
    }

    public void savePlayer(String name, PlayerData data) {
        File f = getPlayerFile(name);
        Tag t = data.save();
        try {
            TagReader.writeTagToFile(t, f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized PlayerServer addPlayer(String name) {
        PlayerServer player = (PlayerServer) EntityType.PLAYER_SERVER.newInstance(true);
        player.name = name;
        PlayerData data = loadPlayer(name);
        Iterator<Map.Entry<UUID, Vector3f>> wpos = data.worldPositions.entrySet().iterator();
        while (wpos.hasNext()) {
            Entry<UUID, Vector3f> d = wpos.next();
            WorldServer w = this.server.getWorld(data.world);
            if (w == null) {
                wpos.remove();
            }
        }
        WorldServer world = null;
        if (data.world != null) {
            world = this.server.getWorld(data.world);
        }
        if (world == null) {
            world = this.server.getSpawnWorld();
        }
        data.world = world.getUUID();
        if (data.joinedChannels.isEmpty()) {
            data.joinedChannels.add(GlobalChannel.TAG);
        }
        player.load(data);
        this.players.put(name, player);
        this.playersLowerCase.put(name.toLowerCase(), player);
        this.serverPlayers = this.players.values().toArray(new PlayerServer[this.players.size()]);
        return player;
    }

    public synchronized void removePlayer(PlayerServer p) {
        this.players.remove(p.name);
        this.playersLowerCase.remove(p.name.toLowerCase());
        PlayerData data = (PlayerData) p.save();
        savePlayer(p.name, data);
        this.serverPlayers = this.players.values().toArray(new PlayerServer[this.players.size()]);
    }

    public void savePlayers() {
        Iterator<Map.Entry<String, PlayerServer>> it = this.players.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PlayerServer> entry = it.next();
            try {
                PlayerServer player = entry.getValue();
                PlayerData data = (PlayerData) player.save();
                savePlayer(entry.getKey(), data);   
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public PlayerServer getPlayer(String string) {
        return this.playersLowerCase.get(string.toLowerCase());
    }

    public PlayerServer matchPlayer(String string) {
        Iterator<Map.Entry<String, PlayerServer>> it = this.playersLowerCase.entrySet().iterator();
        String lowerStr = string.toLowerCase();
        PlayerServer match = null;
        while (it.hasNext()) {
            Map.Entry<String, PlayerServer> entry = it.next();
            String pName = entry.getKey();
            if (pName.equals(lowerStr))
                return entry.getValue();
            if (match == null && pName.contains(lowerStr))
                match = entry.getValue();
        }
        return match;
    }

    /**
     * @return
     */
    public Collection<PlayerServer> getPlayers() {
        return players.values();
    }

    public void updateTick() {
        PlayerServer[] players = this.serverPlayers;
        for (int i = 0; i < players.length; i++) {
            try {
                players[i].updatePostTick();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
