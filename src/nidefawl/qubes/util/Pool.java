package nidefawl.qubes.util;

public abstract class Pool<T extends Poolable> {
    private final T[] array;
    private final boolean[] inUse;
    public Pool(int i) {
        array = (T[]) new Poolable[i];
        inUse = new boolean[i];
    }

    public abstract T create();

    public T get() {
        for (int i = 0; i < array.length; i++) {
            if (!inUse[i]) {
                inUse[i] = true;
                if (array[i] == null) {
                    array[i] = create();
                }
                return array[i];
            }
        }
        return null;
    }

    public void recycle(T task) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == task) {
                array[i].reset();
                inUse[i] = false;
                return;
            }
        }
    }

    public int getFree() {
        int free = 0;
        for (int i = 0; i < array.length; i++) {
            if (!inUse[i]) {
                free++;
            }
        }
        return free;
    }

}
