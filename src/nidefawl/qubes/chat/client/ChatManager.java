/**
 * 
 */
package nidefawl.qubes.chat.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ChatManager {
    ArrayList<ChatLine> line = new ArrayList<>();

    final static ChatManager instance = new ChatManager();

    private static final long CHAT_LINE_MAX_AGE = 3333;

    ChatManager() {
    }

    /**
     * @return the instance
     */
    public static ChatManager getInstance() {
        return instance;
    }

    public void sendMessage(String msg) {
        line.add(0, new ChatLine(msg, System.currentTimeMillis()));
    }

    public void receiveMessage(String channel, String msg) {
        line.add(0, new ChatLine(msg, System.currentTimeMillis()));
    }

    /**
     * @return
     */
    public List<ChatLine> getLines() {
        return this.line;
    }

    /**
     * @return
     */
    public int getNumNewLines() {
        int a = 0;
        for (; a < line.size(); a++) {
            if (line.get(a).getTime() < System.currentTimeMillis() - CHAT_LINE_MAX_AGE) {
                break;
            }
        }
        return a;
    }

    public void syncChannels(ArrayList<String> list) {
    }
}
