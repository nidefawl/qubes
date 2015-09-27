/**
 * 
 */
package nidefawl.qubes.font;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public interface IStringHistory {

    public void addHistory(String str);

    public int getHistorySize();

    public String getHistory(int idx);

    public int indexOfHistory(String str);

    public void removeHistory(int index);

    public void addHistory(int index, String str);
}
