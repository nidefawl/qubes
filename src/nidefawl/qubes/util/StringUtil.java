package nidefawl.qubes.util;

import java.util.UUID;

public class StringUtil {

    public static String[] dropArrIdx(String[] arr, int idx) {
        if (idx < arr.length) {
            String[] newArr = new String[arr.length - 1];
            int targetidx = 0;
            for (int i = 0; i < arr.length; i++) {
                if (i != idx) {
                    newArr[targetidx++] = arr[i];
                }
            }
            return newArr;
        }
        return arr;
    }

    public static long parseLong(String strseed, long l) {
        try {
            return new java.math.BigInteger(strseed, 16).longValue();
        } catch (Exception e) {
            return l;
        }
    }

    public static UUID parseUUID(String string, UUID def) {
        try {
            return UUID.fromString(string);
        } catch (Exception e) {
            return def;
        }
    }

    public static int parseInt(String string, int def) { // TODO: use faster org.craftland.mod.Util.getInt()
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            return def;
        }
    }

    public static String combine(String[] args, int start, int end) {
        String s = "";
        for (int j = start; j < args.length && j < end; j++) {
            if (!s.isEmpty()) {
                s += " ";
            }
            s += args[j];
        }
        return s;
    }
}
