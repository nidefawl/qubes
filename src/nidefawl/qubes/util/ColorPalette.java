package nidefawl.qubes.util;

import java.util.LinkedHashMap;

public class ColorPalette {
    public LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
    public void set(String s, int rgb) {
        map.put(s, rgb);
    }
    public Integer getOrSetColor(String string, int i) {
        Integer n = map.get(string);
        if (n == null) {
            n = i;
            map.put(string, n);
        }
        return n;
    }
    public Integer getColor(String string, int i) {
        Integer n = map.get(string);
        if (n == null) {
            return i;
        }
        return n;
    }
}