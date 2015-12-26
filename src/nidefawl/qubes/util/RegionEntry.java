package nidefawl.qubes.util;

import java.util.Set;

public interface RegionEntry {
    public Set<Integer> getKeys();
    public void addKey(int key);
    public void removeKey(int key);
    public int getMinX();
    public int getMinZ();
    public int getMaxX();
    public int getMaxZ();
}
