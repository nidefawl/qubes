/**
 * 
 */
package nidefawl.qubes.chat.channel;

import java.util.ArrayList;
import java.util.Collection;

import nidefawl.qubes.chat.ChannelManager;
import nidefawl.qubes.chat.ChatUser;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Channel extends AbstractChannel {
    ArrayList<ChatUser> players = new ArrayList<>();
    /**
     * 
     */
    public Channel(ChannelManager channelManager, String tag, String name) {
        super(channelManager, tag, name);
    }
    
    /**
     * @return the players
     */
    public Collection<? extends ChatUser> getUsers() {
        return this.players;
    }
    public void addUser(ChatUser p) {
        this.players.add(p);
    }
    public void removeUser(ChatUser p) {
        this.players.remove(p);
    }
}
