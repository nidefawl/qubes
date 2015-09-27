/**
 * 
 */
package nidefawl.qubes.chat.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.Files;

import nidefawl.qubes.font.IStringHistory;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class ChatManager implements IStringHistory {
    ArrayList<ChatLine> line = new ArrayList<>();
    ArrayList<String> hist = new ArrayList<>();

    private boolean saveHistory;

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

    /**
     * 
     */
    public void loadInputHistory() {
        this.saveHistory = false;
        try {
            File f = new File("commandhistory.txt");
            if (f.exists() && f.canRead()) {
                List<String> result = Files.readLines(f, Charsets.UTF_8);
                this.hist.clear();
                this.hist.addAll(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    /**
     * 
     */
    public void saveInputHistory() {
        if (this.saveHistory) {
            this.saveHistory = false;
            File f = new File("commandhistory.txt");
            if (!f.exists() || f.canWrite()) {
                String separator = System.getProperty( "line.separator" );
                CharSink sink = Files.asCharSink(f, Charsets.UTF_8);
                try {
                    sink.writeLines(this.hist, separator);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public void addHistory(String str) {
        this.hist.add(str);
        this.saveHistory = true;
    }

    @Override
    public int getHistorySize() {
        return this.hist.size();
    }

    @Override
    public String getHistory(int idx) {
        return this.hist.get(idx);
    }

    @Override
    public int indexOfHistory(String str) {
        return this.hist.indexOf(str);
    }

    @Override
    public void removeHistory(int index) {
        this.hist.remove(index);
        this.saveHistory = true;
    }

    
    @Override
    public void addHistory(int index, String str) {
        this.hist.add(index, str);
        this.saveHistory = true;
    }
}
