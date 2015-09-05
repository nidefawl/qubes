package nidefawl.qubes.util;

public class GameContext {
    static Thread mainThread;

    public static void setMainThread(Thread thread) {
        mainThread = thread;
    }
    
    
    /**
     * Do not check against at boottime
     * @return
     */
    public static Thread getMainThread() {
        return mainThread;
    }

}
