/**
 * 
 */
package nidefawl.qubes.chat.channel;

import java.util.ArrayList;
import java.util.Collection;

import nidefawl.qubes.chat.ChannelManager;
import nidefawl.qubes.chat.ChatUser;
import nidefawl.qubes.entity.Player;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class GlobalChannel extends AbstractChannel {

    public static final String TAG = "_global";

    /**
     * @param channelManager 
     * @param name
     */
    public GlobalChannel(ChannelManager channelManager, String name) {
        super(channelManager, TAG, name);
    }

    @Override
    public Collection<? extends ChatUser> getUsers() {
        return this.channelManager.getServer().getPlayerManager().getPlayers();
    }

    @Override
    public void addUser(ChatUser p) {
    }

    @Override
    public void removeUser(ChatUser p) {
    }

}
