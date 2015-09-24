/**
 * 
 */
package nidefawl.qubes.chat;

import java.util.Collection;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface ChatUser {

    Collection<String> getJoinedChannels();

    /**
     * @return 
     * 
     */
    String getChatName();

    /**
     * @param s
     */
    void sendMessage(String channel, String message);
    
}
