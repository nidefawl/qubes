package nidefawl.qubes.util;

public class DumbPool<T> {
    final private static DumbPool<?>[] dumbPools = new DumbPool[8];
    final static int       MAXSIZE = 1024;
    final private Object[] objects = new Object[MAXSIZE];
    private int            nextIdx = 0;

    // need to pass class because of type erasure
    public DumbPool(Class<T> clazz) { 
        for (int i = 0; i < dumbPools.length; i++) {
            if (dumbPools[i] == null) {
                dumbPools[i] = this;
                break;
            }
        }
        try {
            for (int i = 0; i < MAXSIZE; i++) {
                objects[i] = clazz.newInstance();
            }
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    public void poolReset() {
        nextIdx = 0;
    }

    public T get() {
        return (T) objects[nextIdx++];
    }
    
    public static void reset() {
        for (int i = 0; i < dumbPools.length; i++) {
            if (dumbPools[i] != null) {
                dumbPools[i].poolReset();
            } else {
                break;
            }
        }
    }

}
