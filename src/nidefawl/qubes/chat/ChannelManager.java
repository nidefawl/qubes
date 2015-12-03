/**
 * 
 */
package nidefawl.qubes.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.MapMaker;

import nidefawl.qubes.chat.channel.AbstractChannel;
import nidefawl.qubes.chat.channel.GlobalChannel;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.server.GameServer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ChannelManager {
    Map<String, AbstractChannel> channels = new MapMaker().makeMap();
    
    final GlobalChannel global = new GlobalChannel(this, "Global");

    private final GameServer server;
    /**
     * 
     */
    public ChannelManager(GameServer server) {
        registerChannel(global);
        this.server = server;
    }

    /**
     * @return
     */
    public GameServer getServer() {
        return this.server;
    }

    /**
     * @param global2
     */
    private void registerChannel(AbstractChannel channel) {
        this.channels.put(channel.getTag(), channel);
    }

    public AbstractChannel getChannel(String tag) {
        return this.channels.get(tag);
    }
    
    public GlobalChannel getGlobal() {
        return global;
    }

    /**
     * @param player
     */
    public void addUser(ChatUser player) {
        Collection<String> joined = player.getJoinedChannels();
        for (String s : joined) {
            AbstractChannel channel = getChannel(s);
            if (channel != null) {
                channel.addUser(player);
            }
        }
    }

    /**
     * @param player
     */
    public void removeUser(PlayerServer player) {
        Collection<String> joined = player.getJoinedChannels();
        for (String s : joined) {
            AbstractChannel channel = getChannel(s);
            if (channel != null) {
                channel.removeUser(player);
            }
        }
    }

    /**
     * @param player
     * @param channel
     * @param msg
     */
    public void handlePlayerChat(ChatUser player, String ch, String msg) {
        AbstractChannel channel = getChannel(ch);
        if (channel != null && channel.containsUser(player)) {
            channel.onChat(player, msg);
        }
    }
}
