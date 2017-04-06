package nidefawl.qubes.vulkan;

import java.util.HashMap;

public class VkDebug {
    public static final class ImageAllocDebugInfo {

        private StackTraceElement[] stack;

        public ImageAllocDebugInfo() {
            this.stack = Thread.currentThread().getStackTrace();
        }
        public String toString() {
            String s = "";
            for (int i = 0; i < this.stack.length; i++) {
                s += this.stack[i].toString()+"\n";
            }
            return s;
        }
    }
    static HashMap<Long, ImageAllocDebugInfo> map = new HashMap<>();
    static HashMap<Long, ImageAllocDebugInfo> mapSampler = new HashMap<>();
    public static void registerImage(long image) {
        map.put(image, new ImageAllocDebugInfo());
    }

    public static void registerSampler(long image) {
        mapSampler.put(image, new ImageAllocDebugInfo());
    }
    public static void printStack(long l) {
        ImageAllocDebugInfo n = map.get(l);
        if (n != null) {
            System.err.println(n);
        }
    }

    public static void printStackSampler(long l) {
        ImageAllocDebugInfo n = mapSampler.get(l);
        if (n != null) {
            System.err.println(n);
        }
    }

}
