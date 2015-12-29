/**
 * 
 */
package nidefawl.qubes.chat.channel;

import java.util.Collection;

import nidefawl.qubes.chat.ChannelManager;
import nidefawl.qubes.chat.ChatUser;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public abstract class AbstractChannel {

    String name;
    String tag;
    protected ChannelManager channelManager;

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the tag
     */
    public String getTag() {
        return this.tag;
    }

    /**
     * @param channelManager 
     * @param name2
     * 
     */
    public AbstractChannel(ChannelManager channelManager, String tag, String name) {
        this.channelManager = channelManager;
        this.tag = tag;
        this.name = name;
    }

    public abstract Collection<? extends ChatUser> getUsers();

    public abstract void addUser(ChatUser p);

    public abstract void removeUser(ChatUser p);

    /**
     * @param players
     * @return
     */
    public boolean containsUser(ChatUser user) {
        return getUsers().contains(user);
    }

    /**
     * @param player
     * @param msg
     */
    public void onChat(ChatUser player, String msg) {
        String s = this.formatMessage(player, msg);
        broadcastUserMessage(player, s);
    }

    /**
     * @param player
     * @param s
     */
    public void broadcastUserMessage(ChatUser player, String s) {
        for (ChatUser u : getUsers()) {
            u.sendMessage(getTag(), s);
        }
    }

    /**
     * @param s
     */
    public void broadcastMessage(String s) {
        for (ChatUser u : getUsers()) {
            u.sendMessage(getTag(), s);
        }
    }

    /**
     * @param player
     * @param msg
     * @return
     */
    private String formatMessage(ChatUser player, String msg) {
        String name = player.getChatName();
        return String.format("<%s> %s", name, msg);
    }

}
