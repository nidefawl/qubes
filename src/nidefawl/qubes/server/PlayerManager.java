package nidefawl.qubes.server;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.MapMaker;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.nbt.TagReader;
import nidefawl.qubes.player.PlayerData;
import nidefawl.qubes.world.WorldServer;

public class PlayerManager {
    private File directory;
    private Map<String, Player> players = new MapMaker().makeMap();
    private Map<String, Player> playersLowerCase = new MapMaker().makeMap();
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

    public Player addPlayer(String name) {
        Player player = new Player();
        player.name = name;
        PlayerData data = loadPlayer(name);
        WorldServer world = null;
        if (data.world != null) {
            world = this.server.getWorld(data.world);
        }
        if (world == null) {
            world = this.server.getSpawnWorld();
        }
        data.world = world.getUUID();
        player.load(data);
        this.players.put(name, player);
        this.playersLowerCase.put(name.toLowerCase(), player);
        world.addPlayer(player);
        return player;
    }

    public void removePlayer(Player p) {
        this.players.remove(p.name);
        this.playersLowerCase.remove(p.name.toLowerCase());
        WorldServer world = (WorldServer) p.world;
        if (world != null) {
            world.removePlayer(p);
        }
        PlayerData data = p.save();
        savePlayer(p.name, data);
    }

    public void savePlayers() {
        Iterator<Map.Entry<String, Player>> it = this.players.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Player> entry = it.next();
            try {
                Player player = entry.getValue();
                PlayerData data = player.save();
                savePlayer(entry.getKey(), data);   
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public Player getPlayer(String string) {
        return this.playersLowerCase.get(string);
    }

    public Player matchPlayer(String string) {
        Iterator<Map.Entry<String, Player>> it = this.playersLowerCase.entrySet().iterator();
        String lowerStr = string.toLowerCase();
        Player match = null;
        while (it.hasNext()) {
            Map.Entry<String, Player> entry = it.next();
            String pName = entry.getKey();
            if (pName.equals(lowerStr))
                return entry.getValue();
            if (match == null && pName.contains(lowerStr))
                match = entry.getValue();
        }
        return match;
    }
}