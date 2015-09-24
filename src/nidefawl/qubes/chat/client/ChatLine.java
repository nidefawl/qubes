/**
 * 
 */
package nidefawl.qubes.chat.client;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ChatLine {

    String rawMessage;
    long receiveTime;
    /**
     * 
     */
    public ChatLine(String msg, long time) {
        this.rawMessage = msg;
        this.receiveTime = time;
    }
    /**
     * @return
     */
    public String getLine() {
        return this.rawMessage;
    }
    /**
     * @return
     */
    public long getTime() {
        return this.receiveTime;
    }
}
